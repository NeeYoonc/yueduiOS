package io.legado.shared.book

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchCoordinatorTest {
    @Test
    fun searchesEnabledSourcesAndKeepsPerSourceErrors() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return when (request.url) {
                    "https://one.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = "name=One\nbookUrl=/book/1"
                    )
                    "https://bad.test/search?q=metal&page=1" -> error("network down")
                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val coordinator = SearchCoordinator(LegadoSharedClient(fetcher), store)
        val sources = listOf(
            SharedBookSource(
                bookSourceUrl = "https://one.test",
                bookSourceName = "One",
                searchUrl = "https://one.test/search?q={{key}}&page={{page}}"
            ),
            SharedBookSource(
                bookSourceUrl = "https://bad.test",
                bookSourceName = "Bad",
                searchUrl = "https://bad.test/search?q={{key}}&page={{page}}"
            ),
            SharedBookSource(
                bookSourceUrl = "https://disabled.test",
                bookSourceName = "Disabled",
                enabled = false,
                searchUrl = "https://disabled.test/search?q={{key}}"
            )
        )

        val result = coordinator.search(sources, key = "metal", page = 1, nowMillis = 99L)

        assertEquals(listOf("One"), result.books.map { it.name })
        assertEquals("https://one.test/book/1", result.books.single().bookUrl)
        assertEquals(listOf("Bad"), result.errors.map { it.source.bookSourceName })
        assertEquals("network down", result.errors.single().message)
        assertEquals("metal", store.loadDataSnapshot().searchKeywords.single().word)
        assertEquals(1, store.loadDataSnapshot().searchKeywords.single().usage)
        assertEquals(99L, store.loadDataSnapshot().searchKeywords.single().lastUseTime)
    }

    @Test
    fun incrementsExistingSearchKeywordUsage() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return SharedHttpResponse(finalUrl = request.url, statusCode = 200, body = "")
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val coordinator = SearchCoordinator(LegadoSharedClient(fetcher), store)
        val source = SharedBookSource(
            bookSourceUrl = "https://one.test",
            bookSourceName = "One",
            searchUrl = "https://one.test/search?q={{key}}"
        )

        coordinator.search(listOf(source), key = "metal", nowMillis = 1L)
        coordinator.search(listOf(source), key = "metal", nowMillis = 2L)

        val keyword = store.loadDataSnapshot().searchKeywords.single()
        assertEquals(2, keyword.usage)
        assertEquals(2L, keyword.lastUseTime)
    }

    @Test
    fun listsDeletesAndClearsSearchKeywords() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return SharedHttpResponse(finalUrl = request.url, statusCode = 200, body = "")
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val coordinator = SearchCoordinator(LegadoSharedClient(fetcher), store)
        val source = SharedBookSource(
            bookSourceUrl = "https://one.test",
            bookSourceName = "One",
            searchUrl = "https://one.test/search?q={{key}}"
        )

        coordinator.search(listOf(source), key = "old", nowMillis = 1L)
        coordinator.search(listOf(source), key = "new", nowMillis = 3L)
        coordinator.search(listOf(source), key = "old", nowMillis = 4L)

        assertEquals(listOf("old", "new"), coordinator.listKeywords().map { it.word })
        assertEquals(2, coordinator.listKeywords().first().usage)

        coordinator.deleteKeyword("old")
        assertEquals(listOf("new"), coordinator.listKeywords().map { it.word })

        coordinator.clearKeywords()
        assertEquals(emptyList(), coordinator.listKeywords())
    }

    @Test
    fun recordsKeywordWithoutRunningSearch() {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                error("No network expected")
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val coordinator = SearchCoordinator(LegadoSharedClient(fetcher), store)

        coordinator.recordKeyword("manual", nowMillis = 11L)

        val keyword = coordinator.listKeywords().single()
        assertEquals("manual", keyword.word)
        assertEquals(1, keyword.usage)
        assertEquals(11L, keyword.lastUseTime)
    }

    @Test
    fun storesSearchBooksAndListsChangeSourceCandidates() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return when (request.url) {
                    "https://one.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = "name=Metal Story\nauthor=Author\nbookUrl=/book/1"
                    )
                    "https://two.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = "name=Metal Story\nauthor=Author\nbookUrl=/book/2"
                    )
                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val coordinator = SearchCoordinator(LegadoSharedClient(fetcher), store)
        val sources = listOf(
            SharedBookSource(
                bookSourceUrl = "https://one.test",
                bookSourceName = "One",
                customOrder = 2,
                searchUrl = "https://one.test/search?q={{key}}&page={{page}}"
            ),
            SharedBookSource(
                bookSourceUrl = "https://two.test",
                bookSourceName = "Two",
                customOrder = 1,
                searchUrl = "https://two.test/search?q={{key}}&page={{page}}"
            )
        )

        coordinator.search(sources, key = "metal", page = 1, nowMillis = 88L)

        assertEquals(
            listOf("https://two.test/book/2", "https://one.test/book/1"),
            coordinator.listSearchBooks().map { it.bookUrl }
        )
        assertEquals(listOf("Two", "One"), coordinator.listSearchBooks().map { it.originName })
        assertEquals(listOf(88L, 88L), coordinator.listSearchBooks().map { it.time })

        val candidates = coordinator.listChangeSourceCandidates(
            book = SharedBook(name = "Metal Story", author = "Author", bookUrl = "https://one.test/book/1"),
            sources = sources
        )

        assertEquals(listOf("Two"), candidates.map { it.originName })
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

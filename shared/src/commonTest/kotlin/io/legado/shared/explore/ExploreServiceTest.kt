package io.legado.shared.explore

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ExploreServiceTest {
    @Test
    fun listsSourcesAndParsesPlainExploreKinds() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val service = ExploreService(LegadoSharedClient(NoNetworkFetcher), store)
        store.saveBookSources(
            listOf(
                SharedBookSource(
                    bookSourceUrl = "https://one.test",
                    bookSourceName = "One",
                    exploreUrl = "Rank::/rank?page={{page}}\nLatest::https://one.test/latest"
                ),
                SharedBookSource(
                    bookSourceUrl = "https://disabled.test",
                    bookSourceName = "Disabled",
                    enabledExplore = false,
                    exploreUrl = "Hidden::/hidden"
                )
            )
        )

        val sources = service.listExploreSources()
        val kinds = service.listKinds(sources.single())

        assertEquals(listOf("One"), sources.map { it.bookSourceName })
        assertEquals(listOf("Rank", "Latest"), kinds.map { it.title })
        assertEquals("/rank?page={{page}}", kinds.first().url)
    }

    @Test
    fun parsesJsonExploreKinds() {
        val service = ExploreService(LegadoSharedClient(NoNetworkFetcher), SharedLibraryStore(InMemoryCacheStore()))
        val source = SharedBookSource(
            bookSourceUrl = "https://one.test",
            bookSourceName = "One",
            exploreUrl = """[{"title":"Rank","url":"/rank","type":"url"},{"title":"Genre","type":"select","chars":["A","B"],"default":"A"}]"""
        )

        val kinds = service.listKinds(source)

        assertEquals("Rank", kinds[0].title)
        assertEquals("select", kinds[1].type)
        assertEquals(listOf("A", "B"), kinds[1].chars)
    }

    @Test
    fun loadsExplorePageWithExploreRule() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://one.test/rank?page=2", request.url)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = "book|Metal|/book/1"
                )
            }
        }
        val service = ExploreService(LegadoSharedClient(fetcher), SharedLibraryStore(InMemoryCacheStore()))
        val source = SharedBookSource(
            bookSourceUrl = "https://one.test/root/index.html",
            bookSourceName = "One",
            exploreUrl = "Rank::/rank?page={{page}}",
            ruleExplore = SharedSearchRule(
                bookList = """book\|([^|]+)\|([^\n]+)""",
                name = "${'$'}1",
                bookUrl = "${'$'}2"
            )
        )
        val kind = service.listKinds(source).single()

        val page = service.loadExplore(source, kind, page = 2)

        assertEquals(listOf("Metal"), page.books.map { it.name })
        assertEquals("https://one.test/book/1", page.books.single().bookUrl)
    }

    private object NoNetworkFetcher : HttpFetcher {
        override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
            error("No network expected")
        }
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

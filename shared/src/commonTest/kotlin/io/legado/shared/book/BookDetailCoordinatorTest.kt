package io.legado.shared.book

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedTocRule
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BookDetailCoordinatorTest {
    @Test
    fun opensSearchBookSavesBookAndChapterList() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                return when (request.url) {
                    "https://source.test/book/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<h1>Detail Name</h1><a class="toc" href="/book/1/toc.html">toc</a>"""
                    )
                    "https://source.test/book/1/toc.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            <a class="chapter" href="/book/1/1.html">Chapter 1</a>
                            <a class="chapter" href="/book/1/2.html">Chapter 2</a>
                        """.trimIndent()
                    )
                    else -> error("Unexpected URL ${request.url}")
                }
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val bookshelf = BookshelfService(store)
        val coordinator = BookDetailCoordinator(LegadoSharedClient(fetcher), bookshelf, store)
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleBookInfo = SharedBookInfoRule(
                name = """<h1>([^<]+)</h1>""",
                tocUrl = """<a class="toc" href="([^"]+)">"""
            ),
            ruleToc = SharedTocRule(
                chapterList = """<a class="chapter" href="([^"]+)">([^<]+)</a>""",
                chapterUrl = "$1",
                chapterName = "$2"
            )
        )
        val searchBook = SharedSearchBook(
            name = "Search Name",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            origin = "https://source.test"
        )

        val result = coordinator.openSearchBook(source, searchBook, nowMillis = 1234L)

        assertEquals(
            listOf("https://source.test/book/1", "https://source.test/book/1/toc.html"),
            requestedUrls
        )
        assertEquals("Detail Name", result.book.name)
        assertEquals(2, result.chapters.size)
        assertEquals("Chapter 2", result.book.latestChapterTitle)
        assertEquals(2, bookshelf.listBooks().single().totalChapterNum)
        assertEquals(result.chapters, store.loadBookChapters(result.book))
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

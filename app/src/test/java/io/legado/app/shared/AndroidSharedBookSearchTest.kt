package io.legado.app.shared

import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.SearchRule
import io.legado.shared.service.RegexSearchResultParser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidSharedBookSearchTest {

    @Test
    fun searchesAndroidBookSourceThroughSharedServiceAndAndroidHttp() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals(
                    "https://source.test/search?q=metal%20max&page=3",
                    request.url.toString()
                )
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """
                        name=Iron Road
                        author=Tester
                        url=https://source.test/book/1
                        """.trimIndent().toResponseBody("text/plain".toMediaType())
                    )
                    .build()
            }
            .build()
        val search = AndroidSharedBookSearch(AndroidHttpFetcher(client))
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            bookSourceGroup = "Group",
            searchUrl = "https://source.test/search?q={{key}}&page={{page}}"
        )

        val result = search.search(source, key = "metal max", page = 3)

        assertEquals("https://source.test", result.source.bookSourceUrl)
        assertEquals("Source", result.source.bookSourceName)
        assertEquals("Group", result.source.bookSourceGroup)
        assertEquals("Iron Road", result.debugBookName)
        assertEquals(200, result.response.statusCode)
        assertEquals(1, result.books.size)
        assertEquals("Iron Road", result.books.single().name)
        assertEquals("Tester", result.books.single().author)
        assertEquals("https://source.test/book/1", result.books.single().bookUrl)
        assertEquals("https://source.test", result.books.single().origin)
    }

    @Test
    fun rejectsAndroidBookSourceWithoutSearchUrl() {
        val search = AndroidSharedBookSearch(
            AndroidHttpFetcher(OkHttpClient.Builder().build())
        )
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                search.search(source, key = "metal")
            }
        }

        assertEquals("Book source Source has no searchUrl", error.message)
    }

    @Test
    fun canSearchWithInjectedRegexParser() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """<li data-url="/book/1"><span>Regex Road</span><em>Tester</em></li>"""
                            .toResponseBody("text/html".toMediaType())
                    )
                    .build()
            }
            .build()
        val search = AndroidSharedBookSearch(
            AndroidHttpFetcher(client),
            RegexSearchResultParser
        )
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            searchUrl = "https://source.test/search?q={{key}}",
            ruleSearch = SearchRule(
                bookList = """<li data-url="([^"]+)"><span>([^<]+)</span><em>([^<]+)</em></li>""",
                bookUrl = "$1",
                name = "$2",
                author = "$3"
            )
        )

        val result = search.search(source, key = "regex")

        assertEquals(1, result.books.size)
        assertEquals("Regex Road", result.books.single().name)
        assertEquals("Tester", result.books.single().author)
        assertEquals("/book/1", result.books.single().bookUrl)
    }
}

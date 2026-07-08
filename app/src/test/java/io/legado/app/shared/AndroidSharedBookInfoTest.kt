package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.shared.service.RegexBookInfoParser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidSharedBookInfoTest {

    @Test
    fun getsAndroidBookInfoThroughSharedServiceAndAndroidHttp() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals("https://source.test/book/1", request.url.toString())
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """
                        <h1>Metal Detail</h1>
                        <span class="author">Tester</span>
                        <a class="toc" href="https://source.test/book/1/catalog">toc</a>
                        """.trimIndent().toResponseBody("text/html".toMediaType())
                    )
                    .build()
            }
            .build()
        val info = AndroidSharedBookInfo(
            AndroidHttpFetcher(client),
            RegexBookInfoParser
        )
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleBookInfo = BookInfoRule(
                name = """<h1>([^<]+)</h1>""",
                author = """<span class="author">([^<]+)</span>""",
                tocUrl = """<a class="toc" href="([^"]+)">"""
            )
        )
        val book = Book(
            name = "Search Name",
            author = "",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1",
            origin = "https://source.test"
        )

        val result = info.getBookInfo(source, book)

        assertEquals("Search Name", result.inputBook.name)
        assertEquals("Metal Detail", result.book.name)
        assertEquals("Tester", result.book.author)
        assertEquals("https://source.test/book/1/catalog", result.book.tocUrl)
    }
}

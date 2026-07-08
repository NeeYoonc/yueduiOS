package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.shared.service.RegexChapterContentParser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidSharedBookContentTest {

    @Test
    fun getsAndroidBookChapterContentThroughSharedServiceAndAndroidHttp() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val body = when (request.url.toString()) {
                    "https://source.test/chapter/1.html" -> """
                        <main id="content">
                          First paragraph.
                          Second paragraph.
                        </main>
                        <a class="next" href="/chapter/1-2.html">next</a>
                    """.trimIndent()

                    "https://source.test/chapter/1-2.html" -> """
                        <main id="content">
                          Third paragraph.
                        </main>
                    """.trimIndent()

                    else -> error("Unexpected request URL: ${request.url}")
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("text/html".toMediaType()))
                    .build()
            }
            .build()
        val content = AndroidSharedBookContent(
            AndroidHttpFetcher(client),
            RegexChapterContentParser
        )
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleContent = ContentRule(
                content = """<main id="content">([\s\S]*?)</main>""",
                nextContentUrl = """<a class="next" href="([^"]+)">"""
            )
        )
        val book = Book(
            name = "Metal Story",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            origin = "https://source.test"
        )
        val chapter = BookChapter(
            bookUrl = book.bookUrl,
            title = "Chapter 1",
            url = "https://source.test/chapter/1.html",
            index = 0
        )

        val result = content.getContent(source, book, chapter)

        assertEquals("https://source.test/chapter/1.html", result.response.finalUrl)
        assertEquals("First paragraph.\nSecond paragraph.\nThird paragraph.", result.content.content)
        assertEquals(
            listOf("https://source.test/chapter/1-2.html"),
            result.content.nextContentUrls
        )
        assertEquals("Chapter 1", result.chapter.title)
        assertEquals("Metal Story", result.book.name)
    }
}

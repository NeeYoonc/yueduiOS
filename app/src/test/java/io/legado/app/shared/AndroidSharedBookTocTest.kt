package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.TocRule
import io.legado.shared.service.RegexChapterListParser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSharedBookTocTest {

    @Test
    fun getsAndroidBookTocThroughSharedServiceAndAndroidHttp() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val body = when (request.url.toString()) {
                    "https://source.test/book/1/catalog" -> """
                        <li data-vip="false" data-pay="false">
                          <a href="/chapter/1.html">Chapter 1</a>
                          <span>2026-07-08</span>
                          <em>false</em>
                        </li>
                        <li data-vip="true" data-pay="true">
                          <a href="/chapter/2.html">Chapter 2</a>
                          <span>2026-07-09</span>
                          <em>true</em>
                        </li>
                        <a class="next" href="catalog-2.html">next</a>
                    """.trimIndent()

                    "https://source.test/book/1/catalog-2.html" -> """
                        <li data-vip="false" data-pay="false">
                          <a href="/chapter/3.html">Chapter 3</a>
                          <span>2026-07-10</span>
                          <em>false</em>
                        </li>
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
        val toc = AndroidSharedBookToc(
            AndroidHttpFetcher(client),
            RegexChapterListParser
        )
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleToc = TocRule(
                chapterList = """<li data-vip="([^"]+)" data-pay="([^"]+)">\s*<a href="([^"]+)">([^<]+)</a>\s*<span>([^<]+)</span>\s*<em>([^<]+)</em>\s*</li>""",
                chapterName = "$4",
                chapterUrl = "$3",
                updateTime = "$5",
                isVip = "$1",
                isPay = "$2",
                isVolume = "$6",
                nextTocUrl = """<a class="next" href="([^"]+)">"""
            )
        )
        val book = Book(
            name = "Metal Story",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1/catalog",
            origin = "https://source.test"
        )

        val result = toc.getChapterList(source, book)

        assertEquals("https://source.test/book/1/catalog", result.response.finalUrl)
        assertEquals(3, result.chapters.size)
        assertEquals("Chapter 1", result.chapters[0].title)
        assertEquals("https://source.test/chapter/1.html", result.chapters[0].url)
        assertEquals(0, result.chapters[0].index)
        assertEquals("2026-07-08", result.chapters[0].tag)
        assertFalse(result.chapters[0].isVip)
        assertFalse(result.chapters[0].isPay)
        assertFalse(result.chapters[0].isVolume)
        assertEquals("Chapter 2", result.chapters[1].title)
        assertEquals("https://source.test/chapter/2.html", result.chapters[1].url)
        assertEquals(1, result.chapters[1].index)
        assertTrue(result.chapters[1].isVip)
        assertTrue(result.chapters[1].isPay)
        assertTrue(result.chapters[1].isVolume)
        assertEquals("Chapter 3", result.chapters[2].title)
        assertEquals("https://source.test/chapter/3.html", result.chapters[2].url)
        assertEquals(2, result.chapters[2].index)
        assertEquals("2026-07-10", result.chapters[2].tag)
    }
}

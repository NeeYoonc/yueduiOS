package io.legado.app.shared

import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.shared.service.RegexBookInfoParser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidSharedReadingFlowTest {

    @Test
    fun opensFirstSearchResultThroughAndroidSharedFlow() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requestedUrls.add(request.url.toString())
                val body = when (request.url.toString()) {
                    "https://source.test/search?q=metal%20max&page=2" -> """
                        name=Metal Story
                        author=Tester
                        bookUrl=https://source.test/book/1
                    """.trimIndent()

                    "https://source.test/book/1" -> """
                        <h1>Metal Story Detail</h1>
                        <a class="toc" href="catalog/index.html">toc</a>
                    """.trimIndent()

                    "https://source.test/book/catalog/index.html" -> """
                        <li><a href="chapter/1.html">Chapter 1</a></li>
                        <li><a href="chapter/2.html">Chapter 2</a></li>
                    """.trimIndent()

                    "https://source.test/book/catalog/chapter/1.html" -> """
                        <main id="content">
                          First paragraph.
                          Second paragraph.
                        </main>
                    """.trimIndent()

                    else -> error("Unexpected request URL: ${request.url}")
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            searchUrl = "https://source.test/search?q={{key}}&page={{page}}",
            ruleBookInfo = BookInfoRule(
                name = """<h1>([^<]+)</h1>""",
                tocUrl = """<a class="toc" href="([^"]+)">"""
            ),
            ruleToc = TocRule(
                chapterList = """<li><a href="([^"]+)">([^<]+)</a></li>""",
                chapterUrl = "$1",
                chapterName = "$2"
            ),
            ruleContent = ContentRule(
                content = """<main id="content">([\s\S]*?)</main>"""
            )
        )
        val flow = AndroidSharedReadingFlow(
            httpFetcher = AndroidHttpFetcher(client),
            bookInfoParser = RegexBookInfoParser
        )

        val result = flow.openFirstSearchResult(source, "metal max", page = 2)

        assertEquals(
            listOf(
                "https://source.test/search?q=metal%20max&page=2",
                "https://source.test/book/1",
                "https://source.test/book/catalog/index.html",
                "https://source.test/book/catalog/chapter/1.html"
            ),
            requestedUrls
        )
        assertEquals("Metal Story Detail", result.selectedBook?.name)
        assertEquals("Metal Story Detail", result.bookInfo?.book?.name)
        assertEquals("Chapter 1", result.selectedChapter?.title)
        val content = result.content
        assertNotNull(content)
        assertEquals("First paragraph.\nSecond paragraph.", content!!.content.content)
    }
}

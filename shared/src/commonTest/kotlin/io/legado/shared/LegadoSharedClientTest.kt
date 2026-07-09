package io.legado.shared

import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.CookieStorePort
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LegadoSharedClientTest {
    @Test
    fun importsSourcesAndOpensFirstSearchResultThroughFacade() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val requestedHeaders = linkedMapOf<String, Map<String, String>>()
        val cookies = InMemoryCookieStore().apply {
            putCookie("https://client.test", "sid=before")
        }
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                requestedHeaders[request.url] = request.headers
                return when (request.url) {
                    "https://client.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        headers = mapOf("Set-Cookie" to "session=after; Path=/; HttpOnly"),
                        body = """
                            name=Metal Story
                            author=Tester
                            bookUrl=https://client.test/book/1
                        """.trimIndent()
                    )

                    "https://client.test/book/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<a class="toc" href="/book/1/toc.html">toc</a>"""
                    )

                    "https://client.test/book/1/toc.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<a class="chapter" href="/book/1/chapter/1.html">Chapter 1</a>"""
                    )

                    "https://client.test/book/1/chapter/1.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<main>First line.</main>"""
                    )

                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val client = LegadoSharedClient(fetcher, cookieStore = cookies)
        val sources = client.importBookSources(
            """
            {
              "bookSourceUrl": "https://client.test",
              "bookSourceName": "Client",
              "header": "{\"User-Agent\":\"ClientUA\",\"X-Source\":\"shared\"}",
              "searchUrl": "https://client.test/search?q={{key}}&page={{page}}",
              "ruleBookInfo": {
                "tocUrl": "<a class=\"toc\" href=\"([^\"]+)\">"
              },
              "ruleToc": {
                "chapterList": "<a class=\"chapter\" href=\"([^\"]+)\">([^<]+)</a>",
                "chapterUrl": "${'$'}1",
                "chapterName": "${'$'}2"
              },
              "ruleContent": {
                "content": "<main>([\\s\\S]*?)</main>"
              }
            }
            """.trimIndent()
        )

        val result = client.openFirstSearchResult(sources.single(), "metal")

        assertEquals(
            listOf(
                "https://client.test/search?q=metal&page=1",
                "https://client.test/book/1",
                "https://client.test/book/1/toc.html",
                "https://client.test/book/1/chapter/1.html"
            ),
            requestedUrls
        )
        assertEquals("ClientUA", requestedHeaders.getValue("https://client.test/search?q=metal&page=1").value("User-Agent"))
        assertEquals("shared", requestedHeaders.getValue("https://client.test/search?q=metal&page=1").value("X-Source"))
        assertEquals("sid=before", requestedHeaders.getValue("https://client.test/search?q=metal&page=1").value("Cookie"))
        assertEquals("sid=before; session=after", requestedHeaders.getValue("https://client.test/book/1").value("Cookie"))
        assertEquals("Metal Story", result.selectedBook?.name)
        assertEquals("Chapter 1", result.selectedChapter?.title)
        assertEquals("First line.", result.content?.content?.content)
    }

    private fun Map<String, String>.value(name: String): String? {
        return entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private class InMemoryCookieStore : CookieStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getCookie(url: String): String? = values[url]

        override fun putCookie(url: String, cookie: String) {
            values[url] = cookie
        }
    }
}

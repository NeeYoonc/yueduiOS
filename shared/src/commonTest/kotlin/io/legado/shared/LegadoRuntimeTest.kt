package io.legado.shared

import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LegadoRuntimeTest {
    @Test
    fun importsSourcesOpensFirstResultAndCachesContent() = runBlocking {
        val cache = InMemoryCacheStore()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return when (request.url) {
                    "https://runtime.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            name=Metal Runtime
                            author=Tester
                            bookUrl=https://runtime.test/book/1
                        """.trimIndent()
                    )

                    "https://runtime.test/book/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<a class="toc" href="/book/1/toc.html">toc</a>"""
                    )

                    "https://runtime.test/book/1/toc.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<a class="chapter" href="/book/1/chapter/1.html">Chapter 1</a>"""
                    )

                    "https://runtime.test/book/1/chapter/1.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<main>Cached line.</main>"""
                    )

                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val runtime = LegadoRuntime(fetcher, cache)

        val sources = runtime.importAndSaveBookSources(
            """
            {
              "bookSourceUrl": "https://runtime.test",
              "bookSourceName": "Runtime",
              "searchUrl": "https://runtime.test/search?q={{key}}&page={{page}}",
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
        val result = runtime.openFirstSearchResult(sources.single(), "metal")

        assertEquals(sources, runtime.loadBookSources())
        assertEquals("Metal Runtime", result.selectedBook?.name)
        assertEquals("Cached line.", result.content?.content?.content)
        assertEquals("Metal Runtime", runtime.loadBooks().single().name)
        assertEquals("Chapter 1", runtime.loadBooks().single().durChapterTitle)
        assertEquals(
            result.content?.content,
            runtime.libraryStore.loadChapterContent(
                result.selectedBook!!,
                result.selectedChapter!!
            )
        )
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

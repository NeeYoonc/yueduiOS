package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedContentRule
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.model.SharedTocRule
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ReadingFlowServiceTest {
    @Test
    fun opensFirstSearchResultThroughSearchTocAndContentServices() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                return when (request.url) {
                    "https://source.test/search?q=metal%20max&page=2" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            name=Metal Story
                            author=Tester
                            bookUrl=https://source.test/book/1
                        """.trimIndent()
                    )

                    "https://source.test/book/1" -> SharedHttpResponse(
                        finalUrl = "https://source.test/book/1/detail.html",
                        statusCode = 200,
                        body = """
                            <h1>Metal Story Detail</h1>
                            <a class="toc" href="catalog/index.html">toc</a>
                        """.trimIndent()
                    )

                    "https://source.test/book/1/catalog/index.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            <li><a href="chapter/1.html">Chapter 1</a></li>
                            <li><a href="chapter/2.html">Chapter 2</a></li>
                        """.trimIndent()
                    )

                    "https://source.test/book/1/catalog/chapter/1.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            <main id="content">
                              First paragraph.
                              Second paragraph.
                            </main>
                        """.trimIndent()
                    )

                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            searchUrl = "https://source.test/search?q={{key}}&page={{page}}",
            ruleBookInfo = SharedBookInfoRule(
                name = """<h1>([^<]+)</h1>""",
                tocUrl = """<a class="toc" href="([^"]+)">"""
            ),
            ruleToc = SharedTocRule(
                chapterList = """<li><a href="([^"]+)">([^<]+)</a></li>""",
                chapterUrl = "$1",
                chapterName = "$2"
            ),
            ruleContent = SharedContentRule(
                content = """<main id="content">([\s\S]*?)</main>"""
            )
        )

        val result = ReadingFlowService(fetcher).openFirstSearchResult(
            source = source,
            key = "metal max",
            page = 2
        )

        assertEquals(
            listOf(
                "https://source.test/search?q=metal%20max&page=2",
                "https://source.test/book/1",
                "https://source.test/book/1/catalog/index.html",
                "https://source.test/book/1/catalog/chapter/1.html"
            ),
            requestedUrls
        )
        assertEquals(1, result.search.books.size)
        assertEquals("Metal Story Detail", result.selectedBook?.name)
        assertEquals("https://source.test/book/1", result.selectedBook?.bookUrl)
        assertEquals("https://source.test/book/1/catalog/index.html", result.selectedBook?.tocUrl)
        assertEquals("Metal Story Detail", result.bookInfo?.book?.name)
        val chapterList = assertNotNull(result.chapterList)
        assertEquals(2, chapterList.chapters.size)
        assertEquals("Chapter 1", result.selectedChapter?.title)
        val content = assertNotNull(result.content)
        assertEquals("First paragraph.\nSecond paragraph.", content.content.content)
    }

    @Test
    fun opensFirstJsonSearchResultWithDefaultParsers() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                return when (request.url) {
                    "https://api.example.test/search?q=json%20story&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            {"content":{"content":[
                              {"title":"JSON Story","author":"API Author","url":"https://api.example.test/book/1"}
                            ]}}
                        """.trimIndent()
                    )

                    "https://api.example.test/book/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            {"content":{
                              "title":"JSON Story Detail",
                              "authorName":"API Author",
                              "catalogUrl":"/book/1/catalog"
                            }}
                        """.trimIndent()
                    )

                    "https://api.example.test/book/1/catalog" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            {"content":{"content":[
                              {"chapterTitle":"JSON Chapter 1","url":"/book/1/chapter/1"}
                            ]}}
                        """.trimIndent()
                    )

                    "https://api.example.test/book/1/chapter/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            {"content":{"text":"First JSON line.\nSecond JSON line."}}
                        """.trimIndent()
                    )

                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val source = SharedBookSource(
            bookSourceUrl = "https://api.example.test",
            bookSourceName = "JSON Source",
            searchUrl = "https://api.example.test/search?q={{key}}&page={{page}}",
            ruleSearch = SharedSearchRule(
                bookList = "$.content.content",
                name = "$.title",
                author = "$.author",
                bookUrl = "$.url"
            ),
            ruleBookInfo = SharedBookInfoRule(
                name = "$.content.title",
                author = "$.content.authorName",
                tocUrl = "$.content.catalogUrl"
            ),
            ruleToc = SharedTocRule(
                chapterList = "$.content.content",
                chapterName = "$.chapterTitle",
                chapterUrl = "$.url"
            ),
            ruleContent = SharedContentRule(
                content = "$.content.text"
            )
        )

        val result = ReadingFlowService(fetcher).openFirstSearchResult(
            source = source,
            key = "json story"
        )

        assertEquals(
            listOf(
                "https://api.example.test/search?q=json%20story&page=1",
                "https://api.example.test/book/1",
                "https://api.example.test/book/1/catalog",
                "https://api.example.test/book/1/chapter/1"
            ),
            requestedUrls
        )
        assertEquals("JSON Story Detail", result.selectedBook?.name)
        assertEquals("JSON Chapter 1", result.selectedChapter?.title)
        val content = assertNotNull(result.content)
        assertEquals("First JSON line.\nSecond JSON line.", content.content.content)
    }

    @Test
    fun returnsSearchOnlyWhenThereAreNoResults() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = ""
                )
            }
        }
        val source = SharedBookSource(
            bookSourceUrl = "https://empty.test",
            bookSourceName = "Empty",
            searchUrl = "https://empty.test/search?q={{key}}"
        )

        val result = ReadingFlowService(fetcher).openFirstSearchResult(source, "none")

        assertEquals(emptyList(), result.search.books)
        assertEquals(null, result.selectedBook)
        assertEquals(null, result.chapterList)
        assertEquals(null, result.selectedChapter)
        assertEquals(null, result.content)
    }
}

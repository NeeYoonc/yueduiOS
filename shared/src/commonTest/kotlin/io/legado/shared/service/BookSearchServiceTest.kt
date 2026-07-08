package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpMethod
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BookSearchServiceTest {
    @Test
    fun fetchesSearchPageWithKeywordAndPage() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://example.test/search?q=sword&page=2", request.url)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = """
                        name=Metal Sword
                        author=Tester
                        kind=Adventure
                        lastChapter=Chapter 9
                        intro=First intro
                        coverUrl=https://example.test/cover.jpg
                        url=https://example.test/book/1

                        name=Iron Sword
                        author=Second
                        bookUrl=https://example.test/book/2
                    """.trimIndent()
                )
            }
        }
        val service = BookSearchService(fetcher)
        val source = SharedBookSource(
            bookSourceUrl = "https://example.test",
            bookSourceName = "Example",
            searchUrl = "https://example.test/search?q={{key}}&page={{page}}"
        )

        val result = service.search(source, key = "sword", page = 2)

        assertEquals("https://example.test", result.source.bookSourceUrl)
        assertEquals("https://example.test/search?q=sword&page=2", result.response.finalUrl)
        assertEquals("Metal Sword", result.debugBookName)
        assertEquals(2, result.books.size)
        assertEquals("Metal Sword", result.books[0].name)
        assertEquals("Tester", result.books[0].author)
        assertEquals("Adventure", result.books[0].kind)
        assertEquals("Chapter 9", result.books[0].latestChapterTitle)
        assertEquals("First intro", result.books[0].intro)
        assertEquals("https://example.test/cover.jpg", result.books[0].coverUrl)
        assertEquals("https://example.test/book/1", result.books[0].bookUrl)
        assertEquals("https://example.test", result.books[0].origin)
        assertEquals("Iron Sword", result.books[1].name)
        assertEquals("https://example.test/book/2", result.books[1].bookUrl)
    }

    @Test
    fun delegatesResponseBodyToInjectedSearchResultParser() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = "raw parser input"
                )
            }
        }
        val parser = object : SearchResultParser {
            override fun parse(
                source: SharedBookSource,
                body: String
            ): List<SharedSearchBook> {
                assertEquals("https://parser.test", source.bookSourceUrl)
                assertEquals("raw parser input", body)
                return listOf(
                    SharedSearchBook(
                        name = "Parsed By Injected Parser",
                        bookUrl = "https://parser.test/book/1",
                        origin = source.bookSourceUrl
                    )
                )
            }
        }
        val service = BookSearchService(
            httpFetcher = fetcher,
            searchResultParser = parser
        )
        val source = SharedBookSource(
            bookSourceUrl = "https://parser.test",
            bookSourceName = "Parser",
            searchUrl = "https://parser.test/search?q={{key}}"
        )

        val result = service.search(source, key = "metal")

        assertEquals(1, result.books.size)
        assertEquals("Parsed By Injected Parser", result.books.single().name)
        assertEquals("https://parser.test/book/1", result.books.single().bookUrl)
    }

    @Test
    fun buildsPostRequestFromLegadoUrlOptions() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://api.source.test/book-service/findBookName", request.url)
                assertEquals(SharedHttpMethod.POST, request.method)
                assertEquals("ios-test", request.headers["X-Client"])
                assertEquals("""{"title":"metal max","pageNum":3,"pageSize":100}""", request.body)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = """
                        {"content":{"content":[{"title":"Metal Max","author":"Tester","url":"/book/1"}]}}
                    """.trimIndent()
                )
            }
        }
        val source = SharedBookSource(
            bookSourceUrl = "https://api.source.test",
            bookSourceName = "API",
            searchUrl = """
                https://api.source.test/book-service/findBookName,{
                  "method":"POST",
                  "headers":{"X-Client":"ios-test"},
                  "body":{"title":"searchKey","pageNum":{{searchPage}},"pageSize":100}
                }
            """.trimIndent(),
            ruleSearch = io.legado.shared.model.SharedSearchRule(
                bookList = "$.content.content",
                name = "$.title",
                author = "$.author",
                bookUrl = "$.url"
            )
        )

        val result = BookSearchService(fetcher).search(source, key = "metal max", page = 3)

        assertEquals(1, result.books.size)
        assertEquals("Metal Max", result.books.single().name)
    }
}

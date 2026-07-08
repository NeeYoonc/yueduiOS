package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BookInfoServiceTest {
    @Test
    fun fetchesBookUrlAndDelegatesBodyToParser() = runBlocking {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )
        val book = SharedBook(
            name = "Search Name",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1",
            origin = source.bookSourceUrl
        )
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://source.test/book/1", request.url)
                return SharedHttpResponse(
                    finalUrl = "https://source.test/book/1/detail.html",
                    statusCode = 200,
                    body = "raw detail body"
                )
            }
        }
        val parser = object : BookInfoParser {
            override fun parse(
                source: SharedBookSource,
                book: SharedBook,
                body: String
            ): SharedBook {
                assertEquals("https://source.test", source.bookSourceUrl)
                assertEquals("Search Name", book.name)
                assertEquals("raw detail body", body)
                return book.copy(
                    name = "Parsed Name",
                    tocUrl = "/book/1/catalog",
                    coverUrl = "cover.jpg"
                )
            }
        }

        val result = BookInfoService(fetcher, parser).getBookInfo(source, book)

        assertEquals(source, result.source)
        assertEquals(book, result.inputBook)
        assertEquals("https://source.test/book/1/detail.html", result.response.finalUrl)
        assertEquals("Parsed Name", result.book.name)
        assertEquals("https://source.test/book/1/catalog", result.book.tocUrl)
        assertEquals("https://source.test/book/1/cover.jpg", result.book.coverUrl)
    }

    @Test
    fun fallsBackToExistingBookWhenParserDoesNotChangeIt() = runBlocking {
        val source = SharedBookSource(
            bookSourceUrl = "https://fallback.test",
            bookSourceName = "Fallback"
        )
        val book = SharedBook(
            name = "Existing",
            bookUrl = "https://fallback.test/book/1",
            tocUrl = "https://fallback.test/book/1",
            origin = source.bookSourceUrl
        )
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = ""
                )
            }
        }

        val result = BookInfoService(fetcher).getBookInfo(source, book)

        assertEquals(book, result.book)
    }
}

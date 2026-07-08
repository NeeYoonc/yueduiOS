package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse

class BookInfoService(
    private val httpFetcher: HttpFetcher,
    private val bookInfoParser: BookInfoParser = RegexBookInfoParser
) {
    suspend fun getBookInfo(
        source: SharedBookSource,
        book: SharedBook
    ): BookInfoResult {
        val response = httpFetcher.fetch(SharedHttpRequest(url = book.bookUrl))
        val parsedBook = bookInfoParser.parse(source, book, response.body)
        return BookInfoResult(
            source = source,
            inputBook = book,
            response = response,
            book = normalizeBookUrls(response.finalUrl, parsedBook)
        )
    }

    private fun normalizeBookUrls(baseUrl: String, book: SharedBook): SharedBook {
        return book.copy(
            tocUrl = book.tocUrl
                .takeIf { it.isNotBlank() }
                ?.let { SharedUrlResolver.resolve(baseUrl, it) }
                ?: book.tocUrl,
            coverUrl = book.coverUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { SharedUrlResolver.resolve(baseUrl, it) }
        )
    }
}

data class BookInfoResult(
    val source: SharedBookSource,
    val inputBook: SharedBook,
    val response: SharedHttpResponse,
    val book: SharedBook
)

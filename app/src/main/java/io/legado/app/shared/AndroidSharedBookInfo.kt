package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.service.BookInfoParser
import io.legado.shared.service.BookInfoResult
import io.legado.shared.service.BookInfoService
import io.legado.shared.service.RegexBookInfoParser

class AndroidSharedBookInfo(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    bookInfoParser: BookInfoParser = RegexBookInfoParser
) {
    private val bookInfoService = BookInfoService(httpFetcher, bookInfoParser)

    suspend fun getBookInfo(source: BookSource, book: Book): BookInfoResult {
        return bookInfoService.getBookInfo(source.toSharedBookSource(), book.toSharedBook())
    }
}

package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.service.BookTocService
import io.legado.shared.service.ChapterListParser
import io.legado.shared.service.ChapterListResult
import io.legado.shared.service.RegexChapterListParser

class AndroidSharedBookToc(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    chapterListParser: ChapterListParser = RegexChapterListParser
) {
    private val tocService = BookTocService(httpFetcher, chapterListParser)

    suspend fun getChapterList(source: BookSource, book: Book): ChapterListResult {
        return tocService.getChapterList(source.toSharedBookSource(), book.toSharedBook())
    }
}

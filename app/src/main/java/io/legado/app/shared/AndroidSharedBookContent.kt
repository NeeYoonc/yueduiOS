package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.service.BookContentService
import io.legado.shared.service.ChapterContentParser
import io.legado.shared.service.ChapterContentResult
import io.legado.shared.service.RegexChapterContentParser

class AndroidSharedBookContent(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    chapterContentParser: ChapterContentParser = RegexChapterContentParser
) {
    private val contentService = BookContentService(httpFetcher, chapterContentParser)

    suspend fun getContent(
        source: BookSource,
        book: Book,
        chapter: BookChapter
    ): ChapterContentResult {
        return contentService.getContent(
            source.toSharedBookSource(),
            book.toSharedBook(),
            chapter.toSharedBookChapter()
        )
    }
}

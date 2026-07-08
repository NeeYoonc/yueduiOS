package io.legado.shared.book

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.service.BookInfoResult
import io.legado.shared.service.ChapterListResult
import io.legado.shared.storage.SharedLibraryStore

class BookDetailCoordinator(
    private val client: LegadoSharedClient,
    private val bookshelfService: BookshelfService,
    private val libraryStore: SharedLibraryStore
) {
    suspend fun openSearchBook(
        source: SharedBookSource,
        searchBook: SharedSearchBook,
        nowMillis: Long = 0L
    ): BookDetailResult {
        val inputBook = searchBook.toBook()
        val bookInfo = if (source.ruleBookInfo != null) {
            client.getBookInfo(source, inputBook)
        } else {
            null
        }
        val resolvedBook = bookInfo?.book ?: inputBook
        val chapterList = client.getChapterList(source, resolvedBook)
        val chapters = chapterList.chapters
        val savedBook = bookshelfService.upsertBook(
            resolvedBook.copy(
                totalChapterNum = chapters.size,
                latestChapterTitle = resolvedBook.latestChapterTitle
                    ?: chapters.lastOrNull { !it.isVolume }?.title
                    ?: resolvedBook.latestChapterTitle,
                lastCheckTime = nowMillis
            )
        )
        libraryStore.saveBookChapters(savedBook, chapters)
        return BookDetailResult(
            source = source,
            searchBook = searchBook,
            bookInfo = bookInfo,
            chapterList = chapterList,
            book = savedBook,
            chapters = chapters
        )
    }

    suspend fun refreshBook(
        source: SharedBookSource,
        book: SharedBook,
        nowMillis: Long = 0L
    ): BookDetailResult {
        val bookInfo = if (source.ruleBookInfo != null) {
            client.getBookInfo(source, book)
        } else {
            null
        }
        val resolvedBook = bookInfo?.book ?: book
        val chapterList = client.getChapterList(source, resolvedBook)
        val chapters = chapterList.chapters
        val savedBook = bookshelfService.upsertBook(
            resolvedBook.copy(
                totalChapterNum = chapters.size,
                latestChapterTitle = resolvedBook.latestChapterTitle
                    ?: chapters.lastOrNull { !it.isVolume }?.title
                    ?: resolvedBook.latestChapterTitle,
                lastCheckTime = nowMillis
            )
        )
        libraryStore.saveBookChapters(savedBook, chapters)
        return BookDetailResult(
            source = source,
            searchBook = null,
            bookInfo = bookInfo,
            chapterList = chapterList,
            book = savedBook,
            chapters = chapters
        )
    }
}

data class BookDetailResult(
    val source: SharedBookSource,
    val searchBook: SharedSearchBook? = null,
    val bookInfo: BookInfoResult? = null,
    val chapterList: ChapterListResult,
    val book: SharedBook,
    val chapters: List<SharedBookChapter>
)

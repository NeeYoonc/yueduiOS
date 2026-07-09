package io.legado.shared.book

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.storage.SharedLibraryStore

class BookUpdateService(
    private val libraryStore: SharedLibraryStore,
    private val detailCoordinator: BookDetailCoordinator
) {
    suspend fun refreshBooks(
        sources: List<SharedBookSource>,
        nowMillis: Long = 0L
    ): List<BookUpdateResult> {
        return libraryStore.loadBooks()
            .filter { it.canUpdate && it.origin != LOCAL_ORIGIN && it.origin.isNotBlank() }
            .map { book ->
                val source = sourceFor(book, sources)
                if (source == null) {
                    BookUpdateResult(
                        book = book,
                        oldChapterCount = libraryStore.loadBookChapters(book).size,
                        errorMessage = "Source not found"
                    )
                } else {
                    refreshOne(book, source, nowMillis)
                }
            }
    }

    private suspend fun refreshOne(
        book: SharedBook,
        source: SharedBookSource,
        nowMillis: Long
    ): BookUpdateResult {
        val oldChapterCount = libraryStore.loadBookChapters(book).ifEmpty {
            List(book.totalChapterNum.coerceAtLeast(0)) { index ->
                io.legado.shared.model.SharedBookChapter(index = index, bookUrl = book.bookUrl)
            }
        }.size
        return runCatching {
            val refreshed = detailCoordinator.refreshBook(source, book, nowMillis)
            BookUpdateResult(
                book = refreshed.book,
                source = source,
                oldChapterCount = oldChapterCount,
                newChapterCount = (refreshed.chapters.size - oldChapterCount).coerceAtLeast(0),
                latestChapterTitle = refreshed.book.latestChapterTitle
            )
        }.getOrElse { error ->
            BookUpdateResult(
                book = book,
                source = source,
                oldChapterCount = oldChapterCount,
                errorMessage = error.message ?: error.toString()
            )
        }
    }

    private fun sourceFor(book: SharedBook, sources: List<SharedBookSource>): SharedBookSource? {
        return sources.firstOrNull { source ->
            source.enabled && (source.bookSourceUrl == book.origin || source.bookSourceName == book.originName)
        }
    }

    companion object {
        private const val LOCAL_ORIGIN = "loc_book"
    }
}

data class BookUpdateResult(
    val book: SharedBook,
    val source: SharedBookSource? = null,
    val oldChapterCount: Int = 0,
    val newChapterCount: Int = 0,
    val latestChapterTitle: String? = null,
    val errorMessage: String? = null
)

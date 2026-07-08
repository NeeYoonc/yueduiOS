package io.legado.shared.book

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.storage.SharedLibraryStore

class BookshelfService(
    private val libraryStore: SharedLibraryStore
) {
    fun listBooks(): List<SharedBook> {
        return libraryStore.loadBooks().sortedWith(compareBy<SharedBook> { it.order }.thenBy { it.name })
    }

    fun upsertBook(book: SharedBook): SharedBook {
        val books = listBooks().toMutableList()
        val existingIndex = books.indexOfFirst { it.sameBookAs(book) }
        val savedBook = if (existingIndex >= 0) {
            book.copy(order = books[existingIndex].order)
        } else {
            book.copy(order = books.nextOrder())
        }
        if (existingIndex >= 0) {
            books[existingIndex] = savedBook
        } else {
            books.add(savedBook)
        }
        libraryStore.saveBooks(books.sortedBy { it.order })
        return savedBook
    }

    fun removeBook(book: SharedBook) {
        val remaining = listBooks()
            .filterNot { it.sameBookAs(book) }
            .mapIndexed { index, item -> item.copy(order = index) }
        libraryStore.saveBooks(remaining)
    }

    fun updateProgress(
        book: SharedBook,
        chapter: SharedBookChapter,
        position: Int,
        nowMillis: Long
    ): SharedBook {
        val current = listBooks().firstOrNull { it.sameBookAs(book) } ?: book
        val updated = current.copy(
            durChapterTitle = chapter.title,
            durChapterIndex = chapter.index,
            durChapterPos = position,
            durChapterTime = nowMillis
        )
        return upsertBook(updated)
    }

    private fun List<SharedBook>.nextOrder(): Int {
        return maxOfOrNull { it.order }?.plus(1) ?: 0
    }

    private fun SharedBook.sameBookAs(other: SharedBook): Boolean {
        if (origin.isNotBlank() || other.origin.isNotBlank()) {
            return origin == other.origin && bookUrl == other.bookUrl
        }
        return bookUrl == other.bookUrl && name == other.name && author == other.author
    }
}

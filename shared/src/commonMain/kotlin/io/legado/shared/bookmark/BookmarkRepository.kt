package io.legado.shared.bookmark

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookmark
import io.legado.shared.storage.SharedLibraryStore

class BookmarkRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedBookmark> {
        return libraryStore.loadDataSnapshot().bookmarks.sortedByDescending { it.time }
    }

    fun add(
        book: SharedBook,
        chapter: SharedBookChapter,
        bookText: String,
        note: String = "",
        position: Int = 0,
        nowMillis: Long = 0L
    ): SharedBookmark {
        val bookmark = SharedBookmark(
            time = nowMillis.takeUnless { it == 0L } ?: nextTime(),
            bookName = book.name,
            bookAuthor = book.author,
            chapterIndex = chapter.index,
            chapterPos = position,
            chapterName = chapter.title,
            bookText = bookText,
            content = note
        )
        val snapshot = libraryStore.loadDataSnapshot()
        val bookmarks = listOf(bookmark) + snapshot.bookmarks.filterNot { it.time == bookmark.time }
        libraryStore.saveDataSnapshot(snapshot.copy(bookmarks = bookmarks.sortedByDescending { it.time }))
        return bookmark
    }

    fun delete(time: Long): List<SharedBookmark> {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(bookmarks = snapshot.bookmarks.filterNot { it.time == time }))
        return list()
    }

    private fun nextTime(): Long {
        return (libraryStore.loadDataSnapshot().bookmarks.maxOfOrNull { it.time } ?: 0L) + 1L
    }
}

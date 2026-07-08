package io.legado.shared.bookmark

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class BookmarkRepositoryTest {
    @Test
    fun addsListsAndDeletesBookmarks() {
        val repository = BookmarkRepository(SharedLibraryStore(InMemoryCacheStore()))
        val book = SharedBook(name = "Book", author = "Author")
        val chapter = SharedBookChapter(title = "One", index = 2)

        repository.add(book, chapter, bookText = "Text", note = "Note", position = 5, nowMillis = 99L)

        val bookmark = repository.list().single()
        assertEquals("Book", bookmark.bookName)
        assertEquals("One", bookmark.chapterName)
        assertEquals("Text", bookmark.bookText)
        assertEquals("Note", bookmark.content)
        assertEquals(2, bookmark.chapterIndex)
        assertEquals(5, bookmark.chapterPos)
        repository.delete(99L)
        assertEquals(emptyList(), repository.list())
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

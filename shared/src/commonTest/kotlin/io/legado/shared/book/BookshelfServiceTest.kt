package io.legado.shared.book

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class BookshelfServiceTest {
    @Test
    fun upsertsBooksAndKeepsStableOrder() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val service = BookshelfService(store)
        val first = SharedBook(
            name = "First",
            bookUrl = "https://source.test/book/1",
            origin = "https://source.test"
        )
        val second = SharedBook(
            name = "Second",
            bookUrl = "https://source.test/book/2",
            origin = "https://source.test"
        )

        service.upsertBook(first)
        service.upsertBook(second)
        service.upsertBook(first.copy(name = "First Updated"))

        val books = service.listBooks()
        assertEquals(listOf("First Updated", "Second"), books.map { it.name })
        assertEquals(listOf(0, 1), books.map { it.order })
    }

    @Test
    fun updatesReadingProgressForExistingBook() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val service = BookshelfService(store)
        val book = SharedBook(
            name = "Progress",
            bookUrl = "https://source.test/book/progress",
            origin = "https://source.test"
        )
        val chapter = SharedBookChapter(
            title = "Chapter 3",
            url = "https://source.test/book/progress/3",
            index = 2
        )

        service.upsertBook(book)
        service.updateProgress(book, chapter, position = 128, nowMillis = 1234L)

        val updated = service.listBooks().single()
        assertEquals("Chapter 3", updated.durChapterTitle)
        assertEquals(2, updated.durChapterIndex)
        assertEquals(128, updated.durChapterPos)
        assertEquals(1234L, updated.durChapterTime)
    }

    @Test
    fun removesBookByOriginAndUrl() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val service = BookshelfService(store)
        val first = SharedBook(name = "First", bookUrl = "https://source.test/book/1", origin = "https://source.test")
        val second = SharedBook(name = "Second", bookUrl = "https://source.test/book/2", origin = "https://source.test")
        service.upsertBook(first)
        service.upsertBook(second)

        service.removeBook(first)

        assertEquals(listOf("Second"), service.listBooks().map { it.name })
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

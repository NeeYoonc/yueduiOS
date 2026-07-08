package io.legado.shared.backup

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class DataBackupServiceTest {
    @Test
    fun exportsAndImportsLibrarySnapshotWithChapterContent() {
        val sourceStore = SharedLibraryStore(InMemoryCacheStore())
        val sourceService = DataBackupService(sourceStore)
        val book = SharedBook(name = "Book", bookUrl = "https://book", origin = "https://source")
        val chapter = SharedBookChapter(title = "One", url = "https://book/1", index = 0, bookUrl = book.bookUrl)
        sourceStore.saveBookSources(listOf(SharedBookSource(bookSourceUrl = "https://source", bookSourceName = "Source")))
        sourceStore.saveBooks(listOf(book))
        sourceStore.saveBookChapters(book, listOf(chapter))
        sourceStore.saveChapterContent(book, chapter, SharedChapterContent(content = "Body"))

        val json = sourceService.exportJson(nowMillis = 99L)
        val targetStore = SharedLibraryStore(InMemoryCacheStore())
        val imported = DataBackupService(targetStore).importJson(json)

        assertEquals(99L, imported.exportedAtMillis)
        assertEquals("Source", targetStore.loadBookSources().single().bookSourceName)
        assertEquals("Book", targetStore.loadBooks().single().name)
        assertEquals("One", targetStore.loadBookChapters(book).single().title)
        assertEquals("Body", targetStore.loadChapterContent(book, chapter)?.content)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

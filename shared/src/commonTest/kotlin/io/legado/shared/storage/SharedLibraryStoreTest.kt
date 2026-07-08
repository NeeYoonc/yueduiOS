package io.legado.shared.storage

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.model.SharedSearchKeyword
import io.legado.shared.platform.CacheStorePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedLibraryStoreTest {
    @Test
    fun savesAndLoadsBookSourcesAndBooksAsJson() {
        val cache = InMemoryCacheStore()
        val store = SharedLibraryStore(cache)
        val sources = listOf(
            SharedBookSource(
                bookSourceUrl = "https://source.test",
                bookSourceName = "Source"
            )
        )
        val books = listOf(
            SharedBook(
                name = "Metal Story",
                author = "Tester",
                bookUrl = "https://source.test/book/1",
                origin = "https://source.test"
            )
        )

        store.saveBookSources(sources)
        store.saveBooks(books)

        assertEquals(sources, store.loadBookSources())
        assertEquals(books, store.loadBooks())
        assertEquals(true, cache.values.getValue("legado.sources").contains("bookSourceUrl"))
        assertEquals(true, cache.values.getValue("legado.books").contains("Metal Story"))
    }

    @Test
    fun returnsEmptyListsWhenCacheIsMissingOrBlank() {
        val cache = InMemoryCacheStore()
        val store = SharedLibraryStore(cache)

        assertEquals(emptyList(), store.loadBookSources())
        assertEquals(emptyList(), store.loadBooks())

        cache.putText("legado.sources", "")
        cache.putText("legado.books", " ")

        assertEquals(emptyList(), store.loadBookSources())
        assertEquals(emptyList(), store.loadBooks())
    }

    @Test
    fun savesAndLoadsChapterContentByBookAndChapter() {
        val cache = InMemoryCacheStore()
        val store = SharedLibraryStore(cache)
        val book = SharedBook(
            name = "Metal Story",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            origin = "https://source.test"
        )
        val chapter = SharedBookChapter(
            title = "Chapter 1",
            url = "https://source.test/book/1/chapter/1.html",
            index = 0
        )
        val content = SharedChapterContent(
            content = "First line.",
            title = "Parsed Chapter 1",
            subContent = "Side note",
            nextContentUrls = listOf("https://source.test/book/1/chapter/1-2.html")
        )

        store.saveChapterContent(book, chapter, content)

        assertEquals(content, store.loadChapterContent(book, chapter))
        assertNull(
            store.loadChapterContent(
                book,
                chapter.copy(url = "https://source.test/book/1/chapter/missing.html")
            )
        )
    }

    @Test
    fun savesAndLoadsBookChaptersByBook() {
        val cache = InMemoryCacheStore()
        val store = SharedLibraryStore(cache)
        val book = SharedBook(
            name = "Metal Story",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            origin = "https://source.test"
        )
        val chapters = listOf(
            SharedBookChapter(title = "Chapter 1", url = "https://source.test/1", index = 0),
            SharedBookChapter(title = "Chapter 2", url = "https://source.test/2", index = 1)
        )

        store.saveBookChapters(book, chapters)

        assertEquals(chapters, store.loadBookChapters(book))
        assertEquals(emptyList(), store.loadBookChapters(book.copy(bookUrl = "https://source.test/missing")))
    }

    @Test
    fun savesAndLoadsVersionedDataSnapshot() {
        val cache = InMemoryCacheStore()
        val store = SharedLibraryStore(cache)
        val snapshot = SharedDataSnapshot(
            exportedAtMillis = 123L,
            searchKeywords = listOf(SharedSearchKeyword(word = "metal", usage = 2))
        )

        store.saveDataSnapshot(snapshot)

        assertEquals(snapshot, store.loadDataSnapshot())
        store.clearDataSnapshot()
        assertEquals(SharedDataSnapshot(), store.loadDataSnapshot())
    }

    private class InMemoryCacheStore : CacheStorePort {
        val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

package io.legado.shared.book

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedReplaceRule
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ChapterRepositoryTest {
    @Test
    fun loadsCachedChapterWithoutSourceAndUpdatesProgress() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val bookshelf = BookshelfService(store)
        val repository = ChapterRepository(LegadoSharedClient(NoopFetcher), store, bookshelf)
        val book = SharedBook(
            name = "Local",
            bookUrl = "local://book",
            origin = "loc_book"
        )
        val chapters = listOf(
            SharedBookChapter(title = "One", url = "local://book#0", index = 0, bookUrl = book.bookUrl),
            SharedBookChapter(title = "Two", url = "local://book#1", index = 1, bookUrl = book.bookUrl)
        )
        bookshelf.upsertBook(book)
        store.saveBookChapters(book, chapters)
        store.saveChapterContent(book, chapters[1], SharedChapterContent(content = "Cached body"))

        val result = repository.loadCachedChapter(book, chapterIndex = 1, nowMillis = 77L)

        assertEquals(chapters[1], result.chapter)
        assertEquals("Cached body", result.content.content)
        assertEquals("Two", bookshelf.listBooks().single().durChapterTitle)
        assertEquals(1, bookshelf.listBooks().single().durChapterIndex)
        assertEquals(77L, bookshelf.listBooks().single().durChapterTime)
    }

    @Test
    fun appliesReplacementRulesToReturnedCachedContent() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        store.saveDataSnapshot(
            store.loadDataSnapshot().copy(
                replaceRules = listOf(
                    SharedReplaceRule(name = "Clean", pattern = "raw", replacement = "clean")
                )
            )
        )
        val bookshelf = BookshelfService(store)
        val repository = ChapterRepository(LegadoSharedClient(NoopFetcher), store, bookshelf)
        val book = SharedBook(name = "Local", bookUrl = "local://book", origin = "loc_book")
        val chapter = SharedBookChapter(title = "One", url = "local://book#0", index = 0, bookUrl = book.bookUrl)
        bookshelf.upsertBook(book)
        store.saveBookChapters(book, listOf(chapter))
        store.saveChapterContent(book, chapter, SharedChapterContent(content = "raw body"))

        val result = repository.loadCachedChapter(book, chapterIndex = 0)

        assertEquals("clean body", result.content.content)
        assertEquals("raw body", store.loadChapterContent(book, chapter)?.content)
    }

    @Test
    fun loadsChapterCachesAdjacentChaptersAndUpdatesProgress() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = "<main>${request.url.substringAfterLast("/")}</main>"
                )
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val bookshelf = BookshelfService(store)
        val repository = ChapterRepository(LegadoSharedClient(fetcher), store, bookshelf)
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleContent = SharedContentRule(content = """<main>([^<]+)</main>""")
        )
        val book = SharedBook(
            name = "Book",
            bookUrl = "https://source.test/book",
            origin = "https://source.test"
        )
        val chapters = listOf(
            SharedBookChapter(title = "Chapter 1", url = "https://source.test/1.html", index = 0),
            SharedBookChapter(title = "Chapter 2", url = "https://source.test/2.html", index = 1),
            SharedBookChapter(title = "Chapter 3", url = "https://source.test/3.html", index = 2)
        )
        store.saveBookChapters(book, chapters)

        val result = repository.loadChapter(source, book, chapterIndex = 1, nowMillis = 99L)

        assertEquals(
            listOf(
                "https://source.test/2.html",
                "https://source.test/1.html",
                "https://source.test/3.html"
            ),
            requestedUrls
        )
        assertEquals(chapters[1], result.chapter)
        assertEquals("2.html", result.content.content)
        assertEquals("1.html", store.loadChapterContent(book, chapters[0])?.content)
        assertEquals("3.html", store.loadChapterContent(book, chapters[2])?.content)
        assertEquals("Chapter 2", bookshelf.listBooks().single().durChapterTitle)
        assertEquals(1, bookshelf.listBooks().single().durChapterIndex)
        assertEquals(99L, bookshelf.listBooks().single().durChapterTime)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }

    private object NoopFetcher : HttpFetcher {
        override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
            error("Network should not be used for cached local chapters")
        }
    }
}

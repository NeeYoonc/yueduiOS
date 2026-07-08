package io.legado.shared.book

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.storage.SharedLibraryStore

class ChapterRepository(
    private val client: LegadoSharedClient,
    private val libraryStore: SharedLibraryStore,
    private val bookshelfService: BookshelfService
) {
    suspend fun loadChapter(
        source: SharedBookSource,
        book: SharedBook,
        chapterIndex: Int,
        position: Int = 0,
        nowMillis: Long = 0L,
        preloadAdjacent: Boolean = true
    ): ChapterReadResult {
        val chapters = loadChapters(source, book)
        val chapter = chapters.getOrNull(chapterIndex)
            ?: throw IllegalArgumentException("Chapter index $chapterIndex is out of range")
        val cached = libraryStore.loadChapterContent(book, chapter)
        val content = cached ?: fetchAndCache(source, book, chapter)
        if (preloadAdjacent) {
            listOf(chapterIndex - 1, chapterIndex + 1)
                .mapNotNull { chapters.getOrNull(it) }
                .forEach { adjacent ->
                    if (libraryStore.loadChapterContent(book, adjacent) == null) {
                        fetchAndCache(source, book, adjacent)
                    }
                }
        }
        val updatedBook = bookshelfService.updateProgress(book, chapter, position, nowMillis)
        return ChapterReadResult(
            source = source,
            book = updatedBook,
            chapters = chapters,
            chapter = chapter,
            chapterIndex = chapter.index,
            content = content,
            wasCached = cached != null
        )
    }

    fun loadCachedChapter(
        book: SharedBook,
        chapterIndex: Int,
        position: Int = 0,
        nowMillis: Long = 0L
    ): CachedChapterReadResult {
        val chapters = libraryStore.loadBookChapters(book)
        val chapter = chapters.getOrNull(chapterIndex)
            ?: throw IllegalArgumentException("Chapter index $chapterIndex is out of range")
        val content = libraryStore.loadChapterContent(book, chapter)
            ?: SharedChapterContent(title = chapter.title)
        val updatedBook = bookshelfService.updateProgress(book, chapter, position, nowMillis)
        return CachedChapterReadResult(
            book = updatedBook,
            chapters = chapters,
            chapter = chapter,
            chapterIndex = chapter.index,
            content = content,
            wasCached = true
        )
    }

    private suspend fun loadChapters(
        source: SharedBookSource,
        book: SharedBook
    ): List<SharedBookChapter> {
        val cached = libraryStore.loadBookChapters(book)
        if (cached.isNotEmpty()) {
            return cached
        }
        val fetched = client.getChapterList(source, book).chapters
        libraryStore.saveBookChapters(book, fetched)
        return fetched
    }

    private suspend fun fetchAndCache(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter
    ): SharedChapterContent {
        val content = client.getContent(source, book, chapter).content
        libraryStore.saveChapterContent(book, chapter, content)
        return content
    }
}

data class ChapterReadResult(
    val source: SharedBookSource,
    val book: SharedBook,
    val chapters: List<SharedBookChapter>,
    val chapter: SharedBookChapter,
    val chapterIndex: Int,
    val content: SharedChapterContent,
    val wasCached: Boolean = false
)

data class CachedChapterReadResult(
    val book: SharedBook,
    val chapters: List<SharedBookChapter>,
    val chapter: SharedBookChapter,
    val chapterIndex: Int,
    val content: SharedChapterContent,
    val wasCached: Boolean = true
)

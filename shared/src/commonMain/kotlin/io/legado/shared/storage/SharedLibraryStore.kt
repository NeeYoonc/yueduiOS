package io.legado.shared.storage

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.platform.CacheStorePort
import kotlinx.serialization.json.Json

class SharedLibraryStore(
    private val cacheStore: CacheStorePort,
    private val sourceKey: String = DEFAULT_SOURCE_KEY,
    private val booksKey: String = DEFAULT_BOOKS_KEY,
    private val dataSnapshotKey: String = DEFAULT_DATA_SNAPSHOT_KEY
) {
    fun saveBookSources(sources: List<SharedBookSource>) {
        cacheStore.putText(sourceKey, json.encodeToString(sources))
    }

    fun loadBookSources(): List<SharedBookSource> {
        return loadList(sourceKey)
    }

    fun saveBooks(books: List<SharedBook>) {
        cacheStore.putText(booksKey, json.encodeToString(books))
    }

    fun loadBooks(): List<SharedBook> {
        return loadList(booksKey)
    }

    fun saveDataSnapshot(snapshot: SharedDataSnapshot) {
        cacheStore.putText(dataSnapshotKey, json.encodeToString(snapshot))
    }

    fun loadDataSnapshot(): SharedDataSnapshot {
        val rawJson = cacheStore.getText(dataSnapshotKey)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return SharedDataSnapshot()
        return runCatching {
            json.decodeFromString<SharedDataSnapshot>(rawJson)
        }.getOrElse {
            SharedDataSnapshot()
        }
    }

    fun clearDataSnapshot() {
        cacheStore.removeText(dataSnapshotKey)
    }

    fun saveChapterContent(
        book: SharedBook,
        chapter: SharedBookChapter,
        content: SharedChapterContent
    ) {
        cacheStore.putText(chapterContentKey(book, chapter), json.encodeToString(content))
    }

    fun loadChapterContent(
        book: SharedBook,
        chapter: SharedBookChapter
    ): SharedChapterContent? {
        val rawJson = cacheStore.getText(chapterContentKey(book, chapter))?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return runCatching {
            json.decodeFromString<SharedChapterContent>(rawJson)
        }.getOrNull()
    }

    private inline fun <reified T> loadList(key: String): List<T> {
        val rawJson = cacheStore.getText(key)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<T>>(rawJson)
        }.getOrElse {
            emptyList()
        }
    }

    private fun chapterContentKey(book: SharedBook, chapter: SharedBookChapter): String {
        val bookIdentity = listOf(book.origin, book.bookUrl)
            .joinToString("|")
            .ifBlank { book.name }
        val chapterIdentity = chapter.url.ifBlank { chapter.title }
        return "$CHAPTER_CONTENT_KEY_PREFIX${bookIdentity.cacheKeyPart()}:${chapterIdentity.cacheKeyPart()}"
    }

    private fun String.cacheKeyPart(): String {
        return encodeToByteArray().joinToString("") { byte ->
            byte.toUByte().toString(16).padStart(2, '0')
        }
    }

    companion object {
        const val DEFAULT_SOURCE_KEY = "legado.sources"
        const val DEFAULT_BOOKS_KEY = "legado.books"
        const val DEFAULT_DATA_SNAPSHOT_KEY = "legado.dataSnapshot"
        const val CHAPTER_CONTENT_KEY_PREFIX = "legado.chapterContent."

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}

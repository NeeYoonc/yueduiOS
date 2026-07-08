package io.legado.shared.backup

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedChapterContentRecord
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataBackupService(
    private val libraryStore: SharedLibraryStore
) {
    fun exportSnapshot(nowMillis: Long = 0L): SharedDataSnapshot {
        val base = libraryStore.loadDataSnapshot()
        val books = libraryStore.loadBooks()
        val bookSources = libraryStore.loadBookSources()
        val chaptersByBook = books.associateWith { libraryStore.loadBookChapters(it) }
        val chapters = chaptersByBook.values.flatten()
        val chapterContents = chaptersByBook.flatMap { (book, bookChapters) ->
            bookChapters.mapNotNull { chapter ->
                libraryStore.loadChapterContent(book, chapter)?.let { content ->
                    SharedChapterContentRecord(
                        bookUrl = book.bookUrl,
                        chapterUrl = chapter.url,
                        content = content
                    )
                }
            }
        }
        return base.copy(
            exportedAtMillis = nowMillis,
            bookSources = bookSources,
            books = books,
            chapters = chapters,
            chapterContents = chapterContents
        )
    }

    fun exportJson(nowMillis: Long = 0L): String {
        return json.encodeToString(exportSnapshot(nowMillis))
    }

    @Throws(IllegalArgumentException::class)
    fun importJson(rawJson: String): SharedDataSnapshot {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Backup JSON is empty" }
        val snapshot = json.decodeFromString<SharedDataSnapshot>(trimmed)
        libraryStore.saveDataSnapshot(snapshot)
        libraryStore.saveBookSources(snapshot.bookSources)
        libraryStore.saveBooks(snapshot.books)
        restoreChapters(snapshot)
        return snapshot
    }

    private fun restoreChapters(snapshot: SharedDataSnapshot) {
        val booksByUrl = snapshot.books.associateBy { it.bookUrl }
        val chaptersByBookUrl = snapshot.chapters.groupBy { it.bookUrl }
        chaptersByBookUrl.forEach { (bookUrl, chapters) ->
            val book = booksByUrl[bookUrl] ?: return@forEach
            libraryStore.saveBookChapters(book, chapters)
        }
        val chaptersByIdentity = chaptersByBookUrl.mapValues { (_, chapters) ->
            chapters.associateBy { it.url.ifBlank { it.title } }
        }
        snapshot.chapterContents.forEach { record ->
            val book = booksByUrl[record.bookUrl] ?: return@forEach
            val chapter = chaptersByIdentity[record.bookUrl]
                ?.get(record.chapterUrl)
                ?: SharedBookChapter(
                    title = record.chapterUrl,
                    url = record.chapterUrl,
                    bookUrl = record.bookUrl
                )
            libraryStore.saveChapterContent(book, chapter, record.content)
        }
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            prettyPrint = true
        }
    }
}

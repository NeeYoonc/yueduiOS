package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.service.ReadingFlowResult
import io.legado.shared.source.DefaultDataImporter
import io.legado.shared.source.DefaultDataPayload
import io.legado.shared.storage.SharedLibraryStore

open class LegadoRuntime(
    httpFetcher: HttpFetcher,
    cacheStore: CacheStorePort
) {
    val client: LegadoSharedClient = LegadoSharedClient(httpFetcher)
    val libraryStore: SharedLibraryStore = SharedLibraryStore(cacheStore)

    @Throws(IllegalArgumentException::class)
    fun importAndSaveBookSources(json: String): List<SharedBookSource> {
        return client.importBookSources(json).also { sources ->
            libraryStore.saveBookSources(sources)
        }
    }

    fun loadBookSources(): List<SharedBookSource> {
        return libraryStore.loadBookSources()
    }

    fun saveBooks(books: List<SharedBook>) {
        libraryStore.saveBooks(books)
    }

    fun loadBooks(): List<SharedBook> {
        return libraryStore.loadBooks()
    }

    fun importAndSaveDefaultData(payload: DefaultDataPayload) {
        val snapshot = DefaultDataImporter.importSnapshot(payload)
        libraryStore.saveDataSnapshot(snapshot)
        if (snapshot.bookSources.isNotEmpty()) {
            libraryStore.saveBookSources(snapshot.bookSources)
        }
    }

    suspend fun openFirstSearchResult(
        source: SharedBookSource,
        key: String,
        page: Int = 1
    ): ReadingFlowResult {
        return client.openFirstSearchResult(source, key, page).also { result ->
            val book = result.selectedBook
            val chapter = result.selectedChapter
            val content = result.content?.content
            if (book != null && chapter != null && content != null) {
                libraryStore.saveChapterContent(book, chapter, content)
            }
        }
    }
}

package io.legado.shared.book

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedSearchKeyword
import io.legado.shared.service.SearchPageResult
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class SearchCoordinator(
    private val client: LegadoSharedClient,
    private val libraryStore: SharedLibraryStore
) {
    fun listKeywords(): List<SharedSearchKeyword> {
        return libraryStore.loadDataSnapshot().searchKeywords.sortedWith(
            compareByDescending<SharedSearchKeyword> { it.lastUseTime }
                .thenByDescending { it.usage }
                .thenBy { it.word }
        )
    }

    fun deleteKeyword(word: String): List<SharedSearchKeyword> {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(searchKeywords = snapshot.searchKeywords.filterNot { it.word == word }))
        return listKeywords()
    }

    fun clearKeywords(): List<SharedSearchKeyword> {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(searchKeywords = emptyList()))
        return emptyList()
    }

    fun listSearchBooks(): List<SharedSearchBook> {
        return libraryStore.loadDataSnapshot().searchBooks.sortedForUse()
    }

    fun deleteSearchBook(bookUrl: String): List<SharedSearchBook> {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(searchBooks = snapshot.searchBooks.filterNot { it.bookUrl == bookUrl }))
        return listSearchBooks()
    }

    fun clearSearchBooks(): List<SharedSearchBook> {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(searchBooks = emptyList()))
        return emptyList()
    }

    fun clearExpiredSearchBooks(beforeMillis: Long): List<SharedSearchBook> {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(searchBooks = snapshot.searchBooks.filterNot { it.time < beforeMillis }))
        return listSearchBooks()
    }

    fun listChangeSourceCandidates(
        book: SharedBook,
        sources: List<SharedBookSource>,
        key: String = ""
    ): List<SharedSearchBook> {
        val enabledSourceByUrl = sources
            .filter { it.enabled }
            .associateBy { it.bookSourceUrl }
        return listSearchBooks()
            .asSequence()
            .filter { it.bookUrl != book.bookUrl }
            .filter { it.name == book.name }
            .filter { book.author.isBlank() || it.author.contains(book.author, ignoreCase = true) }
            .filter { key.isBlank() || it.originName.contains(key, ignoreCase = true) || (it.latestChapterTitle?.contains(key, ignoreCase = true) == true) }
            .filter { enabledSourceByUrl.containsKey(it.origin) }
            .map { searchBook ->
                val source = enabledSourceByUrl.getValue(searchBook.origin)
                searchBook.copy(
                    originName = searchBook.originName.ifBlank { source.bookSourceName },
                    originOrder = source.customOrder.takeUnless { it == 0 } ?: searchBook.originOrder
                )
            }
            .toList()
            .sortedForUse()
    }

    suspend fun search(
        sources: List<SharedBookSource>,
        key: String,
        page: Int = 1,
        nowMillis: Long = 0L
    ): SearchCoordinatorResult = supervisorScope {
        recordKeyword(key, nowMillis)
        val activeSources = sources.filter { it.enabled && !it.searchUrl.isNullOrBlank() }
        val outcomes = activeSources.map { source ->
            async {
                source to runCatching { client.search(source, key, page) }
            }
        }.awaitAll()
        SearchCoordinatorResult(
            pages = outcomes.mapNotNull { (_, outcome) -> outcome.getOrNull() },
            errors = outcomes.mapNotNull { (source, outcome) ->
                outcome.exceptionOrNull()?.let { error ->
                    SourceSearchError(
                        source = source,
                        message = error.message ?: error::class.simpleName.orEmpty()
                    )
                }
            }
        ).also { result ->
            saveSearchBooks(result.pages.flatMap { pageResult ->
                pageResult.books.map { searchBook ->
                    searchBook.normalizedForStorage(pageResult.source, nowMillis)
                }
            })
        }
    }

    fun recordKeyword(key: String, nowMillis: Long) {
        if (key.isBlank()) {
            return
        }
        val snapshot = libraryStore.loadDataSnapshot()
        val existing = snapshot.searchKeywords.firstOrNull { it.word == key }
        val updatedKeyword = if (existing == null) {
            SharedSearchKeyword(word = key, usage = 1, lastUseTime = nowMillis)
        } else {
            existing.copy(usage = existing.usage + 1, lastUseTime = nowMillis)
        }
        val keywords = listOf(updatedKeyword) + snapshot.searchKeywords.filterNot { it.word == key }
        libraryStore.saveDataSnapshot(snapshot.copy(searchKeywords = keywords))
    }

    private fun saveSearchBooks(books: List<SharedSearchBook>) {
        if (books.isEmpty()) {
            return
        }
        val snapshot = libraryStore.loadDataSnapshot()
        val byUrl = linkedMapOf<String, SharedSearchBook>()
        snapshot.searchBooks.forEach { existing ->
            if (existing.bookUrl.isNotBlank()) {
                byUrl[existing.bookUrl] = existing
            }
        }
        books.forEach { book ->
            if (book.bookUrl.isNotBlank()) {
                byUrl[book.bookUrl] = book
            }
        }
        libraryStore.saveDataSnapshot(snapshot.copy(searchBooks = byUrl.values.toList().sortedForUse()))
    }

    private fun SharedSearchBook.normalizedForStorage(
        source: SharedBookSource,
        nowMillis: Long
    ): SharedSearchBook {
        return copy(
            origin = origin.ifBlank { source.bookSourceUrl },
            originName = originName.ifBlank { source.bookSourceName },
            originOrder = originOrder.takeUnless { it == 0 } ?: source.customOrder,
            time = time.takeUnless { it == 0L } ?: nowMillis
        )
    }

    private fun List<SharedSearchBook>.sortedForUse(): List<SharedSearchBook> {
        return sortedWith(
            compareBy<SharedSearchBook> { it.originOrder }
                .thenBy { it.originName }
                .thenBy { it.name }
                .thenBy { it.author }
                .thenBy { it.bookUrl }
        )
    }
}

data class SearchCoordinatorResult(
    val pages: List<SearchPageResult>,
    val errors: List<SourceSearchError>
) {
    val books: List<SharedSearchBook>
        get() = pages.flatMap { it.books }
}

data class SourceSearchError(
    val source: SharedBookSource,
    val message: String
)

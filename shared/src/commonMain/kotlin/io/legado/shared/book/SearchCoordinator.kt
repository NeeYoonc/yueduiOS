package io.legado.shared.book

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedDataSnapshot
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
        )
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

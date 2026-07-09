package io.legado.shared.source

import io.legado.shared.model.SharedBookSource
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SourceRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedBookSource> {
        return libraryStore.loadBookSources()
            .sortedWith(compareBy<SharedBookSource> { it.customOrder }.thenBy { it.bookSourceName })
    }

    fun saveAll(sources: List<SharedBookSource>): List<SharedBookSource> {
        val normalized = sources.mapIndexed { index, source ->
            source.copy(customOrder = source.customOrder.takeUnless { it == 0 } ?: index)
        }
        libraryStore.saveBookSources(normalized)
        return list()
    }

    fun upsert(source: SharedBookSource): SharedBookSource {
        val sources = libraryStore.loadBookSources().toMutableList()
        val existingIndex = sources.indexOfFirst { it.bookSourceUrl == source.bookSourceUrl }
        val saved = if (existingIndex >= 0) {
            source.copy(customOrder = sources[existingIndex].customOrder)
        } else {
            source.copy(customOrder = nextOrder(sources, source.customOrder))
        }
        if (existingIndex >= 0) {
            sources[existingIndex] = saved
        } else {
            sources.add(saved)
        }
        libraryStore.saveBookSources(sources)
        return saved
    }

    fun setEnabled(bookSourceUrl: String, enabled: Boolean): SharedBookSource? {
        val sources = libraryStore.loadBookSources().toMutableList()
        val index = sources.indexOfFirst { it.bookSourceUrl == bookSourceUrl }
        if (index < 0) {
            return null
        }
        val updated = sources[index].copy(enabled = enabled)
        sources[index] = updated
        libraryStore.saveBookSources(sources)
        return updated
    }

    fun delete(bookSourceUrl: String): List<SharedBookSource> {
        val remaining = libraryStore.loadBookSources()
            .filterNot { it.bookSourceUrl == bookSourceUrl }
            .mapIndexed { index, source -> source.copy(customOrder = index) }
        libraryStore.saveBookSources(remaining)
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedBookSource> {
        val imported = SourceJsonImporter.importBookSources(rawJson)
        if (replace) {
            return saveAll(imported)
        }
        imported.forEach(::upsert)
        return list()
    }

    fun upsertJson(rawJson: String): SharedBookSource {
        val imported = SourceJsonImporter.importBookSources(rawJson)
        require(imported.size == 1) { "Book source editor JSON must contain exactly one source" }
        return upsert(imported.single())
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    private fun nextOrder(sources: List<SharedBookSource>, requestedOrder: Int): Int {
        if (requestedOrder != 0 && sources.none { it.customOrder == requestedOrder }) {
            return requestedOrder
        }
        return sources.maxOfOrNull { it.customOrder }?.plus(1) ?: 0
    }

    private companion object {
        val json = Json {
            explicitNulls = false
            prettyPrint = true
        }
    }
}

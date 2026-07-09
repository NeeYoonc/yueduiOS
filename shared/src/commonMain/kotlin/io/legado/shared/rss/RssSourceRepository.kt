package io.legado.shared.rss

import io.legado.shared.model.SharedRssSource
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class RssSourceRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedRssSource> {
        return libraryStore.loadDataSnapshot().rssSources.sortedForUse()
    }

    fun upsert(source: SharedRssSource): SharedRssSource {
        val sources = libraryStore.loadDataSnapshot().rssSources.toMutableList()
        val saved = source.copy(
            customOrder = source.customOrder.takeUnless { it == 0 } ?: nextOrder(sources)
        )
        val index = sources.indexOfFirst { it.sourceUrl == saved.sourceUrl }
        if (index >= 0) {
            sources[index] = saved
        } else {
            sources.add(saved)
        }
        save(sources)
        return saved
    }

    fun setEnabled(sourceUrl: String, enabled: Boolean): SharedRssSource? {
        val sources = libraryStore.loadDataSnapshot().rssSources.toMutableList()
        val index = sources.indexOfFirst { it.sourceUrl == sourceUrl }
        if (index < 0) {
            return null
        }
        val updated = sources[index].copy(enabled = enabled)
        sources[index] = updated
        save(sources)
        return updated
    }

    fun delete(sourceUrl: String): List<SharedRssSource> {
        save(libraryStore.loadDataSnapshot().rssSources.filterNot { it.sourceUrl == sourceUrl })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedRssSource> {
        val imported = decodeSources(rawJson)
        if (replace) {
            save(imported)
            return list()
        }
        imported.forEach(::upsert)
        return list()
    }

    fun upsertJson(rawJson: String): SharedRssSource {
        val imported = decodeSources(rawJson)
        require(imported.size == 1) { "RSS source editor JSON must contain exactly one source" }
        return upsert(imported.single())
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    private fun decodeSources(rawJson: String): List<SharedRssSource> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "RSS source JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val sources = if (element is JsonArray) {
            json.decodeFromString<List<SharedRssSource>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedRssSource>(trimmed))
        }.filter { it.sourceUrl.isNotBlank() && it.sourceName.isNotBlank() }
        require(sources.isNotEmpty()) { "RSS source JSON does not contain a usable source" }
        return sources
    }

    private fun save(sources: List<SharedRssSource>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(rssSources = sources.sortedForUse()))
    }

    private fun List<SharedRssSource>.sortedForUse(): List<SharedRssSource> {
        return sortedWith(compareBy<SharedRssSource> { it.customOrder }.thenBy { it.sourceName }.thenBy { it.sourceUrl })
    }

    private fun nextOrder(sources: List<SharedRssSource>): Int {
        return (sources.maxOfOrNull { it.customOrder } ?: 0) + 1
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

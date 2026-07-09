package io.legado.shared.config

import io.legado.shared.model.SharedCacheEntry
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class CacheEntryRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedCacheEntry> {
        return libraryStore.loadDataSnapshot().caches.sortedForUse()
    }

    fun getValue(key: String, nowMillis: Long = 0L): String? {
        val entry = list().firstOrNull { it.key == key } ?: return null
        return if (entry.deadline == 0L || nowMillis == 0L || entry.deadline > nowMillis) {
            entry.value
        } else {
            null
        }
    }

    fun putValue(key: String, value: String?, deadline: Long = 0L): SharedCacheEntry {
        return upsert(SharedCacheEntry(key = key, value = value, deadline = deadline))
    }

    fun upsert(entry: SharedCacheEntry): SharedCacheEntry {
        val cleanKey = entry.key.trim()
        require(cleanKey.isNotEmpty()) { "Cache key is empty" }
        val saved = entry.copy(key = cleanKey)
        val entries = libraryStore.loadDataSnapshot().caches.toMutableList()
        val index = entries.indexOfFirst { it.key == cleanKey }
        if (index >= 0) {
            entries[index] = saved
        } else {
            entries.add(saved)
        }
        save(entries)
        return saved
    }

    fun delete(key: String): List<SharedCacheEntry> {
        save(libraryStore.loadDataSnapshot().caches.filterNot { it.key == key })
        return list()
    }

    fun clearExpired(nowMillis: Long): List<SharedCacheEntry> {
        save(libraryStore.loadDataSnapshot().caches.filterNot { it.deadline > 0L && it.deadline < nowMillis })
        return list()
    }

    fun clear(): List<SharedCacheEntry> {
        save(emptyList())
        return emptyList()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedCacheEntry> {
        val imported = decodeEntries(rawJson)
        if (replace) {
            save(imported)
            return list()
        }
        imported.forEach(::upsert)
        return list()
    }

    fun upsertJson(rawJson: String): SharedCacheEntry {
        val imported = decodeEntries(rawJson)
        require(imported.size == 1) { "Cache editor JSON must contain exactly one cache entry" }
        return upsert(imported.single())
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    private fun decodeEntries(rawJson: String): List<SharedCacheEntry> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Cache JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val entries = if (element is JsonArray) {
            json.decodeFromString<List<SharedCacheEntry>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedCacheEntry>(trimmed))
        }.map { it.copy(key = it.key.trim()) }
            .filter { it.key.isNotBlank() }
        require(entries.isNotEmpty()) { "Cache JSON does not contain a usable cache entry" }
        return entries.sortedForUse()
    }

    private fun save(entries: List<SharedCacheEntry>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(caches = entries.sortedForUse()))
    }

    private fun List<SharedCacheEntry>.sortedForUse(): List<SharedCacheEntry> {
        return sortedBy { it.key }
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

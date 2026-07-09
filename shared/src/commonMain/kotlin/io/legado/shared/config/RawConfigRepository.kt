package io.legado.shared.config

import io.legado.shared.model.SharedRawConfigEntry
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class RawConfigRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedRawConfigEntry> {
        return libraryStore.loadDataSnapshot().rawConfigs.toEntries()
    }

    fun upsert(key: String, value: String): SharedRawConfigEntry {
        val cleanKey = key.trim()
        require(cleanKey.isNotEmpty()) { "Raw config key is empty" }
        val snapshot = libraryStore.loadDataSnapshot()
        val updated = snapshot.rawConfigs.toMutableMap()
        updated[cleanKey] = value
        libraryStore.saveDataSnapshot(snapshot.copy(rawConfigs = updated.sortedByKey()))
        return SharedRawConfigEntry(cleanKey, value)
    }

    fun delete(key: String): List<SharedRawConfigEntry> {
        val snapshot = libraryStore.loadDataSnapshot()
        val updated = snapshot.rawConfigs.toMutableMap()
        updated.remove(key)
        libraryStore.saveDataSnapshot(snapshot.copy(rawConfigs = updated.sortedByKey()))
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedRawConfigEntry> {
        val imported = decodeEntries(rawJson)
        val snapshot = libraryStore.loadDataSnapshot()
        val updated = if (replace) {
            mutableMapOf()
        } else {
            snapshot.rawConfigs.toMutableMap()
        }
        imported.forEach { entry ->
            updated[entry.key] = entry.value
        }
        libraryStore.saveDataSnapshot(snapshot.copy(rawConfigs = updated.sortedByKey()))
        return list()
    }

    fun upsertJson(rawJson: String): SharedRawConfigEntry {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Raw config JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val entry = if (element is JsonObject && element.containsKey("key")) {
            json.decodeFromString<SharedRawConfigEntry>(trimmed)
        } else {
            val imported = decodeEntries(trimmed)
            require(imported.size == 1) { "Raw config editor JSON must contain exactly one config" }
            imported.single()
        }
        return upsert(entry.key, entry.value)
    }

    fun exportJson(): String {
        return json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            libraryStore.loadDataSnapshot().rawConfigs.sortedByKey()
        )
    }

    private fun decodeEntries(rawJson: String): List<SharedRawConfigEntry> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Raw config JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val entries = when (element) {
            is JsonArray -> json.decodeFromString<List<SharedRawConfigEntry>>(trimmed)
            is JsonObject -> json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), trimmed)
                .map { (key, value) -> SharedRawConfigEntry(key, value) }
            else -> emptyList()
        }.filter { it.key.isNotBlank() }
        require(entries.isNotEmpty()) { "Raw config JSON does not contain a usable config" }
        return entries.sortedBy { it.key }
    }

    private fun Map<String, String>.toEntries(): List<SharedRawConfigEntry> {
        return entries
            .sortedBy { it.key }
            .map { (key, value) -> SharedRawConfigEntry(key, value) }
    }

    private fun Map<String, String>.sortedByKey(): Map<String, String> {
        return entries
            .sortedBy { it.key }
            .associate { it.key to it.value }
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

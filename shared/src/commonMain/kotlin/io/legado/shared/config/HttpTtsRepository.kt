package io.legado.shared.config

import io.legado.shared.model.SharedHttpTts
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class HttpTtsRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedHttpTts> {
        return libraryStore.loadDataSnapshot().httpTts.sortedForUse()
    }

    fun upsert(engine: SharedHttpTts): SharedHttpTts {
        val engines = libraryStore.loadDataSnapshot().httpTts.toMutableList()
        val saved = engine.copy(id = engine.id.takeUnless { it == 0L } ?: nextId(engines))
        val index = engines.indexOfFirst { it.id == saved.id }
        if (index >= 0) {
            engines[index] = saved
        } else {
            engines.add(saved)
        }
        save(engines)
        return saved
    }

    fun delete(id: Long): List<SharedHttpTts> {
        save(libraryStore.loadDataSnapshot().httpTts.filterNot { it.id == id })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedHttpTts> {
        val imported = decodeEngines(rawJson)
        if (replace) {
            save(imported)
            return list()
        }
        imported.forEach(::upsert)
        return list()
    }

    fun upsertJson(rawJson: String): SharedHttpTts {
        val imported = decodeEngines(rawJson)
        require(imported.size == 1) { "HTTP TTS editor JSON must contain exactly one engine" }
        return upsert(imported.single())
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    private fun decodeEngines(rawJson: String): List<SharedHttpTts> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "HTTP TTS JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val engines = if (element is JsonArray) {
            json.decodeFromString<List<SharedHttpTts>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedHttpTts>(trimmed))
        }.filter { it.name.isNotBlank() && it.url.isNotBlank() }
        require(engines.isNotEmpty()) { "HTTP TTS JSON does not contain a usable engine" }
        return engines
    }

    private fun save(engines: List<SharedHttpTts>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(httpTts = engines.sortedForUse()))
    }

    private fun List<SharedHttpTts>.sortedForUse(): List<SharedHttpTts> {
        return sortedWith(compareBy<SharedHttpTts> { it.name }.thenBy { it.id })
    }

    private fun nextId(engines: List<SharedHttpTts>): Long {
        return (engines.maxOfOrNull { it.id } ?: 0L) + 1L
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

package io.legado.shared.config

import io.legado.shared.model.SharedKeyboardAssist
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class KeyboardAssistRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedKeyboardAssist> {
        return libraryStore.loadDataSnapshot().keyboardAssists.sortedForUse()
    }

    fun upsert(assist: SharedKeyboardAssist): SharedKeyboardAssist {
        val assists = libraryStore.loadDataSnapshot().keyboardAssists.toMutableList()
        val saved = assist.copy(
            serialNo = assist.serialNo.takeUnless { it == 0 } ?: nextOrder(assists, assist.type)
        )
        val index = assists.indexOfFirst { it.type == saved.type && it.key == saved.key }
        if (index >= 0) {
            assists[index] = saved
        } else {
            assists.add(saved)
        }
        save(assists)
        return saved
    }

    fun delete(type: Int, key: String): List<SharedKeyboardAssist> {
        save(libraryStore.loadDataSnapshot().keyboardAssists.filterNot { it.type == type && it.key == key })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedKeyboardAssist> {
        val imported = decodeAssists(rawJson)
        if (replace) {
            save(imported)
            return list()
        }
        imported.forEach(::upsert)
        return list()
    }

    fun upsertJson(rawJson: String): SharedKeyboardAssist {
        val imported = decodeAssists(rawJson)
        require(imported.size == 1) { "Keyboard assist editor JSON must contain exactly one assist" }
        return upsert(imported.single())
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    private fun decodeAssists(rawJson: String): List<SharedKeyboardAssist> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Keyboard assist JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val assists = if (element is JsonArray) {
            json.decodeFromString<List<SharedKeyboardAssist>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedKeyboardAssist>(trimmed))
        }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
        require(assists.isNotEmpty()) { "Keyboard assist JSON does not contain a usable assist" }
        return assists
    }

    private fun save(assists: List<SharedKeyboardAssist>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(keyboardAssists = assists.sortedForUse()))
    }

    private fun List<SharedKeyboardAssist>.sortedForUse(): List<SharedKeyboardAssist> {
        return sortedWith(
            compareBy<SharedKeyboardAssist> { it.type }
                .thenBy { it.serialNo }
                .thenBy { it.key }
        )
    }

    private fun nextOrder(assists: List<SharedKeyboardAssist>, type: Int): Int {
        return (assists.filter { it.type == type }.maxOfOrNull { it.serialNo } ?: 0) + 1
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

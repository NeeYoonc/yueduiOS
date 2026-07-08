package io.legado.shared.config

import io.legado.shared.model.SharedRuleSub
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class RuleSubRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedRuleSub> {
        return libraryStore.loadDataSnapshot().ruleSubs.sortedForUse()
    }

    fun upsert(ruleSub: SharedRuleSub): SharedRuleSub {
        val ruleSubs = libraryStore.loadDataSnapshot().ruleSubs.toMutableList()
        val saved = ruleSub.copy(
            id = ruleSub.id.takeUnless { it == 0L } ?: nextId(ruleSubs),
            customOrder = ruleSub.customOrder.takeUnless { it == 0 } ?: nextOrder(ruleSubs)
        )
        val index = ruleSubs.indexOfFirst { it.id == saved.id }
        if (index >= 0) {
            ruleSubs[index] = saved
        } else {
            ruleSubs.add(saved)
        }
        save(ruleSubs)
        return saved
    }

    fun setAutoUpdate(id: Long, autoUpdate: Boolean): SharedRuleSub? {
        val ruleSubs = libraryStore.loadDataSnapshot().ruleSubs.toMutableList()
        val index = ruleSubs.indexOfFirst { it.id == id }
        if (index < 0) {
            return null
        }
        val updated = ruleSubs[index].copy(autoUpdate = autoUpdate)
        ruleSubs[index] = updated
        save(ruleSubs)
        return updated
    }

    fun delete(id: Long): List<SharedRuleSub> {
        save(libraryStore.loadDataSnapshot().ruleSubs.filterNot { it.id == id })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedRuleSub> {
        val imported = decodeRuleSubs(rawJson)
        if (replace) {
            save(imported)
            return list()
        }
        imported.forEach(::upsert)
        return list()
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    private fun decodeRuleSubs(rawJson: String): List<SharedRuleSub> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Rule subscription JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val ruleSubs = if (element is JsonArray) {
            json.decodeFromString<List<SharedRuleSub>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedRuleSub>(trimmed))
        }.filter { it.name.isNotBlank() && it.url.isNotBlank() }
        require(ruleSubs.isNotEmpty()) { "Rule subscription JSON does not contain a usable subscription" }
        return ruleSubs
    }

    private fun save(ruleSubs: List<SharedRuleSub>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(ruleSubs = ruleSubs.sortedForUse()))
    }

    private fun List<SharedRuleSub>.sortedForUse(): List<SharedRuleSub> {
        return sortedWith(compareBy<SharedRuleSub> { it.customOrder }.thenBy { it.id }.thenBy { it.name })
    }

    private fun nextId(ruleSubs: List<SharedRuleSub>): Long {
        return (ruleSubs.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    private fun nextOrder(ruleSubs: List<SharedRuleSub>): Int {
        return (ruleSubs.maxOfOrNull { it.customOrder } ?: 0) + 1
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

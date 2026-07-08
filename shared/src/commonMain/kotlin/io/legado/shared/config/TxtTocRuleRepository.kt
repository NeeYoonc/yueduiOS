package io.legado.shared.config

import io.legado.shared.model.SharedTxtTocRule
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class TxtTocRuleRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedTxtTocRule> {
        return libraryStore.loadDataSnapshot().txtTocRules.sortedForUse()
    }

    fun upsert(rule: SharedTxtTocRule): SharedTxtTocRule {
        val rules = libraryStore.loadDataSnapshot().txtTocRules.toMutableList()
        val saved = rule.copy(
            id = rule.id.takeUnless { it == 0L } ?: nextId(rules),
            serialNumber = rule.serialNumber.takeUnless { it < 0 } ?: nextOrder(rules)
        )
        val index = rules.indexOfFirst { it.id == saved.id }
        if (index >= 0) {
            rules[index] = saved
        } else {
            rules.add(saved)
        }
        save(rules)
        return saved
    }

    fun setEnabled(id: Long, enabled: Boolean): SharedTxtTocRule? {
        val rules = libraryStore.loadDataSnapshot().txtTocRules.toMutableList()
        val index = rules.indexOfFirst { it.id == id }
        if (index < 0) {
            return null
        }
        val updated = rules[index].copy(enable = enabled)
        rules[index] = updated
        save(rules)
        return updated
    }

    fun delete(id: Long): List<SharedTxtTocRule> {
        save(libraryStore.loadDataSnapshot().txtTocRules.filterNot { it.id == id })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedTxtTocRule> {
        val imported = decodeRules(rawJson)
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

    private fun decodeRules(rawJson: String): List<SharedTxtTocRule> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "TXT TOC rule JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val rules = if (element is JsonArray) {
            json.decodeFromString<List<SharedTxtTocRule>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedTxtTocRule>(trimmed))
        }.filter { it.name.isNotBlank() && it.rule.isNotBlank() }
        require(rules.isNotEmpty()) { "TXT TOC rule JSON does not contain a usable rule" }
        return rules
    }

    private fun save(rules: List<SharedTxtTocRule>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(txtTocRules = rules.sortedForUse()))
    }

    private fun List<SharedTxtTocRule>.sortedForUse(): List<SharedTxtTocRule> {
        return sortedWith(compareBy<SharedTxtTocRule> { it.serialNumber }.thenBy { it.id }.thenBy { it.name })
    }

    private fun nextId(rules: List<SharedTxtTocRule>): Long {
        return (rules.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    private fun nextOrder(rules: List<SharedTxtTocRule>): Int {
        return (rules.maxOfOrNull { it.serialNumber } ?: 0) + 1
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

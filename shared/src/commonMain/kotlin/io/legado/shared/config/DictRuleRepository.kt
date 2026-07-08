package io.legado.shared.config

import io.legado.shared.model.SharedDictRule
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class DictRuleRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedDictRule> {
        return libraryStore.loadDataSnapshot().dictRules.sortedForUse()
    }

    fun upsert(rule: SharedDictRule): SharedDictRule {
        val rules = libraryStore.loadDataSnapshot().dictRules.toMutableList()
        val saved = rule.copy(sortNumber = rule.sortNumber.takeUnless { it == 0 } ?: nextOrder(rules))
        val index = rules.indexOfFirst { it.name == saved.name }
        if (index >= 0) {
            rules[index] = saved
        } else {
            rules.add(saved)
        }
        save(rules)
        return saved
    }

    fun setEnabled(name: String, enabled: Boolean): SharedDictRule? {
        val rules = libraryStore.loadDataSnapshot().dictRules.toMutableList()
        val index = rules.indexOfFirst { it.name == name }
        if (index < 0) {
            return null
        }
        val updated = rules[index].copy(enabled = enabled)
        rules[index] = updated
        save(rules)
        return updated
    }

    fun delete(name: String): List<SharedDictRule> {
        save(libraryStore.loadDataSnapshot().dictRules.filterNot { it.name == name })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedDictRule> {
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

    private fun decodeRules(rawJson: String): List<SharedDictRule> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Dictionary rule JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val rules = if (element is JsonArray) {
            json.decodeFromString<List<SharedDictRule>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedDictRule>(trimmed))
        }.filter { it.name.isNotBlank() && it.urlRule.isNotBlank() }
        require(rules.isNotEmpty()) { "Dictionary rule JSON does not contain a usable rule" }
        return rules
    }

    private fun save(rules: List<SharedDictRule>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(dictRules = rules.sortedForUse()))
    }

    private fun List<SharedDictRule>.sortedForUse(): List<SharedDictRule> {
        return sortedWith(compareBy<SharedDictRule> { it.sortNumber }.thenBy { it.name })
    }

    private fun nextOrder(rules: List<SharedDictRule>): Int {
        return (rules.maxOfOrNull { it.sortNumber } ?: 0) + 1
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

package io.legado.shared.replacement

import io.legado.shared.model.SharedReplaceRule
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class ReplacementRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedReplaceRule> {
        return libraryStore.loadDataSnapshot().replaceRules.sortedForUse()
    }

    fun upsert(rule: SharedReplaceRule): SharedReplaceRule {
        val rules = libraryStore.loadDataSnapshot().replaceRules.toMutableList()
        val saved = rule.normalizedForSave(rules)
        val index = rules.indexOfFirst { it.id == saved.id }
        if (index >= 0) {
            rules[index] = saved
        } else {
            rules.add(saved)
        }
        save(rules)
        return saved
    }

    fun setEnabled(id: Long, enabled: Boolean): SharedReplaceRule? {
        val rules = libraryStore.loadDataSnapshot().replaceRules.toMutableList()
        val index = rules.indexOfFirst { it.id == id }
        if (index < 0) {
            return null
        }
        val updated = rules[index].copy(isEnabled = enabled)
        rules[index] = updated
        save(rules)
        return updated
    }

    fun delete(id: Long): List<SharedReplaceRule> {
        save(libraryStore.loadDataSnapshot().replaceRules.filterNot { it.id == id })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedReplaceRule> {
        val imported = decodeRules(rawJson)
        if (replace) {
            save(imported.mapIndexed { index, rule ->
                rule.copy(order = rule.order.takeUnless { it == Int.MIN_VALUE } ?: index)
            })
            return list()
        }
        imported.forEach(::upsert)
        return list()
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    private fun decodeRules(rawJson: String): List<SharedReplaceRule> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Replace rule JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val elements = when (element) {
            is JsonArray -> element.toList()
            else -> listOf(element)
        }
        val rules = elements.mapNotNull { item ->
            runCatching {
                json.decodeFromJsonElement<SharedReplaceRule>(item.withOrderAlias())
            }.getOrNull()
        }.filter { it.pattern.isNotBlank() }
        require(rules.isNotEmpty()) { "Replace rule JSON does not contain a usable rule" }
        return rules
    }

    private fun JsonElement.withOrderAlias(): JsonElement {
        if (this !is JsonObject || "order" in this || "sortOrder" !in this) {
            return this
        }
        return JsonObject(this + ("order" to this.getValue("sortOrder")))
    }

    private fun SharedReplaceRule.normalizedForSave(existing: List<SharedReplaceRule>): SharedReplaceRule {
        val savedId = id.takeUnless { it == 0L } ?: nextId(existing)
        val savedOrder = order.takeUnless { it == Int.MIN_VALUE } ?: nextOrder(existing)
        return copy(
            id = savedId,
            name = name.ifBlank { pattern },
            order = savedOrder
        )
    }

    private fun save(rules: List<SharedReplaceRule>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(replaceRules = rules.sortedForUse()))
    }

    private fun List<SharedReplaceRule>.sortedForUse(): List<SharedReplaceRule> {
        return sortedWith(compareBy<SharedReplaceRule> { it.order }.thenBy { it.name }.thenBy { it.id })
    }

    private fun nextId(rules: List<SharedReplaceRule>): Long {
        return (rules.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    private fun nextOrder(rules: List<SharedReplaceRule>): Int {
        return (rules.maxOfOrNull { it.order.takeUnless { order -> order == Int.MIN_VALUE } ?: -1 } ?: -1) + 1
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

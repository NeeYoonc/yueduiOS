package io.legado.shared.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

internal object SimpleJsonPath {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(body: String): JsonElement? {
        return runCatching { json.parseToJsonElement(body) }.getOrNull()
    }

    fun select(root: JsonElement, rawPath: String): List<JsonElement> {
        val path = rawPath.trim().substringBefore("@")
        if (path == "$") {
            return listOf(root)
        }
        val parts = path.removePrefix("$.")
            .removePrefix("$")
            .split('.')
            .filter { it.isNotBlank() }
        if (parts.isEmpty()) {
            return listOf(root)
        }
        return parts.fold(listOf(root)) { current, part ->
            current.flatMap { element ->
                when (element) {
                    is JsonObject -> listOfNotNull(element[part])
                    is JsonArray -> element.mapNotNull { child ->
                        child.jsonObject[part]
                    }
                    else -> emptyList()
                }
            }
        }
    }

    fun elements(root: JsonElement, rawPath: String): List<JsonElement> {
        return select(root, rawPath)
            .flatMap { element ->
                if (element is JsonArray) {
                    element.toList()
                } else {
                    listOf(element)
                }
            }
    }

    fun text(root: JsonElement, rule: String?): String? {
        return texts(root, rule).firstOrNull()
    }

    fun texts(root: JsonElement, rule: String?): List<String> {
        val path = rule?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.substringBefore("@")
            ?: return emptyList()
        return elements(root, path)
            .map { it.textValue().trim() }
            .filter { it.isNotEmpty() }
    }

    private fun JsonElement.textValue(): String {
        return when (this) {
            is JsonPrimitive -> contentOrNull ?: toString()
            JsonNull -> ""
            else -> toString()
        }
    }
}

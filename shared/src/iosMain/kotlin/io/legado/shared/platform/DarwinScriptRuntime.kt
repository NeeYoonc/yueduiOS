package io.legado.shared.platform

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import platform.JavaScriptCore.JSContext

class DarwinScriptRuntime : ScriptRuntime {
    override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
        val context = JSContext()
        val bootstrap = bindings.entries.joinToString(separator = "\n") { (key, value) ->
            "var ${key.toSafeIdentifier()} = ${value.toJavaScriptLiteral()};"
        }
        val result = context.evaluateScript(
            listOf(bootstrap, script)
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n")
        )
        return result?.toString()
    }

    private fun String.toSafeIdentifier(): String {
        val candidate = replace(Regex("[^A-Za-z0-9_$]"), "_")
        return if (candidate.firstOrNull()?.let { it == '_' || it == '$' || it.isLetter() } == true) {
            candidate
        } else {
            "_$candidate"
        }
    }

    private fun Any?.toJavaScriptLiteral(): String {
        return when (this) {
            null -> "null"
            is Boolean -> if (this) "true" else "false"
            is Number -> toString()
            is String -> Json.encodeToString(JsonPrimitive(this))
            is Map<*, *> -> JsonObject(
                entries.associate { (key, value) ->
                    key.toString() to value.toJsonElement()
                }
            ).toString()
            is Iterable<*> -> JsonArray(map { it.toJsonElement() }).toString()
            else -> Json.encodeToString(JsonPrimitive(toString()))
        }
    }

    private fun Any?.toJsonElement(): JsonElement {
        return when (this) {
            null -> JsonNull
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is Map<*, *> -> JsonObject(
                entries.associate { (key, value) ->
                    key.toString() to value.toJsonElement()
                }
            )
            is Iterable<*> -> JsonArray(map { it.toJsonElement() })
            else -> JsonPrimitive(toString())
        }
    }
}

package io.legado.shared.service

import io.legado.shared.platform.SharedHttpMethod
import io.legado.shared.platform.SharedHttpRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SharedRequestBuilder {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val optionSeparator = Regex(""",\s*(?=\{)""")

    fun build(
        template: String,
        context: SharedRequestContext = SharedRequestContext()
    ): SharedHttpRequest {
        val (urlTemplate, optionTemplate) = splitTemplate(template)
        val url = replaceUrlPlaceholders(urlTemplate.trim(), context)
        val option = optionTemplate
            ?.let { replaceOptionPlaceholders(it, context) }
            ?.let { runCatching { json.decodeFromString(UrlOption.serializer(), it) }.getOrNull() }
        return SharedHttpRequest(
            url = url,
            method = option?.method.toHttpMethod(),
            headers = option?.headers.headersMap(option?.body),
            body = option?.body?.toBodyString()
        )
    }

    private fun splitTemplate(template: String): Pair<String, String?> {
        val match = optionSeparator.find(template)
        return if (match == null) {
            template to null
        } else {
            template.substring(0, match.range.first) to template.substring(match.range.last + 1)
        }
    }

    private fun replaceUrlPlaceholders(value: String, context: SharedRequestContext): String {
        return value
            .replace("{{key}}", context.encodedKey)
            .replace("{{searchKey}}", context.encodedKey)
            .replace("searchKey", context.encodedKey)
            .replace("{{page}}", context.pageText)
            .replace("{{searchPage}}", context.pageText)
            .replace("searchPage", context.pageText)
    }

    private fun replaceOptionPlaceholders(value: String, context: SharedRequestContext): String {
        return value
            .replace("{{key}}", context.jsonEscapedKey)
            .replace("{{searchKey}}", context.jsonEscapedKey)
            .replace("searchKey", context.jsonEscapedKey)
            .replace("{{page}}", context.pageText)
            .replace("{{searchPage}}", context.pageText)
            .replace("searchPage", context.pageText)
    }

    private fun String?.toHttpMethod(): SharedHttpMethod {
        return when (this?.uppercase()) {
            "POST" -> SharedHttpMethod.POST
            "HEAD" -> SharedHttpMethod.HEAD
            "PUT" -> SharedHttpMethod.PUT
            "DELETE" -> SharedHttpMethod.DELETE
            "PROPFIND" -> SharedHttpMethod.PROPFIND
            else -> SharedHttpMethod.GET
        }
    }

    private fun JsonElement?.headersMap(body: JsonElement?): Map<String, String> {
        val headers = (this as? JsonObject)
            ?.entries
            ?.associate { (key, value) ->
                key to value.asHeaderValue()
            }
            .orEmpty()
        if (body !is JsonObject || headers.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
            return headers
        }
        return headers + ("Content-Type" to "application/json")
    }

    private fun JsonElement.asHeaderValue(): String {
        return jsonPrimitiveOrNull()?.contentOrNull ?: toString()
    }

    private fun JsonElement.toBodyString(): String {
        return jsonPrimitiveOrNull()?.contentOrNull ?: toString()
    }

    private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? {
        return runCatching { jsonPrimitive }.getOrNull()
    }

    private fun encodeQueryValue(value: String): String =
        value.encodeToByteArray().joinToString("") { byte ->
            val code = byte.toInt() and 0xff
            val char = code.toChar()
            when {
                char in 'a'..'z' -> char.toString()
                char in 'A'..'Z' -> char.toString()
                char in '0'..'9' -> char.toString()
                char == '-' || char == '_' || char == '.' || char == '~' -> char.toString()
                char == ' ' -> "%20"
                else -> "%" + code.toString(16).uppercase().padStart(2, '0')
            }
        }

    private fun escapeJsonStringContent(value: String): String {
        return JsonPrimitive(value).toString().removeSurrounding("\"")
    }

    @Serializable
    private data class UrlOption(
        val method: String? = null,
        val headers: JsonElement? = null,
        val body: JsonElement? = null
    )

    data class SharedRequestContext(
        val key: String = "",
        val page: Int = 1
    ) {
        val encodedKey: String = encodeQueryValue(key)
        val jsonEscapedKey: String = escapeJsonStringContent(key)
        val pageText: String = page.toString()
    }
}

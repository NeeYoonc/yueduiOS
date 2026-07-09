package io.legado.shared.source

import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.CookieStorePort
import io.legado.shared.service.SharedUrlResolver
import io.legado.shared.service.SourceRequestFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class SharedSourceLoginRequest(
    val url: String = "",
    val headers: Map<String, String> = emptyMap()
)

@Serializable
data class SharedLoginUiField(
    val name: String = "",
    val type: String = "text",
    val viewName: String? = null,
    val action: String? = null,
    val chars: List<String> = emptyList(),
    val default: String? = null
)

class SourceLoginService(
    cookieStore: CookieStorePort? = null
) {
    private val requestFactory = SourceRequestFactory(cookieStore)

    fun buildWebLoginRequest(source: SharedBookSource): SharedSourceLoginRequest? {
        val loginUrl = source.loginUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        if (!source.loginUi.isNullOrBlank() || loginUrl.isScriptRule()) {
            return null
        }
        val resolvedUrl = SharedUrlResolver.resolve(source.bookSourceUrl, loginUrl)
        val request = requestFactory.build(source, resolvedUrl)
        return SharedSourceLoginRequest(
            url = request.url,
            headers = request.headers
        )
    }

    fun loadLoginUiFields(source: SharedBookSource): List<SharedLoginUiField> {
        val loginUi = source.loginUi
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        if (loginUi.isScriptRule()) {
            return emptyList()
        }
        return runCatching {
            json.decodeFromString<List<SharedLoginUiField>>(loginUi)
                .map { field ->
                    field.copy(
                        name = field.name.trim(),
                        type = field.type.ifBlank { "text" }.lowercase(),
                        chars = field.chars.filter { it.isNotBlank() }
                    )
                }
                .filter { it.name.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun defaultLoginInfoJson(source: SharedBookSource): String {
        return encodeLoginInfoJson(defaultLoginInfo(source))
    }

    fun defaultLoginInfo(source: SharedBookSource): Map<String, String> {
        return loadLoginUiFields(source)
            .filterNot { it.type == "button" }
            .associate { field -> field.name to field.defaultValue() }
    }

    fun encodeLoginInfoJson(values: Map<String, String>): String {
        return json.encodeToString(values)
    }

    fun decodeLoginInfoJson(rawJson: String): Map<String, String> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return emptyMap()
        }
        return (json.parseToJsonElement(trimmed) as? JsonObject)
            ?.entries
            ?.associate { (key, value) ->
                key to ((value as? JsonPrimitive)?.contentOrNull ?: value.toString())
            }
            .orEmpty()
    }

    private fun SharedLoginUiField.defaultValue(): String {
        return default ?: when (type) {
            "select", "toggle" -> chars.firstOrNull().orEmpty()
            else -> ""
        }
    }

    private fun String.isScriptRule(): Boolean {
        return startsWith("@js:", ignoreCase = true) || startsWith("<js>", ignoreCase = true)
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

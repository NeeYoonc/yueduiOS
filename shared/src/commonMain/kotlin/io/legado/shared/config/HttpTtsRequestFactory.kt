package io.legado.shared.config

import io.legado.shared.model.SharedHttpTts
import io.legado.shared.platform.CookieStorePort
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.service.SharedRequestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class HttpTtsRequestFactory(
    private val cookieStore: CookieStorePort? = null
) {
    fun build(
        engine: SharedHttpTts,
        text: String,
        speechRate: Int
    ): SharedHttpRequest {
        val request = SharedRequestBuilder.build(
            engine.url,
            SharedRequestBuilder.SharedRequestContext(
                key = text,
                speakText = text,
                speakSpeed = speechRate
            )
        )
        val headers = linkedMapOf<String, String>()
        engine.header.headersFromRule().forEach { (name, value) ->
            headers.putCaseInsensitive(name, value)
        }
        request.headers.forEach { (name, value) ->
            headers.putCaseInsensitive(name, value)
        }
        val storedCookie = cookieStore.takeUnless { engine.enabledCookieJar == false }
            ?.getCookie(engine.cookieKey())
            ?.takeIf { it.isNotBlank() }
        if (storedCookie != null && !headers.containsKeyIgnoreCase("Cookie")) {
            headers["Cookie"] = storedCookie
        }
        return request.copy(headers = headers)
    }

    private fun SharedHttpTts.cookieKey(): String = "httpTts:$id"

    private fun String?.headersFromRule(): Map<String, String> {
        val rawHeader = this?.trim()?.takeIf { it.isNotBlank() } ?: return emptyMap()
        if (rawHeader.startsWith("@js:", ignoreCase = true) || rawHeader.startsWith("<js>", ignoreCase = true)) {
            return emptyMap()
        }
        return runCatching {
            (json.parseToJsonElement(rawHeader) as? JsonObject)
                ?.entries
                ?.associate { (key, value) ->
                    key to ((value as? JsonPrimitive)?.contentOrNull ?: value.toString())
                }
                .orEmpty()
        }.getOrDefault(emptyMap())
    }

    private fun MutableMap<String, String>.putCaseInsensitive(name: String, value: String) {
        val existingKey = keys.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existingKey != null && existingKey != name) {
            remove(existingKey)
        }
        this[name] = value
    }

    private fun Map<String, String>.containsKeyIgnoreCase(name: String): Boolean {
        return keys.any { it.equals(name, ignoreCase = true) }
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}

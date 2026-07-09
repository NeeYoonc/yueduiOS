package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.CookieStorePort
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class SourceRequestFactory(
    private val cookieStore: CookieStorePort? = null
) {
    fun build(
        source: SharedBookSource,
        template: String,
        context: SharedRequestBuilder.SharedRequestContext = SharedRequestBuilder.SharedRequestContext()
    ): SharedHttpRequest {
        val request = SharedRequestBuilder.build(template, context)
        val headers = linkedMapOf<String, String>()
        source.headersFromRule().forEach { (name, value) ->
            headers.putCaseInsensitive(name, value)
        }
        request.headers.forEach { (name, value) ->
            headers.putCaseInsensitive(name, value)
        }
        val storedCookie = cookieStore.takeUnless { source.enabledCookieJar == false }
            ?.getCookie(source.bookSourceUrl)
            ?.takeIf { it.isNotBlank() }
        if (storedCookie != null && !headers.containsKeyIgnoreCase("Cookie")) {
            headers["Cookie"] = storedCookie
        }
        return request.copy(headers = headers)
    }

    fun storeResponseCookies(source: SharedBookSource, response: SharedHttpResponse) {
        if (source.enabledCookieJar == false) {
            return
        }
        val setCookie = response.headers.valueIgnoreCase("Set-Cookie") ?: return
        val cookiePair = setCookie
            .split(';')
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.contains('=') }
            ?: return
        val merged = mergeCookie(cookieStore?.getCookie(source.bookSourceUrl), cookiePair)
        cookieStore?.putCookie(source.bookSourceUrl, merged)
    }

    private fun SharedBookSource.headersFromRule(): Map<String, String> {
        val rawHeader = header?.trim()?.takeIf { it.isNotBlank() } ?: return emptyMap()
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

    private fun mergeCookie(existing: String?, newPair: String): String {
        val newName = newPair.substringBefore('=').trim()
        val parts = existing
            ?.split(';')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() && !it.substringBefore('=').trim().equals(newName, ignoreCase = true) }
            .orEmpty()
        return (parts + newPair).joinToString("; ")
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

    private fun Map<String, String>.valueIgnoreCase(name: String): String? {
        return entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}

package io.legado.shared.config

import io.legado.shared.model.SharedCookie
import io.legado.shared.platform.CookieStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class CookieRepository(
    private val libraryStore: SharedLibraryStore
) : CookieStorePort {
    fun list(): List<SharedCookie> {
        return libraryStore.loadDataSnapshot().cookies.sortedForUse()
    }

    fun upsert(cookie: SharedCookie): SharedCookie {
        val cleanUrl = cookie.url.trim()
        require(cleanUrl.isNotEmpty()) { "Cookie URL is empty" }
        val saved = cookie.copy(url = cleanUrl)
        val cookies = libraryStore.loadDataSnapshot().cookies.toMutableList()
        val index = cookies.indexOfFirst { it.url == cleanUrl }
        if (index >= 0) {
            cookies[index] = saved
        } else {
            cookies.add(saved)
        }
        save(cookies)
        return saved
    }

    fun delete(url: String): List<SharedCookie> {
        save(libraryStore.loadDataSnapshot().cookies.filterNot { it.url == url })
        return list()
    }

    fun clear(): List<SharedCookie> {
        save(emptyList())
        return emptyList()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedCookie> {
        val imported = decodeCookies(rawJson)
        if (replace) {
            save(imported)
            return list()
        }
        imported.forEach(::upsert)
        return list()
    }

    fun upsertJson(rawJson: String): SharedCookie {
        val imported = decodeCookies(rawJson)
        require(imported.size == 1) { "Cookie editor JSON must contain exactly one cookie" }
        return upsert(imported.single())
    }

    fun exportJson(): String {
        return json.encodeToString(list())
    }

    override fun getCookie(url: String): String? {
        return list().firstOrNull { it.url == url }?.cookie
    }

    override fun putCookie(url: String, cookie: String) {
        upsert(SharedCookie(url = url, cookie = cookie))
    }

    private fun decodeCookies(rawJson: String): List<SharedCookie> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Cookie JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val cookies = if (element is JsonArray) {
            json.decodeFromString<List<SharedCookie>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedCookie>(trimmed))
        }.map { it.copy(url = it.url.trim()) }
            .filter { it.url.isNotBlank() }
        require(cookies.isNotEmpty()) { "Cookie JSON does not contain a usable cookie" }
        return cookies.sortedForUse()
    }

    private fun save(cookies: List<SharedCookie>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(cookies = cookies.sortedForUse()))
    }

    private fun List<SharedCookie>.sortedForUse(): List<SharedCookie> {
        return sortedBy { it.url }
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

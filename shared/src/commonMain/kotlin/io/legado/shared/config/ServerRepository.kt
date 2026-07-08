package io.legado.shared.config

import io.legado.shared.model.SharedServer
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

class ServerRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun list(): List<SharedServer> {
        return libraryStore.loadDataSnapshot().servers.sortedForUse()
    }

    fun upsert(server: SharedServer): SharedServer {
        val servers = libraryStore.loadDataSnapshot().servers.toMutableList()
        val saved = server.copy(
            id = server.id.takeUnless { it == 0L } ?: nextId(servers),
            sortNumber = server.sortNumber.takeUnless { it == 0 } ?: nextOrder(servers)
        )
        val index = servers.indexOfFirst { it.id == saved.id }
        if (index >= 0) {
            servers[index] = saved
        } else {
            servers.add(saved)
        }
        save(servers)
        return saved
    }

    fun delete(id: Long): List<SharedServer> {
        save(libraryStore.loadDataSnapshot().servers.filterNot { it.id == id })
        return list()
    }

    fun importJson(rawJson: String, replace: Boolean = false): List<SharedServer> {
        val imported = decodeServers(rawJson)
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

    private fun decodeServers(rawJson: String): List<SharedServer> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Server JSON is empty" }
        val element = json.parseToJsonElement(trimmed)
        val servers = if (element is JsonArray) {
            json.decodeFromString<List<SharedServer>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedServer>(trimmed))
        }.filter { it.name.isNotBlank() }
        require(servers.isNotEmpty()) { "Server JSON does not contain a usable server" }
        return servers
    }

    private fun save(servers: List<SharedServer>) {
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(servers = servers.sortedForUse()))
    }

    private fun List<SharedServer>.sortedForUse(): List<SharedServer> {
        return sortedWith(compareBy<SharedServer> { it.sortNumber }.thenBy { it.id }.thenBy { it.name })
    }

    private fun nextId(servers: List<SharedServer>): Long {
        return (servers.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    private fun nextOrder(servers: List<SharedServer>): Int {
        return (servers.maxOfOrNull { it.sortNumber } ?: 0) + 1
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

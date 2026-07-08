package io.legado.shared.source

import io.legado.shared.model.SharedBookSource
import kotlinx.serialization.json.Json

object SourceJsonImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Throws(IllegalArgumentException::class)
    fun importBookSources(rawJson: String): List<SharedBookSource> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Book source JSON is empty" }

        val decoded = if (trimmed.startsWith("[")) {
            json.decodeFromString<List<SharedBookSource>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedBookSource>(trimmed))
        }

        val usable = decoded.filter {
            it.bookSourceUrl.isNotBlank() && it.bookSourceName.isNotBlank()
        }
        require(usable.isNotEmpty()) { "Book source JSON does not contain a usable source" }
        return usable
    }
}

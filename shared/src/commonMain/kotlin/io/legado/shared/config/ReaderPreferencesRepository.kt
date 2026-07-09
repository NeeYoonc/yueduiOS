package io.legado.shared.config

import io.legado.shared.model.SharedReaderPreferences
import io.legado.shared.storage.SharedLibraryStore

class ReaderPreferencesRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun load(): SharedReaderPreferences {
        return libraryStore.loadDataSnapshot().readerPreferences.sanitized()
    }

    fun save(preferences: SharedReaderPreferences): SharedReaderPreferences {
        val sanitized = preferences.sanitized()
        val snapshot = libraryStore.loadDataSnapshot()
        libraryStore.saveDataSnapshot(snapshot.copy(readerPreferences = sanitized))
        return sanitized
    }

    private fun SharedReaderPreferences.sanitized(): SharedReaderPreferences {
        return copy(
            fontSize = fontSize.coerceIn(12.0, 36.0),
            lineSpacing = lineSpacing.coerceIn(0.0, 24.0),
            contentPadding = contentPadding.coerceIn(0.0, 64.0),
            theme = theme.takeIf { it in allowedThemes } ?: "system"
        )
    }

    private companion object {
        val allowedThemes = setOf("system", "light", "dark", "sepia")
    }
}

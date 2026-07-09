package io.legado.shared.config

import io.legado.shared.model.SharedReaderPreferences
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderPreferencesRepositoryTest {
    @Test
    fun loadsDefaultsAndSavesReaderPreferences() {
        val repository = ReaderPreferencesRepository(SharedLibraryStore(InMemoryCacheStore()))

        assertEquals(18.0, repository.load().fontSize)
        assertEquals("system", repository.load().theme)

        val saved = repository.save(
            SharedReaderPreferences(
                fontSize = 22.0,
                lineSpacing = 12.0,
                contentPadding = 28.0,
                theme = "sepia"
            )
        )

        assertEquals(22.0, saved.fontSize)
        assertEquals(saved, repository.load())
    }

    @Test
    fun clampsUnsafeReaderPreferenceValues() {
        val repository = ReaderPreferencesRepository(SharedLibraryStore(InMemoryCacheStore()))

        val saved = repository.save(
            SharedReaderPreferences(
                fontSize = 100.0,
                lineSpacing = -5.0,
                contentPadding = 200.0,
                theme = "unknown"
            )
        )

        assertEquals(36.0, saved.fontSize)
        assertEquals(0.0, saved.lineSpacing)
        assertEquals(64.0, saved.contentPadding)
        assertEquals("system", saved.theme)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

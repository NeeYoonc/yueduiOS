package io.legado.shared.config

import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class RawConfigRepositoryTest {
    @Test
    fun importsListsDeletesAndExportsRawConfigs() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = RawConfigRepository(store)

        val imported = repository.importJson(
            """
            {
              "themeConfig": "{\"theme\":\"dark\"}",
              "readConfig": "{\"fontSize\":20}"
            }
            """.trimIndent()
        )

        assertEquals(listOf("readConfig", "themeConfig"), imported.map { it.key })
        repository.delete("themeConfig")
        assertEquals(listOf("readConfig"), repository.list().map { it.key })
        assertEquals("readConfig", repository.importJson(repository.exportJson(), replace = true).single().key)
    }

    @Test
    fun upsertStoresConfigEntry() {
        val repository = RawConfigRepository(SharedLibraryStore(InMemoryCacheStore()))

        val saved = repository.upsert("coverRule", """{"url":"https://cover.test/{{key}}"}""")

        assertEquals("coverRule", saved.key)
        assertEquals("""{"url":"https://cover.test/{{key}}"}""", repository.list().single().value)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

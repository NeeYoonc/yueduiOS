package io.legado.shared.config

import io.legado.shared.model.SharedCacheEntry
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheEntryRepositoryTest {
    @Test
    fun importsReadsAndClearsExpiredCacheEntries() {
        val repository = CacheEntryRepository(SharedLibraryStore(InMemoryCacheStore()))

        val imported = repository.importJson(
            """
            [
              {"key":"expired","value":"old","deadline":1000},
              {"key":"forever","value":"keep","deadline":0},
              {"key":"future","value":"new","deadline":3000}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("expired", "forever", "future"), imported.map { it.key })
        assertNull(repository.getValue("expired", nowMillis = 2000))
        assertEquals("keep", repository.getValue("forever", nowMillis = 2000))
        assertEquals("new", repository.getValue("future", nowMillis = 2000))

        repository.clearExpired(nowMillis = 2000)
        assertEquals(listOf("forever", "future"), repository.list().map { it.key })
        assertEquals("future", repository.importJson(repository.exportJson(), replace = true).last().key)
    }

    @Test
    fun upsertAndDeleteCacheEntries() {
        val repository = CacheEntryRepository(SharedLibraryStore(InMemoryCacheStore()))

        repository.putValue("chapter:1", "content", deadline = 0)
        assertEquals("content", repository.getValue("chapter:1"))

        repository.upsert(SharedCacheEntry(key = "chapter:1", value = "updated", deadline = 5))
        assertEquals("updated", repository.getValue("chapter:1", nowMillis = 1))

        repository.delete("chapter:1")
        assertEquals(emptyList(), repository.list())
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

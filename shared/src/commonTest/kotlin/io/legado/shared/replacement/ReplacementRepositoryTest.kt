package io.legado.shared.replacement

import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ReplacementRepositoryTest {
    @Test
    fun importsTogglesDeletesAndExportsRules() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = ReplacementRepository(store)

        val imported = repository.importJson(
            """
            [
              {"id": 2, "name": "B", "pattern": "b", "replacement": "B", "sortOrder": 2},
              {"id": 1, "name": "A", "pattern": "a", "replacement": "A", "order": 1}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("A", "B"), imported.map { it.name })
        repository.setEnabled(1, false)
        assertFalse(repository.list().first { it.id == 1L }.isEnabled)
        repository.delete(2)
        assertEquals(listOf("A"), repository.list().map { it.name })
        assertEquals("A", repository.importJson(repository.exportJson(), replace = true).single().name)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

package io.legado.shared.config

import io.legado.shared.model.SharedTxtTocRule
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TxtTocRuleRepositoryTest {
    @Test
    fun importsTogglesDeletesAndExportsTxtTocRules() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = TxtTocRuleRepository(store)

        val imported = repository.importJson(
            """
            [
              {"id":2,"name":"B","rule":"^B$","serialNumber":2},
              {"id":1,"name":"A","rule":"^A$","serialNumber":1}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("A", "B"), imported.map { it.name })
        repository.setEnabled(1, false)
        assertFalse(repository.list().first { it.id == 1L }.enable)
        repository.delete(2)
        assertEquals(listOf("A"), repository.list().map { it.name })
        assertEquals("A", repository.importJson(repository.exportJson(), replace = true).single().name)
    }

    @Test
    fun upsertAssignsIdAndOrderForNewRules() {
        val repository = TxtTocRuleRepository(SharedLibraryStore(InMemoryCacheStore()))

        val saved = repository.upsert(
            SharedTxtTocRule(
                name = "Chapter",
                rule = """^Chapter\s+\d+"""
            )
        )

        assertEquals(1L, saved.id)
        assertEquals(1, saved.serialNumber)
        assertEquals(listOf(saved), repository.list())
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

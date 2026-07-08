package io.legado.shared.config

import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DictRuleRepositoryTest {
    @Test
    fun importsTogglesDeletesAndExportsDictRules() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = DictRuleRepository(store)

        val imported = repository.importJson(
            """
            [
              {"name":"B","urlRule":"https://b/{{key}}","showRule":"$.b","sortNumber":2},
              {"name":"A","urlRule":"https://a/{{key}}","showRule":"$.a","sortNumber":1}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("A", "B"), imported.map { it.name })
        repository.setEnabled("A", false)
        assertFalse(repository.list().first { it.name == "A" }.enabled)
        repository.delete("B")
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

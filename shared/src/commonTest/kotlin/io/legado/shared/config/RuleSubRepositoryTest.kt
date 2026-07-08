package io.legado.shared.config

import io.legado.shared.model.SharedRuleSub
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RuleSubRepositoryTest {
    @Test
    fun importsTogglesDeletesAndExportsRuleSubscriptions() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = RuleSubRepository(store)

        val imported = repository.importJson(
            """
            [
              {"id":2,"name":"B","url":"https://b.test/sub.json","customOrder":2,"autoUpdate":true},
              {"id":1,"name":"A","url":"https://a.test/sub.json","customOrder":1,"autoUpdate":true}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("A", "B"), imported.map { it.name })
        repository.setAutoUpdate(1, false)
        assertFalse(repository.list().first { it.id == 1L }.autoUpdate)
        repository.delete(2)
        assertEquals(listOf("A"), repository.list().map { it.name })
        assertEquals("A", repository.importJson(repository.exportJson(), replace = true).single().name)
    }

    @Test
    fun upsertAssignsIdAndOrderForNewSubscriptions() {
        val repository = RuleSubRepository(SharedLibraryStore(InMemoryCacheStore()))

        val saved = repository.upsert(
            SharedRuleSub(
                name = "Sources",
                url = "https://sub.test/sources.json"
            )
        )

        assertEquals(1L, saved.id)
        assertEquals(1, saved.customOrder)
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

package io.legado.shared.config

import io.legado.shared.model.SharedKeyboardAssist
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardAssistRepositoryTest {
    @Test
    fun importsDeletesAndExportsKeyboardAssists() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = KeyboardAssistRepository(store)

        val imported = repository.importJson(
            """
            [
              {"type":1,"key":"next","value":"Next","serialNo":2},
              {"type":1,"key":"prev","value":"Previous","serialNo":1}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("prev", "next"), imported.map { it.key })
        repository.delete(type = 1, key = "next")
        assertEquals(listOf("prev"), repository.list().map { it.key })
        assertEquals("prev", repository.importJson(repository.exportJson(), replace = true).single().key)
    }

    @Test
    fun upsertAssignsOrderForNewAssist() {
        val repository = KeyboardAssistRepository(SharedLibraryStore(InMemoryCacheStore()))

        val saved = repository.upsert(
            SharedKeyboardAssist(
                type = 2,
                key = "menu",
                value = "Menu"
            )
        )

        assertEquals(1, saved.serialNo)
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

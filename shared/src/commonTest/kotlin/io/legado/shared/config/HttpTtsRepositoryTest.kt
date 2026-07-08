package io.legado.shared.config

import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpTtsRepositoryTest {
    @Test
    fun importsDeletesAndExportsHttpTtsEngines() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = HttpTtsRepository(store)

        val imported = repository.importJson(
            """
            [
              {"id": 2, "name":"B","url":"https://b/{{content}}"},
              {"id": 1, "name":"A","url":"https://a/{{content}}"}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("A", "B"), imported.map { it.name })
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

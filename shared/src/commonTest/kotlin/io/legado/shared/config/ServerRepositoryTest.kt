package io.legado.shared.config

import io.legado.shared.model.SharedServer
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerRepositoryTest {
    @Test
    fun importsDeletesAndExportsServers() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = ServerRepository(store)

        val imported = repository.importJson(
            """
            [
              {"id":2,"name":"B","type":"WEBDAV","config":"{\"url\":\"https://b.test/dav/\"}","sortNumber":2},
              {"id":1,"name":"A","type":"WEBDAV","config":"{\"url\":\"https://a.test/dav/\"}","sortNumber":1}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("A", "B"), imported.map { it.name })
        repository.delete(2)
        assertEquals(listOf("A"), repository.list().map { it.name })
        assertEquals("A", repository.importJson(repository.exportJson(), replace = true).single().name)
    }

    @Test
    fun upsertAssignsIdAndOrderForNewServers() {
        val repository = ServerRepository(SharedLibraryStore(InMemoryCacheStore()))

        val saved = repository.upsert(
            SharedServer(
                name = "WebDAV",
                type = "WEBDAV",
                config = """{"url":"https://dav.test/","username":"u","password":"p"}"""
            )
        )

        assertEquals(1L, saved.id)
        assertEquals(1, saved.sortNumber)
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

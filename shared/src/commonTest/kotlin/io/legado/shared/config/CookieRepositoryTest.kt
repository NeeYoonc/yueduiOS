package io.legado.shared.config

import io.legado.shared.model.SharedCookie
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CookieRepositoryTest {
    @Test
    fun importsUpsertsDeletesAndExportsCookies() {
        val repository = CookieRepository(SharedLibraryStore(InMemoryCacheStore()))

        val imported = repository.importJson(
            """
            [
              {"url":"https://b.test","cookie":"b=2"},
              {"url":"https://a.test","cookie":"a=1"}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("https://a.test", "https://b.test"), imported.map { it.url })
        assertEquals("a=1", repository.getCookie("https://a.test"))

        repository.upsert(SharedCookie(url = "https://a.test", cookie = "a=updated; theme=dark"))
        assertEquals("a=updated; theme=dark", repository.getCookie("https://a.test"))

        repository.delete("https://b.test")
        assertEquals(listOf("https://a.test"), repository.list().map { it.url })
        assertEquals("https://a.test", repository.importJson(repository.exportJson(), replace = true).single().url)
    }

    @Test
    fun cookieStorePortWritesAndClearsCookies() {
        val repository = CookieRepository(SharedLibraryStore(InMemoryCacheStore()))

        repository.putCookie("https://source.test", "token=abc")
        assertEquals("token=abc", repository.getCookie("https://source.test"))

        repository.clear()
        assertNull(repository.getCookie("https://source.test"))
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

package io.legado.shared.source

import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceRepositoryTest {
    @Test
    fun upsertsTogglesDeletesAndExportsSources() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = SourceRepository(store)
        val first = SharedBookSource(
            bookSourceUrl = "https://one.test",
            bookSourceName = "One",
            customOrder = 2
        )
        val second = SharedBookSource(
            bookSourceUrl = "https://two.test",
            bookSourceName = "Two",
            customOrder = 1
        )

        repository.upsert(first)
        repository.upsert(second)
        repository.upsert(first.copy(bookSourceName = "One Updated"))
        repository.setEnabled("https://two.test", false)

        assertEquals(listOf("Two", "One Updated"), repository.list().map { it.bookSourceName })
        assertEquals(false, repository.list().first().enabled)

        val exported = repository.exportJson()
        assertEquals(true, exported.contains("One Updated"))

        repository.delete("https://two.test")
        assertEquals(listOf("One Updated"), repository.list().map { it.bookSourceName })
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

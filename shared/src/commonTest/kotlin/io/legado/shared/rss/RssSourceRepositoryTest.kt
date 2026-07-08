package io.legado.shared.rss

import io.legado.shared.model.SharedRssSource
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RssSourceRepositoryTest {
    @Test
    fun importsTogglesDeletesAndExportsRssSources() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = RssSourceRepository(store)

        val imported = repository.importJson(
            """
            [
              {"sourceUrl":"https://b.test/feed","sourceName":"B","customOrder":2},
              {"sourceUrl":"https://a.test/feed","sourceName":"A","customOrder":1}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("A", "B"), imported.map { it.sourceName })
        repository.setEnabled("https://a.test/feed", false)
        assertFalse(repository.list().first { it.sourceUrl == "https://a.test/feed" }.enabled)
        repository.delete("https://b.test/feed")
        assertEquals(listOf("A"), repository.list().map { it.sourceName })
        assertEquals("A", repository.importJson(repository.exportJson(), replace = true).single().sourceName)
    }

    @Test
    fun upsertAssignsOrderForNewSources() {
        val repository = RssSourceRepository(SharedLibraryStore(InMemoryCacheStore()))

        val saved = repository.upsert(
            SharedRssSource(
                sourceUrl = "https://rss.test/feed",
                sourceName = "Feed"
            )
        )

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

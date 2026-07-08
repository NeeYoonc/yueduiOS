package io.legado.shared.replacement

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedReplaceRule
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplacementServiceTest {
    @Test
    fun appliesEnabledRegexAndLiteralRulesInOrder() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        store.saveDataSnapshot(
            store.loadDataSnapshot().copy(
                replaceRules = listOf(
                    SharedReplaceRule(id = 1, name = "regex", pattern = """bad\s+word""", replacement = "clean", order = 1),
                    SharedReplaceRule(id = 2, name = "literal", pattern = "foo", replacement = "bar", isRegex = false, order = 2)
                )
            )
        )
        val service = ReplacementService(store)
        val book = SharedBook(name = "Book", origin = "source")
        val chapter = SharedBookChapter(title = "One")

        val result = service.applyToChapterContent(
            book,
            chapter,
            SharedChapterContent(content = "foo bad word")
        )

        assertEquals("bar clean", result.content)
    }

    @Test
    fun respectsScopeExcludeScopeAndDisabledRules() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        store.saveDataSnapshot(
            store.loadDataSnapshot().copy(
                replaceRules = listOf(
                    SharedReplaceRule(id = 1, name = "scoped", pattern = "A", replacement = "B", scope = "Book"),
                    SharedReplaceRule(id = 2, name = "excluded", pattern = "B", replacement = "C", excludeScope = "source"),
                    SharedReplaceRule(id = 3, name = "disabled", pattern = "B", replacement = "D", isEnabled = false)
                )
            )
        )
        val service = ReplacementService(store)

        val result = service.applyToChapterContent(
            SharedBook(name = "Book", origin = "source"),
            SharedBookChapter(title = "One"),
            SharedChapterContent(content = "A")
        )

        assertEquals("B", result.content)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

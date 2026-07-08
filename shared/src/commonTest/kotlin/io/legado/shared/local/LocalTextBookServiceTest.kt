package io.legado.shared.local

import io.legado.shared.model.SharedTxtTocRule
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalTextBookServiceTest {
    @Test
    fun importsTextFileSplitsChaptersAndCachesContent() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        store.saveDataSnapshot(
            store.loadDataSnapshot().copy(
                txtTocRules = listOf(
                    SharedTxtTocRule(
                        id = 1,
                        name = "Chapter",
                        rule = """(?m)^Chapter\s+\d+.*$""",
                        enable = true
                    )
                )
            )
        )
        val service = LocalTextBookService(store)

        val result = service.importTextFile(
            fileName = "Novel by Tester.txt",
            text = """
                Preface text.

                Chapter 1 Start
                Body one.

                Chapter 2 Next
                Body two.
            """.trimIndent(),
            nowMillis = 123L
        )

        assertEquals("Novel", result.book.name)
        assertEquals("Tester", result.book.author)
        assertEquals(LocalTextBookService.LOCAL_ORIGIN, result.book.origin)
        assertEquals(3, result.book.totalChapterNum)
        assertEquals(listOf("Preface", "Chapter 1 Start", "Chapter 2 Next"), result.chapters.map { it.title })
        assertEquals("Body one.", store.loadChapterContent(result.book, result.chapters[1])?.content?.trim())
        assertEquals(listOf("Novel"), store.loadBooks().map { it.name })
        assertTrue(store.loadBookChapters(result.book).isNotEmpty())
    }

    @Test
    fun importsWholeFileAsSingleChapterWhenNoRuleMatches() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val service = LocalTextBookService(store)

        val result = service.importTextFile(
            fileName = "Notes.txt",
            text = "Only body text.",
            nowMillis = 456L
        )

        assertEquals("Notes", result.book.name)
        assertEquals(listOf("Notes"), result.chapters.map { it.title })
        assertEquals("Only body text.", store.loadChapterContent(result.book, result.chapters.single())?.content)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

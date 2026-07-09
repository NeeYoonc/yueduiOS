package io.legado.shared.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderSearchServiceTest {
    @Test
    fun findsCaseInsensitiveMatchesWithContext() {
        val service = ReaderSearchService()

        val results = service.search(
            content = "Alpha metal line\nSecond METAL chapter\nNo hit",
            query = "metal",
            contextChars = 6
        )

        assertEquals(2, results.size)
        assertEquals(6, results.first().startIndex)
        assertEquals(11, results.first().endIndex)
        assertEquals("Alpha metal line", results.first().snippet)
        assertEquals("econd METAL chapt", results[1].snippet)
    }

    @Test
    fun returnsEmptyForBlankQuery() {
        val service = ReaderSearchService()

        assertEquals(emptyList(), service.search("content", "  "))
    }
}

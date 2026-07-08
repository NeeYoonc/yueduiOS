package io.legado.shared.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun decodesBookSourceUsingLegadoFieldNames() {
        val source = json.decodeFromString<SharedBookSource>(
            """
            {
              "bookSourceUrl": "https://example.test",
              "bookSourceName": "Example Source",
              "bookSourceGroup": "text",
              "searchUrl": "https://example.test/search?q={{key}}",
              "ruleSearch": {
                "bookList": ".result",
                "name": ".name",
                "author": ".author",
                "bookUrl": "a@href"
              }
            }
            """.trimIndent()
        )

        assertEquals("https://example.test", source.bookSourceUrl)
        assertEquals("Example Source", source.bookSourceName)
        assertEquals(".result", source.ruleSearch?.bookList)
        assertEquals("a@href", source.ruleSearch?.bookUrl)
    }

    @Test
    fun encodesChapterWithoutPlatformAnnotations() {
        val encoded = json.encodeToString(
            SharedBookChapter(
                title = "Chapter 1",
                url = "https://example.test/chapter-1",
                index = 0
            )
        )

        assertTrue(encoded.contains("Chapter 1"))
        assertTrue(encoded.contains("chapter-1"))
    }
}

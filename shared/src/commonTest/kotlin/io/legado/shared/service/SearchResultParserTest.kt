package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultParserTest {

    private val source = SharedBookSource(
        bookSourceUrl = "https://source.test",
        bookSourceName = "Source"
    )

    @Test
    fun parsesBlankLineSeparatedSearchBooks() {
        val books = LineSearchResultParser.parse(
            source,
            """
            name=Metal Max
            author=Crea-Tech
            kind=RPG
            lastChapter=Chapter 3
            intro=Tank story
            coverUrl=https://source.test/cover.jpg
            url=https://source.test/book/1

            name=Metal Saga
            author=Success
            latestChapterTitle=Final
            bookUrl=https://source.test/book/2
            """.trimIndent()
        )

        assertEquals(2, books.size)
        assertEquals("Metal Max", books[0].name)
        assertEquals("Crea-Tech", books[0].author)
        assertEquals("RPG", books[0].kind)
        assertEquals("Chapter 3", books[0].latestChapterTitle)
        assertEquals("Tank story", books[0].intro)
        assertEquals("https://source.test/cover.jpg", books[0].coverUrl)
        assertEquals("https://source.test/book/1", books[0].bookUrl)
        assertEquals("https://source.test", books[0].origin)
        assertEquals("Metal Saga", books[1].name)
        assertEquals("Final", books[1].latestChapterTitle)
        assertEquals("https://source.test/book/2", books[1].bookUrl)
    }

    @Test
    fun ignoresBlocksWithoutNameOrUrlAndKeepsValuesContainingEquals() {
        val books = LineSearchResultParser.parse(
            source,
            """
            author=No title

            name=Encoded
            intro=a=b=c
            """.trimIndent()
        )

        assertEquals(1, books.size)
        assertEquals("Encoded", books.single().name)
        assertEquals("a=b=c", books.single().intro)
    }
}

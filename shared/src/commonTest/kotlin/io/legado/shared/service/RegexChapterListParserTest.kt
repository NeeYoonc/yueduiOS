package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedTocRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegexChapterListParserTest {
    @Test
    fun parsesChaptersFromRegexTocRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://example.test",
            bookSourceName = "Example",
            ruleToc = SharedTocRule(
                chapterList = """<li data-vip="([^"]+)" data-pay="([^"]+)">\s*<a href="([^"]+)">([^<]+)</a>\s*<span>([^<]+)</span>\s*<em>([^<]+)</em>\s*</li>""",
                chapterName = "$4",
                chapterUrl = "$3",
                updateTime = "$5",
                isVip = "$1",
                isPay = "$2",
                isVolume = "$6"
            )
        )
        val body = """
            <ol>
              <li data-vip="true" data-pay="false"><a href="/book/1.html">Chapter 1</a><span>2026-07-01</span><em>false</em></li>
              <li data-vip="false" data-pay="true"><a href="/book/2.html">Volume 1</a><span>2026-07-02</span><em>true</em></li>
            </ol>
        """.trimIndent()

        val chapters = RegexChapterListParser.parse(source, body)

        assertEquals(2, chapters.size)
        assertEquals("Chapter 1", chapters[0].title)
        assertEquals("/book/1.html", chapters[0].url)
        assertEquals(0, chapters[0].index)
        assertEquals("2026-07-01", chapters[0].tag)
        assertTrue(chapters[0].isVip)
        assertFalse(chapters[0].isPay)
        assertFalse(chapters[0].isVolume)
        assertEquals("Volume 1", chapters[1].title)
        assertEquals("/book/2.html", chapters[1].url)
        assertEquals(1, chapters[1].index)
        assertEquals("2026-07-02", chapters[1].tag)
        assertFalse(chapters[1].isVip)
        assertTrue(chapters[1].isPay)
        assertTrue(chapters[1].isVolume)
    }

    @Test
    fun parsesChaptersFromJsonPathTocRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://api.example.test",
            bookSourceName = "JSON",
            ruleToc = SharedTocRule(
                chapterList = "$.content.content",
                chapterName = "$.chapterTitle",
                chapterUrl = "$.id@js:'ignored'",
                updateTime = "$.updated",
                isVip = "$.vip",
                isPay = "$.pay",
                isVolume = "$.volume"
            )
        )
        val body = """
            {"content":{"content":[
              {"id":"chapter-1","chapterTitle":"Chapter 1","updated":"2026-07-01","vip":false,"pay":true,"volume":false},
              {"id":"chapter-2","chapterTitle":"Volume 1","updated":"2026-07-02","vip":true,"pay":false,"volume":true}
            ]}}
        """.trimIndent()

        val chapters = RegexChapterListParser.parse(source, body)

        assertEquals(2, chapters.size)
        assertEquals("Chapter 1", chapters[0].title)
        assertEquals("chapter-1", chapters[0].url)
        assertEquals(0, chapters[0].index)
        assertEquals("2026-07-01", chapters[0].tag)
        assertFalse(chapters[0].isVip)
        assertTrue(chapters[0].isPay)
        assertFalse(chapters[0].isVolume)
        assertEquals("Volume 1", chapters[1].title)
        assertEquals("chapter-2", chapters[1].url)
        assertEquals(1, chapters[1].index)
        assertEquals("2026-07-02", chapters[1].tag)
        assertTrue(chapters[1].isVip)
        assertFalse(chapters[1].isPay)
        assertTrue(chapters[1].isVolume)
    }

    @Test
    fun returnsEmptyListWhenTocRuleIsMissing() {
        val source = SharedBookSource(
            bookSourceUrl = "https://empty.test",
            bookSourceName = "Empty"
        )

        val chapters = RegexChapterListParser.parse(source, "<li>Ignored</li>")

        assertEquals(emptyList(), chapters)
    }
}

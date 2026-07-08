package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import kotlin.test.Test
import kotlin.test.assertEquals

class RegexChapterContentParserTest {
    @Test
    fun parsesContentAndNextContentUrlsFromRegexRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://example.test",
            bookSourceName = "Example",
            ruleContent = SharedContentRule(
                content = """<div id="content">([\s\S]*?)</div>""",
                nextContentUrl = """<a class="next" href="([^"]+)">"""
            )
        )
        val book = SharedBook(
            name = "Metal Story",
            bookUrl = "https://example.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(
            title = "Chapter 1",
            url = "https://example.test/book/1/chapter/1"
        )
        val body = """
            <html>
              <div id="content">
                First line.
                Second line.
              </div>
              <a class="next" href="/chapter/1-2.html">next</a>
            </html>
        """.trimIndent()

        val content = RegexChapterContentParser.parse(source, book, chapter, body)

        assertEquals("First line.\nSecond line.", content.content)
        assertEquals(listOf("/chapter/1-2.html"), content.nextContentUrls)
    }

    @Test
    fun parsesTitleAndSubContentFromRegexRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://metadata.test",
            bookSourceName = "Metadata",
            ruleContent = SharedContentRule(
                content = """<article>([\s\S]*?)</article>""",
                title = """<h1>([^<]+)</h1>""",
                subContent = """<aside class="sub">([\s\S]*?)</aside>"""
            )
        )
        val book = SharedBook(
            name = "Metadata Book",
            bookUrl = "https://metadata.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(
            title = "Original Chapter",
            url = "https://metadata.test/chapter/1"
        )
        val body = """
            <html>
              <h1>Parsed Chapter Title</h1>
              <article>
                Main line.
              </article>
              <aside class="sub">
                Extra line 1.
                Extra line 2.
              </aside>
            </html>
        """.trimIndent()

        val content = RegexChapterContentParser.parse(source, book, chapter, body)

        assertEquals("Parsed Chapter Title", content.title)
        assertEquals("Extra line 1.\nExtra line 2.", content.subContent)
        assertEquals("Main line.", content.content)
    }

    @Test
    fun appliesAndroidStyleReplaceRegexToExtractedContent() {
        val source = SharedBookSource(
            bookSourceUrl = "https://replace.test",
            bookSourceName = "Replace",
            ruleContent = SharedContentRule(
                content = """<article>([\s\S]*?)</article>""",
                replaceRegex = """##<span class="ad">[\s\S]*?</span>##"""
            )
        )
        val book = SharedBook(
            name = "Replace Book",
            bookUrl = "https://replace.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(
            title = "Chapter 1",
            url = "https://replace.test/chapter/1"
        )
        val body = """
            <article>
              First line.
              <span class="ad">remove me</span>
              Second line.
            </article>
        """.trimIndent()

        val content = RegexChapterContentParser.parse(source, book, chapter, body)

        assertEquals("First line.\nSecond line.", content.content)
    }

    @Test
    fun parsesContentFromJsonPathRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://api.example.test",
            bookSourceName = "JSON",
            ruleContent = SharedContentRule(
                content = "$.content.text",
                title = "$.content.title",
                subContent = "$.content.note",
                nextContentUrl = "$.content.next",
                replaceRegex = "##AD LINE##"
            )
        )
        val book = SharedBook(
            name = "JSON Book",
            bookUrl = "https://api.example.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(
            title = "Original",
            url = "https://api.example.test/chapter/1"
        )
        val body = """
            {"content":{
              "title":"JSON Chapter",
              "text":"First line.\nAD LINE\nSecond line.",
              "note":"Side line.",
              "next":["/chapter/1-2","/chapter/1-3"]
            }}
        """.trimIndent()

        val content = RegexChapterContentParser.parse(source, book, chapter, body)

        assertEquals("JSON Chapter", content.title)
        assertEquals("Side line.", content.subContent)
        assertEquals("First line.\nSecond line.", content.content)
        assertEquals(listOf("/chapter/1-2", "/chapter/1-3"), content.nextContentUrls)
    }

    @Test
    fun returnsEmptyContentWhenContentRuleIsMissing() {
        val source = SharedBookSource(
            bookSourceUrl = "https://empty.test",
            bookSourceName = "Empty"
        )
        val book = SharedBook(
            name = "Empty Book",
            bookUrl = "https://empty.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(
            title = "Chapter 1",
            url = "https://empty.test/chapter/1"
        )

        val content = RegexChapterContentParser.parse(source, book, chapter, "<html />")

        assertEquals("", content.content)
        assertEquals(emptyList(), content.nextContentUrls)
    }
}

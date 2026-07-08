package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.model.SharedTocRule
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlRuleParserTest {
    @Test
    fun parsesSearchResultsWithHtmlRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleSearch = SharedSearchRule(
                bookList = ".book",
                name = ".name@text",
                author = ".author@text",
                bookUrl = "a@href",
                coverUrl = "img@src",
                wordCount = ".words@text"
            )
        )
        val body = """
            <div class="book"><a href="/one"><span class="name">One</span></a><span class="author">A</span><span class="words">10K</span><img src="/one.jpg"/></div>
            <div class="book"><a href="/two"><span class="name">Two</span></a><span class="author">B</span><span class="words">20K</span><img src="/two.jpg"/></div>
        """.trimIndent()

        val books = RegexSearchResultParser.parse(source, body)

        assertEquals(2, books.size)
        assertEquals("One", books[0].name)
        assertEquals("/one", books[0].bookUrl)
        assertEquals("10K", books[0].wordCount)
        assertEquals("/two.jpg", books[1].coverUrl)
    }

    @Test
    fun parsesBookInfoWithHtmlRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleBookInfo = SharedBookInfoRule(
                name = "h1@text",
                author = ".author@text",
                intro = ".intro@text",
                tocUrl = ".toc@href",
                wordCount = ".words@text"
            )
        )
        val book = SharedBook(bookUrl = "https://source.test/book")
        val body = """
            <h1>Book Name</h1><div class="author">Writer</div><p class="intro">Intro text</p><a class="toc" href="/toc">toc</a><span class="words">50K</span>
        """.trimIndent()

        val parsed = RegexBookInfoParser.parse(source, book, body)

        assertEquals("Book Name", parsed.name)
        assertEquals("Writer", parsed.author)
        assertEquals("Intro text", parsed.intro)
        assertEquals("/toc", parsed.tocUrl)
        assertEquals("50K", parsed.wordCount)
    }

    @Test
    fun parsesTocWithHtmlRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleToc = SharedTocRule(
                chapterList = ".chapter",
                chapterName = "a@text",
                chapterUrl = "a@href",
                updateTime = ".time@text"
            )
        )
        val body = """
            <div class="chapter"><a href="/c1">Chapter 1</a><span class="time">today</span></div>
            <div class="chapter"><a href="/c2">Chapter 2</a><span class="time">tomorrow</span></div>
        """.trimIndent()

        val chapters = RegexChapterListParser.parse(source, body)

        assertEquals(2, chapters.size)
        assertEquals("Chapter 1", chapters[0].title)
        assertEquals("/c2", chapters[1].url)
        assertEquals("tomorrow", chapters[1].tag)
    }

    @Test
    fun parsesContentWithHtmlRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleContent = SharedContentRule(
                title = "h1@text",
                content = ".content@text",
                nextContentUrl = ".next@href",
                replaceRegex = "##Advertisement##"
            )
        )
        val body = """
            <h1>Chapter</h1><article class="content">Line 1<br/>Advertisement<br/>Line 2</article><a class="next" href="/next">next</a>
        """.trimIndent()

        val content = RegexChapterContentParser.parse(source, SharedBook(), io.legado.shared.model.SharedBookChapter(), body)

        assertEquals("Chapter", content.title)
        assertEquals("Line 1\nLine 2", content.content)
        assertEquals(listOf("/next"), content.nextContentUrls)
    }
}


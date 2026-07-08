package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import kotlin.test.Test
import kotlin.test.assertEquals

class RegexBookInfoParserTest {
    @Test
    fun parsesBookInfoFromRegexRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleBookInfo = SharedBookInfoRule(
                name = """<h1>([^<]+)</h1>""",
                author = """<span class="author">([^<]+)</span>""",
                kind = """<span class="kind">([^<]+)</span>""",
                lastChapter = """<a class="latest">([^<]+)</a>""",
                intro = """<p class="intro">([\s\S]*?)</p>""",
                coverUrl = """<img class="cover" src="([^"]+)"""",
                tocUrl = """<a class="toc" href="([^"]+)">"""
            )
        )
        val book = SharedBook(
            name = "Old Name",
            author = "Old Author",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1",
            origin = source.bookSourceUrl
        )
        val body = """
            <article>
              <h1>Metal Story</h1>
              <span class="author">Tester</span>
              <span class="kind">Sci-Fi</span>
              <a class="latest">Chapter 9</a>
              <p class="intro">
                First intro line.
                Second intro line.
              </p>
              <img class="cover" src="https://source.test/cover.jpg" />
              <a class="toc" href="https://source.test/book/1/catalog">toc</a>
            </article>
        """.trimIndent()

        val parsed = RegexBookInfoParser.parse(source, book, body)

        assertEquals("Metal Story", parsed.name)
        assertEquals("Tester", parsed.author)
        assertEquals("Sci-Fi", parsed.kind)
        assertEquals("Chapter 9", parsed.latestChapterTitle)
        assertEquals("First intro line.\nSecond intro line.", parsed.intro)
        assertEquals("https://source.test/cover.jpg", parsed.coverUrl)
        assertEquals("https://source.test/book/1/catalog", parsed.tocUrl)
        assertEquals("https://source.test/book/1", parsed.bookUrl)
        assertEquals("https://source.test", parsed.origin)
    }

    @Test
    fun parsesBookInfoFromJsonPathRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://api.example.test",
            bookSourceName = "JSON",
            ruleBookInfo = SharedBookInfoRule(
                name = "$.content.title",
                author = "$.content.authorName",
                kind = "$.content.category",
                lastChapter = "$.content.newestChapterTitle",
                intro = "$.content.desc",
                coverUrl = "$.content.cover",
                tocUrl = "$.content.catalogUrl@js:'ignored'"
            )
        )
        val book = SharedBook(
            name = "Old",
            author = "Old Author",
            bookUrl = "https://api.example.test/book/1",
            tocUrl = "",
            origin = source.bookSourceUrl
        )
        val body = """
            {"content":{
              "title":"JSON Story",
              "authorName":"API Author",
              "category":"Adventure",
              "newestChapterTitle":"Chapter 12",
              "desc":"First line.\nSecond line.",
              "cover":"/cover.jpg",
              "catalogUrl":"/book/1/catalog"
            }}
        """.trimIndent()

        val parsed = RegexBookInfoParser.parse(source, book, body)

        assertEquals("JSON Story", parsed.name)
        assertEquals("API Author", parsed.author)
        assertEquals("Adventure", parsed.kind)
        assertEquals("Chapter 12", parsed.latestChapterTitle)
        assertEquals("First line.\nSecond line.", parsed.intro)
        assertEquals("/cover.jpg", parsed.coverUrl)
        assertEquals("/book/1/catalog", parsed.tocUrl)
        assertEquals("https://api.example.test/book/1", parsed.bookUrl)
        assertEquals(source.bookSourceUrl, parsed.origin)
    }

    @Test
    fun preservesExistingBookWhenRuleIsMissing() {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )
        val book = SharedBook(
            name = "Existing",
            author = "Author",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1",
            origin = source.bookSourceUrl,
            kind = "Old Kind",
            latestChapterTitle = "Old Chapter",
            intro = "Old Intro",
            coverUrl = "https://source.test/old.jpg"
        )

        val parsed = RegexBookInfoParser.parse(source, book, "<html />")

        assertEquals(book, parsed)
    }
}

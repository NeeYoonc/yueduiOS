package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchRule
import kotlin.test.Test
import kotlin.test.assertEquals

class RegexSearchResultParserTest {

    @Test
    fun parsesSearchBooksFromBookListGroups() {
        val source = SharedBookSource(
            bookSourceUrl = "https://regex.test",
            bookSourceName = "Regex",
            ruleSearch = SharedSearchRule(
                bookList = """<li data-url="([^"]+)"><span class="name">([^<]+)</span><span class="author">([^<]+)</span><span class="last">([^<]+)</span></li>""",
                bookUrl = "$1",
                name = "$2",
                author = "$3",
                lastChapter = "$4"
            )
        )
        val body = """
            <ul>
              <li data-url="/book/1"><span class="name">Metal Max</span><span class="author">Crea-Tech</span><span class="last">Chapter 3</span></li>
              <li data-url="/book/2"><span class="name">Metal Saga</span><span class="author">Success</span><span class="last">Final</span></li>
            </ul>
        """.trimIndent()

        val books = RegexSearchResultParser.parse(source, body)

        assertEquals(2, books.size)
        assertEquals("Metal Max", books[0].name)
        assertEquals("Crea-Tech", books[0].author)
        assertEquals("Chapter 3", books[0].latestChapterTitle)
        assertEquals("/book/1", books[0].bookUrl)
        assertEquals("https://regex.test", books[0].origin)
        assertEquals("Metal Saga", books[1].name)
        assertEquals("/book/2", books[1].bookUrl)
    }

    @Test
    fun parsesFieldsWithFieldSpecificRegexRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://regex.test",
            bookSourceName = "Regex",
            ruleSearch = SharedSearchRule(
                bookList = """<article[\s\S]*?</article>""",
                name = """<h3>([^<]+)</h3>""",
                author = """Author: ([^<]+)""",
                kind = """Kind: ([^<]+)""",
                intro = """Intro: ([^<]+)""",
                coverUrl = """<img src="([^"]+)"""",
                bookUrl = """href="([^"]+)""""
            )
        )
        val body = """
            <article><a href="/book/1"><img src="/cover.jpg"/><h3>Regex Book</h3></a><p>Author: Tester</p><p>Kind: Sci-Fi</p><p>Intro: Tanks</p></article>
        """.trimIndent()

        val books = RegexSearchResultParser.parse(source, body)

        assertEquals(1, books.size)
        assertEquals("Regex Book", books.single().name)
        assertEquals("Tester", books.single().author)
        assertEquals("Sci-Fi", books.single().kind)
        assertEquals("Tanks", books.single().intro)
        assertEquals("/cover.jpg", books.single().coverUrl)
        assertEquals("/book/1", books.single().bookUrl)
    }

    @Test
    fun parsesSearchBooksFromJsonPathRules() {
        val source = SharedBookSource(
            bookSourceUrl = "https://json.test",
            bookSourceName = "Json",
            ruleSearch = SharedSearchRule(
                bookList = "$.content.content",
                name = "$.title",
                author = "$.author",
                bookUrl = "$.id@js:'ignored'",
                intro = "$.desc",
                lastChapter = "$.newestChapter"
            )
        )
        val body = """
            {
              "content": {
                "content": [
                  {
                    "id": "book-1",
                    "title": "Json Book",
                    "author": "Tester",
                    "desc": "Intro text",
                    "newestChapter": "Chapter 9"
                  },
                  {
                    "id": "book-2",
                    "title": "Second Json",
                    "author": "Other",
                    "desc": "Second intro",
                    "newestChapter": "Final"
                  }
                ]
              }
            }
        """.trimIndent()

        val books = RegexSearchResultParser.parse(source, body)

        assertEquals(2, books.size)
        assertEquals("Json Book", books[0].name)
        assertEquals("Tester", books[0].author)
        assertEquals("book-1", books[0].bookUrl)
        assertEquals("Intro text", books[0].intro)
        assertEquals("Chapter 9", books[0].latestChapterTitle)
        assertEquals("Second Json", books[1].name)
        assertEquals("book-2", books[1].bookUrl)
    }

    @Test
    fun returnsEmptyListWhenBookListRuleIsMissing() {
        val source = SharedBookSource(
            bookSourceUrl = "https://regex.test",
            bookSourceName = "Regex",
            ruleSearch = SharedSearchRule(name = "$1")
        )

        val books = RegexSearchResultParser.parse(source, "name=Ignored")

        assertEquals(emptyList(), books)
    }
}

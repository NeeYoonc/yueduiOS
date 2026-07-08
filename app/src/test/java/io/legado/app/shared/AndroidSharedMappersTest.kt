package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSharedMappersTest {

    @Test
    fun mapsBookSourceAndNestedRulesToSharedModel() {
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            bookSourceGroup = "Group",
            bookSourceType = 0,
            bookUrlPattern = "https://source.test/book/.*",
            enabled = true,
            enabledExplore = false,
            enabledCookieJar = false,
            concurrentRate = "2/1000",
            header = """{"User-Agent":"Legado"}""",
            loginUrl = "https://source.test/login",
            loginUi = "user,password",
            loginCheckJs = "result",
            coverDecodeJs = "bytes",
            bookSourceComment = "comment",
            variableComment = "vars",
            exploreUrl = "https://source.test/explore",
            searchUrl = "https://source.test/search?q={{key}}",
            ruleSearch = SearchRule(
                bookList = ".book",
                name = ".name",
                author = ".author",
                kind = ".kind",
                lastChapter = ".last",
                intro = ".intro",
                coverUrl = ".cover@src",
                bookUrl = "a@href",
                updateTime = ".updated",
                wordCount = ".words",
                checkKeyWord = "keyword"
            ),
            ruleBookInfo = BookInfoRule(
                init = ".detail",
                name = "h1",
                author = ".author",
                kind = ".kind",
                lastChapter = ".last",
                intro = ".intro",
                coverUrl = ".cover@src",
                tocUrl = ".toc@href",
                updateTime = ".updated",
                wordCount = ".words",
                canReName = "true",
                downloadUrls = ".download@href"
            ),
            ruleToc = TocRule(
                chapterList = ".chapter",
                chapterName = ".title",
                chapterUrl = "a@href",
                nextTocUrl = ".next@href",
                updateTime = ".time",
                formatJs = "format",
                isVolume = ".volume",
                isVip = ".vip",
                isPay = ".pay",
                preUpdateJs = "pre"
            ),
            ruleContent = ContentRule(
                content = ".content",
                subContent = ".sub",
                title = "h2",
                nextContentUrl = ".next@href",
                replaceRegex = "ads",
                webJs = "web",
                sourceRegex = "source",
                imageStyle = "FULL",
                imageDecode = "decode",
                payAction = "pay",
                callBackJs = "callback"
            )
        )

        val shared = source.toSharedBookSource()

        assertEquals("https://source.test", shared.bookSourceUrl)
        assertEquals("Source", shared.bookSourceName)
        assertEquals("Group", shared.bookSourceGroup)
        assertEquals(false, shared.enabledExplore)
        assertEquals(false, shared.enabledCookieJar)
        assertEquals(".book", shared.ruleSearch?.bookList)
        assertEquals(".updated", shared.ruleSearch?.updateTime)
        assertEquals(".words", shared.ruleSearch?.wordCount)
        assertEquals("keyword", shared.ruleSearch?.checkKeyWord)
        assertEquals(".detail", shared.ruleBookInfo?.init)
        assertEquals(".toc@href", shared.ruleBookInfo?.tocUrl)
        assertEquals(".updated", shared.ruleBookInfo?.updateTime)
        assertEquals(".words", shared.ruleBookInfo?.wordCount)
        assertEquals("true", shared.ruleBookInfo?.canReName)
        assertEquals(".download@href", shared.ruleBookInfo?.downloadUrls)
        assertEquals(".chapter", shared.ruleToc?.chapterList)
        assertEquals("format", shared.ruleToc?.formatJs)
        assertEquals(".volume", shared.ruleToc?.isVolume)
        assertEquals(".sub", shared.ruleContent?.subContent)
        assertEquals("h2", shared.ruleContent?.title)
        assertEquals("ads", shared.ruleContent?.replaceRegex)
        assertEquals("FULL", shared.ruleContent?.imageStyle)
        assertEquals("decode", shared.ruleContent?.imageDecode)
        assertEquals("pay", shared.ruleContent?.payAction)
        assertEquals("callback", shared.ruleContent?.callBackJs)
    }

    @Test
    fun mapsBookAndSearchBookToSharedBookModels() {
        val book = Book(
            name = "Metal",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1/toc",
            origin = "https://source.test",
            kind = "Sci-Fi",
            latestChapterTitle = "Chapter 9",
            intro = "Intro",
            coverUrl = "https://source.test/cover.jpg",
            variable = """{"token":"abc"}"""
        )

        val sharedBook = book.toSharedBook()

        assertEquals("Metal", sharedBook.name)
        assertEquals("Tester", sharedBook.author)
        assertEquals("https://source.test/book/1/toc", sharedBook.tocUrl)
        assertEquals("abc", sharedBook.variableMap["token"])

        val searchBook = SearchBook(
            name = "Result",
            author = "Author",
            bookUrl = "https://source.test/book/2",
            origin = "https://source.test",
            kind = "Fantasy",
            latestChapterTitle = "Latest",
            intro = "Intro",
            coverUrl = "https://source.test/result.jpg"
        )

        val sharedSearchBook = searchBook.toSharedSearchBook()

        assertEquals("Result", sharedSearchBook.name)
        assertEquals("Fantasy", sharedSearchBook.kind)
        assertEquals("Latest", sharedSearchBook.latestChapterTitle)
        assertEquals("https://source.test/result.jpg", sharedSearchBook.coverUrl)
    }

    @Test
    fun mapsChapterToSharedChapter() {
        val chapter = BookChapter(
            title = "Chapter 1",
            url = "/chapter/1",
            index = 3,
            isVolume = true,
            isVip = true,
            isPay = false,
            tag = "2026-07-07",
            variable = """{"page":"10"}"""
        )

        val shared = chapter.toSharedBookChapter()

        assertEquals("Chapter 1", shared.title)
        assertEquals("/chapter/1", shared.url)
        assertEquals(3, shared.index)
        assertTrue(shared.isVolume)
        assertTrue(shared.isVip)
        assertEquals("2026-07-07", shared.tag)
        assertEquals("10", shared.variableMap["page"])
    }
}

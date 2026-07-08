package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.ReviewRule
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
            customOrder = 7,
            enabled = true,
            enabledExplore = false,
            jsLib = "function lib(){}",
            enabledCookieJar = false,
            concurrentRate = "2/1000",
            header = """{"User-Agent":"Legado"}""",
            loginUrl = "https://source.test/login",
            loginUi = "user,password",
            loginCheckJs = "result",
            coverDecodeJs = "bytes",
            bookSourceComment = "comment",
            variableComment = "vars",
            lastUpdateTime = 11L,
            respondTime = 12L,
            weight = 13,
            exploreUrl = "https://source.test/explore",
            exploreScreen = "screen",
            ruleExplore = ExploreRule(
                bookList = ".explore",
                name = ".explore-name",
                author = ".explore-author",
                bookUrl = ".explore-url"
            ),
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
            ),
            ruleReview = ReviewRule(
                reviewUrl = ".review@href",
                avatarRule = ".avatar@src",
                contentRule = ".review-content"
            ),
            eventListener = true,
            customButton = true
        )

        val shared = source.toSharedBookSource()

        assertEquals("https://source.test", shared.bookSourceUrl)
        assertEquals("Source", shared.bookSourceName)
        assertEquals("Group", shared.bookSourceGroup)
        assertEquals(7, shared.customOrder)
        assertEquals(false, shared.enabledExplore)
        assertEquals("function lib(){}", shared.jsLib)
        assertEquals(false, shared.enabledCookieJar)
        assertEquals(11L, shared.lastUpdateTime)
        assertEquals(12L, shared.respondTime)
        assertEquals(13, shared.weight)
        assertEquals("screen", shared.exploreScreen)
        assertEquals(".explore", shared.ruleExplore?.bookList)
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
        assertEquals(".review@href", shared.ruleReview?.reviewUrl)
        assertEquals(".avatar@src", shared.ruleReview?.avatarRule)
        assertTrue(shared.eventListener)
        assertTrue(shared.customButton)
    }

    @Test
    fun mapsBookAndSearchBookToSharedBookModels() {
        val book = Book(
            name = "Metal",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1/toc",
            origin = "https://source.test",
            originName = "Source",
            kind = "Sci-Fi",
            customTag = "Custom Tag",
            latestChapterTitle = "Chapter 9",
            latestChapterTime = 10L,
            lastCheckTime = 11L,
            lastCheckCount = 2,
            totalChapterNum = 9,
            durChapterTitle = "Chapter 3",
            durChapterIndex = 3,
            durVolumeIndex = 1,
            chapterInVolumeIndex = 2,
            durChapterPos = 99,
            durChapterTime = 12L,
            intro = "Intro",
            customIntro = "Custom Intro",
            coverUrl = "https://source.test/cover.jpg",
            customCoverUrl = "https://source.test/custom-cover.jpg",
            charset = "UTF-8",
            type = 1,
            group = 4L,
            wordCount = "100K",
            canUpdate = false,
            order = 5,
            originOrder = 6,
            variable = """{"token":"abc"}""",
            readConfig = Book.ReadConfig(
                reverseToc = true,
                imageStyle = "FULL",
                ttsEngine = "native",
                readSimulating = true
            ),
            syncTime = 13L
        )

        val sharedBook = book.toSharedBook()

        assertEquals("Metal", sharedBook.name)
        assertEquals("Tester", sharedBook.author)
        assertEquals("https://source.test/book/1/toc", sharedBook.tocUrl)
        assertEquals("Source", sharedBook.originName)
        assertEquals("Custom Tag", sharedBook.customTag)
        assertEquals(9, sharedBook.totalChapterNum)
        assertEquals(3, sharedBook.durChapterIndex)
        assertEquals(99, sharedBook.durChapterPos)
        assertEquals("Custom Intro", sharedBook.customIntro)
        assertEquals("https://source.test/custom-cover.jpg", sharedBook.customCoverUrl)
        assertEquals("100K", sharedBook.wordCount)
        assertEquals(false, sharedBook.canUpdate)
        assertEquals("FULL", sharedBook.readConfig?.imageStyle)
        assertEquals(true, sharedBook.readConfig?.readSimulating)
        assertEquals("abc", sharedBook.variableMap["token"])

        val searchBook = SearchBook(
            name = "Result",
            author = "Author",
            bookUrl = "https://source.test/book/2",
            origin = "https://source.test",
            kind = "Fantasy",
            latestChapterTitle = "Latest",
            intro = "Intro",
            coverUrl = "https://source.test/result.jpg",
            tocUrl = "https://source.test/book/2/toc",
            type = 4,
            wordCount = "200K",
            variable = """{"result":"yes"}""",
            originOrder = 8,
            chapterWordCountText = "2K/chapter",
            chapterWordCount = 2000,
            respondTime = 300
        )

        val sharedSearchBook = searchBook.toSharedSearchBook()

        assertEquals("Result", sharedSearchBook.name)
        assertEquals("Fantasy", sharedSearchBook.kind)
        assertEquals("Latest", sharedSearchBook.latestChapterTitle)
        assertEquals("https://source.test/result.jpg", sharedSearchBook.coverUrl)
        assertEquals("https://source.test/book/2/toc", sharedSearchBook.tocUrl)
        assertEquals(4, sharedSearchBook.type)
        assertEquals("200K", sharedSearchBook.wordCount)
        assertEquals(8, sharedSearchBook.originOrder)
        assertEquals(2000, sharedSearchBook.chapterWordCount)
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
            baseUrl = "https://source.test",
            bookUrl = "https://source.test/book/1",
            resourceUrl = "https://source.test/audio/1.mp3",
            wordCount = "2K",
            start = 10L,
            end = 20L,
            startFragmentId = "frag-a",
            endFragmentId = "frag-b",
            imgUrl = "https://source.test/image.jpg",
            tag = "2026-07-07",
            variable = """{"page":"10"}"""
        )

        val shared = chapter.toSharedBookChapter()

        assertEquals("Chapter 1", shared.title)
        assertEquals("/chapter/1", shared.url)
        assertEquals(3, shared.index)
        assertTrue(shared.isVolume)
        assertTrue(shared.isVip)
        assertEquals("https://source.test", shared.baseUrl)
        assertEquals("https://source.test/book/1", shared.bookUrl)
        assertEquals("https://source.test/audio/1.mp3", shared.resourceUrl)
        assertEquals("2K", shared.wordCount)
        assertEquals(10L, shared.start)
        assertEquals(20L, shared.end)
        assertEquals("frag-a", shared.startFragmentId)
        assertEquals("frag-b", shared.endFragmentId)
        assertEquals("https://source.test/image.jpg", shared.imgUrl)
        assertEquals("2026-07-07", shared.tag)
        assertEquals("""{"page":"10"}""", shared.variable)
        assertEquals("10", shared.variableMap["page"])
    }

    @Test
    fun mapsBookGroupToSharedBookGroup() {
        val group = BookGroup(
            groupId = 4L,
            groupName = "Favorites",
            cover = "https://source.test/group.jpg",
            order = 6,
            enableRefresh = false,
            show = false,
            bookSort = 2,
            onlyUpdateRead = true
        )

        val shared = group.toSharedBookGroup()

        assertEquals(4L, shared.groupId)
        assertEquals("Favorites", shared.groupName)
        assertEquals("https://source.test/group.jpg", shared.cover)
        assertEquals(6, shared.order)
        assertEquals(false, shared.enableRefresh)
        assertEquals(false, shared.show)
        assertEquals(2, shared.bookSort)
        assertEquals(true, shared.onlyUpdateRead)
    }
}

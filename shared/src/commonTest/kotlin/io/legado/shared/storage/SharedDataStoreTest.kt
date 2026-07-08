package io.legado.shared.storage

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedBookmark
import io.legado.shared.model.SharedCacheEntry
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedChapterContentRecord
import io.legado.shared.model.SharedCookie
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.model.SharedDictRule
import io.legado.shared.model.SharedHttpTts
import io.legado.shared.model.SharedKeyboardAssist
import io.legado.shared.model.SharedReadConfig
import io.legado.shared.model.SharedReadRecord
import io.legado.shared.model.SharedReplaceRule
import io.legado.shared.model.SharedReviewRule
import io.legado.shared.model.SharedRssArticle
import io.legado.shared.model.SharedRssReadRecord
import io.legado.shared.model.SharedRssSource
import io.legado.shared.model.SharedRssStar
import io.legado.shared.model.SharedRuleSub
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedSearchKeyword
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.model.SharedServer
import io.legado.shared.model.SharedTxtTocRule
import io.legado.shared.platform.CacheStorePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedDataStoreTest {
    @Test
    fun savesAndLoadsFullProductSnapshot() {
        val cache = InMemoryCacheStore()
        val store = SharedDataStore(cache)
        val snapshot = fullSnapshot()

        store.save(snapshot)

        val loaded = store.load()
        assertEquals(snapshot, loaded)
        assertTrue(cache.values.getValue(SharedDataStore.DEFAULT_SNAPSHOT_KEY).contains("bookSources"))
        assertTrue(cache.values.getValue(SharedDataStore.DEFAULT_SNAPSHOT_KEY).contains("rssSources"))
        assertTrue(cache.values.getValue(SharedDataStore.DEFAULT_SNAPSHOT_KEY).contains("keyboardAssists"))
    }

    @Test
    fun decodesMinimalAndUnknownFieldsAsEmptySnapshotParts() {
        val cache = InMemoryCacheStore()
        val store = SharedDataStore(cache)
        cache.putText(
            SharedDataStore.DEFAULT_SNAPSHOT_KEY,
            """
            {
              "schemaVersion": 1,
              "unknownFutureField": true,
              "bookGroups": [
                { "groupId": -1, "groupName": "All", "unknown": "kept forward compatible" }
              ]
            }
            """.trimIndent()
        )

        val loaded = store.load()

        assertEquals(1, loaded.schemaVersion)
        assertEquals("All", loaded.bookGroups.single().groupName)
        assertEquals(emptyList(), loaded.bookSources)
    }

    @Test
    fun clearsSnapshot() {
        val cache = InMemoryCacheStore()
        val store = SharedDataStore(cache)
        store.save(fullSnapshot())

        store.clear()

        assertEquals(SharedDataSnapshot(), store.load())
    }

    private fun fullSnapshot(): SharedDataSnapshot {
        return SharedDataSnapshot(
            exportedAtMillis = 1234L,
            bookSources = listOf(
                SharedBookSource(
                    bookSourceUrl = "https://source.test",
                    bookSourceName = "Source",
                    customOrder = 1,
                    jsLib = "function shared(){}",
                    ruleExplore = SharedSearchRule(bookList = ".book"),
                    ruleSearch = SharedSearchRule(bookList = ".result", name = ".name"),
                    ruleReview = SharedReviewRule(reviewUrl = "https://source.test/review")
                )
            ),
            books = listOf(
                SharedBook(
                    name = "Book",
                    author = "Author",
                    bookUrl = "https://source.test/book",
                    tocUrl = "https://source.test/toc",
                    origin = "https://source.test",
                    originName = "Source",
                    customTag = "tag",
                    latestChapterTitle = "Latest",
                    totalChapterNum = 10,
                    durChapterIndex = 2,
                    durChapterPos = 50,
                    readConfig = SharedReadConfig(imageStyle = "FULL", ttsEngine = "native"),
                    variable = """{"k":"v"}"""
                )
            ),
            bookGroups = listOf(SharedBookGroup(groupId = -1, groupName = "All")),
            chapters = listOf(
                SharedBookChapter(
                    title = "Chapter 1",
                    url = "https://source.test/chapter/1",
                    bookUrl = "https://source.test/book",
                    index = 0,
                    resourceUrl = "https://source.test/audio/1.mp3",
                    variable = """{"lyric":"value"}"""
                )
            ),
            chapterContents = listOf(
                SharedChapterContentRecord(
                    bookUrl = "https://source.test/book",
                    chapterUrl = "https://source.test/chapter/1",
                    content = SharedChapterContent(content = "Body")
                )
            ),
            bookmarks = listOf(SharedBookmark(time = 1L, bookName = "Book", chapterName = "Chapter 1")),
            replaceRules = listOf(SharedReplaceRule(id = 2L, name = "Clean", pattern = "a", replacement = "b")),
            searchBooks = listOf(SharedSearchBook(name = "Book", author = "Author", bookUrl = "https://source.test/book")),
            searchKeywords = listOf(SharedSearchKeyword(word = "metal", usage = 3, lastUseTime = 4L)),
            cookies = listOf(SharedCookie(url = "https://source.test", cookie = "a=b")),
            rssSources = listOf(SharedRssSource(sourceUrl = "https://rss.test", sourceName = "RSS")),
            rssArticles = listOf(SharedRssArticle(origin = "https://rss.test", sort = "default", title = "Article", link = "https://rss.test/a")),
            rssReadRecords = listOf(SharedRssReadRecord(record = "https://rss.test/a", title = "Article")),
            rssStars = listOf(SharedRssStar(origin = "https://rss.test", title = "Article", link = "https://rss.test/a")),
            txtTocRules = listOf(SharedTxtTocRule(id = 5L, name = "TXT", rule = "^Chapter")),
            readRecords = listOf(SharedReadRecord(deviceId = "ios", bookName = "Book", readTime = 6L)),
            httpTts = listOf(SharedHttpTts(id = 7L, name = "TTS", url = "https://tts.test?q={{content}}")),
            caches = listOf(SharedCacheEntry(key = "cache", value = "value", deadline = 8L)),
            ruleSubs = listOf(SharedRuleSub(id = 9L, name = "Sub", url = "https://rules.test")),
            dictRules = listOf(SharedDictRule(name = "Dict", urlRule = "https://dict.test?q={{key}}", showRule = "$.text")),
            keyboardAssists = listOf(SharedKeyboardAssist(type = 1, key = "next", value = "Next")),
            servers = listOf(SharedServer(id = 10L, name = "WebDAV", config = """{"url":"https://dav.test"}"""))
        )
    }

    private class InMemoryCacheStore : CacheStorePort {
        val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}


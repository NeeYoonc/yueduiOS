package io.legado.shared.config

import io.legado.shared.model.SharedRuleSub
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.replacement.ReplacementRepository
import io.legado.shared.rss.RssSourceRepository
import io.legado.shared.source.SourceRepository
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleSubUpdateServiceTest {
    @Test
    fun fetchesAndImportsBookRssAndReplaceRuleSubscriptions() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                return when (request.url) {
                    "https://sub.test/book.json" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """[{"bookSourceUrl":"https://book.test","bookSourceName":"Book","lastUpdateTime":10}]"""
                    )

                    "https://sub.test/rss.json" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """[{"sourceUrl":"https://rss.test/feed","sourceName":"RSS","lastUpdateTime":20}]"""
                    )

                    "https://sub.test/replace.json" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """[{"id":1,"name":"Clean","pattern":"a","replacement":"b","enabled":true}]"""
                    )

                    else -> error("Unexpected ${request.url}")
                }
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val ruleSubs = RuleSubRepository(store)
        val service = RuleSubUpdateService(
            httpFetcher = fetcher,
            ruleSubRepository = ruleSubs,
            sourceRepository = SourceRepository(store),
            rssSourceRepository = RssSourceRepository(store),
            replacementRepository = ReplacementRepository(store)
        )

        val book = service.update(SharedRuleSub(id = 1, name = "Books", url = "https://sub.test/book.json", type = 0), nowMillis = 100)
        val rss = service.update(SharedRuleSub(id = 2, name = "RSS", url = "https://sub.test/rss.json", type = 1), nowMillis = 200)
        val replace = service.update(SharedRuleSub(id = 3, name = "Replace", url = "https://sub.test/replace.json", type = 2), nowMillis = 300)

        assertEquals(listOf("https://sub.test/book.json", "https://sub.test/rss.json", "https://sub.test/replace.json"), requestedUrls)
        assertEquals(1, book.importedCount)
        assertEquals(1, rss.importedCount)
        assertEquals(1, replace.importedCount)
        assertEquals(listOf("Book"), SourceRepository(store).list().map { it.bookSourceName })
        assertEquals(listOf("RSS"), RssSourceRepository(store).list().map { it.sourceName })
        assertEquals(listOf("Clean"), ReplacementRepository(store).list().map { it.name })
        assertEquals(listOf(100L, 200L, 300L), ruleSubs.list().map { it.update })
    }

    @Test
    fun skipsAutoUpdateBeforeConfiguredInterval() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                error("No network expected before interval")
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val ruleSubRepository = RuleSubRepository(store)
        val ruleSub = ruleSubRepository.upsert(
            SharedRuleSub(
                id = 1,
                name = "Books",
                url = "https://sub.test/book.json",
                type = 0,
                autoUpdate = true,
                update = 1_000L,
                updateInterval = 2
            )
        )
        val service = RuleSubUpdateService(
            httpFetcher = fetcher,
            ruleSubRepository = ruleSubRepository,
            sourceRepository = SourceRepository(store),
            rssSourceRepository = RssSourceRepository(store),
            replacementRepository = ReplacementRepository(store)
        )

        val results = service.updateAuto(nowMillis = 1_000L + 60 * 60 * 1000L)

        assertEquals(emptyList(), results)
        assertEquals(ruleSub, ruleSubRepository.list().single())
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

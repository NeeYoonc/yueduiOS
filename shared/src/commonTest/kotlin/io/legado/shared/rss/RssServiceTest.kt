package io.legado.shared.rss

import io.legado.shared.model.SharedRssSource
import io.legado.shared.model.SharedRssArticle
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RssServiceTest {
    @Test
    fun articlePresentationFieldsAvoidPlatformNameCollisions() {
        val introOnly = SharedRssArticle(title = "Title", description = "Intro")
        val fullContent = introOnly.copy(content = "Full content")

        assertEquals("Intro", introOnly.summary)
        assertEquals("Intro", introOnly.readableContent)
        assertEquals("Full content", fullContent.readableContent)
    }

    @Test
    fun refreshesArticlesParsesRulesAndLoadsContent() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return when (request.url) {
                    "https://rss.test/feed?page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            {"items":[{"title":"Article 1","link":"/a/1","desc":"Intro","date":"today"}]}
                        """.trimIndent()
                    )
                    "https://rss.test/a/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """{"content":{"text":"Full article."}}"""
                    )
                    else -> error("Unexpected ${request.url}")
                }
            }
        }
        val store = SharedLibraryStore(InMemoryCacheStore())
        val service = RssService(fetcher, store)
        val source = SharedRssSource(
            sourceUrl = "https://rss.test/feed?page={{page}}",
            sourceName = "RSS",
            ruleArticles = "$.items",
            ruleTitle = "$.title",
            ruleLink = "$.link",
            ruleDescription = "$.desc",
            rulePubDate = "$.date",
            ruleContent = "$.content.text"
        )

        val page = service.refreshArticles(source, page = 1)
        val article = service.loadContent(source, page.articles.single())

        assertEquals("Article 1", page.articles.single().title)
        assertEquals("https://rss.test/a/1", page.articles.single().link)
        assertEquals("Intro", page.articles.single().description)
        assertEquals("today", page.articles.single().pubDate)
        assertEquals("Full article.", article.content)
        assertEquals(listOf("Article 1"), store.loadDataSnapshot().rssArticles.map { it.title })
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

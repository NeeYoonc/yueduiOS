package io.legado.shared.rss

import io.legado.shared.model.SharedRssArticle
import io.legado.shared.model.SharedRssSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.service.SharedRequestBuilder
import io.legado.shared.storage.SharedLibraryStore

class RssService(
    private val httpFetcher: HttpFetcher,
    private val libraryStore: SharedLibraryStore? = null,
    private val parser: RssRuleParser = RssRuleParser()
) {
    suspend fun refreshArticles(
        source: SharedRssSource,
        page: Int = 1,
        sortName: String = "default",
        sortUrl: String? = null,
        key: String? = null
    ): RssArticlePage {
        val response = httpFetcher.fetch(
            SharedRequestBuilder.build(
                template = requestTemplate(source, sortUrl, key),
                context = SharedRequestBuilder.SharedRequestContext(
                    key = key.orEmpty(),
                    page = page
                )
            )
        )
        val articles = parser.parseArticles(
            source = source,
            body = response.body,
            baseUrl = response.finalUrl,
            sort = sortName
        )
        saveArticles(articles)
        return RssArticlePage(
            source = source,
            response = response,
            articles = articles
        )
    }

    suspend fun loadContent(
        source: SharedRssSource,
        article: SharedRssArticle
    ): SharedRssArticle {
        if (source.ruleContent.isNullOrBlank() || article.link.isBlank()) {
            return article
        }
        val response = httpFetcher.fetch(SharedRequestBuilder.build(article.link))
        val parsed = parser.parseContent(source, article, response.body)
        saveArticles(listOf(parsed))
        return parsed
    }

    fun listCachedArticles(source: SharedRssSource? = null): List<SharedRssArticle> {
        val articles = libraryStore?.loadDataSnapshot()?.rssArticles.orEmpty()
        return if (source == null) {
            articles
        } else {
            articles.filter { it.origin == source.sourceUrl }
        }
    }

    private fun requestTemplate(
        source: SharedRssSource,
        sortUrl: String?,
        key: String?
    ): String {
        return when {
            !key.isNullOrBlank() && !source.searchUrl.isNullOrBlank() -> source.searchUrl
            !sortUrl.isNullOrBlank() -> sortUrl
            !source.sortUrl.isNullOrBlank() && source.singleUrl -> source.sortUrl
            else -> source.sourceUrl
        }.orEmpty()
    }

    private fun saveArticles(articles: List<SharedRssArticle>) {
        val store = libraryStore ?: return
        if (articles.isEmpty()) {
            return
        }
        val snapshot = store.loadDataSnapshot()
        val merged = articles + snapshot.rssArticles.filterNot { old ->
            articles.any { it.origin == old.origin && it.link == old.link && it.sort == old.sort }
        }
        store.saveDataSnapshot(snapshot.copy(rssArticles = merged))
    }
}

data class RssArticlePage(
    val source: SharedRssSource,
    val response: SharedHttpResponse,
    val articles: List<SharedRssArticle>
)

package io.legado.shared.rss

import io.legado.shared.model.SharedRssArticle
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.storage.SharedLibraryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RssArticleStateRepositoryTest {
    @Test
    fun marksArticlesReadAndAppliesReadState() {
        val repository = RssArticleStateRepository(SharedLibraryStore(InMemoryCacheStore()))
        val article = article(link = "https://rss.test/a", title = "A")

        val read = repository.markRead(article, read = true, nowMillis = 10L)

        assertTrue(read.read)
        assertEquals(1, repository.listReadRecords().size)
        assertEquals(10L, repository.listReadRecords().single().readTime)
        assertTrue(repository.applyState(article).read)

        val unread = repository.markRead(article, read = false, nowMillis = 20L)

        assertFalse(unread.read)
        assertFalse(repository.applyState(article.copy(read = true)).read)
        assertEquals(false, repository.listReadRecords().single().read)
        assertEquals(20L, repository.listReadRecords().single().readTime)
    }

    @Test
    fun starsAndUnstarsArticles() {
        val repository = RssArticleStateRepository(SharedLibraryStore(InMemoryCacheStore()))
        val article = article(link = "https://rss.test/star", title = "Star")

        val starred = repository.setStarred(article, starred = true, nowMillis = 30L)

        assertEquals(1, starred.size)
        assertTrue(repository.isStarred(article))
        assertEquals("Star", repository.listStars().single().title)
        assertEquals(30L, repository.listStars().single().starTime)

        val unstarred = repository.setStarred(article, starred = false, nowMillis = 40L)

        assertEquals(emptyList(), unstarred)
        assertFalse(repository.isStarred(article))
    }

    @Test
    fun listsStarredArticlesForReaderEntry() {
        val repository = RssArticleStateRepository(SharedLibraryStore(InMemoryCacheStore()))
        val article = article(link = "https://rss.test/starred", title = "Starred")
            .copy(content = "Full text")

        repository.setStarred(article, starred = true, nowMillis = 60L)

        val starred = repository.listStarredArticles()

        assertEquals(listOf("Starred"), starred.map { it.title })
        assertEquals("Full text", starred.single().content)
        assertEquals("https://rss.test/starred", starred.single().link)
    }

    @Test
    fun preservesArticleCacheWhenChangingState() {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = RssArticleStateRepository(store)
        val article = article(link = "https://rss.test/cache", title = "Cached")
        store.saveDataSnapshot(store.loadDataSnapshot().copy(rssArticles = listOf(article)))

        repository.markRead(article, read = true, nowMillis = 50L)

        val cached = store.loadDataSnapshot().rssArticles.single()
        assertEquals("Cached", cached.title)
        assertTrue(cached.read)
    }

    private fun article(
        link: String,
        title: String,
        origin: String = "https://rss.test/feed",
        sort: String = "default"
    ): SharedRssArticle {
        return SharedRssArticle(
            origin = origin,
            sort = sort,
            title = title,
            link = link,
            description = "Summary"
        )
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

package io.legado.shared.rss

import io.legado.shared.model.SharedRssArticle
import io.legado.shared.model.SharedRssReadRecord
import io.legado.shared.model.SharedRssStar
import io.legado.shared.storage.SharedLibraryStore

class RssArticleStateRepository(
    private val libraryStore: SharedLibraryStore
) {
    fun listReadRecords(): List<SharedRssReadRecord> {
        return libraryStore.loadDataSnapshot().rssReadRecords.sortedByDescending { it.readTime ?: 0L }
    }

    fun listStars(): List<SharedRssStar> {
        return libraryStore.loadDataSnapshot().rssStars.sortedByDescending { it.starTime }
    }

    fun markRead(
        article: SharedRssArticle,
        read: Boolean,
        nowMillis: Long = 0L
    ): SharedRssArticle {
        val updated = article.copy(read = read)
        val record = SharedRssReadRecord(
            record = articleIdentity(article),
            title = article.title,
            readTime = nowMillis,
            read = read,
            origin = article.origin,
            sort = article.sort,
            image = article.image,
            type = article.type,
            durPos = article.durPos,
            pubDate = article.pubDate
        )
        val snapshot = libraryStore.loadDataSnapshot()
        val records = listOf(record) + snapshot.rssReadRecords.filterNot { it.matches(article) }
        libraryStore.saveDataSnapshot(
            snapshot.copy(
                rssReadRecords = records,
                rssArticles = snapshot.rssArticles.upsert(updated)
            )
        )
        return updated
    }

    fun setStarred(
        article: SharedRssArticle,
        starred: Boolean,
        nowMillis: Long = 0L
    ): List<SharedRssStar> {
        val snapshot = libraryStore.loadDataSnapshot()
        val oldStars = snapshot.rssStars.filterNot { it.matches(article) }
        val stars = if (starred) {
            listOf(article.toStar(nowMillis)) + oldStars
        } else {
            oldStars
        }
        libraryStore.saveDataSnapshot(snapshot.copy(rssStars = stars))
        return listStars()
    }

    fun isStarred(article: SharedRssArticle): Boolean {
        return libraryStore.loadDataSnapshot().rssStars.any { it.matches(article) }
    }

    fun applyState(article: SharedRssArticle): SharedRssArticle {
        val record = libraryStore.loadDataSnapshot().rssReadRecords.firstOrNull { it.matches(article) }
        return if (record == null) {
            article
        } else {
            article.copy(read = record.read)
        }
    }

    fun applyState(articles: List<SharedRssArticle>): List<SharedRssArticle> {
        val records = libraryStore.loadDataSnapshot().rssReadRecords
        return articles.map { article ->
            val record = records.firstOrNull { it.matches(article) }
            if (record == null) article else article.copy(read = record.read)
        }
    }

    private fun List<SharedRssArticle>.upsert(article: SharedRssArticle): List<SharedRssArticle> {
        return listOf(article) + filterNot { it.matches(article) }
    }

    private fun SharedRssReadRecord.matches(article: SharedRssArticle): Boolean {
        return record == articleIdentity(article) ||
            (origin == article.origin && sort == article.sort && record == article.link) ||
            record == article.link
    }

    private fun SharedRssStar.matches(article: SharedRssArticle): Boolean {
        return origin == article.origin && sort == article.sort && link == article.link
    }

    private fun SharedRssArticle.matches(other: SharedRssArticle): Boolean {
        return origin == other.origin && sort == other.sort && link == other.link
    }

    private fun SharedRssArticle.toStar(nowMillis: Long): SharedRssStar {
        return SharedRssStar(
            origin = origin,
            sort = sort,
            title = title,
            starTime = nowMillis,
            link = link,
            pubDate = pubDate,
            description = description,
            content = content,
            image = image,
            group = group,
            variable = variable,
            type = type,
            durPos = durPos
        )
    }

    private fun articleIdentity(article: SharedRssArticle): String {
        return listOf(article.origin, article.sort, article.link).joinToString("|")
    }
}

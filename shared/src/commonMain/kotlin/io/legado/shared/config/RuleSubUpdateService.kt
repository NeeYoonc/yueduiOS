package io.legado.shared.config

import io.legado.shared.model.SharedRuleSub
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.replacement.ReplacementRepository
import io.legado.shared.rss.RssSourceRepository
import io.legado.shared.service.SharedRequestBuilder
import io.legado.shared.source.SourceRepository

class RuleSubUpdateService(
    private val httpFetcher: HttpFetcher,
    private val ruleSubRepository: RuleSubRepository,
    private val sourceRepository: SourceRepository,
    private val rssSourceRepository: RssSourceRepository,
    private val replacementRepository: ReplacementRepository
) {
    suspend fun update(ruleSub: SharedRuleSub, nowMillis: Long = 0L): RuleSubUpdateResult {
        val request = SharedRequestBuilder.build(ruleSub.url.normalizedRequestUrl())
        val response = httpFetcher.fetch(request)
        val importedCount = when (ruleSub.type) {
            TYPE_BOOK_SOURCE -> sourceRepository.importJson(response.body, replace = false).size
            TYPE_RSS_SOURCE -> rssSourceRepository.importJson(response.body, replace = false).size
            TYPE_REPLACE_RULE -> replacementRepository.importJson(response.body, replace = false).size
            else -> throw IllegalArgumentException("Unsupported rule subscription type: ${ruleSub.type}")
        }
        val updated = ruleSubRepository.upsert(ruleSub.copy(update = nowMillis))
        return RuleSubUpdateResult(
            ruleSub = updated,
            type = ruleSub.type,
            importedCount = importedCount,
            sourceUrl = response.finalUrl
        )
    }

    suspend fun updateAuto(nowMillis: Long): List<RuleSubUpdateResult> {
        return ruleSubRepository.list()
            .filter { it.autoUpdate && it.isDue(nowMillis) }
            .map { update(it, nowMillis) }
    }

    private fun SharedRuleSub.isDue(nowMillis: Long): Boolean {
        if (updateInterval <= 0) {
            return true
        }
        return update + updateInterval * MILLIS_PER_HOUR <= nowMillis
    }

    private fun String.normalizedRequestUrl(): String {
        return if (endsWith(REQUEST_WITHOUT_UA_SUFFIX)) {
            substringBeforeLast(REQUEST_WITHOUT_UA_SUFFIX)
        } else {
            this
        }
    }

    private companion object {
        const val TYPE_BOOK_SOURCE = 0
        const val TYPE_RSS_SOURCE = 1
        const val TYPE_REPLACE_RULE = 2
        const val REQUEST_WITHOUT_UA_SUFFIX = "#requestWithoutUA"
        const val MILLIS_PER_HOUR = 60L * 60L * 1000L
    }
}

data class RuleSubUpdateResult(
    val ruleSub: SharedRuleSub,
    val type: Int,
    val importedCount: Int,
    val sourceUrl: String
)

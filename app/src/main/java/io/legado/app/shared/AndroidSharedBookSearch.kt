package io.legado.app.shared

import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.service.BookSearchService
import io.legado.shared.service.RuleEngineSearchResultParser
import io.legado.shared.service.RuleAwareSearchResultParser
import io.legado.shared.service.SearchResultParser
import io.legado.shared.service.SearchPageResult

class AndroidSharedBookSearch(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    searchResultParser: SearchResultParser = RuleAwareSearchResultParser,
    useRuleEngine: Boolean = true
) {
    private val searchService = BookSearchService(
        httpFetcher = httpFetcher,
        searchResultParser = searchResultParser,
        suspendSearchResultParser = if (useRuleEngine && searchResultParser === RuleAwareSearchResultParser) {
            RuleEngineSearchResultParser(
                AnalyzeRuleEngine(
                    scriptRuntime = AndroidScriptRuntime(),
                    webViewRuntime = AndroidWebViewRuleRuntime()
                ),
                fallbackParser = searchResultParser
            )
        } else {
            null
        }
    )

    suspend fun search(source: BookSource, key: String, page: Int = 1): SearchPageResult {
        return searchService.search(source.toSharedBookSource(), key, page)
    }
}

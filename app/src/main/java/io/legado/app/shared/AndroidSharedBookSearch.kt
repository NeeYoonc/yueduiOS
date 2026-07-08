package io.legado.app.shared

import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.service.BookSearchService
import io.legado.shared.service.RuleAwareSearchResultParser
import io.legado.shared.service.SearchResultParser
import io.legado.shared.service.SearchPageResult

class AndroidSharedBookSearch(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    searchResultParser: SearchResultParser = RuleAwareSearchResultParser
) {
    private val searchService = BookSearchService(httpFetcher, searchResultParser)

    suspend fun search(source: BookSource, key: String, page: Int = 1): SearchPageResult {
        return searchService.search(source.toSharedBookSource(), key, page)
    }
}

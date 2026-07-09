package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse

class BookSearchService(
    private val httpFetcher: HttpFetcher,
    private val searchResultParser: SearchResultParser = RuleAwareSearchResultParser,
    private val suspendSearchResultParser: SuspendSearchResultParser? = null,
    private val requestFactory: SourceRequestFactory = SourceRequestFactory()
) {
    suspend fun search(source: SharedBookSource, key: String, page: Int = 1): SearchPageResult {
        val template = requireNotNull(source.searchUrl) {
            "Book source ${source.bookSourceName} has no searchUrl"
        }
        val request = requestFactory.build(
            source = source,
            template = template,
            context = SharedRequestBuilder.SharedRequestContext(key = key, page = page)
        )
        val response = httpFetcher.fetch(request)
        requestFactory.storeResponseCookies(source, response)
        val parsedBooks = suspendSearchResultParser?.parse(source, response.body)
            ?: searchResultParser.parse(source, response.body)
        return SearchPageResult(
            source = source,
            response = response,
            books = normalizeSearchBookUrls(response.finalUrl, parsedBooks),
            debugBookName = extractDebugValue(response.body, "name")
        )
    }

    private fun normalizeSearchBookUrls(baseUrl: String, books: List<SharedSearchBook>): List<SharedSearchBook> {
        return books.map { book ->
            book.copy(
                bookUrl = book.bookUrl
                    .takeIf { it.isNotBlank() }
                    ?.let { SharedUrlResolver.resolve(baseUrl, it) }
                    ?: book.bookUrl,
                coverUrl = book.coverUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { SharedUrlResolver.resolve(baseUrl, it) },
                tocUrl = book.tocUrl
                    .takeIf { it.isNotBlank() }
                    ?.let { SharedUrlResolver.resolve(baseUrl, it) }
                    ?: book.tocUrl
            )
        }
    }

    private fun extractDebugValue(body: String, key: String): String? {
        val prefix = "$key="
        return body.lineSequence()
            .firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.trim()
    }
}

data class SearchPageResult(
    val source: SharedBookSource,
    val response: SharedHttpResponse,
    val books: List<SharedSearchBook> = emptyList(),
    val debugBookName: String?
)

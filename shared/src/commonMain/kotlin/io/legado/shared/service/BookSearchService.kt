package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse

class BookSearchService(
    private val httpFetcher: HttpFetcher,
    private val searchResultParser: SearchResultParser = RuleAwareSearchResultParser
) {
    suspend fun search(source: SharedBookSource, key: String, page: Int = 1): SearchPageResult {
        val template = requireNotNull(source.searchUrl) {
            "Book source ${source.bookSourceName} has no searchUrl"
        }
        val requestUrl = template
            .replace("{{key}}", encodeQueryValue(key))
            .replace("{{page}}", page.toString())
        val response = httpFetcher.fetch(SharedHttpRequest(url = requestUrl))
        return SearchPageResult(
            source = source,
            response = response,
            books = searchResultParser.parse(source, response.body),
            debugBookName = extractDebugValue(response.body, "name")
        )
    }

    private fun encodeQueryValue(value: String): String =
        value.encodeToByteArray().joinToString("") { byte ->
            val char = byte.toInt().toChar()
            when {
                char in 'a'..'z' -> char.toString()
                char in 'A'..'Z' -> char.toString()
                char in '0'..'9' -> char.toString()
                char == '-' || char == '_' || char == '.' || char == '~' -> char.toString()
                char == ' ' -> "%20"
                else -> "%" + byte.toUByte().toString(16).uppercase().padStart(2, '0')
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

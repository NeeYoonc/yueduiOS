package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.service.BookInfoParser
import io.legado.shared.service.BookInfoResult
import io.legado.shared.service.BookInfoService
import io.legado.shared.service.RegexBookInfoParser
import io.legado.shared.service.RuleEngineBookInfoParser

class AndroidSharedBookInfo(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    bookInfoParser: BookInfoParser = RegexBookInfoParser,
    useRuleEngine: Boolean = true
) {
    private val ruleEngine = if (useRuleEngine) {
        AnalyzeRuleEngine(
            scriptRuntime = AndroidScriptRuntime(),
            webViewRuntime = AndroidWebViewRuleRuntime()
        )
    } else {
        null
    }
    private val bookInfoService = BookInfoService(
        httpFetcher = httpFetcher,
        bookInfoParser = bookInfoParser,
        suspendBookInfoParser = if (useRuleEngine && bookInfoParser === RegexBookInfoParser) {
            ruleEngine?.let { RuleEngineBookInfoParser(it, fallbackParser = bookInfoParser) }
        } else {
            null
        }
    )

    suspend fun getBookInfo(source: BookSource, book: Book): BookInfoResult {
        return bookInfoService.getBookInfo(source.toSharedBookSource(), book.toSharedBook())
    }
}

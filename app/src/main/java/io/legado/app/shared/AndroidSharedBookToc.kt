package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.service.BookTocService
import io.legado.shared.service.ChapterListParser
import io.legado.shared.service.ChapterListResult
import io.legado.shared.service.RegexChapterListParser
import io.legado.shared.service.RuleEngineChapterListParser

class AndroidSharedBookToc(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    chapterListParser: ChapterListParser = RegexChapterListParser,
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
    private val tocService = BookTocService(
        httpFetcher = httpFetcher,
        chapterListParser = chapterListParser,
        suspendChapterListParser = if (useRuleEngine && chapterListParser === RegexChapterListParser) {
            ruleEngine?.let { RuleEngineChapterListParser(it, fallbackParser = chapterListParser) }
        } else {
            null
        }
    )

    suspend fun getChapterList(source: BookSource, book: Book): ChapterListResult {
        return tocService.getChapterList(source.toSharedBookSource(), book.toSharedBook())
    }
}

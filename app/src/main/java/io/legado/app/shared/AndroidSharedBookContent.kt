package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.service.BookContentService
import io.legado.shared.service.ChapterContentParser
import io.legado.shared.service.ChapterContentResult
import io.legado.shared.service.RegexChapterContentParser
import io.legado.shared.service.RuleEngineChapterContentParser

class AndroidSharedBookContent(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    chapterContentParser: ChapterContentParser = RegexChapterContentParser,
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
    private val contentService = BookContentService(
        httpFetcher = httpFetcher,
        chapterContentParser = chapterContentParser,
        suspendChapterContentParser = if (useRuleEngine && chapterContentParser === RegexChapterContentParser) {
            ruleEngine?.let { RuleEngineChapterContentParser(it, fallbackParser = chapterContentParser) }
        } else {
            null
        }
    )

    suspend fun getContent(
        source: BookSource,
        book: Book,
        chapter: BookChapter
    ): ChapterContentResult {
        return contentService.getContent(
            source.toSharedBookSource(),
            book.toSharedBook(),
            chapter.toSharedBookChapter()
        )
    }
}

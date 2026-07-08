package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.service.BookInfoParser
import io.legado.shared.service.ChapterContentParser
import io.legado.shared.service.ChapterContentResult
import io.legado.shared.service.ChapterListParser
import io.legado.shared.service.ChapterListResult
import io.legado.shared.service.ReadingFlowResult
import io.legado.shared.service.ReadingFlowService
import io.legado.shared.service.RegexBookInfoParser
import io.legado.shared.service.RegexChapterContentParser
import io.legado.shared.service.RegexChapterListParser
import io.legado.shared.service.RuleAwareSearchResultParser
import io.legado.shared.service.RuleEngineBookInfoParser
import io.legado.shared.service.RuleEngineChapterContentParser
import io.legado.shared.service.RuleEngineChapterListParser
import io.legado.shared.service.RuleEngineSearchResultParser
import io.legado.shared.service.SearchPageResult
import io.legado.shared.service.SearchResultParser

class AndroidSharedReadingFlow(
    httpFetcher: HttpFetcher = AndroidHttpFetcher(),
    searchResultParser: SearchResultParser = RuleAwareSearchResultParser,
    bookInfoParser: BookInfoParser = RegexBookInfoParser,
    chapterListParser: ChapterListParser = RegexChapterListParser,
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

    private val readingFlowService = ReadingFlowService(
        httpFetcher = httpFetcher,
        searchResultParser = searchResultParser,
        suspendSearchResultParser = if (useRuleEngine && searchResultParser === RuleAwareSearchResultParser) {
            ruleEngine?.let { RuleEngineSearchResultParser(it, fallbackParser = searchResultParser) }
        } else {
            null
        },
        bookInfoParser = bookInfoParser,
        suspendBookInfoParser = if (useRuleEngine && bookInfoParser === RegexBookInfoParser) {
            ruleEngine?.let { RuleEngineBookInfoParser(it, fallbackParser = bookInfoParser) }
        } else {
            null
        },
        chapterListParser = chapterListParser,
        suspendChapterListParser = if (useRuleEngine && chapterListParser === RegexChapterListParser) {
            ruleEngine?.let { RuleEngineChapterListParser(it, fallbackParser = chapterListParser) }
        } else {
            null
        },
        chapterContentParser = chapterContentParser,
        suspendChapterContentParser = if (useRuleEngine && chapterContentParser === RegexChapterContentParser) {
            ruleEngine?.let { RuleEngineChapterContentParser(it, fallbackParser = chapterContentParser) }
        } else {
            null
        }
    )

    suspend fun search(
        source: BookSource,
        key: String,
        page: Int = 1
    ): SearchPageResult {
        return readingFlowService.search(source.toSharedBookSource(), key, page)
    }

    suspend fun getChapterList(source: BookSource, book: Book): ChapterListResult {
        return readingFlowService.getChapterList(source.toSharedBookSource(), book.toSharedBook())
    }

    suspend fun getContent(
        source: BookSource,
        book: Book,
        chapter: BookChapter
    ): ChapterContentResult {
        return readingFlowService.getContent(
            source.toSharedBookSource(),
            book.toSharedBook(),
            chapter.toSharedBookChapter()
        )
    }

    suspend fun openFirstSearchResult(
        source: BookSource,
        key: String,
        page: Int = 1
    ): ReadingFlowResult {
        return readingFlowService.openFirstSearchResult(source.toSharedBookSource(), key, page)
    }
}

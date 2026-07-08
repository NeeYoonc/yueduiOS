package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.service.BookContentService
import io.legado.shared.service.BookInfoParser
import io.legado.shared.service.BookInfoResult
import io.legado.shared.service.BookInfoService
import io.legado.shared.service.BookSearchService
import io.legado.shared.service.BookTocService
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
import io.legado.shared.service.SearchPageResult
import io.legado.shared.service.SearchResultParser
import io.legado.shared.service.SuspendChapterListParser
import io.legado.shared.service.SuspendSearchResultParser
import io.legado.shared.source.SourceJsonImporter

class LegadoSharedClient(
    httpFetcher: HttpFetcher,
    searchResultParser: SearchResultParser = RuleAwareSearchResultParser,
    suspendSearchResultParser: SuspendSearchResultParser? = null,
    bookInfoParser: BookInfoParser = RegexBookInfoParser,
    chapterListParser: ChapterListParser = RegexChapterListParser,
    suspendChapterListParser: SuspendChapterListParser? = null,
    chapterContentParser: ChapterContentParser = RegexChapterContentParser
) {
    private val searchService = BookSearchService(httpFetcher, searchResultParser, suspendSearchResultParser)
    private val bookInfoService = BookInfoService(httpFetcher, bookInfoParser)
    private val tocService = BookTocService(httpFetcher, chapterListParser, suspendChapterListParser)
    private val contentService = BookContentService(httpFetcher, chapterContentParser)
    private val readingFlowService = ReadingFlowService(
        searchService = searchService,
        bookInfoService = bookInfoService,
        tocService = tocService,
        contentService = contentService
    )

    @Throws(IllegalArgumentException::class)
    fun importBookSources(json: String): List<SharedBookSource> {
        return SourceJsonImporter.importBookSources(json)
    }

    suspend fun search(
        source: SharedBookSource,
        key: String,
        page: Int = 1
    ): SearchPageResult {
        return searchService.search(source, key, page)
    }

    suspend fun getBookInfo(
        source: SharedBookSource,
        book: SharedBook
    ): BookInfoResult {
        return bookInfoService.getBookInfo(source, book)
    }

    suspend fun getChapterList(
        source: SharedBookSource,
        book: SharedBook
    ): ChapterListResult {
        return tocService.getChapterList(source, book)
    }

    suspend fun getContent(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter
    ): ChapterContentResult {
        return contentService.getContent(source, book, chapter)
    }

    suspend fun openFirstSearchResult(
        source: SharedBookSource,
        key: String,
        page: Int = 1
    ): ReadingFlowResult {
        return readingFlowService.openFirstSearchResult(source, key, page)
    }
}

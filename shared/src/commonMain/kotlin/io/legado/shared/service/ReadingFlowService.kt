package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.HttpFetcher

class ReadingFlowService(
    private val searchService: BookSearchService,
    private val bookInfoService: BookInfoService,
    private val tocService: BookTocService,
    private val contentService: BookContentService
) {
    constructor(
        httpFetcher: HttpFetcher,
        searchResultParser: SearchResultParser = RuleAwareSearchResultParser,
        suspendSearchResultParser: SuspendSearchResultParser? = null,
        bookInfoParser: BookInfoParser = RegexBookInfoParser,
        suspendBookInfoParser: SuspendBookInfoParser? = null,
        chapterListParser: ChapterListParser = RegexChapterListParser,
        suspendChapterListParser: SuspendChapterListParser? = null,
        chapterContentParser: ChapterContentParser = RegexChapterContentParser,
        suspendChapterContentParser: SuspendChapterContentParser? = null,
        requestFactory: SourceRequestFactory = SourceRequestFactory()
    ) : this(
        searchService = BookSearchService(httpFetcher, searchResultParser, suspendSearchResultParser, requestFactory),
        bookInfoService = BookInfoService(httpFetcher, bookInfoParser, suspendBookInfoParser, requestFactory),
        tocService = BookTocService(
            httpFetcher = httpFetcher,
            chapterListParser = chapterListParser,
            suspendChapterListParser = suspendChapterListParser,
            requestFactory = requestFactory
        ),
        contentService = BookContentService(
            httpFetcher = httpFetcher,
            chapterContentParser = chapterContentParser,
            suspendChapterContentParser = suspendChapterContentParser,
            requestFactory = requestFactory
        )
    )

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
        val search = search(source, key, page)
        val searchBook = search.books.firstOrNull()?.toBook()
            ?: return ReadingFlowResult(search = search)
        val bookInfo = if (source.ruleBookInfo != null) {
            getBookInfo(source, searchBook)
        } else {
            null
        }
        val book = bookInfo?.book ?: searchBook
        val chapterList = getChapterList(source, book)
        val chapter = chapterList.chapters.firstOrNull()
            ?: return ReadingFlowResult(
                search = search,
                bookInfo = bookInfo,
                selectedBook = book,
                chapterList = chapterList
            )
        val content = getContent(source, book, chapter)
        return ReadingFlowResult(
            search = search,
            bookInfo = bookInfo,
            selectedBook = book,
            chapterList = chapterList,
            selectedChapter = chapter,
            content = content
        )
    }
}

data class ReadingFlowResult(
    val search: SearchPageResult,
    val bookInfo: BookInfoResult? = null,
    val selectedBook: SharedBook? = null,
    val chapterList: ChapterListResult? = null,
    val selectedChapter: SharedBookChapter? = null,
    val content: ChapterContentResult? = null
)

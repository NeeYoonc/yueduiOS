package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedRssArticle
import io.legado.shared.model.SharedRssSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.book.BookDetailCoordinator
import io.legado.shared.book.BookDetailResult
import io.legado.shared.book.BookshelfService
import io.legado.shared.book.CachedChapterReadResult
import io.legado.shared.book.ChapterReadResult
import io.legado.shared.book.ChapterRepository
import io.legado.shared.book.SearchCoordinator
import io.legado.shared.book.SearchCoordinatorResult
import io.legado.shared.local.LocalTextBookService
import io.legado.shared.local.LocalTextImportResult
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleWebViewRuntime
import io.legado.shared.rss.RssArticlePage
import io.legado.shared.rss.RssService
import io.legado.shared.service.RuleEngineBookInfoParser
import io.legado.shared.service.RuleEngineChapterContentParser
import io.legado.shared.service.RuleEngineChapterListParser
import io.legado.shared.service.RuleEngineSearchResultParser
import io.legado.shared.service.ReadingFlowResult
import io.legado.shared.source.DefaultDataImporter
import io.legado.shared.source.DefaultDataPayload
import io.legado.shared.source.SourceDebugResult
import io.legado.shared.source.SourceDebugService
import io.legado.shared.source.SourceRepository
import io.legado.shared.storage.SharedLibraryStore

open class LegadoRuntime(
    httpFetcher: HttpFetcher,
    cacheStore: CacheStorePort,
    scriptRuntime: ScriptRuntime? = null,
    webViewRuntime: RuleWebViewRuntime? = null
) {
    private val ruleEngine = AnalyzeRuleEngine(
        scriptRuntime = scriptRuntime,
        webViewRuntime = webViewRuntime
    )
    val client: LegadoSharedClient = LegadoSharedClient(
        httpFetcher = httpFetcher,
        suspendSearchResultParser = RuleEngineSearchResultParser(ruleEngine),
        suspendBookInfoParser = RuleEngineBookInfoParser(ruleEngine),
        suspendChapterListParser = RuleEngineChapterListParser(ruleEngine),
        suspendChapterContentParser = RuleEngineChapterContentParser(ruleEngine)
    )
    val libraryStore: SharedLibraryStore = SharedLibraryStore(cacheStore)
    val bookshelfService: BookshelfService = BookshelfService(libraryStore)
    val searchCoordinator: SearchCoordinator = SearchCoordinator(client, libraryStore)
    val bookDetailCoordinator: BookDetailCoordinator = BookDetailCoordinator(client, bookshelfService, libraryStore)
    val chapterRepository: ChapterRepository = ChapterRepository(client, libraryStore, bookshelfService)
    val sourceRepository: SourceRepository = SourceRepository(libraryStore)
    val sourceDebugService: SourceDebugService = SourceDebugService(client)
    val rssService: RssService = RssService(httpFetcher, libraryStore)
    val localTextBookService: LocalTextBookService = LocalTextBookService(libraryStore, bookshelfService)

    @Throws(IllegalArgumentException::class)
    fun importAndSaveBookSources(json: String): List<SharedBookSource> {
        return sourceRepository.importJson(json, replace = true)
    }

    fun loadBookSources(): List<SharedBookSource> {
        return sourceRepository.list()
    }

    fun loadRssSources(): List<SharedRssSource> {
        return libraryStore.loadDataSnapshot().rssSources.sortedWith(
            compareBy<SharedRssSource> { it.customOrder }.thenBy { it.sourceName }
        )
    }

    fun loadRssArticles(source: SharedRssSource? = null): List<SharedRssArticle> {
        return rssService.listCachedArticles(source)
    }

    fun upsertBookSource(source: SharedBookSource): SharedBookSource {
        return sourceRepository.upsert(source)
    }

    fun setBookSourceEnabled(bookSourceUrl: String, enabled: Boolean): SharedBookSource? {
        return sourceRepository.setEnabled(bookSourceUrl, enabled)
    }

    fun deleteBookSource(bookSourceUrl: String): List<SharedBookSource> {
        return sourceRepository.delete(bookSourceUrl)
    }

    fun exportBookSourcesJson(): String {
        return sourceRepository.exportJson()
    }

    fun saveBooks(books: List<SharedBook>) {
        libraryStore.saveBooks(books)
    }

    fun loadBooks(): List<SharedBook> {
        return libraryStore.loadBooks()
    }

    fun loadBookChapters(book: SharedBook): List<SharedBookChapter> {
        return libraryStore.loadBookChapters(book)
    }

    @Throws(IllegalArgumentException::class)
    fun importLocalTextBook(
        fileName: String,
        text: String,
        nowMillis: Long = 0L
    ): LocalTextImportResult {
        return localTextBookService.importTextFile(fileName, text, nowMillis)
    }

    suspend fun searchEnabledSources(
        key: String,
        page: Int = 1,
        nowMillis: Long = 0L
    ): SearchCoordinatorResult {
        return searchCoordinator.search(loadBookSources(), key, page, nowMillis)
    }

    fun importAndSaveDefaultData(payload: DefaultDataPayload) {
        val snapshot = DefaultDataImporter.importSnapshot(payload)
        libraryStore.saveDataSnapshot(snapshot)
        if (snapshot.bookSources.isNotEmpty()) {
            sourceRepository.saveAll(snapshot.bookSources)
        }
    }

    suspend fun debugSourceFirstContent(
        source: SharedBookSource,
        key: String,
        page: Int = 1
    ): SourceDebugResult {
        return sourceDebugService.debugFirstContent(source, key, page)
    }

    suspend fun refreshRssArticles(
        source: SharedRssSource,
        page: Int = 1
    ): RssArticlePage {
        return rssService.refreshArticles(source, page)
    }

    suspend fun loadRssContent(
        source: SharedRssSource,
        article: SharedRssArticle
    ): SharedRssArticle {
        return rssService.loadContent(source, article)
    }

    suspend fun openSearchBook(
        source: SharedBookSource,
        searchBook: SharedSearchBook,
        nowMillis: Long = 0L
    ): BookDetailResult {
        return bookDetailCoordinator.openSearchBook(source, searchBook, nowMillis)
    }

    suspend fun refreshBook(
        source: SharedBookSource,
        book: SharedBook,
        nowMillis: Long = 0L
    ): BookDetailResult {
        return bookDetailCoordinator.refreshBook(source, book, nowMillis)
    }

    suspend fun loadChapter(
        source: SharedBookSource,
        book: SharedBook,
        chapterIndex: Int,
        position: Int = 0,
        nowMillis: Long = 0L,
        preloadAdjacent: Boolean = true
    ): ChapterReadResult {
        return chapterRepository.loadChapter(
            source = source,
            book = book,
            chapterIndex = chapterIndex,
            position = position,
            nowMillis = nowMillis,
            preloadAdjacent = preloadAdjacent
        )
    }

    @Throws(IllegalArgumentException::class)
    fun loadCachedChapter(
        book: SharedBook,
        chapterIndex: Int,
        position: Int = 0,
        nowMillis: Long = 0L
    ): CachedChapterReadResult {
        return chapterRepository.loadCachedChapter(book, chapterIndex, position, nowMillis)
    }

    suspend fun openFirstSearchResult(
        source: SharedBookSource,
        key: String,
        page: Int = 1
    ): ReadingFlowResult {
        return client.openFirstSearchResult(source, key, page).also { result ->
            val book = result.selectedBook
            val chapter = result.selectedChapter
            val content = result.content?.content
            val chapterList = result.chapterList
            if (book != null) {
                bookshelfService.upsertBook(
                    book.copy(
                        totalChapterNum = chapterList?.chapters?.size ?: book.totalChapterNum,
                        latestChapterTitle = book.latestChapterTitle
                            ?: chapterList?.chapters?.lastOrNull { !it.isVolume }?.title
                    )
                )
            }
            if (book != null && chapterList != null) {
                libraryStore.saveBookChapters(book, chapterList.chapters)
            }
            if (book != null && chapter != null) {
                bookshelfService.updateProgress(book, chapter, position = 0, nowMillis = 0L)
            }
            if (book != null && chapter != null && content != null) {
                libraryStore.saveChapterContent(book, chapter, content)
            }
        }
    }
}

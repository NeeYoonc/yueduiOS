package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedBookmark
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.model.SharedDictRule
import io.legado.shared.model.SharedHttpTts
import io.legado.shared.model.SharedReplaceRule
import io.legado.shared.model.SharedRssArticle
import io.legado.shared.model.SharedRssSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.book.BookDetailCoordinator
import io.legado.shared.book.BookDetailResult
import io.legado.shared.book.BookGroupRepository
import io.legado.shared.book.BookshelfService
import io.legado.shared.book.CachedChapterReadResult
import io.legado.shared.book.ChapterReadResult
import io.legado.shared.book.ChapterRepository
import io.legado.shared.book.SearchCoordinator
import io.legado.shared.book.SearchCoordinatorResult
import io.legado.shared.backup.DataBackupService
import io.legado.shared.bookmark.BookmarkRepository
import io.legado.shared.config.DictRuleRepository
import io.legado.shared.config.HttpTtsRepository
import io.legado.shared.local.LocalTextBookService
import io.legado.shared.local.LocalTextImportResult
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleWebViewRuntime
import io.legado.shared.replacement.ReplacementRepository
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
    val bookGroupRepository: BookGroupRepository = BookGroupRepository(libraryStore)
    val searchCoordinator: SearchCoordinator = SearchCoordinator(client, libraryStore)
    val bookDetailCoordinator: BookDetailCoordinator = BookDetailCoordinator(client, bookshelfService, libraryStore)
    val chapterRepository: ChapterRepository = ChapterRepository(client, libraryStore, bookshelfService)
    val sourceRepository: SourceRepository = SourceRepository(libraryStore)
    val sourceDebugService: SourceDebugService = SourceDebugService(client)
    val rssService: RssService = RssService(httpFetcher, libraryStore)
    val localTextBookService: LocalTextBookService = LocalTextBookService(libraryStore, bookshelfService)
    val replacementRepository: ReplacementRepository = ReplacementRepository(libraryStore)
    val dataBackupService: DataBackupService = DataBackupService(libraryStore)
    val dictRuleRepository: DictRuleRepository = DictRuleRepository(libraryStore)
    val httpTtsRepository: HttpTtsRepository = HttpTtsRepository(libraryStore)
    val bookmarkRepository: BookmarkRepository = BookmarkRepository(libraryStore)

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

    fun loadReplaceRules(): List<SharedReplaceRule> {
        return replacementRepository.list()
    }

    fun loadDictRules(): List<SharedDictRule> {
        return dictRuleRepository.list()
    }

    fun loadHttpTts(): List<SharedHttpTts> {
        return httpTtsRepository.list()
    }

    fun loadBookmarks(): List<SharedBookmark> {
        return bookmarkRepository.list()
    }

    fun loadBookGroups(): List<SharedBookGroup> {
        return bookGroupRepository.list()
    }

    fun loadSelectableBookGroups(): List<SharedBookGroup> {
        return bookGroupRepository.listSelectable()
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

    @Throws(IllegalArgumentException::class)
    fun importAndSaveReplaceRules(json: String, replace: Boolean = false): List<SharedReplaceRule> {
        return replacementRepository.importJson(json, replace)
    }

    fun upsertReplaceRule(rule: SharedReplaceRule): SharedReplaceRule {
        return replacementRepository.upsert(rule)
    }

    fun setReplaceRuleEnabled(id: Long, enabled: Boolean): SharedReplaceRule? {
        return replacementRepository.setEnabled(id, enabled)
    }

    fun deleteReplaceRule(id: Long): List<SharedReplaceRule> {
        return replacementRepository.delete(id)
    }

    fun exportReplaceRulesJson(): String {
        return replacementRepository.exportJson()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveDictRules(json: String, replace: Boolean = false): List<SharedDictRule> {
        return dictRuleRepository.importJson(json, replace)
    }

    fun setDictRuleEnabled(name: String, enabled: Boolean): SharedDictRule? {
        return dictRuleRepository.setEnabled(name, enabled)
    }

    fun deleteDictRule(name: String): List<SharedDictRule> {
        return dictRuleRepository.delete(name)
    }

    fun exportDictRulesJson(): String {
        return dictRuleRepository.exportJson()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveHttpTts(json: String, replace: Boolean = false): List<SharedHttpTts> {
        return httpTtsRepository.importJson(json, replace)
    }

    fun deleteHttpTts(id: Long): List<SharedHttpTts> {
        return httpTtsRepository.delete(id)
    }

    fun exportHttpTtsJson(): String {
        return httpTtsRepository.exportJson()
    }

    fun exportBackupJson(nowMillis: Long = 0L): String {
        return dataBackupService.exportJson(nowMillis)
    }

    @Throws(IllegalArgumentException::class)
    fun importBackupJson(json: String): SharedDataSnapshot {
        return dataBackupService.importJson(json)
    }

    fun saveBooks(books: List<SharedBook>) {
        libraryStore.saveBooks(books)
    }

    fun loadBooks(): List<SharedBook> {
        return libraryStore.loadBooks()
    }

    fun removeBook(book: SharedBook): List<SharedBook> {
        bookshelfService.removeBook(book)
        return loadBooks()
    }

    fun loadBooksForGroup(groupId: Long): List<SharedBook> {
        return bookGroupRepository.booksForGroup(groupId)
    }

    fun upsertBookGroup(group: SharedBookGroup): SharedBookGroup {
        return bookGroupRepository.upsert(group)
    }

    fun setBookGroupVisible(groupId: Long, show: Boolean): SharedBookGroup? {
        return bookGroupRepository.setVisible(groupId, show)
    }

    fun deleteBookGroup(groupId: Long): List<SharedBookGroup> {
        return bookGroupRepository.delete(groupId)
    }

    fun setBookGroupEnabled(
        book: SharedBook,
        groupId: Long,
        enabled: Boolean
    ): SharedBook {
        return bookGroupRepository.setBookGroupEnabled(book, groupId, enabled)
    }

    fun setBookGroupMask(
        book: SharedBook,
        groupMask: Long
    ): SharedBook {
        return bookGroupRepository.setBookGroupMask(book, groupMask)
    }

    fun loadBookChapters(book: SharedBook): List<SharedBookChapter> {
        return libraryStore.loadBookChapters(book)
    }

    fun addBookmark(
        book: SharedBook,
        chapter: SharedBookChapter,
        bookText: String,
        note: String = "",
        position: Int = 0,
        nowMillis: Long = 0L
    ): SharedBookmark {
        return bookmarkRepository.add(book, chapter, bookText, note, position, nowMillis)
    }

    fun deleteBookmark(time: Long): List<SharedBookmark> {
        return bookmarkRepository.delete(time)
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

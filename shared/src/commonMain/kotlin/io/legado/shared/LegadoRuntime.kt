package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedBookmark
import io.legado.shared.model.SharedCacheEntry
import io.legado.shared.model.SharedCookie
import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.model.SharedDictRule
import io.legado.shared.model.SharedDictionaryLookupResult
import io.legado.shared.model.SharedExploreKind
import io.legado.shared.model.SharedHttpTts
import io.legado.shared.model.SharedKeyboardAssist
import io.legado.shared.model.SharedRawConfigEntry
import io.legado.shared.model.SharedReadRecord
import io.legado.shared.model.SharedReaderPreferences
import io.legado.shared.model.SharedReaderSearchResult
import io.legado.shared.model.SharedReplaceRule
import io.legado.shared.model.SharedRssArticle
import io.legado.shared.model.SharedRssReadRecord
import io.legado.shared.model.SharedRssSource
import io.legado.shared.model.SharedRssStar
import io.legado.shared.model.SharedRuleSub
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedSearchKeyword
import io.legado.shared.model.SharedServer
import io.legado.shared.model.SharedTxtTocRule
import io.legado.shared.book.BookDetailCoordinator
import io.legado.shared.book.BookDetailResult
import io.legado.shared.book.BookGroupRepository
import io.legado.shared.book.BookshelfService
import io.legado.shared.book.CachedChapterReadResult
import io.legado.shared.book.ChapterReadResult
import io.legado.shared.book.ChapterRepository
import io.legado.shared.book.SearchCoordinator
import io.legado.shared.book.SearchCoordinatorResult
import io.legado.shared.book.ReadRecordRepository
import io.legado.shared.backup.DataBackupService
import io.legado.shared.backup.WebDavBackupService
import io.legado.shared.bookmark.BookmarkRepository
import io.legado.shared.config.CacheEntryRepository
import io.legado.shared.config.CookieRepository
import io.legado.shared.config.DictionaryLookupService
import io.legado.shared.config.DictRuleRepository
import io.legado.shared.config.HttpTtsRequestFactory
import io.legado.shared.config.HttpTtsRepository
import io.legado.shared.config.KeyboardAssistRepository
import io.legado.shared.config.RawConfigRepository
import io.legado.shared.config.ReaderPreferencesRepository
import io.legado.shared.config.RuleSubRepository
import io.legado.shared.config.RuleSubUpdateResult
import io.legado.shared.config.RuleSubUpdateService
import io.legado.shared.config.ServerRepository
import io.legado.shared.config.TxtTocRuleRepository
import io.legado.shared.explore.ExploreService
import io.legado.shared.local.LocalTextBookService
import io.legado.shared.local.LocalTextImportResult
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleWebViewRuntime
import io.legado.shared.replacement.ReplacementRepository
import io.legado.shared.rss.RssArticlePage
import io.legado.shared.rss.RssArticleStateRepository
import io.legado.shared.rss.RssService
import io.legado.shared.rss.RssSourceRepository
import io.legado.shared.service.RuleEngineBookInfoParser
import io.legado.shared.service.RuleEngineChapterContentParser
import io.legado.shared.service.RuleEngineChapterListParser
import io.legado.shared.service.RuleEngineSearchResultParser
import io.legado.shared.service.ReaderSearchService
import io.legado.shared.service.ReadingFlowResult
import io.legado.shared.service.SearchPageResult
import io.legado.shared.service.SharedRequestBuilder
import io.legado.shared.service.SourceRequestFactory
import io.legado.shared.source.DefaultDataImporter
import io.legado.shared.source.DefaultDataPayload
import io.legado.shared.source.SourceDebugResult
import io.legado.shared.source.SourceDebugService
import io.legado.shared.source.SharedLoginUiField
import io.legado.shared.source.SharedSourceLoginRequest
import io.legado.shared.source.SourceLoginService
import io.legado.shared.source.SourceRepository
import io.legado.shared.storage.SharedLibraryStore

open class LegadoRuntime(
    private val httpFetcher: HttpFetcher,
    cacheStore: CacheStorePort,
    scriptRuntime: ScriptRuntime? = null,
    webViewRuntime: RuleWebViewRuntime? = null
) {
    private val ruleEngine = AnalyzeRuleEngine(
        scriptRuntime = scriptRuntime,
        webViewRuntime = webViewRuntime
    )
    val libraryStore: SharedLibraryStore = SharedLibraryStore(cacheStore)
    val cookieRepository: CookieRepository = CookieRepository(libraryStore)
    val client: LegadoSharedClient = LegadoSharedClient(
        httpFetcher = httpFetcher,
        suspendSearchResultParser = RuleEngineSearchResultParser(ruleEngine),
        suspendBookInfoParser = RuleEngineBookInfoParser(ruleEngine),
        suspendChapterListParser = RuleEngineChapterListParser(ruleEngine),
        suspendChapterContentParser = RuleEngineChapterContentParser(ruleEngine),
        cookieStore = cookieRepository
    )
    val bookshelfService: BookshelfService = BookshelfService(libraryStore)
    val bookGroupRepository: BookGroupRepository = BookGroupRepository(libraryStore)
    val readRecordRepository: ReadRecordRepository = ReadRecordRepository(libraryStore)
    val searchCoordinator: SearchCoordinator = SearchCoordinator(client, libraryStore)
    val bookDetailCoordinator: BookDetailCoordinator = BookDetailCoordinator(client, bookshelfService, libraryStore)
    val chapterRepository: ChapterRepository = ChapterRepository(client, libraryStore, bookshelfService)
    val sourceRepository: SourceRepository = SourceRepository(libraryStore)
    val sourceLoginService: SourceLoginService = SourceLoginService(cookieRepository)
    val sourceDebugService: SourceDebugService = SourceDebugService(client)
    val readerSearchService: ReaderSearchService = ReaderSearchService()
    val exploreService: ExploreService = ExploreService(client, libraryStore)
    val rssArticleStateRepository: RssArticleStateRepository = RssArticleStateRepository(libraryStore)
    val rssService: RssService = RssService(
        httpFetcher = httpFetcher,
        libraryStore = libraryStore,
        stateRepository = rssArticleStateRepository,
        requestFactory = SourceRequestFactory(cookieRepository)
    )
    val rssSourceRepository: RssSourceRepository = RssSourceRepository(libraryStore)
    val localTextBookService: LocalTextBookService = LocalTextBookService(libraryStore, bookshelfService)
    val replacementRepository: ReplacementRepository = ReplacementRepository(libraryStore)
    val dataBackupService: DataBackupService = DataBackupService(libraryStore)
    val webDavBackupService: WebDavBackupService = WebDavBackupService(httpFetcher)
    val dictRuleRepository: DictRuleRepository = DictRuleRepository(libraryStore)
    val dictionaryLookupService: DictionaryLookupService = DictionaryLookupService(httpFetcher, dictRuleRepository, ruleEngine)
    val httpTtsRepository: HttpTtsRepository = HttpTtsRepository(libraryStore)
    val httpTtsRequestFactory: HttpTtsRequestFactory = HttpTtsRequestFactory(cookieRepository)
    val txtTocRuleRepository: TxtTocRuleRepository = TxtTocRuleRepository(libraryStore)
    val serverRepository: ServerRepository = ServerRepository(libraryStore)
    val keyboardAssistRepository: KeyboardAssistRepository = KeyboardAssistRepository(libraryStore)
    val ruleSubRepository: RuleSubRepository = RuleSubRepository(libraryStore)
    val ruleSubUpdateService: RuleSubUpdateService = RuleSubUpdateService(
        httpFetcher = httpFetcher,
        ruleSubRepository = ruleSubRepository,
        sourceRepository = sourceRepository,
        rssSourceRepository = rssSourceRepository,
        replacementRepository = replacementRepository
    )
    val rawConfigRepository: RawConfigRepository = RawConfigRepository(libraryStore)
    val readerPreferencesRepository: ReaderPreferencesRepository = ReaderPreferencesRepository(libraryStore)
    val bookmarkRepository: BookmarkRepository = BookmarkRepository(libraryStore)
    val cacheEntryRepository: CacheEntryRepository = CacheEntryRepository(libraryStore)

    @Throws(IllegalArgumentException::class)
    fun importAndSaveBookSources(json: String): List<SharedBookSource> {
        return sourceRepository.importJson(json, replace = true)
    }

    suspend fun importBookSourcesFromUrl(url: String, replace: Boolean = false): List<SharedBookSource> {
        return sourceRepository.importJson(fetchRemoteJson(url, "Book source URL is empty"), replace)
    }

    fun loadBookSources(): List<SharedBookSource> {
        return sourceRepository.list()
    }

    fun loadRssSources(): List<SharedRssSource> {
        return rssSourceRepository.list()
    }

    suspend fun importRssSourcesFromUrl(url: String, replace: Boolean = false): List<SharedRssSource> {
        return rssSourceRepository.importJson(fetchRemoteJson(url, "RSS source URL is empty"), replace)
    }

    fun loadExploreSources(): List<SharedBookSource> {
        return exploreService.listExploreSources()
    }

    fun loadExploreKinds(source: SharedBookSource): List<SharedExploreKind> {
        return exploreService.listKinds(source)
    }

    fun loadRssArticles(source: SharedRssSource? = null): List<SharedRssArticle> {
        return rssService.listCachedArticles(source)
    }

    fun loadRssReadRecords(): List<SharedRssReadRecord> {
        return rssArticleStateRepository.listReadRecords()
    }

    fun loadRssStars(): List<SharedRssStar> {
        return rssArticleStateRepository.listStars()
    }

    fun loadRssStarredArticles(): List<SharedRssArticle> {
        return rssArticleStateRepository.listStarredArticles()
    }

    fun markRssArticleRead(
        article: SharedRssArticle,
        read: Boolean,
        nowMillis: Long = 0L
    ): SharedRssArticle {
        return rssArticleStateRepository.markRead(article, read, nowMillis)
    }

    fun setRssArticleStarred(
        article: SharedRssArticle,
        starred: Boolean,
        nowMillis: Long = 0L
    ): List<SharedRssStar> {
        return rssArticleStateRepository.setStarred(article, starred, nowMillis)
    }

    fun isRssArticleStarred(article: SharedRssArticle): Boolean {
        return rssArticleStateRepository.isStarred(article)
    }

    fun upsertRssSource(source: SharedRssSource): SharedRssSource {
        return rssSourceRepository.upsert(source)
    }

    fun setRssSourceEnabled(sourceUrl: String, enabled: Boolean): SharedRssSource? {
        return rssSourceRepository.setEnabled(sourceUrl, enabled)
    }

    fun deleteRssSource(sourceUrl: String): List<SharedRssSource> {
        return rssSourceRepository.delete(sourceUrl)
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveRssSources(json: String, replace: Boolean = false): List<SharedRssSource> {
        return rssSourceRepository.importJson(json, replace)
    }

    fun exportRssSourcesJson(): String {
        return rssSourceRepository.exportJson()
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

    fun loadTxtTocRules(): List<SharedTxtTocRule> {
        return txtTocRuleRepository.list()
    }

    fun loadServers(): List<SharedServer> {
        return serverRepository.list()
    }

    fun loadKeyboardAssists(): List<SharedKeyboardAssist> {
        return keyboardAssistRepository.list()
    }

    fun loadRuleSubs(): List<SharedRuleSub> {
        return ruleSubRepository.list()
    }

    fun loadRawConfigs(): List<SharedRawConfigEntry> {
        return rawConfigRepository.list()
    }

    fun loadBookmarks(): List<SharedBookmark> {
        return bookmarkRepository.list()
    }

    fun loadCookies(): List<SharedCookie> {
        return cookieRepository.list()
    }

    fun loadCacheEntries(): List<SharedCacheEntry> {
        return cacheEntryRepository.list()
    }

    fun loadReaderPreferences(): SharedReaderPreferences {
        return readerPreferencesRepository.load()
    }

    fun saveReaderPreferences(preferences: SharedReaderPreferences): SharedReaderPreferences {
        return readerPreferencesRepository.save(preferences)
    }

    fun loadSearchKeywords(): List<SharedSearchKeyword> {
        return searchCoordinator.listKeywords()
    }

    fun loadSearchBooks(): List<SharedSearchBook> {
        return searchCoordinator.listSearchBooks()
    }

    fun loadChangeSourceCandidates(book: SharedBook, key: String = ""): List<SharedSearchBook> {
        return searchCoordinator.listChangeSourceCandidates(book, sourceRepository.list(), key)
    }

    fun searchReaderContent(content: String, query: String, contextChars: Int = 40): List<SharedReaderSearchResult> {
        return readerSearchService.search(content, query, contextChars)
    }

    fun loadReadRecords(): List<SharedReadRecord> {
        return readRecordRepository.list()
    }

    fun recordReadTime(
        book: SharedBook,
        durationMillis: Long,
        nowMillis: Long = 0L,
        deviceId: String = "ios"
    ): List<SharedReadRecord> {
        return readRecordRepository.record(book, durationMillis, nowMillis, deviceId)
    }

    fun deleteReadRecord(deviceId: String, bookName: String): List<SharedReadRecord> {
        return readRecordRepository.delete(deviceId, bookName)
    }

    fun clearReadRecords(): List<SharedReadRecord> {
        return readRecordRepository.clear()
    }

    fun deleteSearchKeyword(word: String): List<SharedSearchKeyword> {
        return searchCoordinator.deleteKeyword(word)
    }

    fun clearSearchKeywords(): List<SharedSearchKeyword> {
        return searchCoordinator.clearKeywords()
    }

    fun deleteSearchBook(bookUrl: String): List<SharedSearchBook> {
        return searchCoordinator.deleteSearchBook(bookUrl)
    }

    fun clearSearchBooks(): List<SharedSearchBook> {
        return searchCoordinator.clearSearchBooks()
    }

    fun clearExpiredSearchBooks(beforeMillis: Long): List<SharedSearchBook> {
        return searchCoordinator.clearExpiredSearchBooks(beforeMillis)
    }

    fun upsertCookie(cookie: SharedCookie): SharedCookie {
        return cookieRepository.upsert(cookie)
    }

    fun deleteCookie(url: String): List<SharedCookie> {
        return cookieRepository.delete(url)
    }

    fun clearCookies(): List<SharedCookie> {
        return cookieRepository.clear()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveCookies(json: String, replace: Boolean = false): List<SharedCookie> {
        return cookieRepository.importJson(json, replace)
    }

    suspend fun importCookiesFromUrl(url: String, replace: Boolean = false): List<SharedCookie> {
        return cookieRepository.importJson(fetchRemoteJson(url, "Cookie URL is empty"), replace)
    }

    fun exportCookiesJson(): String {
        return cookieRepository.exportJson()
    }

    fun upsertCacheEntry(entry: SharedCacheEntry): SharedCacheEntry {
        return cacheEntryRepository.upsert(entry)
    }

    fun deleteCacheEntry(key: String): List<SharedCacheEntry> {
        return cacheEntryRepository.delete(key)
    }

    fun clearExpiredCacheEntries(nowMillis: Long = 0L): List<SharedCacheEntry> {
        return cacheEntryRepository.clearExpired(nowMillis)
    }

    fun clearCacheEntries(): List<SharedCacheEntry> {
        return cacheEntryRepository.clear()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveCacheEntries(json: String, replace: Boolean = false): List<SharedCacheEntry> {
        return cacheEntryRepository.importJson(json, replace)
    }

    suspend fun importCacheEntriesFromUrl(url: String, replace: Boolean = false): List<SharedCacheEntry> {
        return cacheEntryRepository.importJson(fetchRemoteJson(url, "Cache URL is empty"), replace)
    }

    fun exportCacheEntriesJson(): String {
        return cacheEntryRepository.exportJson()
    }

    fun recordSearchKeyword(key: String, nowMillis: Long = 0L): List<SharedSearchKeyword> {
        searchCoordinator.recordKeyword(key, nowMillis)
        return loadSearchKeywords()
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

    @Throws(IllegalArgumentException::class)
    fun upsertBookSourceJson(json: String): SharedBookSource {
        return sourceRepository.upsertJson(json)
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

    fun buildSourceWebLoginRequest(source: SharedBookSource): SharedSourceLoginRequest? {
        return sourceLoginService.buildWebLoginRequest(source)
    }

    fun saveSourceWebLoginCookie(source: SharedBookSource, cookie: String): SharedCookie {
        return cookieRepository.upsert(SharedCookie(url = source.bookSourceUrl, cookie = cookie))
    }

    fun loadSourceLoginFields(source: SharedBookSource): List<SharedLoginUiField> {
        return sourceLoginService.loadLoginUiFields(source)
    }

    fun loadSourceLoginInfoJson(source: SharedBookSource): String {
        return cacheEntryRepository.getValue(source.loginInfoKey())
            ?: sourceLoginService.defaultLoginInfoJson(source)
    }

    fun saveSourceLoginInfoJson(source: SharedBookSource, json: String): SharedCacheEntry {
        val normalized = sourceLoginService.encodeLoginInfoJson(sourceLoginService.decodeLoginInfoJson(json))
        return cacheEntryRepository.putValue(source.loginInfoKey(), normalized)
    }

    fun clearSourceLoginInfo(source: SharedBookSource): List<SharedCacheEntry> {
        return cacheEntryRepository.delete(source.loginInfoKey())
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveReplaceRules(json: String, replace: Boolean = false): List<SharedReplaceRule> {
        return replacementRepository.importJson(json, replace)
    }

    suspend fun importReplaceRulesFromUrl(url: String, replace: Boolean = false): List<SharedReplaceRule> {
        return replacementRepository.importJson(fetchRemoteJson(url, "Replace rule URL is empty"), replace)
    }

    fun upsertReplaceRule(rule: SharedReplaceRule): SharedReplaceRule {
        return replacementRepository.upsert(rule)
    }

    @Throws(IllegalArgumentException::class)
    fun upsertReplaceRuleJson(json: String): SharedReplaceRule {
        return replacementRepository.upsertJson(json)
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

    suspend fun importDictRulesFromUrl(url: String, replace: Boolean = false): List<SharedDictRule> {
        return dictRuleRepository.importJson(fetchRemoteJson(url, "Dictionary rule URL is empty"), replace)
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

    suspend fun lookupDictionary(word: String): List<SharedDictionaryLookupResult> {
        return dictionaryLookupService.lookup(word)
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveHttpTts(json: String, replace: Boolean = false): List<SharedHttpTts> {
        return httpTtsRepository.importJson(json, replace)
    }

    suspend fun importHttpTtsFromUrl(url: String, replace: Boolean = false): List<SharedHttpTts> {
        return httpTtsRepository.importJson(fetchRemoteJson(url, "HTTP TTS URL is empty"), replace)
    }

    fun buildHttpTtsAudioRequest(engine: SharedHttpTts, text: String, speechRate: Int = 15): SharedHttpRequest {
        return httpTtsRequestFactory.build(engine, text, speechRate)
    }

    @Throws(IllegalArgumentException::class)
    fun upsertHttpTtsJson(json: String): SharedHttpTts {
        return httpTtsRepository.upsertJson(json)
    }

    fun deleteHttpTts(id: Long): List<SharedHttpTts> {
        return httpTtsRepository.delete(id)
    }

    fun exportHttpTtsJson(): String {
        return httpTtsRepository.exportJson()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveTxtTocRules(json: String, replace: Boolean = false): List<SharedTxtTocRule> {
        return txtTocRuleRepository.importJson(json, replace)
    }

    suspend fun importTxtTocRulesFromUrl(url: String, replace: Boolean = false): List<SharedTxtTocRule> {
        return txtTocRuleRepository.importJson(fetchRemoteJson(url, "TXT TOC rule URL is empty"), replace)
    }

    fun upsertTxtTocRule(rule: SharedTxtTocRule): SharedTxtTocRule {
        return txtTocRuleRepository.upsert(rule)
    }

    fun setTxtTocRuleEnabled(id: Long, enabled: Boolean): SharedTxtTocRule? {
        return txtTocRuleRepository.setEnabled(id, enabled)
    }

    fun deleteTxtTocRule(id: Long): List<SharedTxtTocRule> {
        return txtTocRuleRepository.delete(id)
    }

    fun exportTxtTocRulesJson(): String {
        return txtTocRuleRepository.exportJson()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveServers(json: String, replace: Boolean = false): List<SharedServer> {
        return serverRepository.importJson(json, replace)
    }

    suspend fun importServersFromUrl(url: String, replace: Boolean = false): List<SharedServer> {
        return serverRepository.importJson(fetchRemoteJson(url, "Server URL is empty"), replace)
    }

    fun upsertServer(server: SharedServer): SharedServer {
        return serverRepository.upsert(server)
    }

    fun deleteServer(id: Long): List<SharedServer> {
        return serverRepository.delete(id)
    }

    fun exportServersJson(): String {
        return serverRepository.exportJson()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveKeyboardAssists(json: String, replace: Boolean = false): List<SharedKeyboardAssist> {
        return keyboardAssistRepository.importJson(json, replace)
    }

    suspend fun importKeyboardAssistsFromUrl(url: String, replace: Boolean = false): List<SharedKeyboardAssist> {
        return keyboardAssistRepository.importJson(fetchRemoteJson(url, "Keyboard assist URL is empty"), replace)
    }

    fun upsertKeyboardAssist(assist: SharedKeyboardAssist): SharedKeyboardAssist {
        return keyboardAssistRepository.upsert(assist)
    }

    fun deleteKeyboardAssist(type: Int, key: String): List<SharedKeyboardAssist> {
        return keyboardAssistRepository.delete(type, key)
    }

    fun exportKeyboardAssistsJson(): String {
        return keyboardAssistRepository.exportJson()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveRuleSubs(json: String, replace: Boolean = false): List<SharedRuleSub> {
        return ruleSubRepository.importJson(json, replace)
    }

    suspend fun importRuleSubsFromUrl(url: String, replace: Boolean = false): List<SharedRuleSub> {
        return ruleSubRepository.importJson(fetchRemoteJson(url, "Rule subscription URL is empty"), replace)
    }

    fun upsertRuleSub(ruleSub: SharedRuleSub): SharedRuleSub {
        return ruleSubRepository.upsert(ruleSub)
    }

    fun setRuleSubAutoUpdate(id: Long, autoUpdate: Boolean): SharedRuleSub? {
        return ruleSubRepository.setAutoUpdate(id, autoUpdate)
    }

    fun deleteRuleSub(id: Long): List<SharedRuleSub> {
        return ruleSubRepository.delete(id)
    }

    suspend fun updateRuleSub(ruleSub: SharedRuleSub, nowMillis: Long = 0L): RuleSubUpdateResult {
        return ruleSubUpdateService.update(ruleSub, nowMillis)
    }

    suspend fun updateAutoRuleSubs(nowMillis: Long): List<RuleSubUpdateResult> {
        return ruleSubUpdateService.updateAuto(nowMillis)
    }

    fun exportRuleSubsJson(): String {
        return ruleSubRepository.exportJson()
    }

    @Throws(IllegalArgumentException::class)
    fun importAndSaveRawConfigs(json: String, replace: Boolean = false): List<SharedRawConfigEntry> {
        return rawConfigRepository.importJson(json, replace)
    }

    suspend fun importRawConfigsFromUrl(url: String, replace: Boolean = false): List<SharedRawConfigEntry> {
        return rawConfigRepository.importJson(fetchRemoteJson(url, "Raw config URL is empty"), replace)
    }

    fun upsertRawConfig(key: String, value: String): SharedRawConfigEntry {
        return rawConfigRepository.upsert(key, value)
    }

    fun deleteRawConfig(key: String): List<SharedRawConfigEntry> {
        return rawConfigRepository.delete(key)
    }

    fun exportRawConfigsJson(): String {
        return rawConfigRepository.exportJson()
    }

    fun exportBackupJson(nowMillis: Long = 0L): String {
        return dataBackupService.exportJson(nowMillis)
    }

    suspend fun uploadBackupToWebDav(
        server: SharedServer,
        fileName: String,
        nowMillis: Long = 0L
    ): SharedHttpResponse {
        val response = webDavBackupService.uploadBackup(server, fileName, exportBackupJson(nowMillis))
        require(response.isSuccess) { "WebDAV upload failed: HTTP ${response.statusCode}" }
        return response
    }

    suspend fun downloadBackupFromWebDav(
        server: SharedServer,
        fileName: String
    ): SharedDataSnapshot {
        return importBackupJson(webDavBackupService.downloadBackup(server, fileName))
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

    fun updateBookMetadata(
        book: SharedBook,
        name: String,
        author: String,
        customIntro: String?,
        customCoverUrl: String?,
        customTag: String?
    ): SharedBook {
        return bookshelfService.updateMetadata(book, name, author, customIntro, customCoverUrl, customTag)
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

    suspend fun loadExplorePage(
        source: SharedBookSource,
        kind: SharedExploreKind,
        page: Int = 1
    ): SearchPageResult {
        return exploreService.loadExplore(source, kind, page)
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

    private suspend fun fetchRemoteJson(url: String, emptyMessage: String): String {
        val requestUrl = url.trim()
        require(requestUrl.isNotEmpty()) { emptyMessage }
        return httpFetcher.fetch(SharedRequestBuilder.build(requestUrl)).body
    }
}

private fun SharedBookSource.loginInfoKey(): String = "userInfo_$bookSourceUrl"

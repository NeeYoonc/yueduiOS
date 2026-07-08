package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.book.BookshelfService
import io.legado.shared.book.SearchCoordinator
import io.legado.shared.book.SearchCoordinatorResult
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleWebViewRuntime
import io.legado.shared.service.RuleEngineBookInfoParser
import io.legado.shared.service.RuleEngineChapterContentParser
import io.legado.shared.service.RuleEngineChapterListParser
import io.legado.shared.service.RuleEngineSearchResultParser
import io.legado.shared.service.ReadingFlowResult
import io.legado.shared.source.DefaultDataImporter
import io.legado.shared.source.DefaultDataPayload
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

    @Throws(IllegalArgumentException::class)
    fun importAndSaveBookSources(json: String): List<SharedBookSource> {
        return client.importBookSources(json).also { sources ->
            libraryStore.saveBookSources(sources)
        }
    }

    fun loadBookSources(): List<SharedBookSource> {
        return libraryStore.loadBookSources()
    }

    fun saveBooks(books: List<SharedBook>) {
        libraryStore.saveBooks(books)
    }

    fun loadBooks(): List<SharedBook> {
        return libraryStore.loadBooks()
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
            libraryStore.saveBookSources(snapshot.bookSources)
        }
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
            if (book != null) {
                bookshelfService.upsertBook(book)
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

import Foundation
import LegadoShared

@MainActor
final class AppState: ObservableObject {
    @Published var sourceJson: String = DefaultSource.json
    @Published var keyword: String = ""
    @Published var selectedSourceIndex: Int = -1

    @Published private(set) var sources: [SharedBookSource] = []
    @Published private(set) var books: [SharedBook] = []
    @Published private(set) var rssSources: [SharedRssSource] = []
    @Published private(set) var rssArticles: [SharedRssArticle] = []
    @Published private(set) var selectedRssSource: SharedRssSource?
    @Published private(set) var selectedRssArticle: SharedRssArticle?
    @Published private(set) var rssContent: String = ""
    @Published private(set) var searchResults: [SharedSearchBook] = []
    @Published private(set) var searchErrors: [SourceSearchError] = []
    @Published private(set) var selectedBook: SharedBook?
    @Published private(set) var selectedSearchBook: SharedSearchBook?
    @Published private(set) var chapters: [SharedBookChapter] = []
    @Published private(set) var currentChapter: SharedBookChapter?
    @Published private(set) var currentChapterIndex: Int = 0
    @Published private(set) var currentContent: String = ""
    @Published private(set) var debugSteps: [SourceDebugStep] = []
    @Published private(set) var debugContent: String = ""
    @Published private(set) var message: String?
    @Published private(set) var isLoading: Bool = false

    private let runtime = DarwinLegadoRuntime()
    private var activeSource: SharedBookSource?

    init() {
        refreshLibrary()
        if sources.isEmpty {
            importBundledDefaultData()
        }
    }

    func refreshLibrary() {
        sources = runtime.loadBookSources() as? [SharedBookSource] ?? []
        books = runtime.loadBooks() as? [SharedBook] ?? []
        rssSources = runtime.loadRssSources() as? [SharedRssSource] ?? []
        if selectedSourceIndex >= sources.count {
            selectedSourceIndex = sources.isEmpty ? -1 : 0
        }
    }

    func importBundledDefaultData() {
        runtime.importAndSaveDefaultData(payload: DefaultDataBundle.payload())
        refreshLibrary()
        message = sources.isEmpty ? "No bundled sources" : "Imported \(sources.count) source(s)"
    }

    func importSources() {
        let rawJson = sourceJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Source JSON is empty"
            return
        }
        do {
            sources = try runtime.importAndSaveBookSources(json: rawJson) as? [SharedBookSource] ?? []
            selectedSourceIndex = sources.isEmpty ? -1 : 0
            refreshLibrary()
            message = sources.isEmpty ? "No usable sources" : "Imported \(sources.count) source(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportSourcesToEditor() {
        sourceJson = runtime.exportBookSourcesJson()
        message = "Exported \(sources.count) source(s)"
    }

    func setSourceEnabled(_ source: SharedBookSource, enabled: Bool) {
        _ = runtime.setBookSourceEnabled(bookSourceUrl: source.bookSourceUrl, enabled: enabled)
        refreshLibrary()
    }

    func deleteSource(_ source: SharedBookSource) {
        _ = runtime.deleteBookSource(bookSourceUrl: source.bookSourceUrl)
        refreshLibrary()
    }

    func debugSource(_ source: SharedBookSource) async {
        let text = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            message = "Keyword is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let result = try await runtime.debugSourceFirstContent(
                source: source,
                key: text,
                page: 1
            )
            debugSteps = result.steps as? [SourceDebugStep] ?? []
            debugContent = result.content?.content.content ?? ""
            message = debugSteps.isEmpty ? "No debug steps" : nil
        } catch {
            debugSteps = []
            debugContent = ""
            message = error.localizedDescription
        }
    }

    func refreshRss(_ source: SharedRssSource) async {
        isLoading = true
        defer { isLoading = false }

        do {
            let page = try await runtime.refreshRssArticles(source: source, page: 1)
            selectedRssSource = source
            rssArticles = page.articles as? [SharedRssArticle] ?? []
            selectedRssArticle = nil
            rssContent = ""
            refreshLibrary()
            message = rssArticles.isEmpty ? "No RSS articles" : nil
        } catch {
            rssArticles = runtime.loadRssArticles(source: source) as? [SharedRssArticle] ?? []
            selectedRssSource = source
            message = error.localizedDescription
        }
    }

    func openRssArticle(_ article: SharedRssArticle) async {
        guard let source = selectedRssSource ?? rssSources.first(where: { $0.sourceUrl == article.origin }) else {
            message = "RSS source not found"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let parsed = try await runtime.loadRssContent(source: source, article: article)
            selectedRssArticle = parsed
            rssContent = parsed.readableContent
            refreshLibrary()
        } catch {
            selectedRssArticle = article
            rssContent = article.readableContent
            message = error.localizedDescription
        }
    }

    func search() async {
        let text = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            message = "Keyword is empty"
            return
        }
        guard !sources.isEmpty else {
            message = "No sources"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            if sources.indices.contains(selectedSourceIndex) {
                let result = try await runtime.client.search(
                    source: sources[selectedSourceIndex],
                    key: text,
                    page: 1
                )
                searchResults = result.books as? [SharedSearchBook] ?? []
                searchErrors = []
            } else {
                let result = try await runtime.searchEnabledSources(
                    key: text,
                    page: 1,
                    nowMillis: nowMillis()
                )
                searchResults = result.books as? [SharedSearchBook] ?? []
                searchErrors = result.errors as? [SourceSearchError] ?? []
            }
            message = searchResults.isEmpty ? "No results" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func openSearchResult(_ searchBook: SharedSearchBook) async {
        guard let source = source(for: searchBook) else {
            message = "Source not found"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let detail = try await runtime.openSearchBook(
                source: source,
                searchBook: searchBook,
                nowMillis: nowMillis()
            )
            activeSource = source
            selectedSearchBook = searchBook
            selectedBook = detail.book
            chapters = detail.chapters as? [SharedBookChapter] ?? []
            currentChapter = nil
            currentContent = ""
            refreshLibrary()
            message = chapters.isEmpty ? "No chapters" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func openBook(_ book: SharedBook) async {
        guard let source = source(for: book) else {
            selectedBook = book
            chapters = runtime.loadBookChapters(book: book) as? [SharedBookChapter] ?? []
            activeSource = nil
            currentChapter = nil
            currentContent = ""
            message = chapters.isEmpty ? "Source not found" : nil
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let cachedChapters = runtime.loadBookChapters(book: book) as? [SharedBookChapter] ?? []
            if cachedChapters.isEmpty {
                let detail = try await runtime.refreshBook(
                    source: source,
                    book: book,
                    nowMillis: nowMillis()
                )
                selectedBook = detail.book
                chapters = detail.chapters as? [SharedBookChapter] ?? []
            } else {
                selectedBook = book
                chapters = cachedChapters
            }
            activeSource = source
            currentChapter = nil
            currentContent = ""
            refreshLibrary()
            message = chapters.isEmpty ? "No chapters" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func loadChapter(at index: Int) async {
        guard let book = selectedBook else {
            message = "No book selected"
            return
        }
        guard chapters.indices.contains(index) else {
            message = "Chapter not found"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            guard let source = activeSource ?? source(for: book) else {
                let result = try runtime.loadCachedChapter(
                    book: book,
                    chapterIndex: Int32(index),
                    position: 0,
                    nowMillis: nowMillis()
                )
                selectedBook = result.book
                chapters = result.chapters as? [SharedBookChapter] ?? chapters
                currentChapter = result.chapter
                currentChapterIndex = Int(result.chapterIndex)
                currentContent = result.content.content
                refreshLibrary()
                message = currentContent.isEmpty ? "No content" : nil
                return
            }
            let result = try await runtime.loadChapter(
                source: source,
                book: book,
                chapterIndex: Int32(index),
                position: 0,
                nowMillis: nowMillis(),
                preloadAdjacent: true
            )
            activeSource = source
            selectedBook = result.book
            chapters = result.chapters as? [SharedBookChapter] ?? chapters
            currentChapter = result.chapter
            currentChapterIndex = Int(result.chapterIndex)
            currentContent = result.content.content
            refreshLibrary()
            message = currentContent.isEmpty ? "No content" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func importLocalTextFile(fileName: String, text: String) {
        do {
            let result = try runtime.importLocalTextBook(
                fileName: fileName,
                text: text,
                nowMillis: nowMillis()
            )
            selectedBook = result.book
            chapters = result.chapters as? [SharedBookChapter] ?? []
            activeSource = nil
            currentChapter = nil
            currentContent = ""
            refreshLibrary()
            message = "Imported \(result.book.name)"
        } catch {
            message = error.localizedDescription
        }
    }

    func showMessage(_ text: String) {
        message = text
    }

    func clearMessage() {
        message = nil
    }

    private func source(for searchBook: SharedSearchBook) -> SharedBookSource? {
        if let source = sources.first(where: { $0.bookSourceUrl == searchBook.origin }) {
            return source
        }
        if sources.indices.contains(selectedSourceIndex) {
            return sources[selectedSourceIndex]
        }
        return sources.first { $0.enabled }
    }

    private func source(for book: SharedBook) -> SharedBookSource? {
        if let source = sources.first(where: { $0.bookSourceUrl == book.origin }) {
            return source
        }
        return sources.first { $0.bookSourceUrl == book.bookUrl || $0.bookSourceName == book.originName }
    }

    private func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}

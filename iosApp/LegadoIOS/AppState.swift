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
    @Published private(set) var searchResults: [SharedSearchBook] = []
    @Published private(set) var searchErrors: [SourceSearchError] = []
    @Published private(set) var selectedBook: SharedBook?
    @Published private(set) var selectedSearchBook: SharedSearchBook?
    @Published private(set) var chapters: [SharedBookChapter] = []
    @Published private(set) var currentChapter: SharedBookChapter?
    @Published private(set) var currentChapterIndex: Int = 0
    @Published private(set) var currentContent: String = ""
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
        let snapshot = runtime.libraryStore.loadDataSnapshot()
        rssSources = snapshot.rssSources as? [SharedRssSource] ?? []
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
        guard let source = activeSource ?? source(for: book) else {
            message = "Source not found"
            return
        }
        guard chapters.indices.contains(index) else {
            message = "Chapter not found"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
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

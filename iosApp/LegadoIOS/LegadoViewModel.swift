import Foundation
import LegadoShared

@MainActor
final class LegadoViewModel: ObservableObject {
    @Published var sourceJson: String = DefaultSource.json
    @Published var keyword: String = ""
    @Published var selectedSourceIndex: Int = -1
    @Published private(set) var sources: [SharedBookSource] = []
    @Published private(set) var books: [SharedBook] = []
    @Published private(set) var searchResults: [SharedSearchBook] = []
    @Published private(set) var searchErrors: [SourceSearchError] = []
    @Published private(set) var bookTitle: String?
    @Published private(set) var chapterTitle: String?
    @Published private(set) var chapterContent: String = ""
    @Published private(set) var message: String?
    @Published private(set) var isLoading: Bool = false

    private let runtime = DarwinLegadoRuntime()

    init() {
        refreshLibrary()
        if sources.isEmpty {
            importBundledDefaultData()
        }
    }

    func refreshLibrary() {
        sources = runtime.loadBookSources() as? [SharedBookSource] ?? []
        books = runtime.loadBooks() as? [SharedBook] ?? []
        if !sources.indices.contains(selectedSourceIndex) {
            selectedSourceIndex = sources.isEmpty ? -1 : -1
        }
    }

    func importBundledDefaultData() {
        runtime.importAndSaveDefaultData(payload: DefaultDataBundle.payload())
        refreshLibrary()
        message = sources.isEmpty
            ? "Bundled default data was not found."
            : "Imported \(sources.count) bundled source(s)."
    }

    func importSources() {
        guard !sourceJson.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            message = "Paste source JSON first."
            return
        }

        do {
            sources = try runtime.importAndSaveBookSources(json: sourceJson) as? [SharedBookSource] ?? []
            selectedSourceIndex = sources.isEmpty ? -1 : 0
            message = sources.isEmpty ? "No usable sources" : "Imported \(sources.count) source(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func search() async {
        guard !keyword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            message = "Enter a search keyword."
            return
        }

        isLoading = true
        defer { isLoading = false }

        do {
            if sources.indices.contains(selectedSourceIndex) {
                let result = try await runtime.client.search(
                    source: sources[selectedSourceIndex],
                    key: keyword,
                    page: 1
                )
                searchResults = result.books as? [SharedSearchBook] ?? []
                searchErrors = []
            } else {
                let result = try await runtime.searchEnabledSources(
                    key: keyword,
                    page: 1,
                    nowMillis: Int64(Date().timeIntervalSince1970 * 1000)
                )
                searchResults = result.books as? [SharedSearchBook] ?? []
                searchErrors = result.errors as? [SourceSearchError] ?? []
            }
            message = searchResults.isEmpty ? "No results" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func openFirstResult() async {
        guard !keyword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            message = "Enter a search keyword."
            return
        }

        let source: SharedBookSource?
        if sources.indices.contains(selectedSourceIndex) {
            source = sources[selectedSourceIndex]
        } else {
            source = sources.first { $0.enabled }
        }

        guard let source else {
            message = "No source selected"
            return
        }

        isLoading = true
        defer { isLoading = false }

        do {
            let result = try await runtime.openFirstSearchResult(
                source: source,
                key: keyword,
                page: 1
            )
            bookTitle = result.selectedBook?.name
            chapterTitle = result.selectedChapter?.title
            chapterContent = result.content?.content.content ?? ""
            refreshLibrary()
            message = chapterContent.isEmpty ? "No content returned" : nil
        } catch {
            message = error.localizedDescription
        }
    }
}

import Foundation
import LegadoShared

@MainActor
final class LegadoViewModel: ObservableObject {
    @Published var sourceJson: String = DefaultSource.json
    @Published var keyword: String = "metal"
    @Published var selectedSourceIndex: Int = 0
    @Published private(set) var sources: [SharedBookSource] = []
    @Published private(set) var bookTitle: String?
    @Published private(set) var chapterTitle: String?
    @Published private(set) var chapterContent: String = ""
    @Published private(set) var message: String?
    @Published private(set) var isLoading: Bool = false

    private let runtime = DarwinLegadoRuntime()

    init() {
        sources = runtime.loadBookSources() as? [SharedBookSource] ?? []
    }

    func importSources() {
        do {
            sources = try runtime.importAndSaveBookSources(json: sourceJson) as? [SharedBookSource] ?? []
            selectedSourceIndex = sources.indices.contains(selectedSourceIndex) ? selectedSourceIndex : 0
            message = sources.isEmpty ? "No usable sources" : "Imported \(sources.count) source(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func openFirstResult() async {
        guard sources.indices.contains(selectedSourceIndex) else {
            message = "No source selected"
            return
        }

        isLoading = true
        defer { isLoading = false }

        do {
            let result = try await runtime.openFirstSearchResult(
                source: sources[selectedSourceIndex],
                key: keyword,
                page: 1
            )
            bookTitle = result.selectedBook?.name
            chapterTitle = result.selectedChapter?.title
            chapterContent = result.content?.content.content ?? ""
            message = chapterContent.isEmpty ? "No content returned" : nil
        } catch {
            message = error.localizedDescription
        }
    }
}

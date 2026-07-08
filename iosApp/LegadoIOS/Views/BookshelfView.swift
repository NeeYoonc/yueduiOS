import LegadoShared
import SwiftUI
import UniformTypeIdentifiers

struct BookshelfView: View {
    @EnvironmentObject private var app: AppState
    @State private var isImportingText = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    if app.books.isEmpty {
                        EmptyStateView(title: "No books", systemImage: "books.vertical")
                    } else {
                        ForEach(app.books.indices, id: \.self) { index in
                            let book = app.books[index]
                            NavigationLink {
                                BookDetailView()
                                    .task {
                                        await app.openBook(book)
                                    }
                            } label: {
                                BookRow(book: book)
                            }
                        }
                        .onDelete { offsets in
                            offsets
                                .map { app.books[$0] }
                                .forEach(app.deleteBook)
                        }
                    }
                }
            }
            .navigationTitle("Legado")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        isImportingText = true
                    } label: {
                        Image(systemName: "doc.badge.plus")
                    }

                    Button {
                        app.refreshLibrary()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .fileImporter(
                isPresented: $isImportingText,
                allowedContentTypes: [.plainText, .text]
            ) { result in
                handleImport(result)
            }
        }
    }

    private func handleImport(_ result: Result<URL, Error>) {
        switch result {
        case .success(let url):
            Task {
                do {
                    let payload = try await Task.detached {
                        try LocalTextFilePayload.load(from: url)
                    }.value
                    await app.importLocalTextFile(fileName: payload.fileName, text: payload.text)
                } catch {
                    await app.showMessage(error.localizedDescription)
                }
            }
        case .failure(let error):
            Task {
                await app.showMessage(error.localizedDescription)
            }
        }
    }
}

private struct LocalTextFilePayload {
    let fileName: String
    let text: String

    static func load(from url: URL) throws -> LocalTextFilePayload {
        let accessed = url.startAccessingSecurityScopedResource()
        defer {
            if accessed {
                url.stopAccessingSecurityScopedResource()
            }
        }
        var encoding = String.Encoding.utf8
        let text = try String(contentsOf: url, usedEncoding: &encoding)
        return LocalTextFilePayload(fileName: url.lastPathComponent, text: text)
    }
}

private struct BookRow: View {
    let book: SharedBook

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(book.name)
                .font(.headline)
            Text(book.author.isEmpty ? book.originName : book.author)
                .foregroundStyle(.secondary)
            if let chapter = book.durChapterTitle, !chapter.isEmpty {
                Label(chapter, systemImage: "bookmark")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

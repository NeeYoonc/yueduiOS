import LegadoShared
import SwiftUI
import UniformTypeIdentifiers

struct BookshelfView: View {
    @EnvironmentObject private var app: AppState
    @State private var isImportingLocalBook = false

    var body: some View {
        NavigationStack {
            List {
                Section("Groups") {
                    if app.visibleBookGroups.isEmpty {
                        EmptyStateView(title: "No groups", systemImage: "folder")
                    } else {
                        ForEach(app.visibleBookGroups.indices, id: \.self) { index in
                            let group = app.visibleBookGroups[index]
                            Button {
                                app.selectBookGroup(group)
                            } label: {
                                HStack {
                                    Label(group.groupName, systemImage: groupIcon(group))
                                    Spacer()
                                    if app.selectedBookGroupId == group.groupId {
                                        Image(systemName: "checkmark")
                                            .foregroundStyle(.tint)
                                    }
                                }
                            }
                            .foregroundStyle(.primary)
                        }
                    }
                }

                Section {
                    if app.visibleBooks.isEmpty {
                        EmptyStateView(title: "No books", systemImage: "books.vertical")
                    } else {
                        ForEach(app.visibleBooks.indices, id: \.self) { index in
                            let book = app.visibleBooks[index]
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
                                .map { app.visibleBooks[$0] }
                                .forEach(app.deleteBook)
                        }
                    }
                }
            }
            .navigationTitle("Legado")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        isImportingLocalBook = true
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
                isPresented: $isImportingLocalBook,
                allowedContentTypes: [.data, .plainText, .text]
            ) { result in
                handleImport(result)
            }
        }
    }

    private func groupIcon(_ group: SharedBookGroup) -> String {
        switch group.groupId {
        case -2:
            return "doc.text"
        case -3:
            return "waveform"
        case -6:
            return "play.rectangle"
        case -11:
            return "exclamationmark.triangle"
        default:
            return group.groupId > 0 ? "folder" : "books.vertical"
        }
    }

    private func handleImport(_ result: Result<URL, Error>) {
        switch result {
        case .success(let url):
            Task {
                do {
                    let payload = try await Task.detached {
                        try LocalBookFilePayload.load(from: url)
                    }.value
                    switch payload {
                    case .text(let fileName, let text):
                        await app.importLocalTextFile(fileName: fileName, text: text)
                    case .document(let fileName, let filePath, let mimeType):
                        await app.importLocalDocumentFile(fileName: fileName, filePath: filePath, mimeType: mimeType)
                    }
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

private enum LocalBookFilePayload {
    case text(fileName: String, text: String)
    case document(fileName: String, filePath: String, mimeType: String?)

    static func load(from url: URL) throws -> LocalBookFilePayload {
        let accessed = url.startAccessingSecurityScopedResource()
        defer {
            if accessed {
                url.stopAccessingSecurityScopedResource()
            }
        }
        let fileName = url.lastPathComponent
        let type = UTType(filenameExtension: url.pathExtension)
        if shouldImportAsText(url: url, type: type) {
            var encoding = String.Encoding.utf8
            let text = try String(contentsOf: url, usedEncoding: &encoding)
            return .text(fileName: fileName, text: text)
        }

        let copiedUrl = try copyIntoDocuments(url: url)
        return .document(
            fileName: fileName,
            filePath: copiedUrl.path,
            mimeType: type?.preferredMIMEType
        )
    }

    private static func shouldImportAsText(url: URL, type: UTType?) -> Bool {
        if type?.conforms(to: .text) == true {
            return true
        }
        return ["txt", "md", "json", "html", "htm", "xml"].contains(url.pathExtension.lowercased())
    }

    private static func copyIntoDocuments(url: URL) throws -> URL {
        let fileManager = FileManager.default
        let documents = try fileManager.url(
            for: .documentDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let directory = documents.appendingPathComponent("ImportedBooks", isDirectory: true)
        try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)

        let destination = uniqueDestination(
            directory: directory,
            fileName: url.lastPathComponent,
            fileManager: fileManager
        )
        try fileManager.copyItem(at: url, to: destination)
        return destination
    }

    private static func uniqueDestination(directory: URL, fileName: String, fileManager: FileManager) -> URL {
        let baseName = fileName.isEmpty ? "ImportedBook" : fileName
        var candidate = directory.appendingPathComponent(baseName)
        guard fileManager.fileExists(atPath: candidate.path) else {
            return candidate
        }

        let name = (baseName as NSString).deletingPathExtension
        let ext = (baseName as NSString).pathExtension
        var index = 1
        repeat {
            let nextName = ext.isEmpty ? "\(name)-\(index)" : "\(name)-\(index).\(ext)"
            candidate = directory.appendingPathComponent(nextName)
            index += 1
        } while fileManager.fileExists(atPath: candidate.path)
        return candidate
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

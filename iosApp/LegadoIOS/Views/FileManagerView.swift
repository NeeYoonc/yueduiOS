import SwiftUI

struct FileManagerView: View {
    @EnvironmentObject private var app: AppState
    @State private var files: [ImportedFileItem] = []

    var body: some View {
        List {
            Section {
                if files.isEmpty {
                    EmptyStateView(title: "No imported files", systemImage: "folder")
                } else {
                    ForEach(files) { file in
                        NavigationLink {
                            DocumentPreviewView(url: file.url)
                                .navigationTitle(file.name)
                                .navigationBarTitleDisplayMode(.inline)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(file.name)
                                    .font(.headline)
                                Text("\(file.sizeText) · \(file.modifiedText)")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .onDelete(perform: deleteFiles)
                }
            } header: {
                Text("ImportedBooks")
            } footer: {
                Text("This folder stores non-text local books copied through the bookshelf import button.")
            }
        }
        .navigationTitle("File Manager")
        .toolbar {
            Button {
                reloadFiles()
            } label: {
                Image(systemName: "arrow.clockwise")
            }
        }
        .task {
            reloadFiles()
        }
    }

    private func reloadFiles() {
        do {
            files = try ImportedFilesStore.load()
        } catch {
            files = []
            app.showMessage(error.localizedDescription)
        }
    }

    private func deleteFiles(_ offsets: IndexSet) {
        do {
            for index in offsets {
                try ImportedFilesStore.delete(files[index])
            }
            reloadFiles()
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }
}

private struct ImportedFileItem: Identifiable {
    let url: URL
    let size: Int64
    let modified: Date?

    var id: String {
        url.path
    }

    var name: String {
        url.lastPathComponent
    }

    var sizeText: String {
        ByteCountFormatter.string(fromByteCount: size, countStyle: .file)
    }

    var modifiedText: String {
        guard let modified else {
            return "Unknown date"
        }
        return modified.formatted(date: .abbreviated, time: .shortened)
    }
}

private enum ImportedFilesStore {
    static func load() throws -> [ImportedFileItem] {
        let directory = try importedBooksDirectory(create: false)
        let fileManager = FileManager.default
        guard fileManager.fileExists(atPath: directory.path) else {
            return []
        }
        return try fileManager
            .contentsOfDirectory(
                at: directory,
                includingPropertiesForKeys: [.fileSizeKey, .contentModificationDateKey, .isDirectoryKey],
                options: [.skipsHiddenFiles]
            )
            .compactMap { url in
                let values = try url.resourceValues(forKeys: [.fileSizeKey, .contentModificationDateKey, .isDirectoryKey])
                guard values.isDirectory != true else {
                    return nil
                }
                return ImportedFileItem(
                    url: url,
                    size: Int64(values.fileSize ?? 0),
                    modified: values.contentModificationDate
                )
            }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    static func delete(_ file: ImportedFileItem) throws {
        try FileManager.default.removeItem(at: file.url)
    }

    private static func importedBooksDirectory(create: Bool) throws -> URL {
        let fileManager = FileManager.default
        let documents = try fileManager.url(
            for: .documentDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let directory = documents.appendingPathComponent("ImportedBooks", isDirectory: true)
        if create {
            try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
        }
        return directory
    }
}

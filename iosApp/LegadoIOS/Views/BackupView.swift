import SwiftUI
import UniformTypeIdentifiers

struct BackupView: View {
    @EnvironmentObject private var app: AppState
    @State private var isExportingBackupFile = false
    @State private var isImportingBackupFile = false

    var body: some View {
        Form {
            Section {
                TextEditor(text: $app.backupJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 360)
            }

            Section {
                Button {
                    app.exportBackupToEditor()
                } label: {
                    Label("Export backup", systemImage: "square.and.arrow.up")
                }

                Button {
                    app.exportBackupToEditor()
                    isExportingBackupFile = true
                } label: {
                    Label("Export backup file", systemImage: "doc.badge.arrow.up")
                }

                Button {
                    app.importBackupFromEditor()
                } label: {
                    Label("Import backup", systemImage: "square.and.arrow.down")
                }

                Button {
                    isImportingBackupFile = true
                } label: {
                    Label("Import backup file", systemImage: "doc.badge.arrow.down")
                }
            }

            Section {
                TextField("Backup file name", text: $app.webDavBackupFileName)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()

                Button {
                    Task {
                        await app.uploadBackupToWebDav()
                    }
                } label: {
                    Label("Upload to first WebDAV server", systemImage: "icloud.and.arrow.up")
                }
                .disabled(app.servers.filter { $0.type.uppercased() == "WEBDAV" }.isEmpty || app.isLoading)

                Button {
                    Task {
                        await app.downloadBackupFromWebDav()
                    }
                } label: {
                    Label("Download and import from WebDAV", systemImage: "icloud.and.arrow.down")
                }
                .disabled(app.servers.filter { $0.type.uppercased() == "WEBDAV" }.isEmpty || app.isLoading)
            } header: {
                Text("WebDAV")
            } footer: {
                Text("Uses the first configured WebDAV server in Remote Servers. The server config JSON should include url, username, and password.")
            }
        }
        .navigationTitle("Backup")
        .navigationBarTitleDisplayMode(.inline)
        .fileExporter(
            isPresented: $isExportingBackupFile,
            document: BackupTextDocument(text: app.backupJson),
            contentType: .json,
            defaultFilename: app.webDavBackupFileName.isEmpty ? "legado-backup.json" : app.webDavBackupFileName
        ) { result in
            if case .failure(let error) = result {
                app.showMessage(error.localizedDescription)
            }
        }
        .fileImporter(
            isPresented: $isImportingBackupFile,
            allowedContentTypes: [.json, .plainText, .text, .data]
        ) { result in
            handleBackupFileImport(result)
        }
    }

    private func handleBackupFileImport(_ result: Result<URL, Error>) {
        switch result {
        case .success(let url):
            Task {
                do {
                    let text = try await Task.detached {
                        try BackupTextDocument.loadText(from: url)
                    }.value
                    await MainActor.run {
                        app.backupJson = text
                        app.importBackupFromEditor()
                    }
                } catch {
                    await app.showMessage(error.localizedDescription)
                }
            }
        case .failure(let error):
            app.showMessage(error.localizedDescription)
        }
    }
}

private struct BackupTextDocument: FileDocument {
    static var readableContentTypes: [UTType] {
        [.json, .plainText, .text]
    }

    var text: String

    init(text: String) {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        if let data = configuration.file.regularFileContents,
           let decoded = String(data: data, encoding: .utf8) {
            text = decoded
        } else {
            text = ""
        }
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }

    static func loadText(from url: URL) throws -> String {
        let accessed = url.startAccessingSecurityScopedResource()
        defer {
            if accessed {
                url.stopAccessingSecurityScopedResource()
            }
        }
        return try String(contentsOf: url, encoding: .utf8)
    }
}

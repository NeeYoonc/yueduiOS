import SwiftUI

struct BackupView: View {
    @EnvironmentObject private var app: AppState

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
                    app.importBackupFromEditor()
                } label: {
                    Label("Import backup", systemImage: "square.and.arrow.down")
                }
            }

            Section("WebDAV") {
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
            } footer: {
                Text("Uses the first configured WebDAV server in Remote Servers. The server config JSON should include url, username, and password.")
            }
        }
        .navigationTitle("Backup")
        .navigationBarTitleDisplayMode(.inline)
    }
}

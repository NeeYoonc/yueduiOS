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
        }
        .navigationTitle("Backup")
        .navigationBarTitleDisplayMode(.inline)
    }
}

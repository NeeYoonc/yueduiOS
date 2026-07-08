import SwiftUI

struct RssSourceEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section("RSS Source JSON") {
                TextEditor(text: $app.rssSourceJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 260)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }

            Section {
                Button {
                    app.importRssSources()
                } label: {
                    Label("Import or update", systemImage: "square.and.arrow.down")
                }

                Button(role: .destructive) {
                    app.importRssSources(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportRssSourcesToEditor()
                } label: {
                    Label("Export current", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("RSS Source JSON")
    }
}

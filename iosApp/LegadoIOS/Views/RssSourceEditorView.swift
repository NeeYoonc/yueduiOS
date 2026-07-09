import SwiftUI

struct RssSourceEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section("Import from URL") {
                TextField("https://example.com/rssSources.json", text: $app.rssSourceImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importRssSourcesFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.rssSourceImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    Task {
                        await app.importRssSourcesFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.rssSourceImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

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

import SwiftUI

struct SourceEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            Section("Import from URL") {
                TextField("https://example.com/bookSources.json", text: $app.sourceImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importSourcesFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.sourceImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button {
                    Task {
                        await app.importSourcesFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.sourceImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section {
                TextEditor(text: $app.sourceJson)
                    .font(.system(.footnote, design: .monospaced))
                    .frame(minHeight: 320)
            }

            Section {
                Button {
                    app.importBundledDefaultData()
                } label: {
                    Label("Import bundled data", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importSources()
                } label: {
                    Label("Import JSON", systemImage: "tray.and.arrow.down")
                }

                Button {
                    app.exportSourcesToEditor()
                } label: {
                    Label("Export JSON", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("Source JSON")
    }
}

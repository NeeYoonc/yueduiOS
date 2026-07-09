import SwiftUI

struct RawConfigEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section("Import from URL") {
                TextField("https://example.com/rawConfigs.json", text: $app.rawConfigImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importRawConfigsFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.rawConfigImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    Task {
                        await app.importRawConfigsFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.rawConfigImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section {
                TextEditor(text: $app.rawConfigJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 320)
            } footer: {
                Text("Import a JSON object map, such as {\"readConfig\":\"{...}\"}, or an array of {key,value} entries.")
            }

            Section {
                Button {
                    app.importRawConfigs()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importRawConfigs(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportRawConfigsToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("Raw Config JSON")
        .navigationBarTitleDisplayMode(.inline)
    }
}

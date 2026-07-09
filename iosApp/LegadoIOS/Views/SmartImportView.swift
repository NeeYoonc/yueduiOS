import SwiftUI

struct SmartImportView: View {
    @EnvironmentObject private var app: AppState

    private var trimmedUrl: String {
        app.smartImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var trimmedJson: String {
        app.smartImportJson.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        Form {
            Section {
                TextField("https://example.com/legado.json", text: $app.smartImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importSmartConfigFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || trimmedUrl.isEmpty)

                Button {
                    Task {
                        await app.importSmartConfigFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace matching category", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || trimmedUrl.isEmpty)
            } header: {
                Text("Import from URL")
            } footer: {
                Text("Automatically detects Android Legado book sources, RSS sources, replace rules, dictionaries, HTTP TTS, TXT TOC rules, servers, keyboard assists, rule subscriptions, raw configs, cookies, cache entries, and backup snapshots.")
            }

            Section {
                TextEditor(text: $app.smartImportJson)
                    .font(.system(.footnote, design: .monospaced))
                    .frame(minHeight: 360)
            } header: {
                Text("JSON")
            } footer: {
                Text("Paste a single JSON object, a JSON array, or a full backup snapshot. Replace only clears the detected category before importing.")
            }

            Section {
                Button {
                    app.importSmartConfig()
                } label: {
                    Label("Import detected config", systemImage: "tray.and.arrow.down")
                }
                .disabled(trimmedJson.isEmpty)

                Button(role: .destructive) {
                    app.importSmartConfig(replace: true)
                } label: {
                    Label("Replace detected category", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(trimmedJson.isEmpty)
            } header: {
                Text("Actions")
            }
        }
        .navigationTitle("Universal Import")
        .navigationBarTitleDisplayMode(.inline)
    }
}

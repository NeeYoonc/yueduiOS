import SwiftUI

struct DictRuleEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section("Import from URL") {
                TextField("https://example.com/dictRules.json", text: $app.dictRuleImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importDictRulesFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.dictRuleImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    Task {
                        await app.importDictRulesFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.dictRuleImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section {
                TextEditor(text: $app.dictRuleJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 320)
            }

            Section {
                Button {
                    app.importDictRules()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importDictRules(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportDictRulesToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("Dictionary JSON")
        .navigationBarTitleDisplayMode(.inline)
    }
}

import SwiftUI

struct ReplaceRuleEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section("Import from URL") {
                TextField("https://example.com/replaceRules.json", text: $app.replaceRuleImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importReplaceRulesFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.replaceRuleImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    Task {
                        await app.importReplaceRulesFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.replaceRuleImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section {
                TextEditor(text: $app.replaceRuleJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 320)
            }

            Section {
                Button {
                    app.importReplaceRules()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importReplaceRules(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportReplaceRulesToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("Rule JSON")
        .navigationBarTitleDisplayMode(.inline)
    }
}

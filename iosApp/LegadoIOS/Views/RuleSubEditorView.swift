import SwiftUI

struct RuleSubEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section("Import from URL") {
                TextField("https://example.com/ruleSubs.json", text: $app.ruleSubImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importRuleSubsFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.ruleSubImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    Task {
                        await app.importRuleSubsFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.ruleSubImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section {
                TextEditor(text: $app.ruleSubJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 320)
            } footer: {
                Text("Import a single rule subscription object or an array exported from Legado.")
            }

            Section {
                Button {
                    app.importRuleSubs()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importRuleSubs(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportRuleSubsToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("RuleSub JSON")
        .navigationBarTitleDisplayMode(.inline)
    }
}

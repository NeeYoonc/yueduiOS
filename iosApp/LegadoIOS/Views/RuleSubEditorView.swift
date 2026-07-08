import SwiftUI

struct RuleSubEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
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

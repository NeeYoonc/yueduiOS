import SwiftUI

struct ReplaceRuleEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
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

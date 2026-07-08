import SwiftUI

struct DictRuleEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
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

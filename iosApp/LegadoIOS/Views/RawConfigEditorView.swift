import SwiftUI

struct RawConfigEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
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

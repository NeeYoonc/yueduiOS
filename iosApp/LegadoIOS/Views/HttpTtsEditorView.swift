import SwiftUI

struct HttpTtsEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section {
                TextEditor(text: $app.httpTtsJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 320)
            }

            Section {
                Button {
                    app.importHttpTts()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importHttpTts(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportHttpTtsToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("HTTP TTS JSON")
        .navigationBarTitleDisplayMode(.inline)
    }
}

import SwiftUI

struct KeyboardAssistEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        Form {
            Section {
                TextEditor(text: $app.keyboardAssistJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 320)
            } footer: {
                Text("Import a single keyboard assist object or an array exported from Legado.")
            }

            Section {
                Button {
                    app.importKeyboardAssists()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importKeyboardAssists(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportKeyboardAssistsToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("Keyboard JSON")
        .navigationBarTitleDisplayMode(.inline)
    }
}

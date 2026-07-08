import SwiftUI

struct SourceEditorView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            Section {
                TextEditor(text: $app.sourceJson)
                    .font(.system(.footnote, design: .monospaced))
                    .frame(minHeight: 320)
            }

            Section {
                Button {
                    app.importBundledDefaultData()
                } label: {
                    Label("Import bundled data", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importSources()
                } label: {
                    Label("Import JSON", systemImage: "tray.and.arrow.down")
                }

                Button {
                    app.exportSourcesToEditor()
                } label: {
                    Label("Export JSON", systemImage: "square.and.arrow.up")
                }
            }
        }
        .navigationTitle("Source JSON")
    }
}

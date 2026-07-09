import SwiftUI

struct AboutView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            Section("Legado iOS") {
                LabeledContent("Migration", value: "Android parity")
                LabeledContent("Sources", value: "\(app.sources.count)")
                LabeledContent("Books", value: "\(app.books.count)")
                LabeledContent("RSS", value: "\(app.rssSources.count)")
                LabeledContent("Rules", value: "\(app.replaceRules.count + app.dictRules.count + app.httpTts.count)")
            }

            Section("Repository") {
                Link("NeeYoonc/yueduiOS", destination: URL(string: "https://github.com/NeeYoonc/yueduiOS")!)
                Link("Actions", destination: URL(string: "https://github.com/NeeYoonc/yueduiOS/actions")!)
            }

            Section("Privacy") {
                Text("Book data, sources, cookies, imported files, and backups are stored in the app sandbox unless you explicitly export, scan, import, or sync them through a configured server.")
                    .foregroundStyle(.secondary)
            }

            Section("License") {
                Text("Legado Android source compatibility is preserved through the shared migration layer. See the repository license and bundled notices for details.")
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("About")
    }
}

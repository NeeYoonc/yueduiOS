import SwiftUI

struct LogView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.messageLog.isEmpty {
                Section {
                    EmptyStateView(title: "No logs", systemImage: "doc.text.magnifyingglass")
                }
            } else {
                Section {
                    ForEach(app.messageLog.indices, id: \.self) { index in
                        Text(app.messageLog[index])
                            .font(.footnote.monospaced())
                            .textSelection(.enabled)
                            .padding(.vertical, 2)
                    }
                }
            }
        }
        .navigationTitle("Logs")
        .toolbar {
            Button(role: .destructive) {
                app.clearMessageLog()
            } label: {
                Image(systemName: "trash")
            }
            .disabled(app.messageLog.isEmpty)
        }
    }
}

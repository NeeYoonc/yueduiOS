import LegadoShared
import SwiftUI

struct KeyboardAssistListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.keyboardAssists.isEmpty {
                Section {
                    EmptyStateView(title: "No keyboard assists", systemImage: "keyboard")
                }
            } else {
                Section {
                    ForEach(app.keyboardAssists.indices, id: \.self) { index in
                        let assist = app.keyboardAssists[index]
                        NavigationLink {
                            KeyboardAssistFormView(assist: assist)
                        } label: {
                            KeyboardAssistRow(assist: assist)
                        }
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.keyboardAssists[$0] }
                            .forEach(app.deleteKeyboardAssist)
                    }
                } footer: {
                    Text("Keyboard assists mirror Android's configurable reading shortcut buttons.")
                }
            }
        }
        .navigationTitle("Keyboard Assists")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                NavigationLink {
                    KeyboardAssistEditorView()
                } label: {
                    Image(systemName: "doc.text")
                }

                NavigationLink {
                    KeyboardAssistFormView()
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

private struct KeyboardAssistRow: View {
    let assist: SharedKeyboardAssist

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(assist.key)
                    .font(.headline.monospaced())
                Spacer()
                Text("type \(assist.type)")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)
            }

            Text(assist.value)
                .font(.body)
                .textSelection(.enabled)

            Text("order \(assist.serialNo)")
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

import LegadoShared
import SwiftUI

struct RuleSubListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.ruleSubs.isEmpty {
                Section {
                    EmptyStateView(title: "No rule subscriptions", systemImage: "arrow.triangle.2.circlepath")
                }
            } else {
                Section {
                    ForEach(app.ruleSubs.indices, id: \.self) { index in
                        let ruleSub = app.ruleSubs[index]
                        RuleSubRow(ruleSub: ruleSub)
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.ruleSubs[$0] }
                            .forEach(app.deleteRuleSub)
                    }
                } footer: {
                    Text("Rule subscriptions track remote source, RSS, TTS, and rule lists for later refresh/import workflows.")
                }
            }
        }
        .navigationTitle("Rule Subscriptions")
        .toolbar {
            NavigationLink {
                RuleSubEditorView()
            } label: {
                Image(systemName: "doc.text")
            }
        }
    }
}

private struct RuleSubRow: View {
    @EnvironmentObject private var app: AppState
    let ruleSub: SharedRuleSub

    var body: some View {
        Toggle(isOn: Binding(
            get: { ruleSub.autoUpdate },
            set: { app.setRuleSubAutoUpdate(ruleSub, autoUpdate: $0) }
        )) {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(ruleSub.name)
                        .font(.headline)
                    if ruleSub.type != 0 {
                        Text("type \(ruleSub.type)")
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(.secondary)
                    }
                }

                Text(ruleSub.url)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .textSelection(.enabled)

                HStack(spacing: 12) {
                    Text("order \(ruleSub.customOrder)")
                    if ruleSub.updateInterval > 0 {
                        Text("interval \(ruleSub.updateInterval)")
                    }
                    if ruleSub.silentUpdate {
                        Text("silent")
                    }
                }
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
            }
            .padding(.vertical, 4)
        }
    }
}

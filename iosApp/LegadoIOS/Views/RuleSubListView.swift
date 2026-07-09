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
                        NavigationLink {
                            RuleSubFormView(ruleSub: ruleSub)
                        } label: {
                            RuleSubRow(ruleSub: ruleSub)
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                Task {
                                    await app.updateRuleSub(ruleSub)
                                }
                            } label: {
                                Label("Update", systemImage: "arrow.triangle.2.circlepath")
                            }
                            .tint(.blue)

                            Button {
                                app.setRuleSubAutoUpdate(ruleSub, autoUpdate: !ruleSub.autoUpdate)
                            } label: {
                                Label(ruleSub.autoUpdate ? "Manual" : "Auto", systemImage: ruleSub.autoUpdate ? "pause.circle" : "clock.arrow.circlepath")
                            }
                            .tint(ruleSub.autoUpdate ? .orange : .green)
                        }
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
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                Button {
                    Task {
                        await app.updateAutoRuleSubs()
                    }
                } label: {
                    Image(systemName: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading)

                NavigationLink {
                    RuleSubEditorView()
                } label: {
                    Image(systemName: "doc.text")
                }

                NavigationLink {
                    RuleSubFormView()
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

private struct RuleSubRow: View {
    let ruleSub: SharedRuleSub

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: ruleSub.autoUpdate ? "clock.arrow.circlepath" : "pause.circle")
                .foregroundStyle(ruleSub.autoUpdate ? .green : .secondary)
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
        }
        .padding(.vertical, 4)
    }
}

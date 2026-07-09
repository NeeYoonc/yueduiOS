import LegadoShared
import SwiftUI

struct TxtTocRuleListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.txtTocRules.isEmpty {
                Section {
                    EmptyStateView(title: "No TXT TOC rules", systemImage: "list.bullet.rectangle")
                }
            } else {
                Section {
                    ForEach(app.txtTocRules.indices, id: \.self) { index in
                        let rule = app.txtTocRules[index]
                        NavigationLink {
                            TxtTocRuleFormView(rule: rule)
                        } label: {
                            TxtTocRuleRow(rule: rule)
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                app.setTxtTocRuleEnabled(rule, enabled: !rule.enable)
                            } label: {
                                Label(rule.enable ? "Disable" : "Enable", systemImage: rule.enable ? "pause.circle" : "play.circle")
                            }
                            .tint(rule.enable ? .orange : .green)
                        }
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.txtTocRules[$0] }
                            .forEach(app.deleteTxtTocRule)
                    }
                } footer: {
                    Text("TXT TOC rules are used when importing local text books.")
                }
            }
        }
        .navigationTitle("TXT TOC Rules")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                NavigationLink {
                    TxtTocRuleEditorView()
                } label: {
                    Image(systemName: "doc.text")
                }

                NavigationLink {
                    TxtTocRuleFormView()
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

private struct TxtTocRuleRow: View {
    let rule: SharedTxtTocRule

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: rule.enable ? "checkmark.circle.fill" : "pause.circle")
                .foregroundStyle(rule.enable ? .green : .secondary)
            VStack(alignment: .leading, spacing: 5) {
                HStack {
                    Text(rule.name)
                        .font(.headline)
                    if rule.serialNumber >= 0 {
                        Text("#\(rule.serialNumber)")
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(.secondary)
                    }
                }
                Text(rule.rule)
                    .font(.footnote.monospaced())
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                if !rule.replacement.isEmpty {
                    Text("Replacement: \(rule.replacement)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                if let example = rule.example, !example.isEmpty {
                    Text(example)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

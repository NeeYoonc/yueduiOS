import LegadoShared
import SwiftUI

struct ReplaceRuleListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.replaceRules.isEmpty {
                Section {
                    EmptyStateView(title: "No replacement rules", systemImage: "wand.and.stars")
                }
            } else {
                Section {
                    ForEach(app.replaceRules.indices, id: \.self) { index in
                        let rule = app.replaceRules[index]
                        NavigationLink {
                            ReplaceRuleFormView(rule: rule)
                        } label: {
                            ReplaceRuleRow(rule: rule)
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                app.setReplaceRuleEnabled(rule, enabled: !rule.enabled)
                            } label: {
                                Label(rule.enabled ? "Disable" : "Enable", systemImage: rule.enabled ? "pause.circle" : "play.circle")
                            }
                            .tint(rule.enabled ? .orange : .green)
                        }
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.replaceRules[$0] }
                            .forEach(app.deleteReplaceRule)
                    }
                }
            }
        }
        .navigationTitle("Replacements")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                NavigationLink {
                    ReplaceRuleEditorView()
                } label: {
                    Image(systemName: "doc.text")
                }

                NavigationLink {
                    ReplaceRuleFormView()
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

private struct ReplaceRuleRow: View {
    let rule: SharedReplaceRule

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: rule.enabled ? "checkmark.circle.fill" : "pause.circle")
                .foregroundStyle(rule.enabled ? .green : .secondary)
            VStack(alignment: .leading, spacing: 5) {
                    Text(rule.name.isEmpty ? rule.pattern : rule.name)
                        .font(.headline)
                    Text(rule.pattern)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                    HStack(spacing: 8) {
                        if rule.scopeTitle {
                            Label("Title", systemImage: "textformat")
                        }
                        if rule.scopeContent {
                            Label("Content", systemImage: "doc.plaintext")
                        }
                        Text(rule.regex ? "Regex" : "Text")
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
        }
        .padding(.vertical, 4)
    }
}

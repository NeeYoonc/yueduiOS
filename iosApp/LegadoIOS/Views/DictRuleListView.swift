import LegadoShared
import SwiftUI

struct DictRuleListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.dictRules.isEmpty {
                Section {
                    EmptyStateView(title: "No dictionary rules", systemImage: "character.book.closed")
                }
            } else {
                Section {
                    ForEach(app.dictRules.indices, id: \.self) { index in
                        let rule = app.dictRules[index]
                        NavigationLink {
                            DictRuleFormView(rule: rule)
                        } label: {
                            DictRuleRow(rule: rule)
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                app.setDictRuleEnabled(rule, enabled: !rule.enabled)
                            } label: {
                                Label(rule.enabled ? "Disable" : "Enable", systemImage: rule.enabled ? "pause.circle" : "play.circle")
                            }
                            .tint(rule.enabled ? .orange : .green)
                        }
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.dictRules[$0] }
                            .forEach(app.deleteDictRule)
                    }
                }
            }
        }
        .navigationTitle("Dictionaries")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                NavigationLink {
                    DictionaryLookupView()
                } label: {
                    Image(systemName: "magnifyingglass")
                }

                NavigationLink {
                    DictRuleEditorView()
                } label: {
                    Image(systemName: "doc.text")
                }

                NavigationLink {
                    DictRuleFormView()
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

private struct DictRuleRow: View {
    let rule: SharedDictRule

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: rule.enabled ? "checkmark.circle.fill" : "pause.circle")
                .foregroundStyle(rule.enabled ? .green : .secondary)
            VStack(alignment: .leading, spacing: 5) {
                Text(rule.name)
                    .font(.headline)
                Text(rule.urlRule)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                Text(rule.showRule.isEmpty ? "No show rule" : rule.showRule)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
    }
}

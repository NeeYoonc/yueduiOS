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
                        DictRuleRow(rule: rule)
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
        }
    }
}

private struct DictRuleRow: View {
    @EnvironmentObject private var app: AppState
    let rule: SharedDictRule

    var body: some View {
        Toggle(isOn: Binding(
            get: { rule.enabled },
            set: { app.setDictRuleEnabled(rule, enabled: $0) }
        )) {
            VStack(alignment: .leading, spacing: 5) {
                Text(rule.name)
                    .font(.headline)
                Text(rule.urlRule)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            .padding(.vertical, 4)
        }
    }
}

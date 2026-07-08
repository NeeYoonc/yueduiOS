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
                        ReplaceRuleRow(rule: rule)
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
            NavigationLink {
                ReplaceRuleEditorView()
            } label: {
                Image(systemName: "doc.text")
            }
        }
    }
}

private struct ReplaceRuleRow: View {
    @EnvironmentObject private var app: AppState
    let rule: SharedReplaceRule

    var body: some View {
        Toggle(isOn: Binding(
            get: { rule.enabled },
            set: { app.setReplaceRuleEnabled(rule, enabled: $0) }
        )) {
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
            .padding(.vertical, 4)
        }
    }
}

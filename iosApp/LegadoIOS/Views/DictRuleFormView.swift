import LegadoShared
import SwiftUI

struct DictRuleFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let rule: SharedDictRule?

    @State private var name: String
    @State private var urlRule: String
    @State private var showRule: String
    @State private var enabled: Bool
    @State private var sortNumber: String

    init(rule: SharedDictRule? = nil) {
        self.rule = rule
        _name = State(initialValue: rule?.name ?? "")
        _urlRule = State(initialValue: rule?.urlRule ?? "")
        _showRule = State(initialValue: rule?.showRule ?? "")
        _enabled = State(initialValue: rule?.enabled ?? true)
        _sortNumber = State(initialValue: rule.map { String($0.sortNumber) } ?? "0")
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $name)
                TextField("Sort number", text: $sortNumber)
                    .keyboardType(.numbersAndPunctuation)
                Toggle("Enabled", isOn: $enabled)
            } header: {
                Text("Identity")
            } footer: {
                Text("Name is the stable key used to update one dictionary rule without replacing the rest.")
            }

            Section {
                VStack(alignment: .leading, spacing: 6) {
                    Text("URL rule")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $urlRule)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 120)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Show rule")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $showRule)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 140)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            } header: {
                Text("Rules")
            } footer: {
                Text("URL supports Android Legado placeholders such as {{word}} and the show rule is evaluated against the fetched dictionary response.")
            }
        }
        .navigationTitle(rule == nil ? "New Dictionary" : "Edit Dictionary")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    save()
                }
            }
        }
    }

    private func save() {
        do {
            let json = try buildRuleJson()
            if app.saveDictRuleJson(json) {
                dismiss()
            }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildRuleJson() throws -> String {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanUrlRule = urlRule.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanShowRule = showRule.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw DictRuleFormError.message("Dictionary rule name is empty") }
        guard !cleanUrlRule.isEmpty else { throw DictRuleFormError.message("Dictionary URL rule is empty") }
        guard !cleanShowRule.isEmpty else { throw DictRuleFormError.message("Dictionary show rule is empty") }

        let object: [String: Any] = [
            "name": cleanName,
            "urlRule": cleanUrlRule,
            "showRule": cleanShowRule,
            "enabled": enabled,
            "sortNumber": Int(sortNumber.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
        ]
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw DictRuleFormError.message("Dictionary rule JSON encode failed")
        }
        return json
    }
}

private enum DictRuleFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

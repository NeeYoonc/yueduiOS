import LegadoShared
import SwiftUI

struct ReplaceRuleFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let rule: SharedReplaceRule?

    @State private var name: String
    @State private var group: String
    @State private var pattern: String
    @State private var replacement: String
    @State private var scope: String
    @State private var excludeScope: String
    @State private var enabled: Bool
    @State private var regex: Bool
    @State private var scopeTitle: Bool
    @State private var scopeContent: Bool
    @State private var timeout: String
    @State private var order: String

    init(rule: SharedReplaceRule? = nil) {
        self.rule = rule
        _name = State(initialValue: rule?.name ?? "")
        _group = State(initialValue: rule?.group ?? "")
        _pattern = State(initialValue: rule?.pattern ?? "")
        _replacement = State(initialValue: rule?.replacement ?? "")
        _scope = State(initialValue: rule?.scope ?? "")
        _excludeScope = State(initialValue: rule?.excludeScope ?? "")
        _enabled = State(initialValue: rule?.enabled ?? true)
        _regex = State(initialValue: rule?.regex ?? true)
        _scopeTitle = State(initialValue: rule?.scopeTitle ?? false)
        _scopeContent = State(initialValue: rule?.scopeContent ?? true)
        _timeout = State(initialValue: rule.map { String($0.timeoutMillisecond) } ?? "3000")
        _order = State(initialValue: rule.map { String($0.order) } ?? "0")
    }

    var body: some View {
        Form {
            Section("Identity") {
                TextField("Name", text: $name)
                TextField("Group", text: $group)
                TextField("Order", text: $order)
                    .keyboardType(.numbersAndPunctuation)
                Toggle("Enabled", isOn: $enabled)
            }

            Section("Match") {
                Toggle("Regex", isOn: $regex)
                Toggle("Apply to title", isOn: $scopeTitle)
                Toggle("Apply to content", isOn: $scopeContent)
                TextField("Include scope", text: $scope, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Exclude scope", text: $excludeScope, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Timeout ms", text: $timeout)
                    .keyboardType(.numberPad)
            }

            Section("Rule") {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Pattern")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $pattern)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 120)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Replacement")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $replacement)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 120)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            } footer: {
                Text("Fields map to Android Legado replacement rule JSON, including title/content scope, regex mode, timeout, and custom ordering.")
            }
        }
        .navigationTitle(rule == nil ? "New Replace Rule" : "Edit Replace Rule")
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
            if app.saveReplaceRuleJson(json) {
                dismiss()
            }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildRuleJson() throws -> String {
        let cleanPattern = pattern.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanPattern.isEmpty else {
            throw ReplaceRuleFormError.message("Pattern is empty")
        }
        var object: [String: Any] = [
            "id": rule?.id ?? 0,
            "name": name.trimmingCharacters(in: .whitespacesAndNewlines),
            "pattern": cleanPattern,
            "replacement": replacement,
            "isEnabled": enabled,
            "isRegex": regex,
            "scopeTitle": scopeTitle,
            "scopeContent": scopeContent,
            "timeoutMillisecond": Int64(timeout.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 3000,
            "order": Int(order.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
        ]
        add(&object, "group", group)
        add(&object, "scope", scope)
        add(&object, "excludeScope", excludeScope)
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw ReplaceRuleFormError.message("Replace rule JSON encode failed")
        }
        return json
    }

    private func add(_ object: inout [String: Any], _ key: String, _ value: String) {
        let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if !clean.isEmpty {
            object[key] = clean
        }
    }
}

private enum ReplaceRuleFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

import LegadoShared
import SwiftUI

struct TxtTocRuleFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let rule: SharedTxtTocRule?

    @State private var name: String
    @State private var pattern: String
    @State private var replacement: String
    @State private var example: String
    @State private var serialNumber: String
    @State private var enabled: Bool

    init(rule: SharedTxtTocRule? = nil) {
        self.rule = rule
        _name = State(initialValue: rule?.name ?? "")
        _pattern = State(initialValue: rule?.rule ?? "")
        _replacement = State(initialValue: rule?.replacement ?? "")
        _example = State(initialValue: rule?.example ?? "")
        _serialNumber = State(initialValue: rule.map { String($0.serialNumber) } ?? "-1")
        _enabled = State(initialValue: rule?.enable ?? true)
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $name)
                TextField("Serial number", text: $serialNumber)
                    .keyboardType(.numbersAndPunctuation)
                Toggle("Enabled", isOn: $enabled)
            } header: {
                Text("Identity")
            } footer: {
                Text("Serial number controls Android Legado TXT TOC rule order; existing id is preserved while editing.")
            }

            Section {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Rule regex")
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
                        .frame(minHeight: 90)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Example")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $example)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 90)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            } header: {
                Text("Match")
            } footer: {
                Text("These fields map to Android TXT TOC parsing rules used for local text imports.")
            }
        }
        .navigationTitle(rule == nil ? "New TXT TOC" : "Edit TXT TOC")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func save() {
        do {
            let json = try buildRuleJson()
            if app.saveTxtTocRuleJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildRuleJson() throws -> String {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanPattern = pattern.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw TxtTocRuleFormError.message("TXT TOC rule name is empty") }
        guard !cleanPattern.isEmpty else { throw TxtTocRuleFormError.message("TXT TOC rule regex is empty") }
        var object: [String: Any] = [
            "id": rule?.id ?? 0,
            "name": cleanName,
            "rule": cleanPattern,
            "replacement": replacement,
            "serialNumber": Int(serialNumber.trimmingCharacters(in: .whitespacesAndNewlines)) ?? -1,
            "enable": enabled
        ]
        add(&object, "example", example)
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw TxtTocRuleFormError.message("TXT TOC rule JSON encode failed")
        }
        return json
    }

    private func add(_ object: inout [String: Any], _ key: String, _ value: String) {
        let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if !clean.isEmpty { object[key] = clean }
    }
}

private enum TxtTocRuleFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

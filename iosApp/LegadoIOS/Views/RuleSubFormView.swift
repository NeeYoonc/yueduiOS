import LegadoShared
import SwiftUI

struct RuleSubFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let ruleSub: SharedRuleSub?

    @State private var name: String
    @State private var url: String
    @State private var type: String
    @State private var customOrder: String
    @State private var updateInterval: String
    @State private var autoUpdate: Bool
    @State private var silentUpdate: Bool
    @State private var js: String
    @State private var showRule: String
    @State private var sourceUrl: String

    init(ruleSub: SharedRuleSub? = nil) {
        self.ruleSub = ruleSub
        _name = State(initialValue: ruleSub?.name ?? "")
        _url = State(initialValue: ruleSub?.url ?? "")
        _type = State(initialValue: ruleSub.map { String($0.type) } ?? "0")
        _customOrder = State(initialValue: ruleSub.map { String($0.customOrder) } ?? "0")
        _updateInterval = State(initialValue: ruleSub.map { String($0.updateInterval) } ?? "0")
        _autoUpdate = State(initialValue: ruleSub?.autoUpdate ?? false)
        _silentUpdate = State(initialValue: ruleSub?.silentUpdate ?? false)
        _js = State(initialValue: ruleSub?.js ?? "")
        _showRule = State(initialValue: ruleSub?.showRule ?? "")
        _sourceUrl = State(initialValue: ruleSub?.sourceUrl ?? "")
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $name)
                TextField("URL", text: $url, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                TextField("Type", text: $type)
                    .keyboardType(.numbersAndPunctuation)
                TextField("Custom order", text: $customOrder)
                    .keyboardType(.numbersAndPunctuation)
                TextField("Update interval", text: $updateInterval)
                    .keyboardType(.numbersAndPunctuation)
                Toggle("Auto update", isOn: $autoUpdate)
                Toggle("Silent update", isOn: $silentUpdate)
            } header: {
                Text("Subscription")
            } footer: {
                Text("Rule subscriptions can update book sources, RSS sources, TTS engines, replacement rules, and other Legado rule lists.")
            }

            Section {
                TextField("Source URL", text: $sourceUrl, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                VStack(alignment: .leading, spacing: 6) {
                    Text("Show rule")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $showRule)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 110)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("JS")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $js)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 150)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            } header: {
                Text("Parsing")
            }
        }
        .navigationTitle(ruleSub == nil ? "New RuleSub" : "Edit RuleSub")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func save() {
        do {
            let json = try buildRuleSubJson()
            if app.saveRuleSubJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildRuleSubJson() throws -> String {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanUrl = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw RuleSubFormError.message("Rule subscription name is empty") }
        guard !cleanUrl.isEmpty else { throw RuleSubFormError.message("Rule subscription URL is empty") }
        var object: [String: Any] = [
            "id": ruleSub?.id ?? 0,
            "name": cleanName,
            "url": cleanUrl,
            "type": Int(type.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "customOrder": Int(customOrder.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "autoUpdate": autoUpdate,
            "update": ruleSub?.update ?? 0,
            "updateInterval": Int(updateInterval.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "silentUpdate": silentUpdate
        ]
        add(&object, "js", js)
        add(&object, "showRule", showRule)
        add(&object, "sourceUrl", sourceUrl)
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw RuleSubFormError.message("Rule subscription JSON encode failed")
        }
        return json
    }

    private func add(_ object: inout [String: Any], _ key: String, _ value: String) {
        let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if !clean.isEmpty { object[key] = clean }
    }
}

private enum RuleSubFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

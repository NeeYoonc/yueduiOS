import LegadoShared
import SwiftUI

struct RawConfigFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let entry: SharedRawConfigEntry?

    @State private var key: String
    @State private var value: String

    init(entry: SharedRawConfigEntry? = nil) {
        self.entry = entry
        _key = State(initialValue: entry?.key ?? "")
        _value = State(initialValue: entry?.value ?? "")
    }

    var body: some View {
        Form {
            Section {
                TextField("Key", text: $key)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("Config key")
            } footer: {
                Text("Raw configs preserve Android readConfig, themeConfig, coverRule, directLinkUpload, and future compatible config blobs.")
            }

            Section {
                TextEditor(text: $value)
                    .font(.system(.footnote, design: .monospaced))
                    .frame(minHeight: 320)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("Value")
            }
        }
        .navigationTitle(entry == nil ? "New Config" : "Edit Config")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func save() {
        do {
            let json = try buildConfigJson()
            if app.saveRawConfigJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildConfigJson() throws -> String {
        let cleanKey = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanKey.isEmpty else { throw RawConfigFormError.message("Raw config key is empty") }
        let object: [String: Any] = [
            "key": cleanKey,
            "value": value
        ]
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw RawConfigFormError.message("Raw config JSON encode failed")
        }
        return json
    }
}

private enum RawConfigFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

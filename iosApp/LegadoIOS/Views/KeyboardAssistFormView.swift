import LegadoShared
import SwiftUI

struct KeyboardAssistFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let assist: SharedKeyboardAssist?

    @State private var type: String
    @State private var key: String
    @State private var value: String
    @State private var serialNo: String

    init(assist: SharedKeyboardAssist? = nil) {
        self.assist = assist
        _type = State(initialValue: assist.map { String($0.type) } ?? "0")
        _key = State(initialValue: assist?.key ?? "")
        _value = State(initialValue: assist?.value ?? "")
        _serialNo = State(initialValue: assist.map { String($0.serialNo) } ?? "0")
    }

    var body: some View {
        Form {
            Section {
                TextField("Type", text: $type)
                    .keyboardType(.numbersAndPunctuation)
                TextField("Key", text: $key)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Serial number", text: $serialNo)
                    .keyboardType(.numbersAndPunctuation)
            } header: {
                Text("Identity")
            } footer: {
                Text("Type + key is the stable Android keyboard assist identity; serial number controls order.")
            }

            Section {
                TextEditor(text: $value)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 220)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("Value")
            }
        }
        .navigationTitle(assist == nil ? "New Assist" : "Edit Assist")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func save() {
        do {
            let json = try buildAssistJson()
            if app.saveKeyboardAssistJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildAssistJson() throws -> String {
        let cleanKey = key.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanValue = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanKey.isEmpty else { throw KeyboardAssistFormError.message("Keyboard assist key is empty") }
        guard !cleanValue.isEmpty else { throw KeyboardAssistFormError.message("Keyboard assist value is empty") }
        let object: [String: Any] = [
            "type": Int(type.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "key": cleanKey,
            "value": cleanValue,
            "serialNo": Int(serialNo.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
        ]
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw KeyboardAssistFormError.message("Keyboard assist JSON encode failed")
        }
        return json
    }
}

private enum KeyboardAssistFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

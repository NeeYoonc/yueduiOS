import LegadoShared
import SwiftUI

struct CacheEntryFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let entry: SharedCacheEntry?

    @State private var key: String
    @State private var value: String
    @State private var deadline: String

    init(entry: SharedCacheEntry? = nil) {
        self.entry = entry
        _key = State(initialValue: entry?.key ?? "")
        _value = State(initialValue: entry?.value ?? "")
        _deadline = State(initialValue: entry.map { String($0.deadline) } ?? "0")
    }

    var body: some View {
        Form {
            Section {
                TextField("Key", text: $key)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Deadline millis", text: $deadline)
                    .keyboardType(.numbersAndPunctuation)
            } header: {
                Text("Cache key")
            } footer: {
                Text("Deadline 0 means never expires; other values are Android-compatible epoch milliseconds.")
            }

            Section {
                TextEditor(text: $value)
                    .font(.system(.footnote, design: .monospaced))
                    .frame(minHeight: 300)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("Value")
            }
        }
        .navigationTitle(entry == nil ? "New Cache" : "Edit Cache")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func save() {
        do {
            let json = try buildCacheJson()
            if app.saveCacheEntryJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildCacheJson() throws -> String {
        let cleanKey = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanKey.isEmpty else { throw CacheEntryFormError.message("Cache key is empty") }
        let object: [String: Any] = [
            "key": cleanKey,
            "value": value,
            "deadline": Int64(deadline.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
        ]
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw CacheEntryFormError.message("Cache JSON encode failed")
        }
        return json
    }
}

private enum CacheEntryFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

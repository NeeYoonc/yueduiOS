import LegadoShared
import SwiftUI

struct CookieFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let cookie: SharedCookie?

    @State private var url: String
    @State private var value: String

    init(cookie: SharedCookie? = nil) {
        self.cookie = cookie
        _url = State(initialValue: cookie?.url ?? "")
        _value = State(initialValue: cookie?.cookie ?? "")
    }

    var body: some View {
        Form {
            Section {
                TextField("URL", text: $url, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
            } header: {
                Text("Cookie scope")
            } footer: {
                Text("URL is the same key used by Android Legado for source login and cookie-aware HTTP flows.")
            }

            Section {
                TextEditor(text: $value)
                    .font(.system(.footnote, design: .monospaced))
                    .frame(minHeight: 260)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("Cookie")
            }
        }
        .navigationTitle(cookie == nil ? "New Cookie" : "Edit Cookie")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func save() {
        do {
            let json = try buildCookieJson()
            if app.saveCookieJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildCookieJson() throws -> String {
        let cleanUrl = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanUrl.isEmpty else { throw CookieFormError.message("Cookie URL is empty") }
        let object: [String: Any] = [
            "url": cleanUrl,
            "cookie": value
        ]
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw CookieFormError.message("Cookie JSON encode failed")
        }
        return json
    }
}

private enum CookieFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

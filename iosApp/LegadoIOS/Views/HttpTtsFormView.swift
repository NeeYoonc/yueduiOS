import LegadoShared
import SwiftUI

struct HttpTtsFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let engine: SharedHttpTts?

    @State private var name: String
    @State private var url: String
    @State private var contentType: String
    @State private var concurrentRate: String
    @State private var loginUrl: String
    @State private var loginUi: String
    @State private var header: String
    @State private var jsLib: String
    @State private var enabledCookieJar: Bool
    @State private var loginCheckJs: String

    init(engine: SharedHttpTts? = nil) {
        self.engine = engine
        _name = State(initialValue: engine?.name ?? "")
        _url = State(initialValue: engine?.url ?? "")
        _contentType = State(initialValue: engine?.contentType ?? "")
        _concurrentRate = State(initialValue: engine?.concurrentRate ?? "0")
        _loginUrl = State(initialValue: engine?.loginUrl ?? "")
        _loginUi = State(initialValue: engine?.loginUi ?? "")
        _header = State(initialValue: engine?.header ?? "")
        _jsLib = State(initialValue: engine?.jsLib ?? "")
        _enabledCookieJar = State(initialValue: engine?.enabledCookieJar?.boolValue ?? false)
        _loginCheckJs = State(initialValue: engine?.loginCheckJs ?? "")
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $name)
                TextField("HTTP TTS URL", text: $url, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                TextField("Expected content type regex", text: $contentType)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Concurrent rate", text: $concurrentRate)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Toggle("Use cookie jar", isOn: $enabledCookieJar)
            } header: {
                Text("Identity")
            } footer: {
                Text("URL supports Android Legado placeholders such as {{speakText}}, {{text}}, and {{speakSpeed}}; the reader uses this engine before falling back to system TTS.")
            }

            Section {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Header JSON")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $header)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 100)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Login URL / JS")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $loginUrl)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 90)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Login UI")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $loginUi)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 90)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Login check JS")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $loginCheckJs)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 90)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("JS library")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $jsLib)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 120)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            } header: {
                Text("Login and script")
            }
        }
        .navigationTitle(engine == nil ? "New HTTP TTS" : "Edit HTTP TTS")
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
            let json = try buildEngineJson()
            if app.saveHttpTtsJson(json) {
                dismiss()
            }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildEngineJson() throws -> String {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanUrl = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw HttpTtsFormError.message("HTTP TTS name is empty") }
        guard !cleanUrl.isEmpty else { throw HttpTtsFormError.message("HTTP TTS URL is empty") }
        var object: [String: Any] = [
            "id": engine?.id ?? 0,
            "name": cleanName,
            "url": cleanUrl,
            "enabledCookieJar": enabledCookieJar,
            "concurrentRate": concurrentRate.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
        add(&object, "contentType", contentType)
        add(&object, "loginUrl", loginUrl)
        add(&object, "loginUi", loginUi)
        add(&object, "header", header)
        add(&object, "jsLib", jsLib)
        add(&object, "loginCheckJs", loginCheckJs)
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw HttpTtsFormError.message("HTTP TTS JSON encode failed")
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

private enum HttpTtsFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

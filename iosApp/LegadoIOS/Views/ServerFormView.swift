import LegadoShared
import SwiftUI

struct ServerFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let server: SharedServer?

    @State private var name: String
    @State private var type: String
    @State private var config: String
    @State private var sortNumber: String

    init(server: SharedServer? = nil) {
        self.server = server
        _name = State(initialValue: server?.name ?? "")
        _type = State(initialValue: server?.type ?? "WEBDAV")
        _config = State(initialValue: server?.config ?? "")
        _sortNumber = State(initialValue: server.map { String($0.sortNumber) } ?? "0")
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $name)
                TextField("Type", text: $type)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                TextField("Sort number", text: $sortNumber)
                    .keyboardType(.numbersAndPunctuation)
            } header: {
                Text("Identity")
            } footer: {
                Text("Server entries back Android-compatible WebDAV/import/export/backup server config snapshots.")
            }

            Section {
                TextEditor(text: $config)
                    .font(.system(.footnote, design: .monospaced))
                    .frame(minHeight: 260)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("Config JSON")
            } footer: {
                Text("Keep the config string in the same JSON shape exported by Android Legado, including WebDAV credentials or tokens when needed.")
            }
        }
        .navigationTitle(server == nil ? "New Server" : "Edit Server")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func save() {
        do {
            let json = try buildServerJson()
            if app.saveServerJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildServerJson() throws -> String {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanType = type.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw ServerFormError.message("Server name is empty") }
        var object: [String: Any] = [
            "id": server?.id ?? 0,
            "name": cleanName,
            "type": cleanType.isEmpty ? "WEBDAV" : cleanType,
            "sortNumber": Int(sortNumber.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
        ]
        let cleanConfig = config.trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleanConfig.isEmpty { object["config"] = cleanConfig }
        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw ServerFormError.message("Server JSON encode failed")
        }
        return json
    }
}

private enum ServerFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

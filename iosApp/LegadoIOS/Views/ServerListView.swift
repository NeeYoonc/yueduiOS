import LegadoShared
import SwiftUI

struct ServerListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.servers.isEmpty {
                Section {
                    EmptyStateView(title: "No remote servers", systemImage: "server.rack")
                }
            } else {
                Section {
                    ForEach(app.servers.indices, id: \.self) { index in
                        let server = app.servers[index]
                        NavigationLink {
                            ServerFormView(server: server)
                        } label: {
                            ServerRow(server: server)
                        }
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.servers[$0] }
                            .forEach(app.deleteServer)
                    }
                } footer: {
                    Text("Server configs are used by WebDAV import, export, backup, and remote book workflows.")
                }
            }
        }
        .navigationTitle("Remote Servers")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                NavigationLink {
                    ServerEditorView()
                } label: {
                    Image(systemName: "doc.text")
                }

                NavigationLink {
                    ServerFormView()
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

private struct ServerRow: View {
    let server: SharedServer

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(server.name)
                    .font(.headline)
                Spacer()
                Text(server.type)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if let config = server.config, !config.isEmpty {
                Text(redactedConfig(config))
                    .font(.footnote.monospaced())
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
                    .textSelection(.enabled)
            }
        }
        .padding(.vertical, 4)
    }

    private func redactedConfig(_ config: String) -> String {
        config
            .replacingOccurrences(
                of: #""password"\s*:\s*"[^"]*""#,
                with: #""password":"***""#,
                options: .regularExpression
            )
            .replacingOccurrences(
                of: #""token"\s*:\s*"[^"]*""#,
                with: #""token":"***""#,
                options: .regularExpression
            )
    }
}

import LegadoShared
import SwiftUI

struct CookieListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            Section("Import from URL") {
                TextField("https://example.com/cookies.json", text: $app.cookieImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importCookiesFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.cookieImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    Task {
                        await app.importCookiesFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.cookieImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section {
                TextEditor(text: $app.cookieJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 180)
            } header: {
                Text("Cookie JSON")
            } footer: {
                Text("Import Android backup cookie entries such as [{\"url\":\"https://example.test\",\"cookie\":\"a=b\"}].")
            }

            Section {
                Button {
                    app.importCookies()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importCookies(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportCookiesToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }

                Button(role: .destructive) {
                    app.clearCookies()
                } label: {
                    Label("Clear cookies", systemImage: "trash")
                }
                .disabled(app.cookies.isEmpty)
            }

            if app.cookies.isEmpty {
                Section {
                    EmptyStateView(title: "No cookies", systemImage: "shippingbox")
                }
            } else {
                Section {
                    ForEach(app.cookies.indices, id: \.self) { index in
                        let cookie = app.cookies[index]
                        CookieRow(cookie: cookie)
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.cookies[$0] }
                            .forEach(app.deleteCookie)
                    }
                } footer: {
                    Text("Cookies are stored in the shared backup snapshot and are used by login/cookie-aware source flows.")
                }
            }
        }
        .navigationTitle("Cookies")
    }
}

private struct CookieRow: View {
    let cookie: SharedCookie

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(cookie.url)
                .font(.headline)
                .textSelection(.enabled)

            Text(redacted(cookie.cookie))
                .font(.footnote.monospaced())
                .foregroundStyle(.secondary)
                .lineLimit(3)
                .textSelection(.enabled)
        }
        .padding(.vertical, 4)
    }

    private func redacted(_ value: String) -> String {
        let parts = value.split(separator: ";", omittingEmptySubsequences: true)
        guard !parts.isEmpty else { return value }
        return parts.map { part in
            let pair = part.split(separator: "=", maxSplits: 1, omittingEmptySubsequences: false)
            guard let name = pair.first else { return "***" }
            return "\(String(name).trimmingCharacters(in: .whitespaces))=***"
        }.joined(separator: "; ")
    }
}

import LegadoShared
import SwiftUI
import WebKit

struct SourceLoginView: View {
    @EnvironmentObject private var app: AppState
    let source: SharedBookSource
    @State private var currentURL: String = ""
    @State private var cookieString: String = ""

    var body: some View {
        Group {
            if let request = app.sourceWebLoginRequest(source) {
                VStack(spacing: 0) {
                    SourceWebLoginContainer(
                        request: request,
                        currentURL: $currentURL,
                        cookieString: $cookieString
                    )
                    Divider()
                    VStack(alignment: .leading, spacing: 8) {
                        if !currentURL.isEmpty {
                            Text(currentURL)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                        Text(cookieString.isEmpty ? "No cookie captured yet" : cookieString)
                            .font(.caption.monospaced())
                            .foregroundStyle(cookieString.isEmpty ? .secondary : .primary)
                            .textSelection(.enabled)
                            .lineLimit(3)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(.thinMaterial)
                }
                .toolbar {
                    Button {
                        app.saveSourceWebLoginCookie(source, cookie: cookieString)
                    } label: {
                        Label("Save Cookie", systemImage: "key")
                    }
                    .disabled(cookieString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            } else {
                List {
                    Section {
                        Text("This source uses a JavaScript or structured login rule. The rule is preserved in source JSON; use the JSON editor or Cookie manager until the full JS login UI runtime is migrated.")
                            .foregroundStyle(.secondary)
                    }
                    if let loginUrl = source.loginUrl, !loginUrl.isEmpty {
                        Section("Login rule") {
                            Text(loginUrl)
                                .font(.footnote.monospaced())
                                .textSelection(.enabled)
                        }
                    }
                    if let loginUi = source.loginUi, !loginUi.isEmpty {
                        Section("Login UI") {
                            Text(loginUi)
                                .font(.footnote.monospaced())
                                .textSelection(.enabled)
                        }
                    }
                }
            }
        }
        .navigationTitle(source.bookSourceName.isEmpty ? "Source Login" : source.bookSourceName)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SourceWebLoginContainer: UIViewRepresentable {
    let request: SharedSourceLoginRequest
    @Binding var currentURL: String
    @Binding var cookieString: String

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        context.coordinator.load(request, in: webView)
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.parent = self
        if context.coordinator.loadedURL != request.url {
            context.coordinator.load(request, in: webView)
        }
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        var parent: SourceWebLoginContainer
        var loadedURL: String?

        init(parent: SourceWebLoginContainer) {
            self.parent = parent
        }

        func load(_ request: SharedSourceLoginRequest, in webView: WKWebView) {
            guard let url = URL(string: request.url) else {
                return
            }
            loadedURL = request.url
            var urlRequest = URLRequest(url: url)
            headers(from: request).forEach { name, value in
                urlRequest.setValue(value, forHTTPHeaderField: name)
            }
            webView.load(urlRequest)
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            parent.currentURL = webView.url?.absoluteString ?? loadedURL ?? ""
            captureCookies(from: webView)
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.currentURL = webView.url?.absoluteString ?? loadedURL ?? ""
            captureCookies(from: webView)
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            parent.currentURL = webView.url?.absoluteString ?? loadedURL ?? ""
            captureCookies(from: webView)
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            parent.currentURL = webView.url?.absoluteString ?? loadedURL ?? ""
            captureCookies(from: webView)
        }

        private func captureCookies(from webView: WKWebView) {
            let host = webView.url?.host
            webView.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies in
                let matching = cookies.filter { cookie in
                    guard let host else {
                        return true
                    }
                    let domain = cookie.domain.trimmingCharacters(in: CharacterSet(charactersIn: "."))
                    return host == domain || host.hasSuffix(".\(domain)") || domain.hasSuffix(host)
                }
                let cookieString = matching
                    .map { "\($0.name)=\($0.value)" }
                    .joined(separator: "; ")
                DispatchQueue.main.async {
                    self.parent.cookieString = cookieString
                }
            }
        }

        private func headers(from request: SharedSourceLoginRequest) -> [String: String] {
            if let headers = request.headers as? [String: String] {
                return headers
            }
            if let headers = request.headers as? NSDictionary {
                var result: [String: String] = [:]
                headers.forEach { key, value in
                    result["\(key)"] = "\(value)"
                }
                return result
            }
            return [:]
        }
    }
}

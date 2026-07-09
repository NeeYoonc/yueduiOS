import LegadoShared
import SwiftUI

struct RssSourceFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let source: SharedRssSource?

    @State private var sourceUrl: String
    @State private var sourceName: String
    @State private var sourceIcon: String
    @State private var sourceGroup: String
    @State private var sourceComment: String
    @State private var variableComment: String
    @State private var concurrentRate: String
    @State private var sortUrl: String
    @State private var searchUrl: String
    @State private var customOrder: String
    @State private var type: String
    @State private var articleStyle: String
    @State private var enabled: Bool
    @State private var enabledCookieJar: Bool
    @State private var singleUrl: Bool
    @State private var enableJs: Bool
    @State private var loadWithBaseUrl: Bool
    @State private var preload: Bool
    @State private var cacheFirst: Bool
    @State private var showWebLog: Bool

    @State private var header: String
    @State private var loginUrl: String
    @State private var loginUi: String
    @State private var loginCheckJs: String
    @State private var jsLib: String
    @State private var coverDecodeJs: String

    @State private var ruleArticles: String
    @State private var ruleNextPage: String
    @State private var ruleTitle: String
    @State private var rulePubDate: String
    @State private var ruleDescription: String
    @State private var ruleImage: String
    @State private var ruleLink: String
    @State private var ruleContent: String
    @State private var contentWhitelist: String
    @State private var contentBlacklist: String
    @State private var shouldOverrideUrlLoading: String
    @State private var style: String
    @State private var injectJs: String
    @State private var preloadJs: String
    @State private var startHtml: String
    @State private var startStyle: String
    @State private var startJs: String

    init(source: SharedRssSource? = nil) {
        self.source = source
        _sourceUrl = State(initialValue: source?.sourceUrl ?? "")
        _sourceName = State(initialValue: source?.sourceName ?? "")
        _sourceIcon = State(initialValue: source?.sourceIcon ?? "")
        _sourceGroup = State(initialValue: source?.sourceGroup ?? "")
        _sourceComment = State(initialValue: source?.sourceComment ?? "")
        _variableComment = State(initialValue: source?.variableComment ?? "")
        _concurrentRate = State(initialValue: source?.concurrentRate ?? "")
        _sortUrl = State(initialValue: source?.sortUrl ?? "")
        _searchUrl = State(initialValue: source?.searchUrl ?? "")
        _customOrder = State(initialValue: source.map { String($0.customOrder) } ?? "0")
        _type = State(initialValue: source.map { String($0.type) } ?? "0")
        _articleStyle = State(initialValue: source.map { String($0.articleStyle) } ?? "0")
        _enabled = State(initialValue: source?.enabled ?? true)
        _enabledCookieJar = State(initialValue: source?.enabledCookieJar?.boolValue ?? true)
        _singleUrl = State(initialValue: source?.singleUrl ?? false)
        _enableJs = State(initialValue: source?.enableJs ?? true)
        _loadWithBaseUrl = State(initialValue: source?.loadWithBaseUrl ?? true)
        _preload = State(initialValue: source?.preload ?? false)
        _cacheFirst = State(initialValue: source?.cacheFirst ?? false)
        _showWebLog = State(initialValue: source?.showWebLog ?? false)
        _header = State(initialValue: source?.header ?? "")
        _loginUrl = State(initialValue: source?.loginUrl ?? "")
        _loginUi = State(initialValue: source?.loginUi ?? "")
        _loginCheckJs = State(initialValue: source?.loginCheckJs ?? "")
        _jsLib = State(initialValue: source?.jsLib ?? "")
        _coverDecodeJs = State(initialValue: source?.coverDecodeJs ?? "")
        _ruleArticles = State(initialValue: source?.ruleArticles ?? "")
        _ruleNextPage = State(initialValue: source?.ruleNextPage ?? "")
        _ruleTitle = State(initialValue: source?.ruleTitle ?? "")
        _rulePubDate = State(initialValue: source?.rulePubDate ?? "")
        _ruleDescription = State(initialValue: source?.ruleDescription ?? "")
        _ruleImage = State(initialValue: source?.ruleImage ?? "")
        _ruleLink = State(initialValue: source?.ruleLink ?? "")
        _ruleContent = State(initialValue: source?.ruleContent ?? "")
        _contentWhitelist = State(initialValue: source?.contentWhitelist ?? "")
        _contentBlacklist = State(initialValue: source?.contentBlacklist ?? "")
        _shouldOverrideUrlLoading = State(initialValue: source?.shouldOverrideUrlLoading ?? "")
        _style = State(initialValue: source?.style ?? "")
        _injectJs = State(initialValue: source?.injectJs ?? "")
        _preloadJs = State(initialValue: source?.preloadJs ?? "")
        _startHtml = State(initialValue: source?.startHtml ?? "")
        _startStyle = State(initialValue: source?.startStyle ?? "")
        _startJs = State(initialValue: source?.startJs ?? "")
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $sourceName)
                TextField("Source URL", text: $sourceUrl, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                TextField("Group", text: $sourceGroup)
                TextField("Icon URL", text: $sourceIcon, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                TextField("Custom order", text: $customOrder)
                    .keyboardType(.numbersAndPunctuation)
                TextField("Type", text: $type)
                    .keyboardType(.numbersAndPunctuation)
                TextField("Article style", text: $articleStyle)
                    .keyboardType(.numbersAndPunctuation)
                Toggle("Enabled", isOn: $enabled)
            } header: {
                Text("Identity")
            } footer: {
                Text("Source URL is the stable key used to update one RSS source without replacing other imported sources.")
            }

            Section {
                Toggle("Use cookie jar", isOn: $enabledCookieJar)
                Toggle("Single URL", isOn: $singleUrl)
                Toggle("Enable JS", isOn: $enableJs)
                Toggle("Load with base URL", isOn: $loadWithBaseUrl)
                Toggle("Preload", isOn: $preload)
                Toggle("Cache first", isOn: $cacheFirst)
                Toggle("Show web log", isOn: $showWebLog)
                TextField("Concurrent rate", text: $concurrentRate)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } header: {
                Text("Runtime")
            }

            Section {
                editor("Sort URL", text: $sortUrl)
                editor("Search URL", text: $searchUrl)
                editor("Header JSON", text: $header)
                editor("Login URL / JS", text: $loginUrl)
                editor("Login UI", text: $loginUi)
                editor("Login check JS", text: $loginCheckJs)
            } header: {
                Text("Network and login")
            }

            Section {
                editor("Articles", text: $ruleArticles)
                editor("Next page", text: $ruleNextPage)
                editor("Title", text: $ruleTitle)
                editor("Pub date", text: $rulePubDate)
                editor("Description", text: $ruleDescription)
                editor("Image", text: $ruleImage)
                editor("Link", text: $ruleLink)
                editor("Content", text: $ruleContent)
            } header: {
                Text("Article rules")
            }

            Section {
                editor("Content whitelist", text: $contentWhitelist)
                editor("Content blacklist", text: $contentBlacklist)
                editor("Should override URL loading", text: $shouldOverrideUrlLoading)
                editor("Style", text: $style)
                editor("Inject JS", text: $injectJs)
                editor("Preload JS", text: $preloadJs)
                editor("Start HTML", text: $startHtml)
                editor("Start style", text: $startStyle)
                editor("Start JS", text: $startJs)
                editor("JS library", text: $jsLib, minHeight: 140)
                editor("Cover decode JS", text: $coverDecodeJs)
            } header: {
                Text("Web rendering")
            }

            Section {
                editor("Source comment", text: $sourceComment)
                editor("Variable comment", text: $variableComment)
            } header: {
                Text("Comments")
            }
        }
        .navigationTitle(source == nil ? "New RSS Source" : "Edit RSS Source")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }
            }
        }
    }

    private func editor(_ title: String, text: Binding<String>, minHeight: CGFloat = 90) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            TextEditor(text: text)
                .font(.system(.footnote, design: .monospaced))
                .frame(minHeight: minHeight)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
    }

    private func save() {
        do {
            let json = try buildSourceJson()
            if app.saveRssSourceJson(json) { dismiss() }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildSourceJson() throws -> String {
        let cleanName = sourceName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanUrl = sourceUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw RssSourceFormError.message("RSS source name is empty") }
        guard !cleanUrl.isEmpty else { throw RssSourceFormError.message("RSS source URL is empty") }

        var object: [String: Any] = [
            "sourceUrl": cleanUrl,
            "sourceName": cleanName,
            "sourceIcon": sourceIcon.trimmingCharacters(in: .whitespacesAndNewlines),
            "enabled": enabled,
            "enabledCookieJar": enabledCookieJar,
            "singleUrl": singleUrl,
            "articleStyle": Int(articleStyle.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "enableJs": enableJs,
            "loadWithBaseUrl": loadWithBaseUrl,
            "showWebLog": showWebLog,
            "lastUpdateTime": source?.lastUpdateTime ?? 0,
            "customOrder": Int(customOrder.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "type": Int(type.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "preload": preload,
            "cacheFirst": cacheFirst
        ]
        add(&object, "sourceGroup", sourceGroup)
        add(&object, "sourceComment", sourceComment)
        add(&object, "variableComment", variableComment)
        add(&object, "jsLib", jsLib)
        add(&object, "concurrentRate", concurrentRate)
        add(&object, "header", header)
        add(&object, "loginUrl", loginUrl)
        add(&object, "loginUi", loginUi)
        add(&object, "loginCheckJs", loginCheckJs)
        add(&object, "coverDecodeJs", coverDecodeJs)
        add(&object, "sortUrl", sortUrl)
        add(&object, "ruleArticles", ruleArticles)
        add(&object, "ruleNextPage", ruleNextPage)
        add(&object, "ruleTitle", ruleTitle)
        add(&object, "rulePubDate", rulePubDate)
        add(&object, "ruleDescription", ruleDescription)
        add(&object, "ruleImage", ruleImage)
        add(&object, "ruleLink", ruleLink)
        add(&object, "ruleContent", ruleContent)
        add(&object, "contentWhitelist", contentWhitelist)
        add(&object, "contentBlacklist", contentBlacklist)
        add(&object, "shouldOverrideUrlLoading", shouldOverrideUrlLoading)
        add(&object, "style", style)
        add(&object, "injectJs", injectJs)
        add(&object, "preloadJs", preloadJs)
        add(&object, "startHtml", startHtml)
        add(&object, "startStyle", startStyle)
        add(&object, "startJs", startJs)
        add(&object, "searchUrl", searchUrl)

        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw RssSourceFormError.message("RSS source JSON encode failed")
        }
        return json
    }

    private func add(_ object: inout [String: Any], _ key: String, _ value: String) {
        let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if !clean.isEmpty { object[key] = clean }
    }
}

private enum RssSourceFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}

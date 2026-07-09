import LegadoShared
import SwiftUI

struct BookSourceFormView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    private let source: SharedBookSource?

    @State private var bookSourceName: String
    @State private var bookSourceUrl: String
    @State private var bookSourceGroup: String
    @State private var bookSourceType: String
    @State private var bookUrlPattern: String
    @State private var customOrder: String
    @State private var enabled: Bool
    @State private var enabledExplore: Bool
    @State private var enabledCookieJar: Bool
    @State private var concurrentRate: String
    @State private var header: String
    @State private var loginUrl: String
    @State private var loginUi: String
    @State private var loginCheckJs: String
    @State private var coverDecodeJs: String
    @State private var jsLib: String
    @State private var bookSourceComment: String
    @State private var variableComment: String
    @State private var exploreUrl: String
    @State private var exploreScreen: String
    @State private var searchUrl: String
    @State private var ruleExploreJson: String
    @State private var ruleSearchJson: String
    @State private var ruleBookInfoJson: String
    @State private var ruleTocJson: String
    @State private var ruleContentJson: String
    @State private var ruleReviewJson: String

    init(source: SharedBookSource? = nil) {
        self.source = source
        _bookSourceName = State(initialValue: source?.bookSourceName ?? "")
        _bookSourceUrl = State(initialValue: source?.bookSourceUrl ?? "")
        _bookSourceGroup = State(initialValue: source?.bookSourceGroup ?? "")
        _bookSourceType = State(initialValue: source.map { String($0.bookSourceType) } ?? "0")
        _bookUrlPattern = State(initialValue: source?.bookUrlPattern ?? "")
        _customOrder = State(initialValue: source.map { String($0.customOrder) } ?? "0")
        _enabled = State(initialValue: source?.enabled ?? true)
        _enabledExplore = State(initialValue: source?.enabledExplore ?? true)
        _enabledCookieJar = State(initialValue: true)
        _concurrentRate = State(initialValue: source?.concurrentRate ?? "")
        _header = State(initialValue: source?.header ?? "")
        _loginUrl = State(initialValue: source?.loginUrl ?? "")
        _loginUi = State(initialValue: source?.loginUi ?? "")
        _loginCheckJs = State(initialValue: source?.loginCheckJs ?? "")
        _coverDecodeJs = State(initialValue: source?.coverDecodeJs ?? "")
        _jsLib = State(initialValue: source?.jsLib ?? "")
        _bookSourceComment = State(initialValue: source?.bookSourceComment ?? "")
        _variableComment = State(initialValue: source?.variableComment ?? "")
        _exploreUrl = State(initialValue: source?.exploreUrl ?? "")
        _exploreScreen = State(initialValue: source?.exploreScreen ?? "")
        _searchUrl = State(initialValue: source?.searchUrl ?? "")
        _ruleExploreJson = State(initialValue: Self.ruleJson(source?.ruleExplore))
        _ruleSearchJson = State(initialValue: Self.ruleJson(source?.ruleSearch))
        _ruleBookInfoJson = State(initialValue: Self.ruleJson(source?.ruleBookInfo))
        _ruleTocJson = State(initialValue: Self.ruleJson(source?.ruleToc))
        _ruleContentJson = State(initialValue: Self.ruleJson(source?.ruleContent))
        _ruleReviewJson = State(initialValue: Self.ruleJson(source?.ruleReview))
    }

    var body: some View {
        Form {
            Section("Identity") {
                TextField("Source name", text: $bookSourceName)
                TextField("https://example.com", text: $bookSourceUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                TextField("Group", text: $bookSourceGroup)
                TextField("Type", text: $bookSourceType)
                    .keyboardType(.numberPad)
                TextField("Order", text: $customOrder)
                    .keyboardType(.numbersAndPunctuation)
                Toggle("Enabled", isOn: $enabled)
                Toggle("Explore enabled", isOn: $enabledExplore)
            }

            if let source = source {
                Section("Tools") {
                    NavigationLink {
                        SourceDebugView(source: source)
                    } label: {
                        Label("Debug source", systemImage: "stethoscope")
                    }

                    if let loginUrl = source.loginUrl, !loginUrl.isEmpty {
                        NavigationLink {
                            SourceLoginView(source: source)
                        } label: {
                            Label("Login / Cookie", systemImage: "key")
                        }
                    }
                }
            }

            Section("Request and login") {
                TextField("Search URL", text: $searchUrl, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Explore URL", text: $exploreUrl, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Explore screen", text: $exploreScreen, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Book URL pattern", text: $bookUrlPattern, axis: .vertical)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField("Concurrent rate", text: $concurrentRate)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Toggle("Use cookie jar", isOn: $enabledCookieJar)
                LabeledEditor(title: "Header JSON", text: $header, minHeight: 90)
                LabeledEditor(title: "JS library", text: $jsLib, minHeight: 120)
                LabeledEditor(title: "Login URL / JS", text: $loginUrl, minHeight: 90)
                LabeledEditor(title: "Login UI", text: $loginUi, minHeight: 90)
                LabeledEditor(title: "Login check JS", text: $loginCheckJs, minHeight: 90)
                LabeledEditor(title: "Cover decode JS", text: $coverDecodeJs, minHeight: 90)
            }

            Section("Comments") {
                TextField("Source comment", text: $bookSourceComment, axis: .vertical)
                TextField("Variable comment", text: $variableComment, axis: .vertical)
            }

            Section {
                LabeledEditor(title: "Explore rule JSON", text: $ruleExploreJson, minHeight: 130)
                LabeledEditor(title: "Search rule JSON", text: $ruleSearchJson, minHeight: 130)
                LabeledEditor(title: "Book info rule JSON", text: $ruleBookInfoJson, minHeight: 150)
                LabeledEditor(title: "TOC rule JSON", text: $ruleTocJson, minHeight: 150)
                LabeledEditor(title: "Content rule JSON", text: $ruleContentJson, minHeight: 150)
                LabeledEditor(title: "Review rule JSON", text: $ruleReviewJson, minHeight: 150)
            } header: {
                Text("Rules")
            } footer: {
                Text("Rule sections use the same JSON objects as Android Legado exports, so complex rule fields are preserved instead of being reduced to a template.")
            }
        }
        .navigationTitle(source == nil ? "New Source" : "Edit Source")
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
            let json = try buildSourceJson()
            if app.saveBookSourceJson(json) {
                dismiss()
            }
        } catch {
            app.showMessage(error.localizedDescription)
        }
    }

    private func buildSourceJson() throws -> String {
        let cleanName = bookSourceName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanUrl = bookSourceUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw SourceFormError.message("Source name is empty") }
        guard !cleanUrl.isEmpty else { throw SourceFormError.message("Source URL is empty") }

        var object: [String: Any] = [
            "bookSourceName": cleanName,
            "bookSourceUrl": cleanUrl,
            "bookSourceType": Int(bookSourceType.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "customOrder": Int(customOrder.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
            "enabled": enabled,
            "enabledExplore": enabledExplore,
            "enabledCookieJar": enabledCookieJar
        ]
        add(&object, "bookSourceGroup", bookSourceGroup)
        add(&object, "bookUrlPattern", bookUrlPattern)
        add(&object, "concurrentRate", concurrentRate)
        add(&object, "header", header)
        add(&object, "loginUrl", loginUrl)
        add(&object, "loginUi", loginUi)
        add(&object, "loginCheckJs", loginCheckJs)
        add(&object, "coverDecodeJs", coverDecodeJs)
        add(&object, "jsLib", jsLib)
        add(&object, "bookSourceComment", bookSourceComment)
        add(&object, "variableComment", variableComment)
        add(&object, "exploreUrl", exploreUrl)
        add(&object, "exploreScreen", exploreScreen)
        add(&object, "searchUrl", searchUrl)
        try addJsonObject(&object, "ruleExplore", ruleExploreJson)
        try addJsonObject(&object, "ruleSearch", ruleSearchJson)
        try addJsonObject(&object, "ruleBookInfo", ruleBookInfoJson)
        try addJsonObject(&object, "ruleToc", ruleTocJson)
        try addJsonObject(&object, "ruleContent", ruleContentJson)
        try addJsonObject(&object, "ruleReview", ruleReviewJson)

        let data = try JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
        guard let json = String(data: data, encoding: .utf8) else {
            throw SourceFormError.message("Source JSON encode failed")
        }
        return json
    }

    private func add(_ object: inout [String: Any], _ key: String, _ value: String) {
        let clean = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if !clean.isEmpty {
            object[key] = clean
        }
    }

    private func addJsonObject(_ object: inout [String: Any], _ key: String, _ rawJson: String) throws {
        let clean = rawJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        guard let data = clean.data(using: .utf8) else {
            throw SourceFormError.message("\(key) is not UTF-8")
        }
        let parsed = try JSONSerialization.jsonObject(with: data)
        guard parsed is [String: Any] else {
            throw SourceFormError.message("\(key) must be a JSON object")
        }
        object[key] = parsed
    }

    private static func ruleJson(_ rule: SharedSearchRule?) -> String {
        guard let rule = rule else { return "" }
        var object: [String: Any] = [:]
        add(&object, "bookList", rule.bookList)
        add(&object, "name", rule.name)
        add(&object, "author", rule.author)
        add(&object, "kind", rule.kind)
        add(&object, "lastChapter", rule.lastChapter)
        add(&object, "updateTime", rule.updateTime)
        add(&object, "intro", rule.intro)
        add(&object, "coverUrl", rule.coverUrl)
        add(&object, "bookUrl", rule.bookUrl)
        add(&object, "wordCount", rule.wordCount)
        add(&object, "checkKeyWord", rule.checkKeyWord)
        return prettyJson(object)
    }

    private static func ruleJson(_ rule: SharedBookInfoRule?) -> String {
        guard let rule = rule else { return "" }
        var object: [String: Any] = [:]
        add(&object, "name", rule.name)
        add(&object, "author", rule.author)
        add(&object, "kind", rule.kind)
        add(&object, "lastChapter", rule.lastChapter)
        add(&object, "updateTime", rule.updateTime)
        add(&object, "intro", rule.intro)
        add(&object, "coverUrl", rule.coverUrl)
        add(&object, "tocUrl", rule.tocUrl)
        add(&object, "wordCount", rule.wordCount)
        add(&object, "canReName", rule.canReName)
        add(&object, "downloadUrls", rule.downloadUrls)
        return prettyJson(object)
    }

    private static func ruleJson(_ rule: SharedTocRule?) -> String {
        guard let rule = rule else { return "" }
        var object: [String: Any] = [:]
        add(&object, "chapterList", rule.chapterList)
        add(&object, "chapterName", rule.chapterName)
        add(&object, "chapterUrl", rule.chapterUrl)
        add(&object, "formatJs", rule.formatJs)
        add(&object, "nextTocUrl", rule.nextTocUrl)
        add(&object, "updateTime", rule.updateTime)
        add(&object, "isVolume", rule.isVolume)
        add(&object, "isVip", rule.isVip)
        add(&object, "isPay", rule.isPay)
        add(&object, "preUpdateJs", rule.preUpdateJs)
        return prettyJson(object)
    }

    private static func ruleJson(_ rule: SharedContentRule?) -> String {
        guard let rule = rule else { return "" }
        var object: [String: Any] = [:]
        add(&object, "content", rule.content)
        add(&object, "subContent", rule.subContent)
        add(&object, "title", rule.title)
        add(&object, "nextContentUrl", rule.nextContentUrl)
        add(&object, "replaceRegex", rule.replaceRegex)
        add(&object, "webJs", rule.webJs)
        add(&object, "sourceRegex", rule.sourceRegex)
        add(&object, "imageStyle", rule.imageStyle)
        add(&object, "imageDecode", rule.imageDecode)
        add(&object, "payAction", rule.payAction)
        add(&object, "callBackJs", rule.callBackJs)
        return prettyJson(object)
    }

    private static func ruleJson(_ rule: SharedReviewRule?) -> String {
        guard let rule = rule else { return "" }
        var object: [String: Any] = [:]
        add(&object, "reviewUrl", rule.reviewUrl)
        add(&object, "avatarRule", rule.avatarRule)
        add(&object, "contentRule", rule.contentRule)
        add(&object, "postTimeRule", rule.postTimeRule)
        add(&object, "reviewQuoteUrl", rule.reviewQuoteUrl)
        add(&object, "voteUpUrl", rule.voteUpUrl)
        add(&object, "voteDownUrl", rule.voteDownUrl)
        add(&object, "postReviewUrl", rule.postReviewUrl)
        add(&object, "postQuoteUrl", rule.postQuoteUrl)
        add(&object, "deleteUrl", rule.deleteUrl)
        return prettyJson(object)
    }

    private static func add(_ object: inout [String: Any], _ key: String, _ value: String?) {
        if let value, !value.isEmpty {
            object[key] = value
        }
    }

    private static func prettyJson(_ object: [String: Any]) -> String {
        guard !object.isEmpty,
              let data = try? JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys]),
              let json = String(data: data, encoding: .utf8) else {
            return ""
        }
        return json
    }
}

private struct LabeledEditor: View {
    let title: String
    @Binding var text: String
    let minHeight: CGFloat

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            TextEditor(text: $text)
                .font(.system(.footnote, design: .monospaced))
                .frame(minHeight: minHeight)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
    }
}

private enum SourceFormError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case let .message(text):
            return text
        }
    }
}


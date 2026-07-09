import Foundation
import LegadoShared

@MainActor
final class AppState: ObservableObject {
    @Published var sourceJson: String = DefaultSource.json
    @Published var sourceImportUrl: String = ""
    @Published var replaceRuleJson: String = ""
    @Published var replaceRuleImportUrl: String = ""
    @Published var backupJson: String = ""
    @Published var dictRuleJson: String = ""
    @Published var dictRuleImportUrl: String = ""
    @Published var httpTtsJson: String = ""
    @Published var httpTtsImportUrl: String = ""
    @Published var txtTocRuleJson: String = ""
    @Published var txtTocRuleImportUrl: String = ""
    @Published var serverJson: String = ""
    @Published var serverImportUrl: String = ""
    @Published var keyboardAssistJson: String = ""
    @Published var keyboardAssistImportUrl: String = ""
    @Published var ruleSubJson: String = ""
    @Published var ruleSubImportUrl: String = ""
    @Published var rawConfigJson: String = ""
    @Published var rawConfigImportUrl: String = ""
    @Published var rssSourceJson: String = ""
    @Published var rssSourceImportUrl: String = ""
    @Published var cookieJson: String = ""
    @Published var cookieImportUrl: String = ""
    @Published var cacheJson: String = ""
    @Published var cacheImportUrl: String = ""
    @Published var smartImportJson: String = ""
    @Published var smartImportUrl: String = ""
    @Published var webDavBackupFileName: String = "legado-backup.json"
    @Published var keyword: String = ""
    @Published var dictionaryKeyword: String = ""
    @Published var selectedSourceIndex: Int = -1

    @Published private(set) var sources: [SharedBookSource] = []
    @Published private(set) var books: [SharedBook] = []
    @Published private(set) var visibleBooks: [SharedBook] = []
    @Published private(set) var bookUpdateResults: [BookUpdateResult] = []
    @Published private(set) var bookGroups: [SharedBookGroup] = []
    @Published private(set) var visibleBookGroups: [SharedBookGroup] = []
    @Published private(set) var selectableBookGroups: [SharedBookGroup] = []
    @Published private(set) var readRecords: [SharedReadRecord] = []
    @Published var selectedBookGroupId: Int64 = -1
    @Published private(set) var bookmarks: [SharedBookmark] = []
    @Published private(set) var replaceRules: [SharedReplaceRule] = []
    @Published private(set) var dictRules: [SharedDictRule] = []
    @Published private(set) var dictionaryLookupResults: [SharedDictionaryLookupResult] = []
    @Published private(set) var httpTts: [SharedHttpTts] = []
    @Published private(set) var txtTocRules: [SharedTxtTocRule] = []
    @Published private(set) var servers: [SharedServer] = []
    @Published private(set) var keyboardAssists: [SharedKeyboardAssist] = []
    @Published private(set) var ruleSubs: [SharedRuleSub] = []
    @Published private(set) var rawConfigs: [SharedRawConfigEntry] = []
    @Published private(set) var cookies: [SharedCookie] = []
    @Published private(set) var cacheEntries: [SharedCacheEntry] = []
    @Published private(set) var readerPreferences = SharedReaderPreferences(
        fontSize: 18.0,
        lineSpacing: 8.0,
        contentPadding: 20.0,
        theme: "system"
    )
    @Published private(set) var rssSources: [SharedRssSource] = []
    @Published private(set) var rssArticles: [SharedRssArticle] = []
    @Published private(set) var rssReadRecords: [SharedRssReadRecord] = []
    @Published private(set) var rssStars: [SharedRssStar] = []
    @Published private(set) var rssStarredArticles: [SharedRssArticle] = []
    @Published private(set) var exploreSources: [SharedBookSource] = []
    @Published private(set) var selectedExploreSource: SharedBookSource?
    @Published private(set) var exploreKinds: [SharedExploreKind] = []
    @Published private(set) var exploreResults: [SharedSearchBook] = []
    @Published private(set) var selectedRssSource: SharedRssSource?
    @Published private(set) var selectedRssArticle: SharedRssArticle?
    @Published private(set) var rssContent: String = ""
    @Published private(set) var searchResults: [SharedSearchBook] = []
    @Published private(set) var searchBooks: [SharedSearchBook] = []
    @Published private(set) var changeSourceCandidates: [SharedSearchBook] = []
    @Published private(set) var searchKeywords: [SharedSearchKeyword] = []
    @Published private(set) var searchErrors: [SourceSearchError] = []
    @Published private(set) var selectedBook: SharedBook?
    @Published private(set) var selectedSearchBook: SharedSearchBook?
    @Published private(set) var chapters: [SharedBookChapter] = []
    @Published private(set) var currentChapter: SharedBookChapter?
    @Published private(set) var currentChapterIndex: Int = 0
    @Published private(set) var currentContent: String = ""
    @Published private(set) var readerSearchResults: [SharedReaderSearchResult] = []
    @Published private(set) var debugSteps: [SourceDebugStep] = []
    @Published private(set) var debugContent: String = ""
    @Published private(set) var message: String?
    @Published private(set) var isLoading: Bool = false

    private let runtime = DarwinLegadoRuntime()
    private var activeSource: SharedBookSource?

    init() {
        refreshLibrary()
        if sources.isEmpty {
            importBundledDefaultData()
        }
    }

    func refreshLibrary() {
        sources = runtime.loadBookSources() as? [SharedBookSource] ?? []
        books = runtime.loadBooks() as? [SharedBook] ?? []
        bookGroups = runtime.loadBookGroups() as? [SharedBookGroup] ?? []
        visibleBookGroups = bookGroups.filter { $0.show }
        selectableBookGroups = runtime.loadSelectableBookGroups() as? [SharedBookGroup] ?? []
        readRecords = runtime.loadReadRecords() as? [SharedReadRecord] ?? []
        visibleBooks = runtime.loadBooksForGroup(groupId: selectedBookGroupId) as? [SharedBook] ?? books
        bookmarks = runtime.loadBookmarks() as? [SharedBookmark] ?? []
        replaceRules = runtime.loadReplaceRules() as? [SharedReplaceRule] ?? []
        dictRules = runtime.loadDictRules() as? [SharedDictRule] ?? []
        httpTts = runtime.loadHttpTts() as? [SharedHttpTts] ?? []
        txtTocRules = runtime.loadTxtTocRules() as? [SharedTxtTocRule] ?? []
        servers = runtime.loadServers() as? [SharedServer] ?? []
        keyboardAssists = runtime.loadKeyboardAssists() as? [SharedKeyboardAssist] ?? []
        ruleSubs = runtime.loadRuleSubs() as? [SharedRuleSub] ?? []
        rawConfigs = runtime.loadRawConfigs() as? [SharedRawConfigEntry] ?? []
        cookies = runtime.loadCookies() as? [SharedCookie] ?? []
        cacheEntries = runtime.loadCacheEntries() as? [SharedCacheEntry] ?? []
        readerPreferences = runtime.loadReaderPreferences()
        rssSources = runtime.loadRssSources() as? [SharedRssSource] ?? []
        rssReadRecords = runtime.loadRssReadRecords() as? [SharedRssReadRecord] ?? []
        rssStars = runtime.loadRssStars() as? [SharedRssStar] ?? []
        rssStarredArticles = runtime.loadRssStarredArticles() as? [SharedRssArticle] ?? []
        exploreSources = runtime.loadExploreSources() as? [SharedBookSource] ?? []
        searchBooks = runtime.loadSearchBooks() as? [SharedSearchBook] ?? []
        searchKeywords = runtime.loadSearchKeywords() as? [SharedSearchKeyword] ?? []
        if selectedSourceIndex >= sources.count {
            selectedSourceIndex = sources.isEmpty ? -1 : 0
        }
    }

    func importBundledDefaultData() {
        runtime.importAndSaveDefaultData(payload: DefaultDataBundle.payload())
        refreshLibrary()
        message = sources.isEmpty ? "No bundled sources" : "Imported \(sources.count) source(s)"
    }

    func importSources() {
        let rawJson = sourceJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Source JSON is empty"
            return
        }
        do {
            sources = try runtime.importAndSaveBookSources(json: rawJson) as? [SharedBookSource] ?? []
            selectedSourceIndex = sources.isEmpty ? -1 : 0
            refreshLibrary()
            message = sources.isEmpty ? "No usable sources" : "Imported \(sources.count) source(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportSourcesToEditor() {
        sourceJson = runtime.exportBookSourcesJson()
        message = "Exported \(sources.count) source(s)"
    }

    func saveBookSourceJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Book source JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertBookSourceJson(json: rawJson)
            refreshLibrary()
            message = "Saved book source"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importSourcesFromUrl(replace: Bool = false) async {
        let url = sourceImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Source URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            sources = try await runtime.importBookSourcesFromUrl(url: url, replace: replace) as? [SharedBookSource] ?? []
            refreshLibrary()
            message = sources.isEmpty ? "No usable sources" : "Imported \(sources.count) source(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func importReplaceRules(replace: Bool = false) {
        let rawJson = replaceRuleJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Replace rule JSON is empty"
            return
        }
        do {
            replaceRules = try runtime.importAndSaveReplaceRules(json: rawJson, replace: replace) as? [SharedReplaceRule] ?? []
            refreshLibrary()
            message = replaceRules.isEmpty ? "No usable rules" : "Imported \(replaceRules.count) rule(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportReplaceRulesToEditor() {
        replaceRuleJson = runtime.exportReplaceRulesJson()
        message = "Exported \(replaceRules.count) rule(s)"
    }

    func saveReplaceRuleJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Replace rule JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertReplaceRuleJson(json: rawJson)
            refreshLibrary()
            message = "Saved replace rule"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importReplaceRulesFromUrl(replace: Bool = false) async {
        let url = replaceRuleImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Replace rule URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            replaceRules = try await runtime.importReplaceRulesFromUrl(url: url, replace: replace) as? [SharedReplaceRule] ?? []
            refreshLibrary()
            message = replaceRules.isEmpty ? "No usable replace rules" : "Imported \(replaceRules.count) replace rule(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportBackupToEditor() {
        backupJson = runtime.exportBackupJson(nowMillis: nowMillis())
        message = "Exported backup"
    }

    func importDictRules(replace: Bool = false) {
        let rawJson = dictRuleJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Dictionary rule JSON is empty"
            return
        }
        do {
            dictRules = try runtime.importAndSaveDictRules(json: rawJson, replace: replace) as? [SharedDictRule] ?? []
            refreshLibrary()
            message = dictRules.isEmpty ? "No usable dictionary rules" : "Imported \(dictRules.count) dictionary rule(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportDictRulesToEditor() {
        dictRuleJson = runtime.exportDictRulesJson()
        message = "Exported \(dictRules.count) dictionary rule(s)"
    }

    func saveDictRuleJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Dictionary rule JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertDictRuleJson(json: rawJson)
            refreshLibrary()
            message = "Saved dictionary rule"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importDictRulesFromUrl(replace: Bool = false) async {
        let url = dictRuleImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Dictionary rule URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            dictRules = try await runtime.importDictRulesFromUrl(url: url, replace: replace) as? [SharedDictRule] ?? []
            refreshLibrary()
            message = dictRules.isEmpty ? "No usable dictionary rules" : "Imported \(dictRules.count) dictionary rule(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func setDictRuleEnabled(_ rule: SharedDictRule, enabled: Bool) {
        _ = runtime.setDictRuleEnabled(name: rule.name, enabled: enabled)
        refreshLibrary()
    }

    func deleteDictRule(_ rule: SharedDictRule) {
        _ = runtime.deleteDictRule(name: rule.name)
        refreshLibrary()
    }

    func lookupDictionary() async {
        let text = dictionaryKeyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            message = "Dictionary keyword is empty"
            return
        }
        guard dictRules.contains(where: { $0.enabled }) else {
            dictionaryLookupResults = []
            message = "No enabled dictionary rules"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            dictionaryLookupResults = try await runtime.lookupDictionary(word: text) as? [SharedDictionaryLookupResult] ?? []
            if dictionaryLookupResults.isEmpty {
                message = "No dictionary results"
            } else {
                let errors = dictionaryLookupResults.filter { $0.errorMessage != nil }.count
                message = errors == 0 ? nil : "\(errors) dictionary rule(s) failed"
            }
        } catch {
            dictionaryLookupResults = []
            message = error.localizedDescription
        }
    }

    func importHttpTts(replace: Bool = false) {
        let rawJson = httpTtsJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "HTTP TTS JSON is empty"
            return
        }
        do {
            httpTts = try runtime.importAndSaveHttpTts(json: rawJson, replace: replace) as? [SharedHttpTts] ?? []
            refreshLibrary()
            message = httpTts.isEmpty ? "No usable HTTP TTS engines" : "Imported \(httpTts.count) HTTP TTS engine(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportHttpTtsToEditor() {
        httpTtsJson = runtime.exportHttpTtsJson()
        message = "Exported \(httpTts.count) HTTP TTS engine(s)"
    }

    func saveHttpTtsJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "HTTP TTS JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertHttpTtsJson(json: rawJson)
            refreshLibrary()
            message = "Saved HTTP TTS engine"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importHttpTtsFromUrl(replace: Bool = false) async {
        let url = httpTtsImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "HTTP TTS URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            httpTts = try await runtime.importHttpTtsFromUrl(url: url, replace: replace) as? [SharedHttpTts] ?? []
            refreshLibrary()
            message = httpTts.isEmpty ? "No usable HTTP TTS engines" : "Imported \(httpTts.count) HTTP TTS engine(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func httpTtsAudioRequest(for text: String) -> SharedHttpRequest? {
        let content = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !content.isEmpty, let engine = httpTts.first else {
            return nil
        }
        return runtime.buildHttpTtsAudioRequest(engine: engine, text: content, speechRate: Int32(15))
    }

    func deleteHttpTts(_ engine: SharedHttpTts) {
        _ = runtime.deleteHttpTts(id: engine.id)
        refreshLibrary()
    }

    func importTxtTocRules(replace: Bool = false) {
        let rawJson = txtTocRuleJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "TXT TOC rule JSON is empty"
            return
        }
        do {
            txtTocRules = try runtime.importAndSaveTxtTocRules(json: rawJson, replace: replace) as? [SharedTxtTocRule] ?? []
            refreshLibrary()
            message = txtTocRules.isEmpty ? "No usable TXT TOC rules" : "Imported \(txtTocRules.count) TXT TOC rule(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportTxtTocRulesToEditor() {
        txtTocRuleJson = runtime.exportTxtTocRulesJson()
        message = "Exported \(txtTocRules.count) TXT TOC rule(s)"
    }

    func saveTxtTocRuleJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "TXT TOC rule JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertTxtTocRuleJson(json: rawJson)
            refreshLibrary()
            message = "Saved TXT TOC rule"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importTxtTocRulesFromUrl(replace: Bool = false) async {
        let url = txtTocRuleImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "TXT TOC rule URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            txtTocRules = try await runtime.importTxtTocRulesFromUrl(url: url, replace: replace) as? [SharedTxtTocRule] ?? []
            refreshLibrary()
            message = txtTocRules.isEmpty ? "No usable TXT TOC rules" : "Imported \(txtTocRules.count) TXT TOC rule(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func setTxtTocRuleEnabled(_ rule: SharedTxtTocRule, enabled: Bool) {
        _ = runtime.setTxtTocRuleEnabled(id: rule.id, enabled: enabled)
        refreshLibrary()
    }

    func deleteTxtTocRule(_ rule: SharedTxtTocRule) {
        _ = runtime.deleteTxtTocRule(id: rule.id)
        refreshLibrary()
    }

    func importServers(replace: Bool = false) {
        let rawJson = serverJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Server JSON is empty"
            return
        }
        do {
            servers = try runtime.importAndSaveServers(json: rawJson, replace: replace) as? [SharedServer] ?? []
            refreshLibrary()
            message = servers.isEmpty ? "No usable servers" : "Imported \(servers.count) server(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportServersToEditor() {
        serverJson = runtime.exportServersJson()
        message = "Exported \(servers.count) server(s)"
    }

    func saveServerJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Server JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertServerJson(json: rawJson)
            refreshLibrary()
            message = "Saved server"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importServersFromUrl(replace: Bool = false) async {
        let url = serverImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Server URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            servers = try await runtime.importServersFromUrl(url: url, replace: replace) as? [SharedServer] ?? []
            refreshLibrary()
            message = servers.isEmpty ? "No usable servers" : "Imported \(servers.count) server(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func deleteServer(_ server: SharedServer) {
        servers = runtime.deleteServer(id: server.id) as? [SharedServer] ?? []
        refreshLibrary()
    }

    func importKeyboardAssists(replace: Bool = false) {
        let rawJson = keyboardAssistJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Keyboard assist JSON is empty"
            return
        }
        do {
            keyboardAssists = try runtime.importAndSaveKeyboardAssists(json: rawJson, replace: replace) as? [SharedKeyboardAssist] ?? []
            refreshLibrary()
            message = keyboardAssists.isEmpty ? "No usable keyboard assists" : "Imported \(keyboardAssists.count) keyboard assist(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportKeyboardAssistsToEditor() {
        keyboardAssistJson = runtime.exportKeyboardAssistsJson()
        message = "Exported \(keyboardAssists.count) keyboard assist(s)"
    }

    func saveKeyboardAssistJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Keyboard assist JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertKeyboardAssistJson(json: rawJson)
            refreshLibrary()
            message = "Saved keyboard assist"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importKeyboardAssistsFromUrl(replace: Bool = false) async {
        let url = keyboardAssistImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Keyboard assist URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            keyboardAssists = try await runtime.importKeyboardAssistsFromUrl(url: url, replace: replace) as? [SharedKeyboardAssist] ?? []
            refreshLibrary()
            message = keyboardAssists.isEmpty ? "No usable keyboard assists" : "Imported \(keyboardAssists.count) keyboard assist(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func deleteKeyboardAssist(_ assist: SharedKeyboardAssist) {
        keyboardAssists = runtime.deleteKeyboardAssist(type: assist.type, key: assist.key) as? [SharedKeyboardAssist] ?? []
        refreshLibrary()
    }

    func importRuleSubs(replace: Bool = false) {
        let rawJson = ruleSubJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Rule subscription JSON is empty"
            return
        }
        do {
            ruleSubs = try runtime.importAndSaveRuleSubs(json: rawJson, replace: replace) as? [SharedRuleSub] ?? []
            refreshLibrary()
            message = ruleSubs.isEmpty ? "No usable rule subscriptions" : "Imported \(ruleSubs.count) rule subscription(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportRuleSubsToEditor() {
        ruleSubJson = runtime.exportRuleSubsJson()
        message = "Exported \(ruleSubs.count) rule subscription(s)"
    }

    func saveRuleSubJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Rule subscription JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertRuleSubJson(json: rawJson)
            refreshLibrary()
            message = "Saved rule subscription"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importRuleSubsFromUrl(replace: Bool = false) async {
        let url = ruleSubImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Rule subscription URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            ruleSubs = try await runtime.importRuleSubsFromUrl(url: url, replace: replace) as? [SharedRuleSub] ?? []
            refreshLibrary()
            message = ruleSubs.isEmpty ? "No usable rule subscriptions" : "Imported \(ruleSubs.count) rule subscription(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func setRuleSubAutoUpdate(_ ruleSub: SharedRuleSub, autoUpdate: Bool) {
        _ = runtime.setRuleSubAutoUpdate(id: ruleSub.id, autoUpdate: autoUpdate)
        refreshLibrary()
    }

    func deleteRuleSub(_ ruleSub: SharedRuleSub) {
        ruleSubs = runtime.deleteRuleSub(id: ruleSub.id) as? [SharedRuleSub] ?? []
        refreshLibrary()
    }

    func updateRuleSub(_ ruleSub: SharedRuleSub) async {
        isLoading = true
        defer { isLoading = false }

        do {
            let result = try await runtime.updateRuleSub(ruleSub: ruleSub, nowMillis: nowMillis())
            refreshLibrary()
            message = "Updated \(ruleSub.name): imported \(result.importedCount) item(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func updateAutoRuleSubs() async {
        isLoading = true
        defer { isLoading = false }

        do {
            let results = try await runtime.updateAutoRuleSubs(nowMillis: nowMillis()) as? [RuleSubUpdateResult] ?? []
            refreshLibrary()
            let total = results.reduce(0) { $0 + Int($1.importedCount) }
            message = results.isEmpty ? "No due rule subscriptions" : "Updated \(results.count) subscription(s), imported \(total) item(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func importRawConfigs(replace: Bool = false) {
        let rawJson = rawConfigJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Raw config JSON is empty"
            return
        }
        do {
            rawConfigs = try runtime.importAndSaveRawConfigs(json: rawJson, replace: replace) as? [SharedRawConfigEntry] ?? []
            refreshLibrary()
            message = rawConfigs.isEmpty ? "No usable raw configs" : "Imported \(rawConfigs.count) raw config(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportRawConfigsToEditor() {
        rawConfigJson = runtime.exportRawConfigsJson()
        message = "Exported \(rawConfigs.count) raw config(s)"
    }

    func saveRawConfigJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Raw config JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertRawConfigJson(json: rawJson)
            refreshLibrary()
            message = "Saved raw config"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importRawConfigsFromUrl(replace: Bool = false) async {
        let url = rawConfigImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Raw config URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            rawConfigs = try await runtime.importRawConfigsFromUrl(url: url, replace: replace) as? [SharedRawConfigEntry] ?? []
            refreshLibrary()
            message = rawConfigs.isEmpty ? "No usable raw configs" : "Imported \(rawConfigs.count) raw config(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func deleteRawConfig(_ entry: SharedRawConfigEntry) {
        rawConfigs = runtime.deleteRawConfig(key: entry.key) as? [SharedRawConfigEntry] ?? []
        refreshLibrary()
    }

    func importCookies(replace: Bool = false) {
        let rawJson = cookieJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Cookie JSON is empty"
            return
        }
        do {
            cookies = try runtime.importAndSaveCookies(json: rawJson, replace: replace) as? [SharedCookie] ?? []
            refreshLibrary()
            message = cookies.isEmpty ? "No usable cookies" : "Imported \(cookies.count) cookie(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportCookiesToEditor() {
        cookieJson = runtime.exportCookiesJson()
        message = "Exported \(cookies.count) cookie(s)"
    }

    func saveCookieJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Cookie JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertCookieJson(json: rawJson)
            refreshLibrary()
            message = "Saved cookie"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importCookiesFromUrl(replace: Bool = false) async {
        let url = cookieImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Cookie URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            cookies = try await runtime.importCookiesFromUrl(url: url, replace: replace) as? [SharedCookie] ?? []
            refreshLibrary()
            message = cookies.isEmpty ? "No usable cookies" : "Imported \(cookies.count) cookie(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func deleteCookie(_ cookie: SharedCookie) {
        cookies = runtime.deleteCookie(url: cookie.url) as? [SharedCookie] ?? []
        refreshLibrary()
    }

    func clearCookies() {
        cookies = runtime.clearCookies() as? [SharedCookie] ?? []
        refreshLibrary()
    }

    func importCacheEntries(replace: Bool = false) {
        let rawJson = cacheJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Cache JSON is empty"
            return
        }
        do {
            cacheEntries = try runtime.importAndSaveCacheEntries(json: rawJson, replace: replace) as? [SharedCacheEntry] ?? []
            refreshLibrary()
            message = cacheEntries.isEmpty ? "No usable cache entries" : "Imported \(cacheEntries.count) cache entry(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportCacheEntriesToEditor() {
        cacheJson = runtime.exportCacheEntriesJson()
        message = "Exported \(cacheEntries.count) cache entry(s)"
    }

    func saveCacheEntryJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Cache JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertCacheEntryJson(json: rawJson)
            refreshLibrary()
            message = "Saved cache entry"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importCacheEntriesFromUrl(replace: Bool = false) async {
        let url = cacheImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Cache URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            cacheEntries = try await runtime.importCacheEntriesFromUrl(url: url, replace: replace) as? [SharedCacheEntry] ?? []
            refreshLibrary()
            message = cacheEntries.isEmpty ? "No usable cache entries" : "Imported \(cacheEntries.count) cache entry(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func deleteCacheEntry(_ entry: SharedCacheEntry) {
        cacheEntries = runtime.deleteCacheEntry(key: entry.key) as? [SharedCacheEntry] ?? []
        refreshLibrary()
    }

    func clearExpiredCacheEntries() {
        cacheEntries = runtime.clearExpiredCacheEntries(nowMillis: nowMillis()) as? [SharedCacheEntry] ?? []
        refreshLibrary()
    }

    func clearCacheEntries() {
        cacheEntries = runtime.clearCacheEntries() as? [SharedCacheEntry] ?? []
        refreshLibrary()
    }

    func importRssSources(replace: Bool = false) {
        let rawJson = rssSourceJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "RSS source JSON is empty"
            return
        }
        do {
            rssSources = try runtime.importAndSaveRssSources(json: rawJson, replace: replace) as? [SharedRssSource] ?? []
            refreshLibrary()
            message = rssSources.isEmpty ? "No usable RSS sources" : "Imported \(rssSources.count) RSS source(s)"
        } catch {
            message = error.localizedDescription
        }
    }

    func exportRssSourcesToEditor() {
        rssSourceJson = runtime.exportRssSourcesJson()
        message = "Exported \(rssSources.count) RSS source(s)"
    }

    func saveRssSourceJson(_ json: String) -> Bool {
        let rawJson = json.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "RSS source JSON is empty"
            return false
        }
        do {
            _ = try runtime.upsertRssSourceJson(json: rawJson)
            refreshLibrary()
            message = "Saved RSS source"
            return true
        } catch {
            message = error.localizedDescription
            return false
        }
    }

    func importRssSourcesFromUrl(replace: Bool = false) async {
        let url = rssSourceImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "RSS source URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            rssSources = try await runtime.importRssSourcesFromUrl(url: url, replace: replace) as? [SharedRssSource] ?? []
            refreshLibrary()
            message = rssSources.isEmpty ? "No usable RSS sources" : "Imported \(rssSources.count) RSS source(s) from URL"
        } catch {
            message = error.localizedDescription
        }
    }

    func setRssSourceEnabled(_ source: SharedRssSource, enabled: Bool) {
        _ = runtime.setRssSourceEnabled(sourceUrl: source.sourceUrl, enabled: enabled)
        refreshLibrary()
    }

    func deleteRssSource(_ source: SharedRssSource) {
        rssSources = runtime.deleteRssSource(sourceUrl: source.sourceUrl) as? [SharedRssSource] ?? []
        if selectedRssSource?.sourceUrl == source.sourceUrl {
            selectedRssSource = nil
            selectedRssArticle = nil
            rssArticles = []
            rssContent = ""
        }
        refreshLibrary()
    }

    func setRssArticleRead(_ article: SharedRssArticle, read: Bool) {
        let updated = runtime.markRssArticleRead(article: article, read: read, nowMillis: nowMillis())
        replaceRssArticle(updated)
        if let selected = selectedRssArticle, sameRssArticle(selected, updated) {
            selectedRssArticle = updated
            rssContent = updated.readableContent
        }
        refreshLibrary()
    }

    func setRssArticleStarred(_ article: SharedRssArticle, starred: Bool) {
        rssStars = runtime.setRssArticleStarred(article: article, starred: starred, nowMillis: nowMillis()) as? [SharedRssStar] ?? []
        rssStarredArticles = runtime.loadRssStarredArticles() as? [SharedRssArticle] ?? []
        refreshLibrary()
    }

    func isRssArticleStarred(_ article: SharedRssArticle) -> Bool {
        return rssStars.contains { star in
            star.origin == article.origin && star.sort == article.sort && star.link == article.link
        }
    }

    func importBackupFromEditor() {
        let rawJson = backupJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Backup JSON is empty"
            return
        }
        do {
            _ = try runtime.importBackupJson(json: rawJson)
            selectedBook = nil
            selectedSearchBook = nil
            chapters = []
            currentChapter = nil
            currentContent = ""
            refreshLibrary()
            message = "Imported backup"
        } catch {
            message = error.localizedDescription
        }
    }

    func importSmartConfig(replace: Bool = false) {
        let rawJson = smartImportJson.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !rawJson.isEmpty else {
            message = "Import JSON is empty"
            return
        }
        do {
            let summary = try runtime.importAnyConfigJson(json: rawJson, replace: replace)
            selectedBook = nil
            selectedSearchBook = nil
            selectedRssSource = nil
            selectedRssArticle = nil
            chapters = []
            currentChapter = nil
            currentContent = ""
            rssArticles = []
            rssContent = ""
            refreshLibrary()
            message = smartImportMessage(summary)
        } catch {
            message = error.localizedDescription
        }
    }

    func importSmartConfigFromUrl(replace: Bool = false) async {
        let url = smartImportUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !url.isEmpty else {
            message = "Import URL is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let summary = try await runtime.importAnyConfigFromUrl(url: url, replace: replace)
            selectedBook = nil
            selectedSearchBook = nil
            selectedRssSource = nil
            selectedRssArticle = nil
            chapters = []
            currentChapter = nil
            currentContent = ""
            rssArticles = []
            rssContent = ""
            refreshLibrary()
            message = smartImportMessage(summary, suffix: " from URL")
        } catch {
            message = error.localizedDescription
        }
    }

    func uploadBackupToWebDav() async {
        guard let server = servers.first(where: { $0.type.uppercased() == "WEBDAV" }) else {
            message = "No WebDAV server configured"
            return
        }
        let fileName = webDavBackupFileName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !fileName.isEmpty else {
            message = "WebDAV backup file name is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let response = try await runtime.uploadBackupToWebDav(
                server: server,
                fileName: fileName,
                nowMillis: nowMillis()
            )
            message = "Uploaded backup to \(server.name) (HTTP \(response.statusCode))"
        } catch {
            message = error.localizedDescription
        }
    }

    func downloadBackupFromWebDav() async {
        guard let server = servers.first(where: { $0.type.uppercased() == "WEBDAV" }) else {
            message = "No WebDAV server configured"
            return
        }
        let fileName = webDavBackupFileName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !fileName.isEmpty else {
            message = "WebDAV backup file name is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            _ = try await runtime.downloadBackupFromWebDav(server: server, fileName: fileName)
            backupJson = runtime.exportBackupJson(nowMillis: nowMillis())
            selectedBook = nil
            selectedSearchBook = nil
            chapters = []
            currentChapter = nil
            currentContent = ""
            refreshLibrary()
            message = "Downloaded and imported backup from \(server.name)"
        } catch {
            message = error.localizedDescription
        }
    }

    func setReplaceRuleEnabled(_ rule: SharedReplaceRule, enabled: Bool) {
        _ = runtime.setReplaceRuleEnabled(id: rule.id, enabled: enabled)
        refreshLibrary()
    }

    func deleteReplaceRule(_ rule: SharedReplaceRule) {
        _ = runtime.deleteReplaceRule(id: rule.id)
        refreshLibrary()
    }

    func setSourceEnabled(_ source: SharedBookSource, enabled: Bool) {
        _ = runtime.setBookSourceEnabled(bookSourceUrl: source.bookSourceUrl, enabled: enabled)
        refreshLibrary()
    }

    func deleteSource(_ source: SharedBookSource) {
        _ = runtime.deleteBookSource(bookSourceUrl: source.bookSourceUrl)
        refreshLibrary()
    }

    func sourceWebLoginRequest(_ source: SharedBookSource) -> SharedSourceLoginRequest? {
        runtime.buildSourceWebLoginRequest(source: source)
    }

    func sourceLoginFields(_ source: SharedBookSource) -> [SharedLoginUiField] {
        runtime.loadSourceLoginFields(source: source) as? [SharedLoginUiField] ?? []
    }

    func sourceLoginInfo(_ source: SharedBookSource) -> [String: String] {
        let json = runtime.loadSourceLoginInfoJson(source: source)
        guard let data = json.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return [:]
        }
        return object.reduce(into: [String: String]()) { result, item in
            result[item.key] = "\(item.value)"
        }
    }

    func saveSourceLoginInfo(_ source: SharedBookSource, values: [String: String]) {
        guard let data = try? JSONSerialization.data(withJSONObject: values, options: [.sortedKeys]),
              let json = String(data: data, encoding: .utf8) else {
            message = "Login info JSON encode failed"
            return
        }
        _ = runtime.saveSourceLoginInfoJson(source: source, json: json)
        refreshLibrary()
        message = "Saved login info for \(source.bookSourceName.isEmpty ? source.bookSourceUrl : source.bookSourceName)"
    }

    func clearSourceLoginInfo(_ source: SharedBookSource) {
        _ = runtime.clearSourceLoginInfo(source: source)
        refreshLibrary()
        message = "Cleared login info"
    }

    func saveSourceWebLoginCookie(_ source: SharedBookSource, cookie: String) {
        let cleanCookie = cookie.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanCookie.isEmpty else {
            message = "Cookie is empty"
            return
        }
        _ = runtime.saveSourceWebLoginCookie(source: source, cookie: cleanCookie)
        refreshLibrary()
        message = "Saved login cookie for \(source.bookSourceName.isEmpty ? source.bookSourceUrl : source.bookSourceName)"
    }

    func deleteBook(_ book: SharedBook) {
        books = runtime.removeBook(book: book) as? [SharedBook] ?? []
        if selectedBook?.bookUrl == book.bookUrl {
            selectedBook = nil
            chapters = []
            currentChapter = nil
            currentContent = ""
        }
        refreshLibrary()
    }

    func updateSelectedBookMetadata(
        name: String,
        author: String,
        customIntro: String,
        customCoverUrl: String,
        customTag: String
    ) {
        guard let book = selectedBook else {
            message = "No book selected"
            return
        }
        selectedBook = runtime.updateBookMetadata(
            book: book,
            name: name,
            author: author,
            customIntro: customIntro,
            customCoverUrl: customCoverUrl,
            customTag: customTag
        )
        refreshLibrary()
        message = "Book metadata saved"
    }

    func selectBookGroup(_ group: SharedBookGroup) {
        selectedBookGroupId = group.groupId
        visibleBooks = runtime.loadBooksForGroup(groupId: group.groupId) as? [SharedBook] ?? books
    }

    func createBookGroup(name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            message = "Group name is empty"
            return
        }
        _ = runtime.upsertBookGroup(
            group: SharedBookGroup(
                groupId: Int64(0),
                groupName: trimmed,
                cover: nil,
                order: Int32(0),
                enableRefresh: true,
                show: true,
                bookSort: Int32(-1),
                onlyUpdateRead: false
            )
        )
        refreshLibrary()
        message = "Group created"
    }

    func setBookGroupVisible(_ group: SharedBookGroup, show: Bool) {
        _ = runtime.setBookGroupVisible(groupId: group.groupId, show: show)
        if !show && selectedBookGroupId == group.groupId {
            selectedBookGroupId = -1
        }
        refreshLibrary()
    }

    func deleteBookGroup(_ group: SharedBookGroup) {
        _ = runtime.deleteBookGroup(groupId: group.groupId)
        if selectedBookGroupId == group.groupId {
            selectedBookGroupId = -1
        }
        refreshLibrary()
    }

    func setSelectedBookGroup(_ group: SharedBookGroup, enabled: Bool) {
        guard let book = selectedBook else {
            message = "No book selected"
            return
        }
        selectedBook = runtime.setBookGroupEnabled(book: book, groupId: group.groupId, enabled: enabled)
        refreshLibrary()
    }

    func debugSource(_ source: SharedBookSource) async {
        let text = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            message = "Keyword is empty"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let result = try await runtime.debugSourceFirstContent(
                source: source,
                key: text,
                page: 1
            )
            debugSteps = result.steps as? [SourceDebugStep] ?? []
            debugContent = result.content?.content.content ?? ""
            message = debugSteps.isEmpty ? "No debug steps" : nil
        } catch {
            debugSteps = []
            debugContent = ""
            message = error.localizedDescription
        }
    }

    func refreshRss(_ source: SharedRssSource) async {
        isLoading = true
        defer { isLoading = false }

        do {
            let page = try await runtime.refreshRssArticles(source: source, page: 1)
            selectedRssSource = source
            rssArticles = page.articles as? [SharedRssArticle] ?? []
            selectedRssArticle = nil
            rssContent = ""
            refreshLibrary()
            message = rssArticles.isEmpty ? "No RSS articles" : nil
        } catch {
            rssArticles = runtime.loadRssArticles(source: source) as? [SharedRssArticle] ?? []
            selectedRssSource = source
            message = error.localizedDescription
        }
    }

    func openExploreSource(_ source: SharedBookSource) {
        selectedExploreSource = source
        exploreKinds = runtime.loadExploreKinds(source: source) as? [SharedExploreKind] ?? []
        exploreResults = []
        message = exploreKinds.isEmpty ? "No explore categories" : nil
    }

    func openExploreKind(_ kind: SharedExploreKind) async {
        guard let source = selectedExploreSource else {
            message = "Explore source not selected"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let page = try await runtime.loadExplorePage(source: source, kind: kind, page: 1)
            exploreResults = page.books as? [SharedSearchBook] ?? []
            message = exploreResults.isEmpty ? "No explore results" : nil
        } catch {
            exploreResults = []
            message = error.localizedDescription
        }
    }

    func openRssArticle(_ article: SharedRssArticle) async {
        guard let source = rssSources.first(where: { $0.sourceUrl == article.origin }) ?? selectedRssSource else {
            message = "RSS source not found"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let parsed = try await runtime.loadRssContent(source: source, article: article)
            let read = runtime.markRssArticleRead(article: parsed, read: true, nowMillis: nowMillis())
            selectedRssArticle = read
            rssContent = read.readableContent
            replaceRssArticle(read)
            refreshLibrary()
        } catch {
            let read = runtime.markRssArticleRead(article: article, read: true, nowMillis: nowMillis())
            selectedRssArticle = read
            rssContent = read.readableContent
            replaceRssArticle(read)
            message = error.localizedDescription
        }
    }

    func search() async {
        let text = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            message = "Keyword is empty"
            return
        }
        guard !sources.isEmpty else {
            message = "No sources"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            if sources.indices.contains(selectedSourceIndex) {
                _ = runtime.recordSearchKeyword(key: text, nowMillis: nowMillis())
                let result = try await runtime.client.search(
                    source: sources[selectedSourceIndex],
                    key: text,
                    page: 1
                )
                searchResults = result.books as? [SharedSearchBook] ?? []
                searchErrors = []
            } else {
                let result = try await runtime.searchEnabledSources(
                    key: text,
                    page: 1,
                    nowMillis: nowMillis()
                )
                searchResults = result.books as? [SharedSearchBook] ?? []
                searchErrors = result.errors as? [SourceSearchError] ?? []
            }
            searchKeywords = runtime.loadSearchKeywords() as? [SharedSearchKeyword] ?? []
            searchBooks = runtime.loadSearchBooks() as? [SharedSearchBook] ?? []
            message = searchResults.isEmpty ? "No results" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func search(keyword searchKeyword: SharedSearchKeyword) async {
        keyword = searchKeyword.word
        await search()
    }

    func deleteSearchKeyword(_ searchKeyword: SharedSearchKeyword) {
        searchKeywords = runtime.deleteSearchKeyword(word: searchKeyword.word) as? [SharedSearchKeyword] ?? []
    }

    func clearSearchKeywords() {
        searchKeywords = runtime.clearSearchKeywords() as? [SharedSearchKeyword] ?? []
    }

    func deleteSearchBook(_ searchBook: SharedSearchBook) {
        searchBooks = runtime.deleteSearchBook(bookUrl: searchBook.bookUrl) as? [SharedSearchBook] ?? []
        changeSourceCandidates.removeAll { $0.bookUrl == searchBook.bookUrl }
        refreshLibrary()
    }

    func clearSearchBooks() {
        searchBooks = runtime.clearSearchBooks() as? [SharedSearchBook] ?? []
        changeSourceCandidates = []
        refreshLibrary()
    }

    func clearExpiredSearchBooks(days: Int = 30) {
        let cutoff = nowMillis() - Int64(days) * 24 * 60 * 60 * 1000
        searchBooks = runtime.clearExpiredSearchBooks(beforeMillis: cutoff) as? [SharedSearchBook] ?? []
        refreshLibrary()
        message = "Cleared search books older than \(days) day(s)"
    }

    func openSearchResult(_ searchBook: SharedSearchBook) async {
        guard let source = source(for: searchBook) else {
            message = "Source not found"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let detail = try await runtime.openSearchBook(
                source: source,
                searchBook: searchBook,
                nowMillis: nowMillis()
            )
            activeSource = source
            selectedSearchBook = searchBook
            selectedBook = detail.book
            chapters = detail.chapters as? [SharedBookChapter] ?? []
            currentChapter = nil
            currentContent = ""
            readerSearchResults = []
            changeSourceCandidates = runtime.loadChangeSourceCandidates(book: detail.book, key: "") as? [SharedSearchBook] ?? []
            refreshLibrary()
            message = chapters.isEmpty ? "No chapters" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func openBook(_ book: SharedBook) async {
        guard let source = source(for: book) else {
            selectedBook = book
            chapters = runtime.loadBookChapters(book: book) as? [SharedBookChapter] ?? []
            activeSource = nil
            currentChapter = nil
            currentContent = ""
            readerSearchResults = []
            changeSourceCandidates = runtime.loadChangeSourceCandidates(book: book, key: "") as? [SharedSearchBook] ?? []
            message = chapters.isEmpty ? "Source not found" : nil
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            let cachedChapters = runtime.loadBookChapters(book: book) as? [SharedBookChapter] ?? []
            if cachedChapters.isEmpty {
                let detail = try await runtime.refreshBook(
                    source: source,
                    book: book,
                    nowMillis: nowMillis()
                )
                selectedBook = detail.book
                chapters = detail.chapters as? [SharedBookChapter] ?? []
            } else {
                selectedBook = book
                chapters = cachedChapters
            }
            activeSource = source
            currentChapter = nil
            currentContent = ""
            readerSearchResults = []
            if let selected = selectedBook {
                changeSourceCandidates = runtime.loadChangeSourceCandidates(book: selected, key: "") as? [SharedSearchBook] ?? []
            }
            refreshLibrary()
            message = chapters.isEmpty ? "No chapters" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func refreshBookshelfUpdates() async {
        isLoading = true
        defer { isLoading = false }

        do {
            bookUpdateResults = try await runtime.refreshUpdatableBooks(nowMillis: nowMillis()) as? [BookUpdateResult] ?? []
            refreshLibrary()
            let errors = bookUpdateResults.filter { $0.errorMessage != nil }.count
            let newChapters = bookUpdateResults.reduce(0) { total, result in
                total + Int(result.newChapterCount)
            }
            if bookUpdateResults.isEmpty {
                message = "No updateable books"
            } else if errors > 0 {
                message = "Updated \(bookUpdateResults.count) book(s), \(newChapters) new chapter(s), \(errors) error(s)"
            } else {
                message = "Updated \(bookUpdateResults.count) book(s), \(newChapters) new chapter(s)"
            }
        } catch {
            message = error.localizedDescription
        }
    }

    func loadChapter(at index: Int) async {
        guard let book = selectedBook else {
            message = "No book selected"
            return
        }
        guard chapters.indices.contains(index) else {
            message = "Chapter not found"
            return
        }
        isLoading = true
        defer { isLoading = false }

        do {
            guard let source = activeSource ?? source(for: book) else {
                let result = try runtime.loadCachedChapter(
                    book: book,
                    chapterIndex: Int32(index),
                    position: 0,
                    nowMillis: nowMillis()
                )
                selectedBook = result.book
                chapters = result.chapters as? [SharedBookChapter] ?? chapters
                currentChapter = result.chapter
                currentChapterIndex = Int(result.chapterIndex)
                currentContent = result.content.content
                readerSearchResults = []
                refreshLibrary()
                message = currentContent.isEmpty ? "No content" : nil
                return
            }
            let result = try await runtime.loadChapter(
                source: source,
                book: book,
                chapterIndex: Int32(index),
                position: 0,
                nowMillis: nowMillis(),
                preloadAdjacent: true
            )
            activeSource = source
            selectedBook = result.book
            chapters = result.chapters as? [SharedBookChapter] ?? chapters
            currentChapter = result.chapter
            currentChapterIndex = Int(result.chapterIndex)
            currentContent = result.content.content
            readerSearchResults = []
            refreshLibrary()
            message = currentContent.isEmpty ? "No content" : nil
        } catch {
            message = error.localizedDescription
        }
    }

    func loadChangeSourceCandidates() {
        guard let book = selectedBook else {
            message = "No book selected"
            return
        }
        changeSourceCandidates = runtime.loadChangeSourceCandidates(book: book, key: "") as? [SharedSearchBook] ?? []
        message = changeSourceCandidates.isEmpty ? "No change source candidates" : nil
    }

    func changeSource(to searchBook: SharedSearchBook) async {
        await openSearchResult(searchBook)
    }

    func searchCurrentContent(query: String) {
        readerSearchResults = runtime.searchReaderContent(content: currentContent, query: query, contextChars: Int32(48)) as? [SharedReaderSearchResult] ?? []
        message = query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !readerSearchResults.isEmpty ? nil : "No reader search matches"
    }

    func updateReaderPreferences(
        fontSize: Double? = nil,
        lineSpacing: Double? = nil,
        contentPadding: Double? = nil,
        theme: String? = nil
    ) {
        readerPreferences = runtime.saveReaderPreferences(
            preferences: SharedReaderPreferences(
                fontSize: fontSize ?? readerPreferences.fontSize,
                lineSpacing: lineSpacing ?? readerPreferences.lineSpacing,
                contentPadding: contentPadding ?? readerPreferences.contentPadding,
                theme: theme ?? readerPreferences.theme
            )
        )
    }

    func addCurrentBookmark() {
        guard let book = selectedBook, let chapter = currentChapter else {
            message = "No chapter selected"
            return
        }
        _ = runtime.addBookmark(
            book: book,
            chapter: chapter,
            bookText: String(currentContent.prefix(240)),
            note: "",
            position: 0,
            nowMillis: nowMillis()
        )
        refreshLibrary()
        message = "Bookmark added"
    }

    func recordCurrentReadingTime(durationMillis: Int64) {
        guard durationMillis > 0, let book = selectedBook else {
            return
        }
        readRecords = runtime.recordReadTime(
            book: book,
            durationMillis: durationMillis,
            nowMillis: nowMillis(),
            deviceId: "ios"
        ) as? [SharedReadRecord] ?? []
        refreshLibrary()
    }

    func deleteReadRecord(_ record: SharedReadRecord) {
        readRecords = runtime.deleteReadRecord(deviceId: record.deviceId, bookName: record.bookName) as? [SharedReadRecord] ?? []
        refreshLibrary()
    }

    func clearReadRecords() {
        readRecords = runtime.clearReadRecords() as? [SharedReadRecord] ?? []
        refreshLibrary()
    }

    func deleteBookmark(_ bookmark: SharedBookmark) {
        bookmarks = runtime.deleteBookmark(time: bookmark.time) as? [SharedBookmark] ?? []
        refreshLibrary()
    }

    func importLocalTextFile(fileName: String, text: String) {
        do {
            let result = try runtime.importLocalTextBook(
                fileName: fileName,
                text: text,
                nowMillis: nowMillis()
            )
            selectedBook = result.book
            chapters = result.chapters as? [SharedBookChapter] ?? []
            activeSource = nil
            currentChapter = nil
            currentContent = ""
            refreshLibrary()
            message = "Imported \(result.book.name)"
        } catch {
            message = error.localizedDescription
        }
    }

    func importLocalDocumentFile(fileName: String, filePath: String, mimeType: String?) {
        do {
            let result = try runtime.importLocalDocumentBook(
                fileName: fileName,
                fileUrl: filePath,
                mimeType: mimeType,
                nowMillis: nowMillis()
            )
            selectedBook = result.book
            chapters = result.chapters as? [SharedBookChapter] ?? []
            activeSource = nil
            currentChapter = nil
            currentContent = ""
            refreshLibrary()
            message = "Imported \(result.book.name)"
        } catch {
            message = error.localizedDescription
        }
    }

    func showMessage(_ text: String) {
        message = text
    }

    func clearMessage() {
        message = nil
    }

    private func source(for searchBook: SharedSearchBook) -> SharedBookSource? {
        if let source = sources.first(where: { $0.bookSourceUrl == searchBook.origin }) {
            return source
        }
        if sources.indices.contains(selectedSourceIndex) {
            return sources[selectedSourceIndex]
        }
        return sources.first { $0.enabled }
    }

    private func source(for book: SharedBook) -> SharedBookSource? {
        if let source = sources.first(where: { $0.bookSourceUrl == book.origin }) {
            return source
        }
        return sources.first { $0.bookSourceUrl == book.bookUrl || $0.bookSourceName == book.originName }
    }

    private func replaceRssArticle(_ article: SharedRssArticle) {
        if let index = rssArticles.firstIndex(where: { sameRssArticle($0, article) }) {
            rssArticles[index] = article
        }
    }

    private func sameRssArticle(_ lhs: SharedRssArticle, _ rhs: SharedRssArticle) -> Bool {
        return lhs.origin == rhs.origin && lhs.sort == rhs.sort && lhs.link == rhs.link
    }

    private func smartImportMessage(_ summary: SharedImportSummary, suffix: String = "") -> String {
        var parts: [String] = []
        if summary.bookSources > 0 { parts.append("\(summary.bookSources) source(s)") }
        if summary.rssSources > 0 { parts.append("\(summary.rssSources) RSS source(s)") }
        if summary.replaceRules > 0 { parts.append("\(summary.replaceRules) replace rule(s)") }
        if summary.dictRules > 0 { parts.append("\(summary.dictRules) dictionary rule(s)") }
        if summary.httpTts > 0 { parts.append("\(summary.httpTts) HTTP TTS engine(s)") }
        if summary.txtTocRules > 0 { parts.append("\(summary.txtTocRules) TXT TOC rule(s)") }
        if summary.servers > 0 { parts.append("\(summary.servers) server(s)") }
        if summary.keyboardAssists > 0 { parts.append("\(summary.keyboardAssists) keyboard assist(s)") }
        if summary.ruleSubs > 0 { parts.append("\(summary.ruleSubs) rule subscription(s)") }
        if summary.rawConfigs > 0 { parts.append("\(summary.rawConfigs) raw config(s)") }
        if summary.cookies > 0 { parts.append("\(summary.cookies) cookie(s)") }
        if summary.cacheEntries > 0 { parts.append("\(summary.cacheEntries) cache entry(s)") }

        if summary.backup {
            let detail = parts.isEmpty ? "" : ": \(parts.joined(separator: ", "))"
            return "Imported backup\(suffix)\(detail)"
        }
        guard !parts.isEmpty else {
            return "No usable Legado config\(suffix)"
        }
        return "Imported \(summary.total) item(s)\(suffix): \(parts.joined(separator: ", "))"
    }

    private func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}

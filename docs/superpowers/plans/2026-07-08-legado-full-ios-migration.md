# Legado Full iOS Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current iOS prototype with a full iOS migration of the Android Legado app, preserving compileable Android and iOS artifacts throughout the migration.

**Architecture:** Expand `:shared` from a search/content prototype into the cross-platform product core. Keep Android adapters thin, add iOS platform ports in `iosMain`, and rebuild `iosApp` as a real SwiftUI app driven by shared state. Each phase ends with shared tests, Android adapter/build checks, iOS KMP compile checks, and CI unsigned IPA packaging.

**Tech Stack:** Kotlin Multiplatform, kotlinx.coroutines, kotlinx.serialization, existing Android Room/OkHttp/Rhino adapters, Darwin/iOS platform ports, SwiftUI, Xcode CI.

---

## Phase 0: Scope Reset and Guardrails

**Files:**
- Create: `docs/superpowers/specs/2026-07-08-legado-full-ios-migration-design.md`
- Create: `docs/superpowers/plans/2026-07-08-legado-full-ios-migration.md`
- Modify: `iosApp/README.md`
- Modify: `iosApp/LegadoIOS/DefaultSource.swift`
- Modify: `iosApp/LegadoIOS/ContentView.swift`

- [x] **Step 1: Write the full migration design**

Write the full product parity design and make it explicit that the previous prototype non-goals are now part of the migration backlog.

- [x] **Step 2: Remove prototype claims from iOS documentation**

Update `iosApp/README.md` so it says the app is under full migration and lists missing parity areas. Do not describe the existing shell as a usable reader.

- [x] **Step 3: Remove fake default source from the product path**

Replace `DefaultSource.json` with an empty import state or a bundled default-data loader. The UI must not open on `https://api.example.test`.

- [x] **Step 4: Add a visible migration build marker**

Update the iOS UI title/status to say `Legado Migration` until the real main shell is implemented. This prevents mistaking the shell for parity.

- [x] **Step 5: Verify**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon -q :shared:jvmTest
.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"
.\gradlew.bat --no-daemon -q :app:assembleDebug
```

Expected: all commands pass.

## Phase 1: Shared Product Models and Persistent Store

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/model/SharedLibraryModels.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/storage/SharedDataStore.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/storage/SharedLibraryStore.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/platform/Ports.kt`
- Create: `shared/src/commonTest/kotlin/io/legado/shared/storage/SharedDataStoreTest.kt`

- [x] **Step 1: Add shared data models for Android Room parity**

Add serializable models for book groups, bookmarks, replace rules, search keywords, cookies, RSS sources/articles/stars/read records, TXT TOC rules, read records, HTTP TTS, cache entries, rule subscriptions, dictionary rules, keyboard assists, and servers.

- [x] **Step 2: Add versioned shared data snapshot**

Create a `SharedDataSnapshot` model with schema version, timestamp, and lists for every persisted product data type.

- [x] **Step 3: Add storage port methods**

Extend the cache/storage port with key-value load/save/delete for larger JSON blobs so iOS can persist full library state without waiting for SQLDelight.

- [x] **Step 4: Write persistence tests**

Test saving/loading a snapshot containing at least one item of each model. Test unknown/missing fields by decoding a minimal JSON fixture.

- [x] **Step 5: Verify**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon -q :shared:jvmTest
```

Expected: PASS.

## Phase 2: Default Data Import

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/source/DefaultDataImporter.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/source/SourceJsonImporter.kt`
- Create: `shared/src/commonTest/kotlin/io/legado/shared/source/DefaultDataImporterTest.kt`
- Modify: `iosApp/LegadoIOS/DefaultSource.swift`
- Create: `iosApp/LegadoIOS/DefaultDataBundle.swift`

- [x] **Step 1: Add default data importer**

Support Android asset JSON shapes from:

```text
app/src/main/assets/defaultData/bookSources.json
app/src/main/assets/defaultData/rssSources.json
app/src/main/assets/defaultData/httpTTS.json
app/src/main/assets/defaultData/dictRules.json
app/src/main/assets/defaultData/txtTocRule.json
app/src/main/assets/defaultData/keyboardAssists.json
app/src/main/assets/defaultData/readConfig.json
app/src/main/assets/defaultData/themeConfig.json
app/src/main/assets/defaultData/coverRule.json
app/src/main/assets/defaultData/directLinkUpload.json
```

- [x] **Step 2: Add fixture tests copied from real asset structures**

Use small representative JSON fixtures in common tests, not network calls.

- [x] **Step 3: Wire iOS first launch import**

The iOS app should load bundled default data on first launch and then read from shared persistence.

- [x] **Step 4: Verify**

Run shared tests and iOS compile tasks:

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon -q :shared:jvmTest :shared:compileKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
```

Expected: PASS.

## Phase 3: Full Rule Runtime

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/SearchResultParser.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookInfoParser.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterListParser.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterContentParser.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/rule/AnalyzeRuleEngine.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/rule/RuleAnalyzer.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/rule/RuleScriptRuntime.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/rule/RuleWebViewRuntime.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/platform/Ports.kt`
- Create: `shared/src/iosMain/kotlin/io/legado/shared/platform/DarwinScriptRuntime.kt`
- Create: `shared/src/iosMain/kotlin/io/legado/shared/platform/DarwinWebViewRuntime.kt`
- Create: `app/src/main/java/io/legado/app/shared/AndroidScriptRuntime.kt`
- Create: `app/src/main/java/io/legado/app/shared/AndroidWebViewRuleRuntime.kt`

**Progress note 2026-07-08:** The first shared rule-runtime slice now covers JSON/basic CSS/HTML/regex/XPath-like extraction, rule chains, `||`/`&&`/`%%`, `@put`/`@get`, `@js`/`<js>` sequencing, Android Rhino, Android BackstageWebView, iOS JavaScriptCore, iOS WKWebView, Legado URL options, search-result URL normalization, and an async engine-backed search parser. Remaining parity work in this phase still includes the full JSoup index/filter grammar, deeper XPath parity, richer Java/JS bridge APIs, and engine-backed async parsers for detail/TOC/content.

- [ ] **Step 1: Port rule tokenizer and chaining**

Support Legado rule chains, intermediate result passing, put maps, list/string extraction, and URL normalization.

- [ ] **Step 2: Port selector families**

Support JSONPath-like extraction, CSS/JSoup-style selectors, XPath-like selectors, regex extraction, and replacement chains.

- [ ] **Step 3: Add script port**

Use Android Rhino through an adapter and iOS JavaScriptCore through an iOS adapter. Keep common code dependent only on the port.

- [ ] **Step 4: Add webJs port**

Use Android BackstageWebView adapter and iOS WKWebView adapter. Return structured errors for platform-impossible calls only.

- [ ] **Step 5: Verify with fixtures**

Create tests for search, detail, TOC, content, pagination, cookies, headers, JavaScript, and webJs fallback behavior.

## Phase 4: Bookshelf/Search/Detail/TOC/Content Product Core

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/book/BookshelfService.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/book/SearchCoordinator.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/book/BookDetailCoordinator.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/book/ChapterRepository.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/LegadoRuntime.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/LegadoSharedClient.kt`
- Create: `shared/src/commonTest/kotlin/io/legado/shared/book/BookshelfServiceTest.kt`
- Create: `shared/src/commonTest/kotlin/io/legado/shared/book/SearchCoordinatorTest.kt`

- [ ] **Step 1: Store and load bookshelf state**

Books, groups, progress, latest chapter, cover, origin, variable map, and update status must round-trip through shared storage.

- [ ] **Step 2: Implement concurrent source search**

Search across selected enabled sources, persist history, and expose partial results plus errors.

- [ ] **Step 3: Implement detail and TOC coordination**

Fetch detail, save book, fetch TOC, update chapters, and mark changed chapters.

- [ ] **Step 4: Implement content loading and cache**

Fetch current/previous/next chapters, apply replacements, save content cache, and expose reading progress.

## Phase 5: Replace iOS Test Shell with Real Main Shell

**Files:**
- Replace: `iosApp/LegadoIOS/ContentView.swift`
- Create: `iosApp/LegadoIOS/AppState.swift`
- Create: `iosApp/LegadoIOS/Views/MainTabView.swift`
- Create: `iosApp/LegadoIOS/Views/BookshelfView.swift`
- Create: `iosApp/LegadoIOS/Views/ExploreView.swift`
- Create: `iosApp/LegadoIOS/Views/RssHomeView.swift`
- Create: `iosApp/LegadoIOS/Views/SettingsHomeView.swift`
- Create: `iosApp/LegadoIOS/Views/SearchView.swift`
- Create: `iosApp/LegadoIOS/Views/BookDetailView.swift`
- Create: `iosApp/LegadoIOS/Views/ChapterListView.swift`
- Create: `iosApp/LegadoIOS/Views/ReaderView.swift`
- Modify: `iosApp/LegadoIOS.xcodeproj/project.pbxproj`

- [ ] **Step 1: Build app state wrapper**

Create a Swift observable app state that calls shared runtime and exposes loading/error/content states.

- [ ] **Step 2: Implement tab shell**

Bookshelf, Explore, RSS, and Settings tabs must be first-viewport app surfaces.

- [ ] **Step 3: Implement core reading workflow**

Search -> result -> detail -> TOC -> reader must work without editing JSON manually.

- [ ] **Step 4: Verify Xcode build**

Run on macOS CI through existing workflow and confirm unsigned IPA artifact is created.

## Phase 6: Source Management, Editor, and Debugger

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/source/SourceRepository.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/source/SourceDebugService.kt`
- Create: `iosApp/LegadoIOS/Views/Sources/SourceListView.swift`
- Create: `iosApp/LegadoIOS/Views/Sources/SourceEditorView.swift`
- Create: `iosApp/LegadoIOS/Views/Sources/SourceDebugView.swift`
- Modify: `iosApp/LegadoIOS.xcodeproj/project.pbxproj`

- [ ] **Step 1: Implement source CRUD**
- [ ] **Step 2: Implement import/export/share**
- [ ] **Step 3: Implement source editor sections**
- [ ] **Step 4: Implement step debug logs for search/detail/TOC/content**

## Phase 7: RSS

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/rss/RssService.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/rss/RssRuleParser.kt`
- Create: `iosApp/LegadoIOS/Views/RSS/RssSourceListView.swift`
- Create: `iosApp/LegadoIOS/Views/RSS/RssArticleListView.swift`
- Create: `iosApp/LegadoIOS/Views/RSS/RssReaderView.swift`

- [ ] **Step 1: Port RSS source storage and parser**
- [ ] **Step 2: Port article list/favorites/read records**
- [ ] **Step 3: Implement iOS RSS screens**

## Phase 8: Local and Remote Books

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/localbook/LocalBookService.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/remote/RemoteBookService.kt`
- Create: `iosApp/LegadoIOS/Platform/DocumentPicker.swift`
- Create: `iosApp/LegadoIOS/Views/Import/LocalImportView.swift`
- Create: `iosApp/LegadoIOS/Views/Import/RemoteImportView.swift`

- [ ] **Step 1: Port TXT parsing**
- [ ] **Step 2: Port or wrap EPUB parsing**
- [ ] **Step 3: Add PDF/image import path**
- [ ] **Step 4: Add WebDAV/remote book import**

## Phase 9: Replacements, Dictionary, TTS, Audio, Video, Manga

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/replace/ReplaceRuleService.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/dict/DictionaryService.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/tts/TtsService.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/media/MediaPlaybackService.kt`
- Create: `iosApp/LegadoIOS/Views/Rules/ReplaceRuleView.swift`
- Create: `iosApp/LegadoIOS/Views/Rules/DictionaryRuleView.swift`
- Create: `iosApp/LegadoIOS/Platform/IosTtsEngine.swift`
- Create: `iosApp/LegadoIOS/Platform/IosMediaPlayer.swift`

- [ ] **Step 1: Port replacement rules**
- [ ] **Step 2: Port dictionary lookup**
- [ ] **Step 3: Port native and HTTP TTS**
- [ ] **Step 4: Port audio/video playback wrappers**
- [ ] **Step 5: Port manga/image reader configuration**

## Phase 10: Backup, WebDAV, File Management, Settings, Associations

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/backup/BackupService.kt`
- Create: `shared/src/commonMain/kotlin/io/legado/shared/webdav/WebDavService.kt`
- Create: `iosApp/LegadoIOS/Views/Settings/SettingsView.swift`
- Create: `iosApp/LegadoIOS/Views/Settings/BackupView.swift`
- Create: `iosApp/LegadoIOS/Views/Files/FileManagerView.swift`
- Modify: `iosApp/LegadoIOS/LegadoIOSApp.swift`

- [ ] **Step 1: Port backup export/import format**
- [ ] **Step 2: Port WebDAV sync**
- [ ] **Step 3: Add file manager**
- [ ] **Step 4: Add URL/document import handlers**
- [ ] **Step 5: Add settings/about/log/crash-log screens**

## Phase 11: Final Parity Audit and IPA

**Files:**
- Create: `docs/parity/android-ios-parity-audit.md`
- Modify: `.github/workflows/ios-ci.yml`

- [ ] **Step 1: Audit manifest parity**

Every Android manifest activity/service/provider/receiver must map to an iOS workflow, platform equivalent, or explicit platform exception.

- [ ] **Step 2: Audit entity parity**

Every Room entity and default data file must have shared/iOS handling.

- [ ] **Step 3: Audit UI package parity**

Every Android `ui/*` package must have an iOS screen or platform-equivalent exception.

- [ ] **Step 4: Build final IPA**

Run GitHub Actions and verify the unsigned IPA artifact from CI.

- [ ] **Step 5: Completion gate**

Only after all audits are complete and the IPA opens into the full app can the migration be reported as complete.

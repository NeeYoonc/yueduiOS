# Android/iOS Parity Audit

Updated: 2026-07-09

This audit maps Android manifest surfaces, persisted entities, default data, and UI packages to the migrated shared runtime plus SwiftUI iOS workflows. CI packaging is handled by `.github/workflows/ios-ci.yml` and uploads the unsigned IPA artifact.

## Manifest surfaces

| Android surface | iOS/shared mapping |
| --- | --- |
| WelcomeActivity / Launcher1-6 | `LegadoIOSApp` -> `ContentView` -> `MainTabView`; no Android launcher aliases on iOS. |
| MainActivity | `MainTabView` with Bookshelf, Explore, RSS, Settings. |
| ReadBookActivity / TocActivity / SearchContentActivity | `ReaderView`, `ChapterListView`, reader search, bookmark/read-record state in shared runtime. |
| ReadMangaActivity | Local image/document import plus `DocumentPreviewView`; deep manga-specific tuning remains represented by shared book type/config fields. |
| BookInfoActivity / BookInfoEditActivity | `BookDetailView`, `BookEditView`. |
| AudioPlayActivity / VideoPlayerActivity | `SpeechController`, HTTP TTS playback, `MediaPlayerView` for RSS media. |
| QrCodeActivity | `QRImportView` plus universal import. |
| RuleSubActivity | `RuleSubListView`, `RuleSubFormView`, URL update/import. |
| BookSourceEditActivity / BookSourceActivity / BookSourceDebugActivity | `BookSourceFormView`, `SourceListView`, `SourceEditorView`, `SourceDebugView`, source login/cookie capture. |
| RssSourceEditActivity / RssSourceActivity / RssSourceDebugActivity | `RssSourceFormView`, `RssSourceListView`, `RssSourceEditorView`; RSS parsing/debug uses shared source rule runtime. |
| ReplaceEditActivity / ReplaceRuleActivity | `ReplaceRuleFormView`, `ReplaceRuleListView`, `ReplaceRuleEditorView`; runtime replacement service. |
| CodeEditActivity | JSON/source rule editors use SwiftUI forms and text editors. |
| ConfigActivity | Settings hub plus raw config, reader settings, WebDAV/server, backup, logs, about/privacy screens. |
| SearchActivity / ExploreShowActivity | `SearchView`, `ExploreView`, shared search/explore coordinators. |
| AboutActivity / ReadRecordActivity | `AboutView`, `ReadRecordListView`, `LogView`. |
| BookshelfManageActivity | `BookshelfView`, `BookGroupListView`, delete/update/group controls. |
| RssSortActivity / ReadRssActivity / RssFavoritesActivity | `RssArticleListView`, `RssReaderView`, `RssStarListView`. |
| ImportBookActivity / RemoteBookActivity | Bookshelf file importer, local TXT/document import, WebDAV server/backup path. |
| CacheActivity | Shared chapter/cache storage plus `CacheEntryListView`. |
| WebViewActivity / SourceLoginActivity | `SourceLoginView` with `WKWebView` cookie capture. |
| DictRuleActivity | `DictRuleListView`, `DictRuleFormView`, `DictionaryLookupView`. |
| FileManageActivity / HandleFileActivity | `FileManagerView`, `DocumentPreviewView`, file import/export flows. |
| SharedReceiverActivity / association activities | Universal import, QR import, backup/document importers; Android broadcast-only surfaces map to explicit iOS user actions. |
| CheckSourceService / CacheBookService / ExportBookService / DownloadService | Shared source debug/update/cache/export services triggered from SwiftUI. |
| WebService / WebTileService | iOS sandbox-friendly file manager/export/import instead of always-on Android local web services. |
| TTSReadAloudService / HttpReadAloudService | `SpeechController` native TTS and HTTP TTS playback. |
| AudioPlayService / VideoPlayService / MediaButtonReceiver | `MediaPlayerView`/`AVPlayer`; Android notification/media-button service lifecycle maps to iOS player controls. |
| ReaderProvider / FileProvider | iOS file importer/exporter/QuickLook equivalents. |

## Persisted data/entity parity

Shared snapshot/import/export covers: book sources, books, book groups, chapters, chapter content, bookmarks, replace rules, search books, search keywords, cookies, RSS sources/articles/read records/stars, TXT TOC rules, read records, HTTP TTS, cache entries, rule subscriptions, dictionary rules, keyboard assists, servers, reader preferences, and raw configs.

## Default data parity

Bundled Android-style default data import supports book sources, RSS sources, HTTP TTS, dictionary rules, TXT TOC rules, keyboard assists, read config, theme config, cover rule, and direct-link upload/raw config files.

## iOS verification gate

For each migration slice run:

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon --max-workers=1 -q :shared:jvmTest :shared:compileKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :app:testAppDebugUnitTest --tests "io.legado.app.shared.*" :app:assembleDebug
git diff --check
```

Then push to `main` and confirm the latest GitHub Actions `iOS CI` run succeeds and uploads `LegadoIOS-unsigned-ipa`.

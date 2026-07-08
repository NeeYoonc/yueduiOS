# Legado Full iOS Migration Design

## Context

The current repository has two very different states:

- The Android app is the real product. It has more than 50 manifest activities, 10 background services, a Room database at version 89, 21 Room entities plus one database view, 17 UI package groups, local book parsers, RSS, TTS, media playback, WebDAV/backup, source editing/debugging, a web management server, default rule data, and a large Android-bound rule runtime.
- The current iOS app is a prototype shell. It imports JSON, searches through one selected source, opens the first result, and displays one chapter content string. It proves build plumbing, not product parity.

This design replaces the earlier prototype scope. Anything previously listed as a prototype non-goal is now part of the full migration backlog unless iOS platform policy makes it impossible.

## Goal

Deliver an iOS app that is functionally equivalent to the source Android Legado app for normal user workflows and produces an unsigned IPA from CI.

The migration is complete only when:

- The iOS app launches into a real app, not a source JSON test shell.
- Core Android user workflows have iOS equivalents.
- The shared runtime can run real Legado source rules, including JavaScript and WebView-backed rules where the Android app supports them.
- Existing default data can be loaded on iOS.
- User data is stored persistently on iOS and can be imported/exported.
- CI builds the shared framework and unsigned iOS IPA.

## Product Parity Matrix

### Main Navigation

Android source packages:

- `ui/main`
- `ui/main/bookshelf`
- `ui/main/explore`
- `ui/main/rss`
- `ui/main/my`

iOS scope:

- Tab based main shell: Bookshelf, Explore, RSS, Settings/Profile.
- Startup privacy/update/local-password/backup-sync flow translated to iOS-safe equivalents.
- Badge/update indicators for pending book updates.

### Bookshelf and Book Management

Android source packages:

- `ui/book/manage`
- `ui/book/group`
- `ui/book/info`
- `ui/book/info/edit`
- `ui/book/bookmark`
- `ui/book/cache`
- `ui/book/changesource`
- `ui/book/changecover`

iOS scope:

- Bookshelf grid/list styles.
- Book groups and group management.
- Book detail, edit metadata, change cover.
- Bookmark list and per-book bookmarks.
- Source switching and chapter source switching.
- Offline cache management.
- Book update/refresh state.

### Reading

Android source packages:

- `ui/book/read`
- `ui/book/read/config`
- `ui/book/read/page`
- `ui/book/toc`
- `ui/book/toc/rule`
- `model/ReadBook.kt`

iOS scope:

- Text reader with chapter paging, scroll mode, progress persistence, previous/current/next chapter preloading, and jump history.
- Reader menu, table of contents, bookmark panel, search-in-book, content edit, effective replacements, and style settings.
- Reading themes, background images/colors, text layout, margins, font selection, page animation where feasible.
- Simulated chapter reading and local/remote book differences.

### Manga/Image/PDF Reading

Android source packages:

- `ui/book/manga`
- `ui/book/manga/config`
- `model/ReadManga.kt`
- local parsers for image/PDF capable books.

iOS scope:

- Image chapter rendering with single/full page styles.
- Manga footer/color/filter/e-ink style equivalents where feasible.
- PDF/image local book viewing using iOS native document/image surfaces.

### Book Sources

Android source packages:

- `ui/book/source/manage`
- `ui/book/source/edit`
- `ui/book/source/debug`
- `model/webBook`
- `model/analyzeRule`
- `help/source`

iOS scope:

- Source list, grouping, enable/disable, import/export, sorting, duplicate handling.
- Source editor with all rule sections and source validation.
- Source debugger for search, detail, TOC, and content steps.
- Online import links and QR/import flows.
- Rule subscription update flow.

### Rule Runtime

Android source packages:

- `model/analyzeRule`
- `modules/rhino`
- `help/JsExtensions.kt`
- `help/RegexJsExtensions.kt`
- `help/http/BackstageWebView.kt`
- `help/webView`

iOS scope:

- Shared URL/request parser with GET/POST/header/charset/body/page/key/variable handling.
- Shared rule analyzer for JSONPath, CSS/JSoup-like selectors, XPath, regex, rule chaining, replacement, and source variables.
- Shared JavaScript runtime abstraction backed by Rhino on Android and JavaScriptCore on iOS.
- WKWebView-backed `webJs` runtime on iOS matching Android BackstageWebView behavior as closely as possible.
- Cookie, cache, redirect, UA, and header behavior equivalent enough for real sources.
- Explicit unsupported-surface errors only for platform-impossible Android internals, not for normal Legado rules.

### Search and Discovery

Android source packages:

- `ui/book/search`
- `ui/book/explore`
- `ui/main/explore`
- `data/entities/SearchBook.kt`
- `data/entities/SearchKeyword.kt`

iOS scope:

- Search history, scope/source picker, concurrent source search, result list, add to bookshelf.
- Explore pages from source explore rules.
- Last search/result metadata persistence.

### RSS

Android source packages:

- `ui/rss`
- `ui/main/rss`
- `model/rss`
- RSS entities and DAOs.

iOS scope:

- RSS source management, source editor/debugger, article list, favorites, sort/read-record views.
- RSS article reader and read progress.
- RSS rule subscription/import/export.

### Local Books and Remote Books

Android source packages:

- `ui/book/import/local`
- `ui/book/import/remote`
- `model/localBook`
- `model/remote`
- `modules/book`

iOS scope:

- Local import via iOS document picker.
- TXT, EPUB, MOBI, UMD, PDF support where parsers can be ported or replaced with iOS-safe equivalents.
- Remote/WebDAV book import and server configuration.
- File association/import equivalents using iOS document/open-in behavior.

### Replace Rules, Dictionary, TTS, Keyboard Assist

Android source packages:

- `ui/replace`
- `ui/dict`
- `ui/book/read/config`
- `data/entities/ReplaceRule.kt`
- `data/entities/DictRule.kt`
- `data/entities/HttpTTS.kt`
- `data/entities/KeyboardAssist.kt`

iOS scope:

- Replace rule list/edit/import/export/grouping and runtime application.
- Dictionary rule list/edit/import/export and lookup UI.
- Native TTS plus HTTP TTS rules, read-aloud controls, skip credits, voice/speech engine selection equivalents.
- Keyboard assist rules where applicable to iOS UI.

### Media Playback

Android source packages:

- `ui/book/audio`
- `ui/video`
- `service/AudioPlayService.kt`
- `service/VideoPlayService.kt`
- `model/AudioPlay.kt`
- `model/VideoPlay.kt`

iOS scope:

- Audio chapter playback and remote-control integration.
- Video playback for source-provided video chapters.
- Playback state persistence where the Android app persists it.

### Backup, WebDAV, Web Server, File Management

Android source packages:

- `help/storage`
- `help/AppWebDav.kt`
- `service/WebService.kt`
- `ui/file`
- `modules/web`

iOS scope:

- Backup and restore.
- WebDAV sync.
- File manager for app documents.
- Local web server or iOS-safe equivalent for upload/management if feasible under iOS background/network rules.
- Existing web assets are migration inputs, not a substitute for the native app unless used deliberately inside a feature.

### System Integrations

Android source packages:

- `ui/association`
- `ui/qrcode`
- `ui/login`
- `ui/browser`
- `receiver`
- `api/ReaderProvider`

iOS scope:

- URL scheme/universal-link handling for source/rule/book imports.
- Document import/open-in handlers.
- QR scan/import.
- Source login through WKWebView and cookie handoff.
- Browser/manual verification surfaces.
- Process-text and Android content provider features get closest iOS equivalents where platform APIs allow.

### Settings, Theme, About

Android source packages:

- `ui/config`
- `ui/about`
- `help/config`
- assets default config JSON.

iOS scope:

- App, reading, theme, cover, backup, source-check, welcome/startup settings.
- About, logs/crash logs, update notes, privacy/license/disclaimer pages.
- Default theme/read/source data import from `app/src/main/assets/defaultData`.

## Data Architecture

Full migration cannot keep Room-only persistence. The shared module becomes the owner of product data contracts.

Planned shared persistence:

- SQLDelight or another KMP SQLite layer for shared database tables.
- Schema mirrors Android Room entities closely enough to import/export Android data:
  - books
  - book groups
  - book sources and source parts
  - chapters
  - bookmarks
  - replace rules
  - search books and keywords
  - cookies
  - RSS sources/articles/stars/read records
  - TXT TOC rules
  - read records
  - HTTP TTS
  - cache entries
  - rule subscriptions
  - dictionary rules
  - keyboard assists
  - servers
- Migration/import layer reads Android-style JSON/backup/database exports and writes shared storage.
- Android keeps thin adapters until it can also consume shared storage safely.

## Runtime Architecture

### shared commonMain

Owns:

- Data models and database interfaces.
- Rule parsing and execution pipeline.
- Search/detail/TOC/content/RSS/local-book/read-aloud business services.
- Import/export and backup data transforms.
- Source debug events.
- Platform ports for HTTP, cookies, cache, JS, WebView JS, file IO, media, TTS, QR, clipboard, share/open-in, logging, clock, and permissions.

Must not depend on Android SDK, Room, Parcelable, OkHttp, Rhino, WebView, Glide, ExoPlayer, or Android resources.

### androidMain and Android app

Owns:

- Compatibility adapters from existing Android entities/services to shared contracts.
- Android platform port implementations.
- Existing Android UI remains buildable during migration.

### iosMain and Swift

Owns:

- Darwin HTTP, cookie/cache/file implementations.
- JavaScriptCore and WKWebView-backed rule execution.
- iOS file/document handling.
- Swift-facing runtime facades.

### iosApp

Owns:

- SwiftUI native product UI.
- Navigation and presentation.
- iOS-specific permission prompts and platform affordances.
- Calling shared runtime and observing shared state.

## Migration Strategy

This is not a single patch. It must be delivered as verified vertical slices, each ending in green tests and a buildable IPA:

1. Replace prototype framing with full migration docs and CI labels.
2. Build shared persistence and import default data.
3. Port full source/rule runtime.
4. Port bookshelf/search/book detail/TOC/content flows.
5. Build production iOS navigation and reader UI.
6. Port source management/editor/debugger.
7. Port RSS.
8. Port local/remote books and file handling.
9. Port replacements, dictionary, TTS, read-aloud.
10. Port media playback and manga/PDF/image reading.
11. Port backup/WebDAV/web upload/file manager.
12. Port settings/about/logs/import associations.
13. Run parity audit against Android manifest, Room schema, assets, and UI packages.

## Testing and Verification

Required before claiming completion:

- `:shared:allTests`
- `:shared:assemble`
- Android shared adapter tests.
- Android debug build remains green.
- iOS KMP target compile for x64, arm64, and simulator arm64.
- Xcode simulator build.
- Unsigned device IPA build and artifact upload.
- Fixture tests using real default source data where legal and deterministic.
- Rule runtime tests for JSONPath, CSS, XPath, regex, JS, WebView JS, cookies, headers, redirects, pagination, TOC pagination, and content pagination.
- Data tests for import/export, backup restore, and persistence.
- iOS UI smoke tests for core workflows on simulator.

## Acceptance Criteria

Full migration is complete only when this checklist is true:

- No iOS screen remains a prototype-only test shell.
- Every Android manifest activity has an iOS workflow or a documented iOS platform-equivalent exception.
- Every Room entity has a shared/iOS storage representation or a documented iOS platform-equivalent exception.
- Every default data file used by Android has an iOS import/use path.
- Book source search, detail, TOC, content, explore, source edit, and source debug work with real source rules.
- Bookshelf, reading, RSS, local import, replacements, dictionary, TTS, settings, backup/import/export, and source login are implemented.
- CI produces an unsigned IPA.
- The IPA can be installed after user signing and opens into the full app.


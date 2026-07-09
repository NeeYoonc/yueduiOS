# Legado iOS Migration

This iOS target is the SwiftUI front end for the Android-to-iOS migration. It is backed by the shared KMP runtime and is built by CI into an unsigned IPA artifact.

Current state:

- The app imports the `LegadoShared` KMP framework and persists data through the shared snapshot store.
- Bookshelf, search, explore, detail, TOC, reader, reader settings/search/bookmarks/read records, book groups, source switching, and refresh/update checks are available.
- Book source, RSS source, replace rule, dictionary rule, HTTP TTS, TXT TOC, server/WebDAV, keyboard assist, rule subscription, raw config, cookie, and cache management all have import/export/edit flows.
- Universal import, URL import, QR import, backup files, WebDAV backup, local TXT/document import, file manager, RSS reader/star/read records, RSS media playback, source login cookie capture, logs, and about/privacy screens are available.
- The fake JSON API template has been removed from the default product path; first launch imports bundled default data where present.

Full migration scope is tracked in:

- `../docs/superpowers/specs/2026-07-08-legado-full-ios-migration-design.md`
- `../docs/superpowers/plans/2026-07-08-legado-full-ios-migration.md`

On macOS with Xcode installed, open `LegadoIOS.xcodeproj`. The Xcode target runs `scripts/build-shared-xcframework.sh` before compiling the app, which builds:

`../shared/build/XCFrameworks/debug/LegadoShared.xcframework`

Windows validation can compile the KMP iOS klibs, but Xcode app build and simulator launch require macOS.

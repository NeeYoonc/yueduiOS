# Legado iOS Migration

This iOS target is under a full Android-to-iOS migration. It currently proves the KMP/Xcode build path and will be replaced by a full SwiftUI app that matches the Android product workflows.

Current state:

- The app imports the `LegadoShared` KMP framework.
- The shared runtime has early source import/search/detail/TOC/content services.
- The iOS UI is not product-complete and must not be treated as a usable reader.
- The fake JSON API template has been removed from the default product path.

Full migration scope is tracked in:

- `../docs/superpowers/specs/2026-07-08-legado-full-ios-migration-design.md`
- `../docs/superpowers/plans/2026-07-08-legado-full-ios-migration.md`

On macOS with Xcode installed, open `LegadoIOS.xcodeproj`. The Xcode target runs `scripts/build-shared-xcframework.sh` before compiling the app, which builds:

`../shared/build/XCFrameworks/debug/LegadoShared.xcframework`

Windows validation can compile the KMP iOS klibs, but Xcode app build and simulator launch require macOS.

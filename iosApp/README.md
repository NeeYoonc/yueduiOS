# Legado iOS

This SwiftUI shell imports the `LegadoShared` KMP framework and uses `DarwinLegadoRuntime` for default iOS HTTP and local cache ports.

On macOS with Xcode installed, open `LegadoIOS.xcodeproj`. The Xcode target runs `scripts/build-shared-xcframework.sh` before compiling the app, which builds:

`../shared/build/XCFrameworks/debug/LegadoShared.xcframework`

Windows validation can compile the KMP iOS klibs, but Xcode app build and simulator launch require macOS.

# Legado iOS App Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an iOS SwiftUI app shell that integrates the generated `LegadoShared` framework.

**Architecture:** Create `iosApp/LegadoIOS.xcodeproj` plus SwiftUI app sources. The app imports `LegadoShared`, uses `DarwinLegadoRuntime`, lets a user import a source JSON, search, open the first result, and display fetched chapter content. A build script runs the Gradle XCFramework task on macOS/Xcode.

**Tech Stack:** SwiftUI, Xcode project file, Kotlin Multiplatform XCFramework.

---

### Task 1: Static Red-Green

**Files:**
- Create: `iosApp/LegadoIOS.xcodeproj/project.pbxproj`
- Create: `iosApp/LegadoIOS/LegadoIOSApp.swift`
- Create: `iosApp/LegadoIOS/ContentView.swift`
- Create: `iosApp/LegadoIOS/LegadoViewModel.swift`
- Create: `iosApp/LegadoIOS/DefaultSource.swift`
- Create: `iosApp/scripts/build-shared-xcframework.sh`
- Create: `iosApp/README.md`

- [x] **Step 1: Run failing static check**

Check that the expected Xcode project and Swift files exist and reference `LegadoShared` / `DarwinLegadoRuntime`.

- [x] **Step 2: Verify red**

Expected: files are missing.

- [x] **Step 3: Create app shell**

Add the project, SwiftUI sources, and build script.

- [x] **Step 4: Verify green**

Run the same static check.

### Task 2: Regression Verification

**Files:**
- No additional files.

- [x] **Step 1: Run iOS KMP compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64`

- [x] **Step 2: Run shared and Android checks**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

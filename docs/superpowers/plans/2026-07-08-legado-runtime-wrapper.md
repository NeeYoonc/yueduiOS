# Legado Runtime Wrapper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a high-level runtime object that bundles shared client and storage for iOS app code.

**Architecture:** Add common `LegadoRuntime` with injected `HttpFetcher` and `CacheStorePort`, then add iOS `DarwinLegadoRuntime` with default `DarwinHttpFetcher` and `DarwinUserDefaultsCacheStore`. Runtime methods import/save sources, load stored sources/books, open the first search result, and cache returned chapter content.

**Tech Stack:** Kotlin Multiplatform, kotlinx.coroutines, Kotlin/Native.

---

### Task 1: Common Runtime

**Files:**
- Create: `shared/src/commonTest/kotlin/io/legado/shared/LegadoRuntimeTest.kt`
- Create after red: `shared/src/commonMain/kotlin/io/legado/shared/LegadoRuntime.kt`

- [x] **Step 1: Write the failing test**

Test `importAndSaveBookSources`, `loadBookSources`, `openFirstSearchResult`, and content cache population.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.LegadoRuntimeTest"`

Expected: compile fails because `LegadoRuntime` does not exist.

- [x] **Step 3: Implement runtime**

Compose `LegadoSharedClient` and `SharedLibraryStore`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.LegadoRuntimeTest"`

### Task 2: iOS Runtime

**Files:**
- Create: `shared/src/iosTest/kotlin/io/legado/shared/DarwinLegadoRuntimeCompileTest.kt`
- Create after red: `shared/src/iosMain/kotlin/io/legado/shared/DarwinLegadoRuntime.kt`

- [x] **Step 1: Write failing compile test**

Assign `DarwinLegadoRuntime()` to `LegadoRuntime`.

- [x] **Step 2: Run iOS test compile to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:compileTestKotlinIosX64`

- [x] **Step 3: Implement iOS runtime**

Subclass `LegadoRuntime` with default Darwin platform ports.

- [x] **Step 4: Run iOS compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64`

### Task 3: Regression Verification

**Files:**
- No additional files.

- [x] **Step 1: Run shared JVM tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 2: Run Android shared tests and debug build**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

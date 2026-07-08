# Legado iOS Storage Ports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add minimal shared persistence services and an iOS `NSUserDefaults` cache port.

**Architecture:** Store book sources and bookshelf entries as JSON through the existing `CacheStorePort`. Keep this storage layer small and replaceable so iOS can start without a database while Android can still use its existing Room-backed entities.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Kotlin/Native Foundation interop.

---

### Task 1: Common Store

**Files:**
- Create: `shared/src/commonTest/kotlin/io/legado/shared/storage/SharedLibraryStoreTest.kt`
- Create after red: `shared/src/commonMain/kotlin/io/legado/shared/storage/SharedLibraryStore.kt`

- [x] **Step 1: Write the failing test**

Add a fake `CacheStorePort`, save/load `SharedBookSource` and `SharedBook` lists, and assert blank/missing storage returns empty lists.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.storage.SharedLibraryStoreTest"`

Expected: compile fails because `SharedLibraryStore` does not exist.

- [x] **Step 3: Implement store**

Use kotlinx.serialization JSON with `ignoreUnknownKeys`, `isLenient`, and `explicitNulls = false`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.storage.SharedLibraryStoreTest"`

### Task 2: iOS Cache Port

**Files:**
- Create: `shared/src/iosTest/kotlin/io/legado/shared/platform/DarwinUserDefaultsCacheStoreCompileTest.kt`
- Create after red: `shared/src/iosMain/kotlin/io/legado/shared/platform/DarwinUserDefaultsCacheStore.kt`

- [x] **Step 1: Write the failing compile test**

Add an iOS test that assigns `DarwinUserDefaultsCacheStore()` to `CacheStorePort`.

- [x] **Step 2: Run iOS test compile to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:compileTestKotlinIosX64`

Expected: compile fails because `DarwinUserDefaultsCacheStore` does not exist.

- [x] **Step 3: Implement cache port**

Bridge `getText`/`putText` to `NSUserDefaults.stringForKey` and `setObject`.

- [x] **Step 4: Run iOS compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64`

### Task 3: Regression Verification

**Files:**
- No additional files.

- [x] **Step 1: Run shared JVM tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 2: Run Android shared tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

- [x] **Step 3: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

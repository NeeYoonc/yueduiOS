# Legado iOS HTTP Fetcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide an iOS `HttpFetcher` implementation backed by Foundation `NSURLSession`.

**Architecture:** Add `DarwinHttpFetcher` under `iosMain` so Swift users can create `LegadoSharedClient(DarwinHttpFetcher())` without writing their own network bridge. Keep the implementation limited to shared request/response fields: method, headers, text body, timeout, status, final URL, and response headers.

**Tech Stack:** Kotlin Multiplatform, Kotlin/Native Foundation interop, kotlinx.coroutines.

---

### Task 1: iOS Fetcher Compile Test

**Files:**
- Create: `shared/src/iosTest/kotlin/io/legado/shared/platform/DarwinHttpFetcherCompileTest.kt`
- Create after red: `shared/src/iosMain/kotlin/io/legado/shared/platform/DarwinHttpFetcher.kt`

- [x] **Step 1: Write the failing test**

Add an iOS test that assigns `DarwinHttpFetcher()` to `HttpFetcher`.

- [x] **Step 2: Run test compile to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:compileTestKotlinIosX64`

Expected: compile fails because `DarwinHttpFetcher` does not exist.

- [x] **Step 3: Implement the fetcher**

Use `NSMutableURLRequest` and `NSURLSession.sharedSession.dataTaskWithRequest`, bridge request body to `NSData`, and decode response `NSData` as UTF-8 text.

- [x] **Step 4: Run iOS compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileIosMainKotlinMetadata`

Expected: iOS main/test/metadata compile.

### Task 2: Regression Verification

**Files:**
- No additional files.

- [x] **Step 1: Run shared JVM tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 2: Run Android shared tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

- [x] **Step 3: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

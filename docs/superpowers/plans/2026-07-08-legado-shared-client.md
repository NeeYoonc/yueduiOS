# Legado Shared Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a single Swift-friendly shared client entry point for iOS integration.

**Architecture:** Add `LegadoSharedClient` in `commonMain` as a thin facade over the existing source importer, search, book info, TOC, content, and reading-flow services. Platform apps provide an `HttpFetcher`; iOS can call this facade from the generated `LegadoShared` framework.

**Tech Stack:** Kotlin Multiplatform, kotlinx.coroutines suspend APIs, Gradle JVM tests.

---

### Task 1: Client Facade

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/LegadoSharedClient.kt`
- Create: `shared/src/commonTest/kotlin/io/legado/shared/LegadoSharedClientTest.kt`

- [x] **Step 1: Write the failing test**

Add a common test that constructs `LegadoSharedClient` with a fake `HttpFetcher`, imports source JSON, then calls `openFirstSearchResult`.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.LegadoSharedClientTest"`

Expected: compile fails because `LegadoSharedClient` does not exist.

- [x] **Step 3: Implement the facade**

Expose `importBookSources`, `search`, `getBookInfo`, `getChapterList`, `getContent`, and `openFirstSearchResult`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.LegadoSharedClientTest"`

Expected: client test passes.

### Task 2: Verification

**Files:**
- No additional files.

- [x] **Step 1: Run shared tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 2: Run Android shared tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

- [x] **Step 3: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

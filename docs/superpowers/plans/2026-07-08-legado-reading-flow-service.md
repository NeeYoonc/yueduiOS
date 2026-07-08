# Reading Flow Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide one shared orchestration entrypoint for the current search, toc, and content services so iOS can call a single KMP facade for the first readable flow.

**Architecture:** Add `ReadingFlowService` in `:shared` that composes `BookSearchService`, `BookTocService`, and `BookContentService`. Add `openFirstSearchResult()` to run search, convert the first search book to `SharedBook`, parse its chapter list, and load the first chapter content. Add an Android facade that maps Android entities to shared models and delegates to the shared flow.

**Tech Stack:** Kotlin Multiplatform `:shared`, Kotlin common tests, Android unit tests, existing `HttpFetcher` port.

---

### Task 1: Shared Reading Flow

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/ReadingFlowService.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/ReadingFlowServiceTest.kt`

- [ ] **Step 1: Write the failing shared test**

Create a fake `HttpFetcher` that returns three bodies by URL: a line-based search result, a regex toc body, and a regex content body. Call `ReadingFlowService.openFirstSearchResult(source, "metal max", page = 2)` and assert request order, selected book, chapter, and content.

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.ReadingFlowServiceTest"
```

Expected: FAIL because `ReadingFlowService` does not exist.

- [ ] **Step 3: Implement minimal shared flow**

Add `ReadingFlowService` with injectable parsers and `ReadingFlowResult`. Do not add detail-page parsing, HTML cleanup beyond existing content parser, JS, caching, database writes, or pagination loops.

- [ ] **Step 4: Run test to verify it passes**

Run the same shared test command and expect PASS.

### Task 2: Android Reading Flow Facade

**Files:**
- Create: `app/src/main/java/io/legado/app/shared/AndroidSharedReadingFlow.kt`
- Test: `app/src/test/java/io/legado/app/shared/AndroidSharedReadingFlowTest.kt`

- [ ] **Step 1: Write the failing Android test**

Use an `OkHttpClient` interceptor to return search, toc, and content responses by request URL. Call `AndroidSharedReadingFlow.openFirstSearchResult(source, "metal max", page = 2)` and assert the shared result content.

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedReadingFlowTest"
```

Expected: FAIL because `AndroidSharedReadingFlow` does not exist.

- [ ] **Step 3: Implement Android facade**

Add `AndroidSharedReadingFlow` with `AndroidHttpFetcher` default and parser injection matching the shared flow constructor.

- [ ] **Step 4: Run test to verify it passes**

Run the same Android test command and expect PASS.

### Task 3: Final Verification

- [ ] Run `:shared:jvmTest`.
- [ ] Run `:app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`.
- [ ] Run `:app:assembleDebug`.
- [ ] Check whether this directory is a Git repo; if not, report commit skipped.

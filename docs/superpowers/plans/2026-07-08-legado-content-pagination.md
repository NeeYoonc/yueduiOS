# Content Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let shared content loading follow `nextContentUrl` links and merge multi-page chapter content for iOS and Android shared facades.

**Architecture:** Keep `ChapterContentParser` responsible only for parsing one page. Extend `BookContentService` to fetch the first chapter page, normalize its `nextContentUrls`, fetch unseen next pages in order, parse each page, append nonblank content with newlines, and avoid repeated URLs. Keep `ChapterContentResult.response` as the first response for backward compatibility.

**Tech Stack:** Kotlin Multiplatform `:shared`, Kotlin common tests, existing Android shared unit tests.

---

### Task 1: Shared Content Pagination

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookContentService.kt`
- Modify test: `shared/src/commonTest/kotlin/io/legado/shared/service/BookContentServiceTest.kt`

- [ ] **Step 1: Write failing test**

Add a test where page 1 returns `page-2.html`, page 2 returns `page-3.html`, and page 3 has no next URL. Assert request order and merged content.

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookContentServiceTest"
```

Expected: FAIL because only the first page is fetched.

- [ ] **Step 3: Implement service loop**

Use a visited URL set and queue of normalized next URLs. Append each parsed page content if nonblank. Keep a small default page cap to avoid unbounded loops.

- [ ] **Step 4: Run test to verify pass**

Run the same shared test command and expect PASS.

### Task 2: Android Facade Coverage

**Files:**
- Modify: `app/src/test/java/io/legado/app/shared/AndroidSharedBookContentTest.kt`

- [ ] Add a second-page response in the Android shared content test and assert merged content.

### Task 3: Final Verification

- [ ] Run `:shared:jvmTest`.
- [ ] Run `:app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`.
- [ ] Run `:app:assembleDebug`.
- [ ] Check whether this directory is a Git repo; if not, report commit skipped.

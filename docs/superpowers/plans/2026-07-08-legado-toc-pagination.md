# Toc Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let shared chapter-list loading follow `nextTocUrl` links and merge multi-page toc results for iOS and Android shared facades.

**Architecture:** Extend `ChapterListParser` with a default `parseNextUrls()` method so existing parsers remain source-compatible. Implement `RegexChapterListParser.parseNextUrls()` from `SharedTocRule.nextTocUrl`. Extend `BookTocService` to fetch the first toc page, normalize discovered next URLs, fetch unseen pages in order, merge chapters, re-index them, and cap pagination to avoid loops.

**Tech Stack:** Kotlin Multiplatform `:shared`, Kotlin common tests, Android unit tests, existing `HttpFetcher` port.

---

### Task 1: Shared Toc Pagination

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterListParser.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookTocService.kt`
- Modify test: `shared/src/commonTest/kotlin/io/legado/shared/service/BookTocServiceTest.kt`

- [ ] **Step 1: Write failing test**

Add a shared test where page 1 returns one chapter and `next=page-2.html`, page 2 returns one chapter and `next=page-3.html`, and page 3 returns one chapter. Assert request order and merged chapter indexes.

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookTocServiceTest"
```

Expected: FAIL because only the first toc page is fetched.

- [ ] **Step 3: Implement parser and service loop**

Add default `parseNextUrls()`, implement regex next-url parsing, then add a visited/pending loop in `BookTocService`.

### Task 2: Android Facade Coverage

**Files:**
- Modify: `app/src/test/java/io/legado/app/shared/AndroidSharedBookTocTest.kt`

- [ ] Add a second toc page response in the Android shared toc test and assert merged chapters.

### Task 3: Final Verification

- [ ] Run `:shared:jvmTest`.
- [ ] Run `:app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`.
- [ ] Run `:app:assembleDebug`.
- [ ] Check whether the directory is a Git repo; if not, report commit skipped.

# Book Info Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared detail-page parser/service so the cross-platform reading flow can resolve `tocUrl` from a search result before fetching chapters.

**Architecture:** Add `BookInfoParser` and `RegexBookInfoParser` in `:shared` to parse `SharedBookInfoRule` fields from a detail page body. Add `BookInfoService` to fetch `book.bookUrl`, parse detail fields, and return an updated `SharedBook`. Update `ReadingFlowService.openFirstSearchResult()` to fetch book info before toc, while preserving fallback behavior when no detail rule or no `tocUrl` is found. Add Android thin facades using existing mappers and `AndroidHttpFetcher`.

**Tech Stack:** Kotlin Multiplatform `:shared`, Kotlin common tests, Android unit tests, existing `HttpFetcher` port.

---

### Task 1: Shared Book Info Parser

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/BookInfoParser.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexBookInfoParserTest.kt`

- [ ] **Step 1: Write failing parser test**

Create a `SharedBookSource.ruleBookInfo` with regex rules for `name`, `author`, `kind`, `lastChapter`, `intro`, `coverUrl`, and `tocUrl`. Parse a detail HTML body and assert an updated `SharedBook` that preserves existing fields when parser fields are blank.

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.RegexBookInfoParserTest"
```

Expected: FAIL because `RegexBookInfoParser` does not exist.

- [ ] **Step 3: Implement minimal parser**

Add `BookInfoParser` and `RegexBookInfoParser`. Use regex first capture group when present, otherwise the full match. Apply only nonblank parsed fields over the input book.

- [ ] **Step 4: Run parser test to verify pass**

Run the same parser test command and expect PASS.

### Task 2: Shared Book Info Service

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/BookInfoService.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/BookInfoServiceTest.kt`

- [ ] **Step 1: Write failing service test**

Fake `HttpFetcher` should assert the request URL is `book.bookUrl`, return a detail page body, and assert `BookInfoService.getBookInfo()` returns parser output and response metadata.

- [ ] **Step 2: Run service test to verify failure**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookInfoServiceTest"
```

Expected: FAIL because `BookInfoService` does not exist.

- [ ] **Step 3: Implement service**

Add `BookInfoService(httpFetcher, bookInfoParser)` and `BookInfoResult`.

- [ ] **Step 4: Run service test to verify pass**

Run the same service test command and expect PASS.

### Task 3: Flow Integration

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ReadingFlowService.kt`
- Modify tests: `shared/src/commonTest/kotlin/io/legado/shared/service/ReadingFlowServiceTest.kt`

- [ ] **Step 1: Update flow test to expect detail request**

Change the test request order to search -> detail -> toc -> content. The detail response should provide `tocUrl`, and toc should be fetched from that URL.

- [ ] **Step 2: Run flow test to verify failure**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.ReadingFlowServiceTest"
```

Expected: FAIL because the current flow skips detail parsing.

- [ ] **Step 3: Insert book info service in flow**

Update constructor injection and `ReadingFlowResult` to include `bookInfo`. Use parsed book for toc/content.

- [ ] **Step 4: Run flow test to verify pass**

Run the same flow test command and expect PASS.

### Task 4: Android Facade

**Files:**
- Create: `app/src/main/java/io/legado/app/shared/AndroidSharedBookInfo.kt`
- Test: `app/src/test/java/io/legado/app/shared/AndroidSharedBookInfoTest.kt`
- Modify: `app/src/main/java/io/legado/app/shared/AndroidSharedReadingFlow.kt`
- Modify: `app/src/test/java/io/legado/app/shared/AndroidSharedReadingFlowTest.kt`

- [ ] **Step 1: Write failing Android book info test**

Use `OkHttpClient` interceptor and Android `BookSource.ruleBookInfo` to assert detail parsing through shared.

- [ ] **Step 2: Implement Android book info facade**

Add `AndroidSharedBookInfo` delegating through Android mappers.

- [ ] **Step 3: Update Android reading flow to pass book info parser**

Expose a parser injection point and ensure flow uses shared detail parsing.

### Task 5: Final Verification

- [ ] Run `:shared:jvmTest`.
- [ ] Run `:app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`.
- [ ] Run `:app:assembleDebug`.
- [ ] Check whether this directory is a Git repo; if not, report commit skipped.

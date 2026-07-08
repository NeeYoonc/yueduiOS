# Shared Toc Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move a small, testable chapter-list parsing path into `:shared` so iOS can reuse the same toc extraction behavior.

**Architecture:** Add a shared `ChapterListParser` boundary and a regex-backed implementation that reads `SharedTocRule`. Add `BookTocService` to fetch a book toc URL through `HttpFetcher` and return parsed `SharedBookChapter` values. Android gets a thin facade that maps `BookSource` and `Book` into shared models and delegates to the shared service.

**Tech Stack:** Kotlin Multiplatform `:shared`, Kotlin common tests, Android unit tests, existing `HttpFetcher` port.

---

### Task 1: Shared Regex Toc Parser

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterListParser.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexChapterListParserTest.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`

- [ ] **Step 1: Write the failing test**

Create a test that builds a `SharedBookSource` with `ruleToc.chapterList`, `chapterName`, `chapterUrl`, `updateTime`, `isVip`, `isPay`, and `isVolume`; parse a small HTML-like body; assert two `SharedBookChapter` values with titles, URLs, flags, tags, and indexes.

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.RegexChapterListParserTest"
```

Expected: FAIL because `RegexChapterListParser` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add `ChapterListParser` and `RegexChapterListParser`. Strip a leading `+` or `-` from `chapterList` before compiling the regex. For each match, extract fields either from `$1` style group references or from field-specific regex rules, and keep only chapters with a nonblank title.

- [ ] **Step 4: Run test to verify it passes**

Run the same `:shared:jvmTest --tests "io.legado.shared.service.RegexChapterListParserTest"` command and expect PASS.

### Task 2: Shared Toc Service

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/BookTocService.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/BookTocServiceTest.kt`

- [ ] **Step 1: Write the failing test**

Create a fake `HttpFetcher` that asserts the request URL is `book.tocUrl`, returns a small body, and assert `BookTocService.getChapterList()` returns the parser output and response metadata.

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookTocServiceTest"
```

Expected: FAIL because `BookTocService` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add `BookTocService(httpFetcher, chapterListParser)` with `getChapterList(source, book)` that requests `book.tocUrl.ifBlank { book.bookUrl }` and delegates response body parsing to the injected parser.

- [ ] **Step 4: Run test to verify it passes**

Run the same `BookTocServiceTest` command and expect PASS.

### Task 3: Android Toc Facade

**Files:**
- Create: `app/src/main/java/io/legado/app/shared/AndroidSharedBookToc.kt`
- Test: `app/src/test/java/io/legado/app/shared/AndroidSharedBookTocTest.kt`

- [ ] **Step 1: Write the failing Android test**

Create an `AndroidSharedBookToc(AndroidHttpFetcher(client), RegexChapterListParser)` test with `MockWebServer`, a `BookSource.ruleToc`, and a `Book.tocUrl`. Assert the returned shared chapters.

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedBookTocTest"
```

Expected: FAIL because `AndroidSharedBookToc` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add `AndroidSharedBookToc` that creates `BookTocService` and delegates `getChapterList(source: BookSource, book: Book)` through `toSharedBookSource()` and `toSharedBook()`.

- [ ] **Step 4: Run test to verify it passes**

Run the same Android test command and expect PASS.

### Task 4: Final Verification

- [ ] Run `:shared:jvmTest`.
- [ ] Run `:app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`.
- [ ] Run `:app:assembleDebug`.
- [ ] Check whether the directory is a Git repo; if it is not, report that commit is skipped.

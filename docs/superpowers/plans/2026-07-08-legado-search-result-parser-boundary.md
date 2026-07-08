# Legado Search Result Parser Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract shared search result parsing behind a `SearchResultParser` boundary so future CSS/JSONPath/XPath/Regex parsers can be swapped in without changing `BookSearchService`.

**Architecture:** Move the current line-oriented parsing logic out of `BookSearchService` into `LineSearchResultParser`. `BookSearchService` receives a `SearchResultParser` with a default line parser, preserving current callers while enabling parser injection in tests and future platform integrations.

**Tech Stack:** Gradle 8.14.4, Kotlin 2.3.10, Kotlin Multiplatform, kotlin.test.

---

## File Structure

- Create `shared/src/commonMain/kotlin/io/legado/shared/service/SearchResultParser.kt`: shared parser interface and default line parser.
- Create `shared/src/commonTest/kotlin/io/legado/shared/service/SearchResultParserTest.kt`: parser unit tests.
- Modify `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`: inject parser and delegate result parsing.
- Modify `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt`: verify parser injection.

---

### Task 1: Extract Search Result Parser

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/SearchResultParser.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/SearchResultParserTest.kt`

- [ ] **Step 1: Write the failing parser tests**

Create `SearchResultParserTest.kt` with tests for two result blocks, `url`/`bookUrl` compatibility, origin fill, and ignored empty blocks.

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.SearchResultParserTest"
```

Expected: FAIL with unresolved reference `LineSearchResultParser`.

- [ ] **Step 3: Add parser interface and line parser**

Move the line parser logic from `BookSearchService` into `SearchResultParser.kt`.

- [ ] **Step 4: Run parser tests**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.SearchResultParserTest"
```

Expected: PASS.

---

### Task 2: Inject Parser Into BookSearchService

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt`

- [ ] **Step 1: Write the failing parser injection test**

Add a `BookSearchServiceTest` case that injects a custom `SearchResultParser` returning a known `SharedSearchBook`.

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookSearchServiceTest"
```

Expected: FAIL because `BookSearchService` does not accept `searchResultParser` yet.

- [ ] **Step 3: Add parser injection**

Change `BookSearchService` constructor to accept `searchResultParser: SearchResultParser = LineSearchResultParser`, and replace private parsing with `searchResultParser.parse(source, response.body)`.

- [ ] **Step 4: Run service tests**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookSearchServiceTest"
```

Expected: PASS.

---

### Task 3: Verify Integration

- [ ] **Step 1: Run shared tests**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest
```

Expected: PASS.

- [ ] **Step 2: Run app shared tests**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"
```

Expected: PASS.

- [ ] **Step 3: Compile app debug APK**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:assembleDebug
```

Expected: PASS.

---

## Self-Review

Spec coverage:

- Parser boundary is covered by Task 1.
- Service parser injection is covered by Task 2.
- Existing Android facade and APK integration are covered by Task 3.

Placeholder scan:

- No placeholder tokens are present.
- All commands and file paths are concrete.

Type consistency:

- `SearchResultParser.parse` returns `List<SharedSearchBook>`.
- `LineSearchResultParser` keeps the current line-oriented parser behavior.

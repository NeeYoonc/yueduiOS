# Legado Regex Search Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first real shared search rule parser, backed by Kotlin Regex, while keeping `commonMain` dependency-free.

**Architecture:** Add `RegexSearchResultParser` implementing `SearchResultParser`. It reads `SharedBookSource.ruleSearch`: `bookList` selects each result block, and field rules either reference list-match groups (`$1`, `$2`) or run a field-specific regex against the block. Android facade gets parser injection so Android can opt into the Regex parser without changing the shared service again.

**Tech Stack:** Gradle 8.14.4, Kotlin 2.3.10, Kotlin Multiplatform, kotlin.test, JUnit 4.

---

## File Structure

- Modify `shared/src/commonMain/kotlin/io/legado/shared/service/SearchResultParser.kt`: add `RegexSearchResultParser`.
- Create `shared/src/commonTest/kotlin/io/legado/shared/service/RegexSearchResultParserTest.kt`: parser unit tests.
- Modify `app/src/main/java/io/legado/app/shared/AndroidSharedBookSearch.kt`: allow parser injection.
- Modify `app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt`: verify Android facade can use Regex parser.

---

### Task 1: Add Shared Regex Parser

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/SearchResultParser.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexSearchResultParserTest.kt`

- [ ] **Step 1: Write failing Regex parser tests**

Create tests that parse two books with `bookList` and `$1` field references, parse field-specific regex rules, and return empty results when `bookList` is missing.

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.RegexSearchResultParserTest"
```

Expected: FAIL with unresolved reference `RegexSearchResultParser`.

- [ ] **Step 3: Implement `RegexSearchResultParser`**

Add an object implementing `SearchResultParser` in `SearchResultParser.kt`.

- [ ] **Step 4: Run Regex parser tests**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.RegexSearchResultParserTest"
```

Expected: PASS.

---

### Task 2: Let Android Facade Inject Parser

**Files:**
- Modify: `app/src/main/java/io/legado/app/shared/AndroidSharedBookSearch.kt`
- Modify: `app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt`

- [ ] **Step 1: Write failing Android parser injection test**

Add a test constructing `AndroidSharedBookSearch(AndroidHttpFetcher(client), RegexSearchResultParser)`.

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedBookSearchTest"
```

Expected: FAIL because `AndroidSharedBookSearch` does not accept a parser argument.

- [ ] **Step 3: Add parser injection**

Change Android facade constructor to accept `searchResultParser: SearchResultParser = LineSearchResultParser` and pass it to `BookSearchService`.

- [ ] **Step 4: Run Android shared test**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedBookSearchTest"
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

- Shared Regex parser is covered by Task 1.
- Android parser injection is covered by Task 2.
- Shared/app/APK verification is covered by Task 3.

Placeholder scan:

- No placeholder tokens are present.
- Commands and file paths are concrete.

Type consistency:

- `RegexSearchResultParser` implements `SearchResultParser`.
- Android facade passes a `SearchResultParser` into `BookSearchService`.

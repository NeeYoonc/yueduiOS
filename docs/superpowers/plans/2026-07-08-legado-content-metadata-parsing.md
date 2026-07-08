# Legado Content Metadata Parsing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let shared content parsing carry chapter title and sub-content metadata from `ruleContent.title` and `ruleContent.subContent`.

**Architecture:** Extend `SharedChapterContent` with nullable metadata fields and teach `RegexChapterContentParser` to extract them using the same regex helper used for chapter body content. Pagination continues to merge only body text while preserving first-page metadata through the existing `copy` call in `BookContentService`.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Gradle JVM tests, Android JVM tests.

---

### Task 1: Parser Metadata Test

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexChapterContentParserTest.kt`
- Modify after red: `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`
- Modify after red: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterContentParser.kt`

- [x] **Step 1: Write the failing test**

Add a test that builds `SharedContentRule(content, title, subContent)` and asserts that `RegexChapterContentParser.parse` returns `SharedChapterContent.title` and `SharedChapterContent.subContent`.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.RegexChapterContentParserTest"`

Expected: compile fails because `SharedChapterContent` does not expose `title` or `subContent`.

- [x] **Step 3: Write minimal implementation**

Add `title: String? = null` and `subContent: String? = null` to `SharedChapterContent`, then set them in `RegexChapterContentParser`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexChapterContentParserTest"`

Expected: parser tests pass.

### Task 2: Service Metadata Preservation Test

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/BookContentServiceTest.kt`

- [x] **Step 1: Write the failing test**

Add assertions to the pagination test or a focused service test that `BookContentService` preserves the first parsed `title` and `subContent` when it returns merged content.

- [x] **Step 2: Run test to verify it fails if service drops metadata**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookContentServiceTest"`

Expected: pass if existing `firstContent.copy(...)` already preserves the metadata; fail only if service reconstructs content without metadata.

- [x] **Step 3: Implement only if needed**

If the test fails, update `BookContentService` to carry the first non-null metadata values while merging pages.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.BookContentServiceTest"`

Expected: service tests pass.

### Task 3: Verification

**Files:**
- No additional files.

- [x] **Step 1: Run shared tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

Expected: all shared JVM tests pass.

- [x] **Step 2: Run Android shared tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

Expected: Android shared tests pass.

- [x] **Step 3: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

Expected: debug APK build completes successfully.

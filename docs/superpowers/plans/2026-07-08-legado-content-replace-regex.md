# Legado Content Replace Regex Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Support the Android `replaceRegex` content cleanup format in the shared regex content parser.

**Architecture:** Keep the shared parser conservative: after extracting and normalizing body content, apply `ruleContent.replaceRegex` only for the Android-compatible `##match##replacement` form. This does not port the full Android rule engine, JS evaluation, or `@get` expansion.

**Tech Stack:** Kotlin Multiplatform, Gradle JVM tests, Android JVM tests.

---

### Task 1: Parser Replacement Test

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexChapterContentParserTest.kt`
- Modify after red: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterContentParser.kt`

- [x] **Step 1: Write the failing test**

Add a test with `SharedContentRule(content = ..., replaceRegex = "##<span class=\"ad\">[\\s\\S]*?</span>##")` and assert the extracted content no longer contains the ad span.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.RegexChapterContentParserTest"`

Expected: assertion fails because the parser currently returns extracted content without applying `replaceRegex`.

- [x] **Step 3: Write minimal implementation**

Parse `##match##replacement` into a regex pattern plus replacement, apply it to extracted `content`, and normalize the result. Invalid or unsupported replacement rules should leave content unchanged.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexChapterContentParserTest"`

Expected: parser tests pass.

### Task 2: Verification

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

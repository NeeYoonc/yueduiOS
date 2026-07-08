# Legado JSON Search Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Support basic Android-style JSON search rules in the shared search parser.

**Architecture:** Extend the existing shared parser to detect `bookList` rules starting with `$` and parse the response with kotlinx.serialization JSON. Support dot paths such as `$.content.content` and field paths such as `$.title`; strip unsupported suffixes like `@js:` so the base JSON value is still usable.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization JSON, Gradle JVM tests.

---

### Task 1: JSON Search Rules

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexSearchResultParserTest.kt`
- Modify after red: `shared/src/commonMain/kotlin/io/legado/shared/service/SearchResultParser.kt`

- [x] **Step 1: Write the failing test**

Add a JSON response with `content.content` array and rules using `$.title`, `$.author`, `$.id`, `$.newestChapter`.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.RegexSearchResultParserTest"`

Expected: JSON test fails because the parser only treats `bookList` as regex.

- [x] **Step 3: Implement basic JSON path parser**

Use kotlinx.serialization `JsonElement`; support object dot paths and arrays returned by the final path.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexSearchResultParserTest"`

### Task 2: Verification

**Files:**
- No additional files.

- [x] **Step 1: Run iOS compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64`

- [x] **Step 2: Run shared and Android checks**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

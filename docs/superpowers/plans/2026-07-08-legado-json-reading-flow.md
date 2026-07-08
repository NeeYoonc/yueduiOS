# Legado JSON Reading Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the default shared runtime path use rule-based search when a source has `ruleSearch`, so iOS can complete JSON search to content without custom parser injection.

**Architecture:** Add an adaptive default `SearchResultParser` that delegates to `RegexSearchResultParser` for sources with `ruleSearch`, and to `LineSearchResultParser` for demo or test sources without a rule. Use it as the default in shared and Android facade entrypoints.

**Tech Stack:** Kotlin Multiplatform, kotlin.test, Gradle.

---

### Task 1: JSON Reading Flow Coverage

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/ReadingFlowServiceTest.kt`

- [x] **Step 1: Write the failing end-to-end test**

Add a test where `ReadingFlowService(fetcher)` uses default parsers and a JSON rule source to fetch search, book info, TOC, and content.

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.ReadingFlowServiceTest"`

Expected: the JSON flow test fails because only the search URL is requested.

### Task 2: Rule-Aware Default Search Parser

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/SearchResultParser.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ReadingFlowService.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/LegadoSharedClient.kt`
- Modify: `app/src/main/java/io/legado/app/shared/AndroidSharedBookSearch.kt`
- Modify: `app/src/main/java/io/legado/app/shared/AndroidSharedReadingFlow.kt`

- [x] **Step 3: Add adaptive parser**

Create `RuleAwareSearchResultParser` in `SearchResultParser.kt`.

- [x] **Step 4: Use adaptive parser as default**

Replace default `LineSearchResultParser` parameters with `RuleAwareSearchResultParser` in shared services and Android facades.

- [x] **Step 5: Run focused test and verify GREEN**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.ReadingFlowServiceTest"`

Expected: all `ReadingFlowServiceTest` tests pass.

### Task 3: Cross Target Verification

**Files:**
- No code changes.

- [x] **Step 6: Run full shared JVM tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 7: Run iOS compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64`

- [x] **Step 8: Run Android adapter tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

- [x] **Step 9: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

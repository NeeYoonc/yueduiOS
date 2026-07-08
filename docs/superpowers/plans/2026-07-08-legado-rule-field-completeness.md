# Legado Rule Field Completeness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve Android rule fields in the shared KMP data contract and Android-to-shared mapper.

**Architecture:** Add nullable serializable properties to the shared rule data classes and map the matching Android rule entity fields. This task does not change rule execution or parser behavior.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Android JVM unit tests, Gradle.

---

### Task 1: Importer Contract Test

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/source/SourceJsonImporterTest.kt`
- Modify after red: `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`

- [x] **Step 1: Write the failing test**

Add a test that imports a source JSON object containing these fields: `ruleSearch.updateTime`, `ruleSearch.wordCount`, `ruleBookInfo.init`, `ruleBookInfo.updateTime`, `ruleBookInfo.wordCount`, `ruleBookInfo.canReName`, `ruleBookInfo.downloadUrls`, `ruleToc.formatJs`, `ruleContent.subContent`, `ruleContent.title`, `ruleContent.imageStyle`, `ruleContent.imageDecode`, `ruleContent.payAction`, and `ruleContent.callBackJs`.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.source.SourceJsonImporterTest"`

Expected: compile fails because the new shared rule properties do not exist yet.

- [x] **Step 3: Write minimal implementation**

Add nullable `String? = null` properties to the shared rule data classes for the missing Android fields.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.source.SourceJsonImporterTest"`

Expected: test passes.

### Task 2: Android Mapper Contract Test

**Files:**
- Modify: `app/src/test/java/io/legado/app/shared/AndroidSharedMappersTest.kt`
- Modify after red: `app/src/main/java/io/legado/app/shared/AndroidSharedMappers.kt`

- [x] **Step 1: Write the failing test**

Extend `mapsBookSourceAndNestedRulesToSharedModel` so each Android rule sets the missing fields and asserts that the shared rule keeps the same values.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedMappersTest"`

Expected: compile fails until the shared properties and mapper arguments exist.

- [x] **Step 3: Write minimal implementation**

Map the new properties in `SearchRule.toSharedSearchRule`, `BookInfoRule.toSharedBookInfoRule`, `TocRule.toSharedTocRule`, and `ContentRule.toSharedContentRule`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedMappersTest"`

Expected: test passes.

### Task 3: Verification

**Files:**
- No additional files.

- [x] **Step 1: Run shared tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

Expected: all shared JVM tests pass.

- [x] **Step 2: Run Android shared adapter tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

Expected: all Android shared adapter tests pass.

- [x] **Step 3: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

Expected: debug APK build completes successfully.

# Legado Chapter Content Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist fetched chapter content through the shared storage layer for offline reading.

**Architecture:** Extend `SharedLibraryStore` with content cache methods keyed by book origin/book URL/chapter URL. Store one `SharedChapterContent` JSON payload per chapter through `CacheStorePort`, preserving title, sub-content, and pagination metadata.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Gradle JVM tests.

---

### Task 1: Chapter Content Cache

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/storage/SharedLibraryStoreTest.kt`
- Modify after red: `shared/src/commonMain/kotlin/io/legado/shared/storage/SharedLibraryStore.kt`

- [x] **Step 1: Write the failing test**

Add a test that saves `SharedChapterContent` for a `SharedBook` and `SharedBookChapter`, then loads it back and checks a miss returns `null`.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.storage.SharedLibraryStoreTest"`

Expected: compile fails because content cache methods do not exist.

- [x] **Step 3: Implement content cache methods**

Add `saveChapterContent(book, chapter, content)` and `loadChapterContent(book, chapter)`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.storage.SharedLibraryStoreTest"`

### Task 2: Verification

**Files:**
- No additional files.

- [x] **Step 1: Run iOS compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64`

- [x] **Step 2: Run shared JVM tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 3: Run Android shared tests and debug build**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

# Legado JSON TOC Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add basic JSON path TOC parsing to shared code so iOS can open chapter lists from JSON book sources.

**Architecture:** Keep `ChapterListParser` as the boundary. Add a JSON branch to `RegexChapterListParser` when `ruleToc.chapterList` starts with `$`, mirroring the shared search parser behavior while preserving the existing regex branch.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization JSON, kotlin.test, Gradle.

---

### Task 1: JSON TOC Parser Coverage

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexChapterListParserTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
@Test
fun parsesChaptersFromJsonPathTocRules() {
    val source = SharedBookSource(
        bookSourceUrl = "https://api.example.test",
        bookSourceName = "JSON",
        ruleToc = SharedTocRule(
            chapterList = "$.content.content",
            chapterName = "$.chapterTitle",
            chapterUrl = "$.id@js:'ignored'",
            updateTime = "$.updated",
            isVip = "$.vip",
            isPay = "$.pay",
            isVolume = "$.volume"
        )
    )
    val body = """
        {"content":{"content":[
          {"id":"chapter-1","chapterTitle":"Chapter 1","updated":"2026-07-01","vip":false,"pay":true,"volume":false},
          {"id":"chapter-2","chapterTitle":"Volume 1","updated":"2026-07-02","vip":true,"pay":false,"volume":true}
        ]}}
    """.trimIndent()

    val chapters = RegexChapterListParser.parse(source, body)

    assertEquals(2, chapters.size)
    assertEquals("Chapter 1", chapters[0].title)
    assertEquals("chapter-1", chapters[0].url)
    assertEquals("2026-07-01", chapters[0].tag)
    assertFalse(chapters[0].isVip)
    assertTrue(chapters[0].isPay)
    assertFalse(chapters[0].isVolume)
    assertEquals("Volume 1", chapters[1].title)
    assertEquals("chapter-2", chapters[1].url)
    assertEquals("2026-07-02", chapters[1].tag)
    assertTrue(chapters[1].isVip)
    assertFalse(chapters[1].isPay)
    assertTrue(chapters[1].isVolume)
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexChapterListParserTest"`

Expected: the JSON test fails with `Expected <2>, actual <0>`.

### Task 2: Minimal JSON Path Implementation

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterListParser.kt`

- [x] **Step 3: Implement JSON branch**

Use `kotlinx.serialization.json.Json` to parse the body. Select chapter items from `ruleToc.chapterList`, strip any `@...` suffix from field rules, and map fields into `SharedBookChapter`.

- [x] **Step 4: Run focused test and verify GREEN**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexChapterListParserTest"`

Expected: all `RegexChapterListParserTest` tests pass.

### Task 3: Cross Target Verification

**Files:**
- No code changes.

- [x] **Step 5: Run full shared JVM tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 6: Run iOS compile checks**

Run: `.\gradlew.bat --no-daemon -q :shared:compileKotlinIosX64 :shared:compileTestKotlinIosX64 :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64`

- [x] **Step 7: Run Android adapter tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

- [x] **Step 8: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`

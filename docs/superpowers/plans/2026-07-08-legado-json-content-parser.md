# Legado JSON Content Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add basic JSON path chapter content parsing to shared code so iOS can read chapter bodies from JSON book sources.

**Architecture:** Keep `ChapterContentParser` as the boundary. Detect JSON path rules in `RegexChapterContentParser`, parse the response with `SimpleJsonPath`, and reuse the existing normalization and `replaceRegex` behavior.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization JSON, kotlin.test, Gradle.

---

### Task 1: JSON Content Parser Coverage

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexChapterContentParserTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
@Test
fun parsesContentFromJsonPathRules() {
    val source = SharedBookSource(
        bookSourceUrl = "https://api.example.test",
        bookSourceName = "JSON",
        ruleContent = SharedContentRule(
            content = "$.content.text",
            title = "$.content.title",
            subContent = "$.content.note",
            nextContentUrl = "$.content.next",
            replaceRegex = "##AD LINE##"
        )
    )
    val book = SharedBook(
        name = "JSON Book",
        bookUrl = "https://api.example.test/book/1",
        origin = source.bookSourceUrl
    )
    val chapter = SharedBookChapter(
        title = "Original",
        url = "https://api.example.test/chapter/1"
    )
    val body = """
        {"content":{
          "title":"JSON Chapter",
          "text":"First line.\nAD LINE\nSecond line.",
          "note":"Side line.",
          "next":["/chapter/1-2","/chapter/1-3"]
        }}
    """.trimIndent()

    val content = RegexChapterContentParser.parse(source, book, chapter, body)

    assertEquals("JSON Chapter", content.title)
    assertEquals("Side line.", content.subContent)
    assertEquals("First line.\nSecond line.", content.content)
    assertEquals(listOf("/chapter/1-2", "/chapter/1-3"), content.nextContentUrls)
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexChapterContentParserTest"`

Expected: the JSON test fails because content remains empty.

### Task 2: Minimal JSON Path Implementation

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/SimpleJsonPath.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterContentParser.kt`

- [x] **Step 3: Add JSON text list helper**

Add `SimpleJsonPath.texts` so array fields such as `nextContentUrl` return one URL per element.

- [x] **Step 4: Add JSON branch to content parser**

When any mapped content rule starts with `$`, parse body as JSON and map content, title, subContent, and nextContentUrls.

- [x] **Step 5: Run focused test and verify GREEN**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexChapterContentParserTest"`

Expected: all `RegexChapterContentParserTest` tests pass.

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

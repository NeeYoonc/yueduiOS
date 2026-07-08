# Legado JSON Book Info Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add basic JSON path book info parsing to shared code so iOS can populate book details before opening the TOC.

**Architecture:** Keep `BookInfoParser` as the boundary. Detect JSON path field rules in `RegexBookInfoParser`, parse the body as JSON, and map known `SharedBook` fields while preserving regex behavior.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization JSON, kotlin.test, Gradle.

---

### Task 1: JSON Book Info Parser Coverage

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/legado/shared/service/RegexBookInfoParserTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
@Test
fun parsesBookInfoFromJsonPathRules() {
    val source = SharedBookSource(
        bookSourceUrl = "https://api.example.test",
        bookSourceName = "JSON",
        ruleBookInfo = SharedBookInfoRule(
            name = "$.content.title",
            author = "$.content.authorName",
            kind = "$.content.category",
            lastChapter = "$.content.newestChapterTitle",
            intro = "$.content.desc",
            coverUrl = "$.content.cover",
            tocUrl = "$.content.catalogUrl@js:'ignored'"
        )
    )
    val book = SharedBook(
        name = "Old",
        author = "Old Author",
        bookUrl = "https://api.example.test/book/1",
        tocUrl = "",
        origin = source.bookSourceUrl
    )
    val body = """
        {"content":{
          "title":"JSON Story",
          "authorName":"API Author",
          "category":"Adventure",
          "newestChapterTitle":"Chapter 12",
          "desc":"First line.\nSecond line.",
          "cover":"/cover.jpg",
          "catalogUrl":"/book/1/catalog"
        }}
    """.trimIndent()

    val parsed = RegexBookInfoParser.parse(source, book, body)

    assertEquals("JSON Story", parsed.name)
    assertEquals("API Author", parsed.author)
    assertEquals("Adventure", parsed.kind)
    assertEquals("Chapter 12", parsed.latestChapterTitle)
    assertEquals("First line.\nSecond line.", parsed.intro)
    assertEquals("/cover.jpg", parsed.coverUrl)
    assertEquals("/book/1/catalog", parsed.tocUrl)
    assertEquals("https://api.example.test/book/1", parsed.bookUrl)
    assertEquals(source.bookSourceUrl, parsed.origin)
}
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexBookInfoParserTest"`

Expected: the JSON test fails because the parser preserves old fields.

### Task 2: Minimal JSON Path Implementation

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/SimpleJsonPath.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/ChapterListParser.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookInfoParser.kt`

- [x] **Step 3: Extract shared JSON path helper**

Move the existing JSON path parse/select/text behavior into `SimpleJsonPath` for reuse by TOC and book info parsers.

- [x] **Step 4: Add JSON branch to book info parser**

When any `ruleBookInfo` field rule starts with `$`, parse the body as JSON and map known `SharedBook` fields from `SimpleJsonPath.text`.

- [x] **Step 5: Run focused test and verify GREEN**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest --tests "io.legado.shared.service.RegexBookInfoParserTest"`

Expected: all `RegexBookInfoParserTest` tests pass.

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

# Legado Android Shared Adapters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect Android Room/Parcelable entities to the new Kotlin Multiplatform `:shared` models without changing existing app behavior.

**Architecture:** Keep Android-only entities in `app`, and add a thin mapper package that converts them to `io.legado.shared.model` types. The mapper is one-way for now because shared services should consume stable common models before write-back behavior is introduced.

**Tech Stack:** Gradle 8.14.4, Android Gradle Plugin 8.13.2, Kotlin 2.3.10, JUnit 4, Kotlin Multiplatform `:shared`.

---

## File Structure

- Modify `app/build.gradle`: add `implementation(project(path: ':shared'))`.
- Create `app/src/main/java/io/legado/app/shared/AndroidSharedMappers.kt`: Android entity to shared model mapping extensions.
- Create `app/src/test/java/io/legado/app/shared/AndroidSharedMappersTest.kt`: unit tests for source, book, search book, and chapter mappings.

---

### Task 1: Add Shared Module Dependency

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: Verify app cannot import shared models directly yet**

Create the adapter test in Task 2 before adding this dependency, then run:

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedMappersTest"
```

Expected: FAIL because `io.legado.shared.model.SharedBookSource` or `toSharedBookSource` cannot be resolved.

- [ ] **Step 2: Add the dependency**

In `app/build.gradle`, add this dependency near the existing module dependencies:

```groovy
implementation(project(path: ':shared'))
```

- [ ] **Step 3: Commit if this directory is inside Git**

```powershell
if (Test-Path .git) {
  git add app/build.gradle
  git commit -m "build: depend on shared module from app"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 2: Map Android Entities to Shared Models

**Files:**
- Create: `app/src/main/java/io/legado/app/shared/AndroidSharedMappers.kt`
- Test: `app/src/test/java/io/legado/app/shared/AndroidSharedMappersTest.kt`

- [ ] **Step 1: Write the failing mapper tests**

Create `app/src/test/java/io/legado/app/shared/AndroidSharedMappersTest.kt`:

```kotlin
package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSharedMappersTest {
    @Test
    fun mapsBookSourceAndNestedRulesToSharedModel() {
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            bookSourceGroup = "Group",
            bookSourceType = 0,
            bookUrlPattern = "https://source.test/book/.*",
            enabled = true,
            enabledExplore = false,
            enabledCookieJar = false,
            concurrentRate = "2/1000",
            header = """{"User-Agent":"Legado"}""",
            loginUrl = "https://source.test/login",
            loginUi = "user,password",
            loginCheckJs = "result",
            coverDecodeJs = "bytes",
            bookSourceComment = "comment",
            variableComment = "vars",
            exploreUrl = "https://source.test/explore",
            searchUrl = "https://source.test/search?q={{key}}",
            ruleSearch = SearchRule(
                bookList = ".book",
                name = ".name",
                author = ".author",
                kind = ".kind",
                lastChapter = ".last",
                intro = ".intro",
                coverUrl = ".cover@src",
                bookUrl = "a@href",
                checkKeyWord = "keyword"
            ),
            ruleBookInfo = BookInfoRule(
                name = "h1",
                author = ".author",
                kind = ".kind",
                lastChapter = ".last",
                intro = ".intro",
                coverUrl = ".cover@src",
                tocUrl = ".toc@href"
            ),
            ruleToc = TocRule(
                chapterList = ".chapter",
                chapterName = ".title",
                chapterUrl = "a@href",
                nextTocUrl = ".next@href",
                updateTime = ".time",
                isVip = ".vip",
                isPay = ".pay",
                preUpdateJs = "pre"
            ),
            ruleContent = ContentRule(
                content = ".content",
                nextContentUrl = ".next@href",
                replaceRegex = "ads",
                webJs = "web",
                sourceRegex = "source"
            )
        )

        val shared = source.toSharedBookSource()

        assertEquals("https://source.test", shared.bookSourceUrl)
        assertEquals("Source", shared.bookSourceName)
        assertEquals("Group", shared.bookSourceGroup)
        assertEquals(false, shared.enabledExplore)
        assertEquals(false, shared.enabledCookieJar)
        assertEquals(".book", shared.ruleSearch?.bookList)
        assertEquals("keyword", shared.ruleSearch?.checkKeyWord)
        assertEquals(".toc@href", shared.ruleBookInfo?.tocUrl)
        assertEquals(".chapter", shared.ruleToc?.chapterList)
        assertEquals("ads", shared.ruleContent?.replaceRegex)
    }

    @Test
    fun mapsBookAndSearchBookToSharedBookModels() {
        val book = Book(
            name = "Metal",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1/toc",
            origin = "https://source.test",
            kind = "Sci-Fi",
            latestChapterTitle = "Chapter 9",
            intro = "Intro",
            coverUrl = "https://source.test/cover.jpg",
            variable = """{"token":"abc"}"""
        )

        val sharedBook = book.toSharedBook()

        assertEquals("Metal", sharedBook.name)
        assertEquals("Tester", sharedBook.author)
        assertEquals("https://source.test/book/1/toc", sharedBook.tocUrl)
        assertEquals("abc", sharedBook.variableMap["token"])

        val searchBook = SearchBook(
            name = "Result",
            author = "Author",
            bookUrl = "https://source.test/book/2",
            origin = "https://source.test",
            kind = "Fantasy",
            latestChapterTitle = "Latest",
            intro = "Intro",
            coverUrl = "https://source.test/result.jpg"
        )

        val sharedSearchBook = searchBook.toSharedSearchBook()

        assertEquals("Result", sharedSearchBook.name)
        assertEquals("Fantasy", sharedSearchBook.kind)
        assertEquals("Latest", sharedSearchBook.latestChapterTitle)
        assertEquals("https://source.test/result.jpg", sharedSearchBook.coverUrl)
    }

    @Test
    fun mapsChapterToSharedChapter() {
        val chapter = BookChapter(
            title = "Chapter 1",
            url = "/chapter/1",
            index = 3,
            isVolume = true,
            isVip = true,
            isPay = false,
            tag = "2026-07-07",
            variable = """{"page":"10"}"""
        )

        val shared = chapter.toSharedBookChapter()

        assertEquals("Chapter 1", shared.title)
        assertEquals("/chapter/1", shared.url)
        assertEquals(3, shared.index)
        assertTrue(shared.isVolume)
        assertTrue(shared.isVip)
        assertEquals("2026-07-07", shared.tag)
        assertEquals("10", shared.variableMap["page"])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedMappersTest"
```

Expected: FAIL with unresolved references such as `toSharedBookSource`.

- [ ] **Step 3: Add the mapper implementation**

Create `app/src/main/java/io/legado/app/shared/AndroidSharedMappers.kt`:

```kotlin
package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.model.SharedTocRule

fun BookSource.toSharedBookSource(): SharedBookSource = SharedBookSource(
    bookSourceUrl = bookSourceUrl,
    bookSourceName = bookSourceName,
    bookSourceGroup = bookSourceGroup,
    bookSourceType = bookSourceType,
    bookUrlPattern = bookUrlPattern,
    enabled = enabled,
    enabledExplore = enabledExplore,
    enabledCookieJar = enabledCookieJar,
    concurrentRate = concurrentRate,
    header = header,
    loginUrl = loginUrl,
    loginUi = loginUi,
    loginCheckJs = loginCheckJs,
    coverDecodeJs = coverDecodeJs,
    bookSourceComment = bookSourceComment,
    variableComment = variableComment,
    exploreUrl = exploreUrl,
    searchUrl = searchUrl,
    ruleSearch = ruleSearch?.toSharedSearchRule(),
    ruleBookInfo = ruleBookInfo?.toSharedBookInfoRule(),
    ruleToc = ruleToc?.toSharedTocRule(),
    ruleContent = ruleContent?.toSharedContentRule()
)

fun SearchRule.toSharedSearchRule(): SharedSearchRule = SharedSearchRule(
    bookList = bookList,
    name = name,
    author = author,
    kind = kind,
    lastChapter = lastChapter,
    intro = intro,
    coverUrl = coverUrl,
    bookUrl = bookUrl,
    checkKeyWord = checkKeyWord
)

fun BookInfoRule.toSharedBookInfoRule(): SharedBookInfoRule = SharedBookInfoRule(
    name = name,
    author = author,
    kind = kind,
    lastChapter = lastChapter,
    intro = intro,
    coverUrl = coverUrl,
    tocUrl = tocUrl
)

fun TocRule.toSharedTocRule(): SharedTocRule = SharedTocRule(
    chapterList = chapterList,
    chapterName = chapterName,
    chapterUrl = chapterUrl,
    nextTocUrl = nextTocUrl,
    updateTime = updateTime,
    isVip = isVip,
    isPay = isPay,
    preUpdateJs = preUpdateJs
)

fun ContentRule.toSharedContentRule(): SharedContentRule = SharedContentRule(
    content = content,
    nextContentUrl = nextContentUrl,
    replaceRegex = replaceRegex,
    webJs = webJs,
    sourceRegex = sourceRegex
)

fun Book.toSharedBook(): SharedBook = SharedBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    tocUrl = tocUrl,
    origin = origin,
    kind = kind,
    latestChapterTitle = latestChapterTitle,
    intro = intro,
    coverUrl = coverUrl,
    variableMap = variableMap
)

fun SearchBook.toSharedSearchBook(): SharedSearchBook = SharedSearchBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    origin = origin,
    kind = kind,
    latestChapterTitle = latestChapterTitle,
    intro = intro,
    coverUrl = coverUrl
)

fun BookChapter.toSharedBookChapter(): SharedBookChapter = SharedBookChapter(
    title = title,
    url = url,
    index = index,
    isVolume = isVolume,
    isVip = isVip,
    isPay = isPay,
    tag = tag,
    variableMap = variableMap
)
```

- [ ] **Step 4: Run mapper tests**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedMappersTest"
```

Expected: PASS with 3 tests.

- [ ] **Step 5: Commit if this directory is inside Git**

```powershell
if (Test-Path .git) {
  git add app/build.gradle app/src/main/java/io/legado/app/shared/AndroidSharedMappers.kt app/src/test/java/io/legado/app/shared/AndroidSharedMappersTest.kt
  git commit -m "feat: map android entities to shared models"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 3: Verify App and Shared Builds

**Files:**
- Read: all files touched by Tasks 1-2.

- [ ] **Step 1: Run shared tests**

```powershell
cd D:\games\AI\legado-beta-ascii
.\gradlew.bat --no-daemon :shared:jvmTest
```

Expected: PASS.

- [ ] **Step 2: Run mapper unit tests**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedMappersTest"
```

Expected: PASS.

- [ ] **Step 3: Compile app debug APK**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Record final status**

```powershell
if (Test-Path .git) {
  git status --short
} else {
  Get-ChildItem -Path app/src/main/java/io/legado/app/shared,app/src/test/java/io/legado/app/shared -Recurse | Select-Object FullName,Length
}
```

Expected in this extracted project: file list for the new adapter and test.

---

## Self-Review

Spec coverage:

- Android `BookSource` to shared source mapping is covered by Task 2.
- Nested search/book-info/toc/content rule mapping is covered by Task 2.
- Android `Book`, `SearchBook`, and `BookChapter` read model mapping is covered by Task 2.
- Build integration is covered by Task 1 and Task 3.

Placeholder scan:

- No placeholder tokens are present.
- Each code-producing step includes exact code and verification commands.

Type consistency:

- `toSharedBookSource`, `toSharedBook`, `toSharedSearchBook`, and `toSharedBookChapter` are defined before verification references them.
- Mapped destination types match the `:shared` model names currently present in `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`.

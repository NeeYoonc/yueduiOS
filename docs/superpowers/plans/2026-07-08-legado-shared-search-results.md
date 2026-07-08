# Legado Shared Search Results Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the shared search service return structured `SharedSearchBook` results instead of only a debug book name.

**Architecture:** Keep the existing `BookSearchService` entrypoint and add a small line-oriented parser for the current test/debug response format. The parser treats blank-line separated `key=value` blocks as search books and fills `SharedSearchBook` fields; this preserves the existing fake HTTP tests while creating the first typed result contract for Android and iOS.

**Tech Stack:** Gradle 8.14.4, Kotlin 2.3.10, Kotlin Multiplatform, kotlinx.coroutines, kotlin.test.

---

## File Structure

- Modify `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`: return `books: List<SharedSearchBook>` in `SearchPageResult` and parse line-oriented result blocks.
- Modify `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt`: assert structured search result output.
- Modify `app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt`: assert the Android facade exposes structured shared search books.

---

### Task 1: Add Structured Shared Search Results

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt`

- [ ] **Step 1: Write the failing shared service tests**

Update `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt` so the HTTP body contains two blank-line separated result blocks and assertions read `result.books`:

```kotlin
package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BookSearchServiceTest {
    @Test
    fun fetchesSearchPageWithKeywordAndPage() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://example.test/search?q=sword&page=2", request.url)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = """
                        name=Metal Sword
                        author=Tester
                        kind=Adventure
                        lastChapter=Chapter 9
                        intro=First intro
                        coverUrl=https://example.test/cover.jpg
                        url=https://example.test/book/1

                        name=Iron Sword
                        author=Second
                        bookUrl=https://example.test/book/2
                    """.trimIndent()
                )
            }
        }
        val service = BookSearchService(fetcher)
        val source = SharedBookSource(
            bookSourceUrl = "https://example.test",
            bookSourceName = "Example",
            searchUrl = "https://example.test/search?q={{key}}&page={{page}}"
        )

        val result = service.search(source, key = "sword", page = 2)

        assertEquals("https://example.test", result.source.bookSourceUrl)
        assertEquals("https://example.test/search?q=sword&page=2", result.response.finalUrl)
        assertEquals("Metal Sword", result.debugBookName)
        assertEquals(2, result.books.size)
        assertEquals("Metal Sword", result.books[0].name)
        assertEquals("Tester", result.books[0].author)
        assertEquals("Adventure", result.books[0].kind)
        assertEquals("Chapter 9", result.books[0].latestChapterTitle)
        assertEquals("First intro", result.books[0].intro)
        assertEquals("https://example.test/cover.jpg", result.books[0].coverUrl)
        assertEquals("https://example.test/book/1", result.books[0].bookUrl)
        assertEquals("https://example.test", result.books[0].origin)
        assertEquals("Iron Sword", result.books[1].name)
        assertEquals("https://example.test/book/2", result.books[1].bookUrl)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookSearchServiceTest"
```

Expected: FAIL with unresolved reference `books`.

- [ ] **Step 3: Add parser and result field**

Update `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`:

```kotlin
package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse

class BookSearchService(
    private val httpFetcher: HttpFetcher
) {
    suspend fun search(source: SharedBookSource, key: String, page: Int = 1): SearchPageResult {
        val template = requireNotNull(source.searchUrl) {
            "Book source ${source.bookSourceName} has no searchUrl"
        }
        val requestUrl = template
            .replace("{{key}}", encodeQueryValue(key))
            .replace("{{page}}", page.toString())
        val response = httpFetcher.fetch(SharedHttpRequest(url = requestUrl))
        return SearchPageResult(
            source = source,
            response = response,
            books = parseSearchBooks(source, response.body),
            debugBookName = extractDebugValue(response.body, "name")
        )
    }

    private fun encodeQueryValue(value: String): String =
        value.encodeToByteArray().joinToString("") { byte ->
            val char = byte.toInt().toChar()
            when {
                char in 'a'..'z' -> char.toString()
                char in 'A'..'Z' -> char.toString()
                char in '0'..'9' -> char.toString()
                char == '-' || char == '_' || char == '.' || char == '~' -> char.toString()
                char == ' ' -> "%20"
                else -> "%" + byte.toUByte().toString(16).uppercase().padStart(2, '0')
            }
        }

    private fun parseSearchBooks(source: SharedBookSource, body: String): List<SharedSearchBook> {
        return body.splitToSequence(Regex("\\n\\s*\\n"))
            .mapNotNull { block -> parseSearchBook(source, block) }
            .toList()
    }

    private fun parseSearchBook(source: SharedBookSource, block: String): SharedSearchBook? {
        val values = block.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && "=" in it }
            .map {
                val key = it.substringBefore("=").trim()
                val value = it.substringAfter("=").trim()
                key to value
            }
            .toMap()
        val name = values["name"].orEmpty()
        val bookUrl = values["bookUrl"] ?: values["url"] ?: ""
        if (name.isBlank() && bookUrl.isBlank()) {
            return null
        }
        return SharedSearchBook(
            name = name,
            author = values["author"].orEmpty(),
            bookUrl = bookUrl,
            origin = source.bookSourceUrl,
            kind = values["kind"],
            latestChapterTitle = values["lastChapter"] ?: values["latestChapterTitle"],
            intro = values["intro"],
            coverUrl = values["coverUrl"]
        )
    }

    private fun extractDebugValue(body: String, key: String): String? {
        val prefix = "$key="
        return body.lineSequence()
            .firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.trim()
    }
}

data class SearchPageResult(
    val source: SharedBookSource,
    val response: SharedHttpResponse,
    val books: List<SharedSearchBook> = emptyList(),
    val debugBookName: String?
)
```

- [ ] **Step 4: Run shared service tests**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookSearchServiceTest"
```

Expected: PASS.

---

### Task 2: Verify Android Facade Exposes Structured Results

**Files:**
- Modify: `app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt`

- [ ] **Step 1: Add Android facade assertions**

In `AndroidSharedBookSearchTest.searchesAndroidBookSourceThroughSharedServiceAndAndroidHttp`, add:

```kotlin
assertEquals(1, result.books.size)
assertEquals("Iron Road", result.books.single().name)
assertEquals("Tester", result.books.single().author)
assertEquals("https://source.test/book/1", result.books.single().bookUrl)
assertEquals("https://source.test", result.books.single().origin)
```

- [ ] **Step 2: Run Android facade test**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedBookSearchTest"
```

Expected: PASS.

---

### Task 3: Verify Search Result Integration

**Files:**
- Read: all files touched by Tasks 1-2.

- [ ] **Step 1: Run shared JVM tests**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest
```

Expected: PASS.

- [ ] **Step 2: Run app shared adapter tests**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"
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
  Get-ChildItem -Path shared/src/commonMain/kotlin/io/legado/shared/service,shared/src/commonTest/kotlin/io/legado/shared/service,app/src/test/java/io/legado/app/shared -Recurse | Select-Object FullName,Length
}
```

Expected in this extracted project: file list for touched search files.

---

## Self-Review

Spec coverage:

- Structured shared search results are covered by Task 1.
- Existing `debugBookName` compatibility is covered by Task 1.
- Android facade exposure is covered by Task 2.
- Shared/app/APK verification is covered by Task 3.

Placeholder scan:

- No placeholder tokens are present.
- Each code-producing step includes exact code and verification commands.

Type consistency:

- `SearchPageResult.books` uses `List<SharedSearchBook>`.
- Parser keys map to existing `SharedSearchBook` fields: `name`, `author`, `bookUrl`, `origin`, `kind`, `latestChapterTitle`, `intro`, and `coverUrl`.

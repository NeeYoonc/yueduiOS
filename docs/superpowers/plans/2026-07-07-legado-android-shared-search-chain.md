# Legado Android Shared Search Chain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first Android entrypoint that runs the shared search service through Android entity mapping and the Android HTTP fetcher.

**Architecture:** Add a small Android-side facade that accepts existing `BookSource`, converts it with `toSharedBookSource()`, and delegates to `BookSearchService`. The facade takes an injectable `HttpFetcher` for tests and defaults to `AndroidHttpFetcher` for production.

**Tech Stack:** Gradle 8.14.4, Android Gradle Plugin 8.13.2, Kotlin 2.3.10, OkHttp 5.3.2, kotlinx.coroutines, JUnit 4.

---

## File Structure

- Create `app/src/main/java/io/legado/app/shared/AndroidSharedBookSearch.kt`: Android facade for the shared search service.
- Create `app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt`: JVM tests covering the end-to-end adapter chain with a fake OkHttp interceptor.

---

### Task 1: Add Android Shared Search Facade

**Files:**
- Create: `app/src/main/java/io/legado/app/shared/AndroidSharedBookSearch.kt`
- Test: `app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt`

- [ ] **Step 1: Write the failing search-chain tests**

Create `app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt`:

```kotlin
package io.legado.app.shared

import io.legado.app.data.entities.BookSource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidSharedBookSearchTest {

    @Test
    fun searchesAndroidBookSourceThroughSharedServiceAndAndroidHttp() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals(
                    "https://source.test/search?q=metal%20max&page=3",
                    request.url.toString()
                )
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """
                        name=Iron Road
                        author=Tester
                        url=https://source.test/book/1
                        """.trimIndent().toResponseBody("text/plain".toMediaType())
                    )
                    .build()
            }
            .build()
        val search = AndroidSharedBookSearch(AndroidHttpFetcher(client))
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            bookSourceGroup = "Group",
            searchUrl = "https://source.test/search?q={{key}}&page={{page}}"
        )

        val result = search.search(source, key = "metal max", page = 3)

        assertEquals("https://source.test", result.source.bookSourceUrl)
        assertEquals("Source", result.source.bookSourceName)
        assertEquals("Group", result.source.bookSourceGroup)
        assertEquals("Iron Road", result.debugBookName)
        assertEquals(200, result.response.statusCode)
    }

    @Test
    fun rejectsAndroidBookSourceWithoutSearchUrl() {
        val search = AndroidSharedBookSearch(
            AndroidHttpFetcher(OkHttpClient.Builder().build())
        )
        val source = BookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                search.search(source, key = "metal")
            }
        }

        assertEquals("Book source Source has no searchUrl", error.message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedBookSearchTest"
```

Expected: FAIL with unresolved reference `AndroidSharedBookSearch`.

- [ ] **Step 3: Add the facade implementation**

Create `app/src/main/java/io/legado/app/shared/AndroidSharedBookSearch.kt`:

```kotlin
package io.legado.app.shared

import io.legado.app.data.entities.BookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.service.BookSearchService
import io.legado.shared.service.SearchPageResult

class AndroidSharedBookSearch(
    httpFetcher: HttpFetcher = AndroidHttpFetcher()
) {
    private val searchService = BookSearchService(httpFetcher)

    suspend fun search(source: BookSource, key: String, page: Int = 1): SearchPageResult {
        return searchService.search(source.toSharedBookSource(), key, page)
    }
}
```

- [ ] **Step 4: Run search-chain tests**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidSharedBookSearchTest"
```

Expected: PASS with 2 tests.

- [ ] **Step 5: Commit if this directory is inside Git**

```powershell
if (Test-Path .git) {
  git add app/src/main/java/io/legado/app/shared/AndroidSharedBookSearch.kt app/src/test/java/io/legado/app/shared/AndroidSharedBookSearchTest.kt
  git commit -m "feat: add android shared search chain"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 2: Verify Shared Search Integration

**Files:**
- Read: all files touched by Task 1.

- [ ] **Step 1: Run app shared adapter tests**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"
```

Expected: PASS.

- [ ] **Step 2: Run shared JVM tests**

```powershell
cd D:\games\AI\legado-beta-ascii
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :shared:jvmTest
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

Expected in this extracted project: file list for shared adapter files.

---

## Self-Review

Spec coverage:

- Android `BookSource` to shared model handoff is covered by Task 1.
- Shared `BookSearchService` delegation is covered by Task 1.
- Android `AndroidHttpFetcher` integration is covered by Task 1.
- Regression verification for app shared adapters, shared tests, and APK build is covered by Task 2.

Placeholder scan:

- No placeholder tokens are present.
- Each code-producing step includes exact code and verification commands.

Type consistency:

- `AndroidSharedBookSearch` depends on `HttpFetcher`, `AndroidHttpFetcher`, `BookSearchService`, and `SearchPageResult`, all of which already exist.
- The test body format matches the current `BookSearchService.debugBookName` extraction behavior.

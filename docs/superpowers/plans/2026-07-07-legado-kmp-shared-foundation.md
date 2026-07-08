# Legado KMP Shared Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first testable Kotlin Multiplatform `:shared` foundation for Legado's iOS port.

**Architecture:** Create a new `:shared` KMP module with `commonMain`, `androidMain`, `iosMain`, and a host `jvm` test target. Keep Android-only dependencies out of `commonMain`; shared code starts with serializable source/book models, platform ports, JSON import, and a minimal service contract that can be tested with fake HTTP.

**Tech Stack:** Gradle 8.14.4, Android Gradle Plugin 8.13.2, Kotlin 2.3.10, Kotlin Multiplatform, kotlinx.coroutines 1.10.2, kotlinx.serialization-json 1.10.0.

---

## File Structure

- Modify `settings.gradle`: include the new `:shared` module.
- Modify `build.gradle`: register Kotlin Multiplatform and serialization plugins at the root.
- Modify `gradle/libs.versions.toml`: add KMP plugin alias and serialization JSON dependency.
- Create `shared/build.gradle.kts`: configure Android, iOS, and JVM targets.
- Create `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`: shared serializable data models.
- Create `shared/src/commonMain/kotlin/io/legado/shared/platform/Ports.kt`: platform boundary interfaces.
- Create `shared/src/commonMain/kotlin/io/legado/shared/source/SourceJsonImporter.kt`: source JSON import helper.
- Create `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`: first service contract using `HttpFetcher`.
- Create `shared/src/commonTest/kotlin/io/legado/shared/model/SharedModelsTest.kt`: model serialization tests.
- Create `shared/src/commonTest/kotlin/io/legado/shared/platform/PortsContractTest.kt`: fake port contract tests.
- Create `shared/src/commonTest/kotlin/io/legado/shared/source/SourceJsonImporterTest.kt`: source importer tests.
- Create `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt`: fake HTTP search service test.

This plan intentionally covers the first independent implementation slice. Follow-up plans will migrate real rule parsing, Android mappers, and the SwiftUI iOS prototype.

---

### Task 1: Wire the `:shared` KMP Module

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Modify: `gradle/libs.versions.toml`
- Create: `shared/build.gradle.kts`

- [ ] **Step 1: Verify the module does not exist yet**

Run:

```powershell
.\gradlew.bat :shared:tasks
```

Expected: FAIL with text like `Project 'shared' not found`.

- [ ] **Step 2: Add version catalog entries**

In `gradle/libs.versions.toml`, set the serialization version and add the library and plugin aliases:

```toml
[versions]
kotlinxSerialization = "1.10.0"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

Keep the existing `kotlin-serialization` plugin alias.

- [ ] **Step 3: Register root plugins**

In the root `build.gradle` `plugins` block, add:

```groovy
alias libs.plugins.kotlin.multiplatform apply false
alias libs.plugins.kotlin.serialization apply false
```

- [ ] **Step 4: Include the shared module**

Append this line to `settings.gradle`:

```groovy
include ':shared'
```

- [ ] **Step 5: Create the shared Gradle build file**

Create `shared/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "io.legado.shared"
    compileSdk = rootProject.extra["compile_sdk_version"] as Int

    defaultConfig {
        minSdk = 23
    }
}
```

- [ ] **Step 6: Verify Gradle sees the module**

Run:

```powershell
.\gradlew.bat :shared:tasks --all
```

Expected: PASS and output includes `jvmTest`, `compileKotlinAndroid`, and iOS target tasks.

- [ ] **Step 7: Commit if this directory is inside Git**

Run:

```powershell
if (Test-Path .git) {
  git add settings.gradle build.gradle gradle/libs.versions.toml shared/build.gradle.kts
  git commit -m "build: add shared multiplatform module"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 2: Add Shared Models

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/model/SharedModelsTest.kt`

- [ ] **Step 1: Write the failing model serialization test**

Create `shared/src/commonTest/kotlin/io/legado/shared/model/SharedModelsTest.kt`:

```kotlin
package io.legado.shared.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun decodesBookSourceUsingLegadoFieldNames() {
        val source = json.decodeFromString<SharedBookSource>(
            """
            {
              "bookSourceUrl": "https://example.test",
              "bookSourceName": "Example Source",
              "bookSourceGroup": "text",
              "searchUrl": "https://example.test/search?q={{key}}",
              "ruleSearch": {
                "bookList": ".result",
                "name": ".name",
                "author": ".author",
                "bookUrl": "a@href"
              }
            }
            """.trimIndent()
        )

        assertEquals("https://example.test", source.bookSourceUrl)
        assertEquals("Example Source", source.bookSourceName)
        assertEquals(".result", source.ruleSearch?.bookList)
        assertEquals("a@href", source.ruleSearch?.bookUrl)
    }

    @Test
    fun encodesChapterWithoutPlatformAnnotations() {
        val encoded = json.encodeToString(
            SharedBookChapter(
                title = "Chapter 1",
                url = "https://example.test/chapter-1",
                index = 0
            )
        )

        assertTrue(encoded.contains("Chapter 1"))
        assertTrue(encoded.contains("chapter-1"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.model.SharedModelsTest"
```

Expected: FAIL with unresolved references such as `SharedBookSource` and `SharedBookChapter`.

- [ ] **Step 3: Add serializable shared models**

Create `shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt`:

```kotlin
package io.legado.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedBookSource(
    val bookSourceUrl: String = "",
    val bookSourceName: String = "",
    val bookSourceGroup: String? = null,
    val bookSourceType: Int = 0,
    val bookUrlPattern: String? = null,
    val enabled: Boolean = true,
    val enabledExplore: Boolean = true,
    val enabledCookieJar: Boolean? = true,
    val concurrentRate: String? = null,
    val header: String? = null,
    val loginUrl: String? = null,
    val loginUi: String? = null,
    val loginCheckJs: String? = null,
    val coverDecodeJs: String? = null,
    val bookSourceComment: String? = null,
    val variableComment: String? = null,
    val exploreUrl: String? = null,
    val searchUrl: String? = null,
    val ruleSearch: SharedSearchRule? = null,
    val ruleBookInfo: SharedBookInfoRule? = null,
    val ruleToc: SharedTocRule? = null,
    val ruleContent: SharedContentRule? = null
) {
    val key: String
        get() = bookSourceUrl

    val displayName: String
        get() = if (bookSourceGroup.isNullOrBlank()) {
            bookSourceName
        } else {
            "$bookSourceName ($bookSourceGroup)"
        }
}

@Serializable
data class SharedSearchRule(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val kind: String? = null,
    val lastChapter: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val bookUrl: String? = null,
    val checkKeyWord: String? = null
)

@Serializable
data class SharedBookInfoRule(
    val name: String? = null,
    val author: String? = null,
    val kind: String? = null,
    val lastChapter: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val tocUrl: String? = null
)

@Serializable
data class SharedTocRule(
    val chapterList: String? = null,
    val chapterName: String? = null,
    val chapterUrl: String? = null,
    val nextTocUrl: String? = null,
    val updateTime: String? = null,
    val isVip: String? = null,
    val isPay: String? = null,
    val preUpdateJs: String? = null
)

@Serializable
data class SharedContentRule(
    val content: String? = null,
    val nextContentUrl: String? = null,
    val replaceRegex: String? = null,
    val webJs: String? = null,
    val sourceRegex: String? = null
)

@Serializable
data class SharedBook(
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val tocUrl: String = bookUrl,
    val origin: String = "",
    val kind: String? = null,
    val latestChapterTitle: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val variableMap: Map<String, String> = emptyMap()
)

@Serializable
data class SharedSearchBook(
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val origin: String = "",
    val kind: String? = null,
    val latestChapterTitle: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null
) {
    fun toBook(): SharedBook = SharedBook(
        name = name,
        author = author,
        bookUrl = bookUrl,
        tocUrl = bookUrl,
        origin = origin,
        kind = kind,
        latestChapterTitle = latestChapterTitle,
        intro = intro,
        coverUrl = coverUrl
    )
}

@Serializable
data class SharedBookChapter(
    val title: String = "",
    val url: String = "",
    val index: Int = 0,
    val isVolume: Boolean = false,
    val isVip: Boolean = false,
    val isPay: Boolean = false,
    val tag: String? = null,
    val variableMap: Map<String, String> = emptyMap()
)
```

- [ ] **Step 4: Run the model tests**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.model.SharedModelsTest"
```

Expected: PASS with 2 tests.

- [ ] **Step 5: Commit if this directory is inside Git**

Run:

```powershell
if (Test-Path .git) {
  git add shared/src/commonMain/kotlin/io/legado/shared/model/SharedModels.kt shared/src/commonTest/kotlin/io/legado/shared/model/SharedModelsTest.kt
  git commit -m "feat: add shared reading models"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 3: Add Platform Ports

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/platform/Ports.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/platform/PortsContractTest.kt`

- [ ] **Step 1: Write the failing port contract test**

Create `shared/src/commonTest/kotlin/io/legado/shared/platform/PortsContractTest.kt`:

```kotlin
package io.legado.shared.platform

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PortsContractTest {
    @Test
    fun fetcherReceivesRequestAndReturnsResponse() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://example.test/search?q=book", request.url)
                assertEquals(SharedHttpMethod.GET, request.method)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    headers = mapOf("Content-Type" to "text/html"),
                    body = "<html>ok</html>"
                )
            }
        }

        val response = fetcher.fetch(
            SharedHttpRequest(url = "https://example.test/search?q=book")
        )

        assertEquals(200, response.statusCode)
        assertEquals("<html>ok</html>", response.body)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.platform.PortsContractTest"
```

Expected: FAIL with unresolved references such as `HttpFetcher` and `SharedHttpRequest`.

- [ ] **Step 3: Add platform port interfaces**

Create `shared/src/commonMain/kotlin/io/legado/shared/platform/Ports.kt`:

```kotlin
package io.legado.shared.platform

enum class SharedHttpMethod {
    GET,
    POST,
    HEAD
}

data class SharedHttpRequest(
    val url: String,
    val method: SharedHttpMethod = SharedHttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val connectTimeoutMillis: Long? = null,
    val readTimeoutMillis: Long? = null
)

data class SharedHttpResponse(
    val finalUrl: String,
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String
) {
    val isSuccess: Boolean
        get() = statusCode in 200..299
}

interface HttpFetcher {
    suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse
}

interface ScriptRuntime {
    suspend fun evaluate(script: String, bindings: Map<String, Any?> = emptyMap()): Any?
}

interface CookieStorePort {
    fun getCookie(url: String): String?
    fun putCookie(url: String, cookie: String)
}

interface CacheStorePort {
    fun getText(key: String): String?
    fun putText(key: String, value: String)
}

interface RuleLogger {
    fun log(sourceKey: String?, message: String, error: Throwable? = null)
}

object NoopRuleLogger : RuleLogger {
    override fun log(sourceKey: String?, message: String, error: Throwable?) = Unit
}
```

- [ ] **Step 4: Run the port tests**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.platform.PortsContractTest"
```

Expected: PASS with 1 test.

- [ ] **Step 5: Commit if this directory is inside Git**

Run:

```powershell
if (Test-Path .git) {
  git add shared/src/commonMain/kotlin/io/legado/shared/platform/Ports.kt shared/src/commonTest/kotlin/io/legado/shared/platform/PortsContractTest.kt
  git commit -m "feat: add shared platform ports"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 4: Add Source JSON Import

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/source/SourceJsonImporter.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/source/SourceJsonImporterTest.kt`

- [ ] **Step 1: Write the failing importer tests**

Create `shared/src/commonTest/kotlin/io/legado/shared/source/SourceJsonImporterTest.kt`:

```kotlin
package io.legado.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceJsonImporterTest {
    @Test
    fun importsSingleSourceObject() {
        val sources = SourceJsonImporter.importBookSources(
            """
            {
              "bookSourceUrl": "https://example.test",
              "bookSourceName": "Example",
              "searchUrl": "https://example.test/search?q={{key}}"
            }
            """.trimIndent()
        )

        assertEquals(1, sources.size)
        assertEquals("https://example.test", sources.single().bookSourceUrl)
        assertEquals("Example", sources.single().bookSourceName)
    }

    @Test
    fun importsSourceArrayAndDropsBlankEntries() {
        val sources = SourceJsonImporter.importBookSources(
            """
            [
              {
                "bookSourceUrl": "https://one.test",
                "bookSourceName": "One"
              },
              {
                "bookSourceUrl": "",
                "bookSourceName": "Broken"
              }
            ]
            """.trimIndent()
        )

        assertEquals(1, sources.size)
        assertEquals("One", sources.single().bookSourceName)
    }

    @Test
    fun rejectsJsonThatDoesNotContainUsableSources() {
        assertFailsWith<IllegalArgumentException> {
            SourceJsonImporter.importBookSources("""{"bookSourceName":"Missing URL"}""")
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.source.SourceJsonImporterTest"
```

Expected: FAIL with unresolved reference `SourceJsonImporter`.

- [ ] **Step 3: Add the importer**

Create `shared/src/commonMain/kotlin/io/legado/shared/source/SourceJsonImporter.kt`:

```kotlin
package io.legado.shared.source

import io.legado.shared.model.SharedBookSource
import kotlinx.serialization.json.Json

object SourceJsonImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun importBookSources(rawJson: String): List<SharedBookSource> {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Book source JSON is empty" }

        val decoded = if (trimmed.startsWith("[")) {
            json.decodeFromString<List<SharedBookSource>>(trimmed)
        } else {
            listOf(json.decodeFromString<SharedBookSource>(trimmed))
        }

        val usable = decoded.filter {
            it.bookSourceUrl.isNotBlank() && it.bookSourceName.isNotBlank()
        }
        require(usable.isNotEmpty()) { "Book source JSON does not contain a usable source" }
        return usable
    }
}
```

- [ ] **Step 4: Run the importer tests**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.source.SourceJsonImporterTest"
```

Expected: PASS with 3 tests.

- [ ] **Step 5: Commit if this directory is inside Git**

Run:

```powershell
if (Test-Path .git) {
  git add shared/src/commonMain/kotlin/io/legado/shared/source/SourceJsonImporter.kt shared/src/commonTest/kotlin/io/legado/shared/source/SourceJsonImporterTest.kt
  git commit -m "feat: import shared book sources"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 5: Add the First Search Service Contract

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt`

- [ ] **Step 1: Write the failing service test**

Create `shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt`:

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
                        url=https://example.test/book/1
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
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.service.BookSearchServiceTest"
```

Expected: FAIL with unresolved references such as `BookSearchService`.

- [ ] **Step 3: Add the minimal search service contract**

Create `shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt`:

```kotlin
package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
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
    val debugBookName: String?
)
```

- [ ] **Step 4: Run the service test**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests "io.legado.shared.service.BookSearchServiceTest"
```

Expected: PASS with 1 test.

- [ ] **Step 5: Commit if this directory is inside Git**

Run:

```powershell
if (Test-Path .git) {
  git add shared/src/commonMain/kotlin/io/legado/shared/service/BookSearchService.kt shared/src/commonTest/kotlin/io/legado/shared/service/BookSearchServiceTest.kt
  git commit -m "feat: add shared search service contract"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 6: Run Foundation Verification

**Files:**
- Read: all files touched by Tasks 1-5.

- [ ] **Step 1: Run all shared JVM tests**

Run:

```powershell
.\gradlew.bat :shared:jvmTest
```

Expected: PASS with all shared tests.

- [ ] **Step 2: Compile shared Android target**

Run:

```powershell
.\gradlew.bat :shared:compileDebugKotlinAndroid
```

Expected: PASS.

- [ ] **Step 3: Verify iOS tasks are configured without running Apple compilation on Windows**

Run:

```powershell
.\gradlew.bat :shared:tasks --all | Select-String -Pattern 'iosX64|iosArm64|iosSimulatorArm64'
```

Expected: output includes iOS target task names. Do not run iOS link tasks on Windows; Apple target compilation requires macOS tooling.

- [ ] **Step 4: Compile the existing Android app**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: PASS. If this fails because existing upstream dependencies cannot be downloaded, record the exact dependency or network error before changing code.

- [ ] **Step 5: Record final status**

Run:

```powershell
if (Test-Path .git) {
  git status --short
} else {
  Get-ChildItem -Recurse -Path shared,docs\superpowers | Select-Object FullName,Length | Format-Table -AutoSize
}
```

Expected in this extracted project: file list for `shared` and `docs\superpowers`.

---

## Self-Review

Spec coverage:

- `:shared` module foundation is covered by Task 1.
- Shared model extraction is covered by Task 2.
- Platform boundaries are covered by Task 3.
- Source JSON import starts in Task 4.
- A fake HTTP service contract starts in Task 5.
- Windows-verifiable build and test evidence is covered by Task 6.

Gaps intentionally left for follow-up plans:

- Real Legado rule parsing for CSS/JSON/XPath/Regex.
- Android `BookSource` / `Book` / `BookChapter` mappers.
- Android Rhino and OkHttp adapters.
- iOS JavaScriptCore and Ktor Darwin adapters.
- SwiftUI prototype.

Placeholder scan:

- No placeholder tokens are present.
- Every code-producing step includes concrete code.

Type consistency:

- `SharedBookSource`, `SharedBookChapter`, `HttpFetcher`, `SharedHttpRequest`, and `SharedHttpResponse` are introduced before later tasks reference them.
- `BookSearchService.search` returns `SearchPageResult`, and the test uses that exact type shape.

References:

- Kotlin Multiplatform documentation: https://kotlinlang.org/docs/multiplatform.html
- Kotlin serialization documentation: https://kotlinlang.org/docs/serialization.html
- Ktor client engines documentation: https://ktor.io/docs/client-engines.html

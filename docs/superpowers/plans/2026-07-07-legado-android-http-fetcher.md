# Legado Android HTTP Fetcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Android implementation of the shared `HttpFetcher` port backed by the existing app OkHttp stack.

**Architecture:** Keep shared networking as a platform port in `:shared`, and implement it in `app` as `AndroidHttpFetcher`. The adapter accepts an injected `OkHttpClient` for tests and defaults to the app's configured `okHttpClient` for production, preserving existing interceptors, UA handling, decompression, and Cookie behavior.

**Tech Stack:** Gradle 8.14.4, Android Gradle Plugin 8.13.2, Kotlin 2.3.10, OkHttp 5.3.2, kotlinx.coroutines, JUnit 4.

---

## File Structure

- Create `app/src/main/java/io/legado/app/shared/AndroidHttpFetcher.kt`: Android OkHttp-backed implementation of `io.legado.shared.platform.HttpFetcher`.
- Create `app/src/test/java/io/legado/app/shared/AndroidHttpFetcherTest.kt`: JVM unit tests with a test `OkHttpClient` interceptor.

---

### Task 1: Add Android HTTP Fetcher

**Files:**
- Create: `app/src/main/java/io/legado/app/shared/AndroidHttpFetcher.kt`
- Test: `app/src/test/java/io/legado/app/shared/AndroidHttpFetcherTest.kt`

- [ ] **Step 1: Write the failing fetcher tests**

Create `app/src/test/java/io/legado/app/shared/AndroidHttpFetcherTest.kt`:

```kotlin
package io.legado.app.shared

import io.legado.shared.platform.SharedHttpMethod
import io.legado.shared.platform.SharedHttpRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidHttpFetcherTest {

    @Test
    fun fetchesGetRequestThroughOkHttpClient() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals("https://source.test/search?q=metal", request.url.toString())
                assertEquals("GET", request.method)
                assertEquals("LegadoTest", request.header("User-Agent"))
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "text/html")
                    .body("<html>ok</html>".toResponseBody("text/html".toMediaType()))
                    .build()
            }
            .build()

        val response = AndroidHttpFetcher(client).fetch(
            SharedHttpRequest(
                url = "https://source.test/search?q=metal",
                headers = mapOf("User-Agent" to "LegadoTest")
            )
        )

        assertEquals("https://source.test/search?q=metal", response.finalUrl)
        assertEquals(200, response.statusCode)
        assertEquals("text/html", response.headers["Content-Type"])
        assertEquals("<html>ok</html>", response.body)
    }

    @Test
    fun sendsPostBodyAndMethod() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals("POST", request.method)
                assertEquals("keyword=metal", request.bodyAsText())
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(201)
                    .message("Created")
                    .body("created".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val response = AndroidHttpFetcher(client).fetch(
            SharedHttpRequest(
                url = "https://source.test/search",
                method = SharedHttpMethod.POST,
                body = "keyword=metal"
            )
        )

        assertEquals(201, response.statusCode)
        assertEquals("created", response.body)
    }

    @Test
    fun sendsHeadRequestWithoutReadingBody() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals("HEAD", request.method)
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(204)
                    .message("No Content")
                    .header("X-Source", "head")
                    .body("ignored".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val response = AndroidHttpFetcher(client).fetch(
            SharedHttpRequest(
                url = "https://source.test/ping",
                method = SharedHttpMethod.HEAD
            )
        )

        assertEquals(204, response.statusCode)
        assertEquals("head", response.headers["X-Source"])
        assertEquals("", response.body)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidHttpFetcherTest"
```

Expected: FAIL with unresolved reference `AndroidHttpFetcher`.

- [ ] **Step 3: Add the fetcher implementation**

Create `app/src/main/java/io/legado/app/shared/AndroidHttpFetcher.kt`:

```kotlin
package io.legado.app.shared

import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.newCallResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpMethod
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AndroidHttpFetcher(
    private val client: OkHttpClient = okHttpClient
) : HttpFetcher {

    override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
        val callClient = request.configureClient(client)
        val response = callClient.newCallResponse {
            url(request.url)
            addHeaders(request.headers)
            when (request.method) {
                SharedHttpMethod.GET -> get()
                SharedHttpMethod.POST -> post((request.body ?: "").toRequestBody())
                SharedHttpMethod.HEAD -> head()
            }
        }
        val body = if (request.method == SharedHttpMethod.HEAD) {
            ""
        } else {
            response.body.text()
        }
        return SharedHttpResponse(
            finalUrl = response.request.url.toString(),
            statusCode = response.code,
            headers = response.headers.toMap(),
            body = body
        )
    }

    private fun SharedHttpRequest.configureClient(baseClient: OkHttpClient): OkHttpClient {
        if (connectTimeoutMillis == null && readTimeoutMillis == null) {
            return baseClient
        }
        return baseClient.newBuilder().apply {
            connectTimeoutMillis?.let {
                connectTimeout(it, TimeUnit.MILLISECONDS)
            }
            readTimeoutMillis?.let {
                readTimeout(it, TimeUnit.MILLISECONDS)
            }
        }.build()
    }
}
```

- [ ] **Step 4: Run fetcher tests**

```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'
.\gradlew.bat --no-daemon :app:testAppDebugUnitTest --tests "io.legado.app.shared.AndroidHttpFetcherTest"
```

Expected: PASS with 3 tests.

- [ ] **Step 5: Commit if this directory is inside Git**

```powershell
if (Test-Path .git) {
  git add app/src/main/java/io/legado/app/shared/AndroidHttpFetcher.kt app/src/test/java/io/legado/app/shared/AndroidHttpFetcherTest.kt
  git commit -m "feat: add android shared http fetcher"
} else {
  Write-Output "No git repository; commit skipped"
}
```

Expected in this extracted project: `No git repository; commit skipped`.

---

### Task 2: Verify Shared and App Integration

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

- Android `HttpFetcher` implementation is covered by Task 1.
- GET, POST, HEAD, request headers, response headers, status code, final URL, and response body behavior are covered by Task 1 tests.
- Shared and app integration verification is covered by Task 2.

Placeholder scan:

- No placeholder tokens are present.
- Each code-producing step includes exact code and verification commands.

Type consistency:

- `AndroidHttpFetcher` implements `HttpFetcher`.
- `SharedHttpRequest` and `SharedHttpResponse` fields match `shared/src/commonMain/kotlin/io/legado/shared/platform/Ports.kt`.

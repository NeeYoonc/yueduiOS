package io.legado.app.shared

import io.legado.shared.platform.SharedHttpMethod
import io.legado.shared.platform.SharedHttpRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
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

    private fun okhttp3.Request.bodyAsText(): String {
        val buffer = Buffer()
        body?.writeTo(buffer)
        return buffer.readUtf8()
    }
}

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
                SharedHttpMethod.PUT -> put((request.body ?: "").toRequestBody())
                SharedHttpMethod.DELETE -> {
                    val requestBody = request.body
                    if (requestBody == null) {
                        delete()
                    } else {
                        method("DELETE", requestBody.toRequestBody())
                    }
                }
                SharedHttpMethod.PROPFIND -> method("PROPFIND", (request.body ?: "").toRequestBody())
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

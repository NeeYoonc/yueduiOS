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

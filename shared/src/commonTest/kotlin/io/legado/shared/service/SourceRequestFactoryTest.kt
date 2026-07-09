package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.CookieStorePort
import io.legado.shared.platform.SharedHttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceRequestFactoryTest {
    @Test
    fun mergesSourceHeadersRequestHeadersAndStoredCookies() {
        val cookies = InMemoryCookieStore().apply {
            putCookie("https://source.test", "sid=stored")
        }
        val factory = SourceRequestFactory(cookies)
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            header = """{"User-Agent":"SourceUA","X-Source":"base"}""",
            enabledCookieJar = true
        )

        val request = factory.build(
            source = source,
            template = """https://source.test/search,{"headers":{"X-Source":"request","Accept":"application/json"}}"""
        )

        assertEquals("SourceUA", request.headers.value("User-Agent"))
        assertEquals("request", request.headers.value("X-Source"))
        assertEquals("application/json", request.headers.value("Accept"))
        assertEquals("sid=stored", request.headers.value("Cookie"))
    }

    @Test
    fun storesSetCookieResponseForTheSourceCookieJar() {
        val cookies = InMemoryCookieStore()
        val factory = SourceRequestFactory(cookies)
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            enabledCookieJar = true
        )

        factory.storeResponseCookies(
            source = source,
            response = SharedHttpResponse(
                finalUrl = "https://source.test/search",
                statusCode = 200,
                headers = mapOf("Set-Cookie" to "token=abc; Path=/; HttpOnly"),
                body = ""
            )
        )

        assertEquals("token=abc", cookies.getCookie("https://source.test"))
    }

    private fun Map<String, String>.value(name: String): String? {
        return entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private class InMemoryCookieStore : CookieStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getCookie(url: String): String? = values[url]

        override fun putCookie(url: String, cookie: String) {
            values[url] = cookie
        }
    }
}

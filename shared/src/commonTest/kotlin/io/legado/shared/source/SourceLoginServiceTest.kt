package io.legado.shared.source

import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.CookieStorePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceLoginServiceTest {
    @Test
    fun buildsWebLoginRequestFromRelativeLoginUrlWithHeadersAndCookies() {
        val cookies = InMemoryCookieStore().apply {
            putCookie("https://login-source.test/books", "sid=stored")
        }
        val service = SourceLoginService(cookies)
        val source = SharedBookSource(
            bookSourceUrl = "https://login-source.test/books",
            bookSourceName = "Login Source",
            header = """{"User-Agent":"LoginUA"}""",
            loginUrl = "/account/login",
            enabledCookieJar = true
        )

        val request = service.buildWebLoginRequest(source)

        assertEquals("https://login-source.test/account/login", request?.url)
        assertEquals("LoginUA", request?.headers?.value("User-Agent"))
        assertEquals("sid=stored", request?.headers?.value("Cookie"))
    }

    @Test
    fun skipsJavascriptAndStructuredLoginRulesForWebLogin() {
        val service = SourceLoginService(InMemoryCookieStore())

        assertNull(service.buildWebLoginRequest(SharedBookSource(loginUrl = "@js:login()")))
        assertNull(service.buildWebLoginRequest(SharedBookSource(loginUrl = "https://source.test/login", loginUi = "[]")))
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

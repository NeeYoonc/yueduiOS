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

    @Test
    fun parsesStructuredLoginUiAndBuildsDefaultLoginInfoJson() {
        val service = SourceLoginService(InMemoryCookieStore())
        val source = SharedBookSource(
            bookSourceUrl = "https://login-form.test",
            bookSourceName = "Login Form",
            loginUrl = "function login() {}",
            loginUi = """
            [
              {"name":"telephone","type":"text"},
              {"name":"password","type":"password","default":"123456"},
              {"name":"endpoint","type":"select","chars":["A","B"],"default":"B"},
              {"name":"remember","type":"toggle","chars":["yes","no"]},
              {"name":"register","type":"button","action":"https://login-form.test/register"}
            ]
            """.trimIndent()
        )

        val fields = service.loadLoginUiFields(source)
        val defaults = service.defaultLoginInfoJson(source)

        assertEquals(listOf("telephone", "password", "endpoint", "remember", "register"), fields.map { it.name })
        assertEquals(listOf("text", "password", "select", "toggle", "button"), fields.map { it.type })
        assertEquals(listOf("A", "B"), fields[2].chars)
        assertEquals(
            mapOf(
                "telephone" to "",
                "password" to "123456",
                "endpoint" to "B",
                "remember" to "yes"
            ),
            service.decodeLoginInfoJson(defaults)
        )
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

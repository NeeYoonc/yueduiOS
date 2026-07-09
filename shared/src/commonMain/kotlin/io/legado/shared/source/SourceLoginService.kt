package io.legado.shared.source

import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.CookieStorePort
import io.legado.shared.service.SharedUrlResolver
import io.legado.shared.service.SourceRequestFactory

data class SharedSourceLoginRequest(
    val url: String = "",
    val headers: Map<String, String> = emptyMap()
)

class SourceLoginService(
    cookieStore: CookieStorePort? = null
) {
    private val requestFactory = SourceRequestFactory(cookieStore)

    fun buildWebLoginRequest(source: SharedBookSource): SharedSourceLoginRequest? {
        val loginUrl = source.loginUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        if (!source.loginUi.isNullOrBlank() || loginUrl.isScriptRule()) {
            return null
        }
        val resolvedUrl = SharedUrlResolver.resolve(source.bookSourceUrl, loginUrl)
        val request = requestFactory.build(source, resolvedUrl)
        return SharedSourceLoginRequest(
            url = request.url,
            headers = request.headers
        )
    }

    private fun String.isScriptRule(): Boolean {
        return startsWith("@js:", ignoreCase = true) || startsWith("<js>", ignoreCase = true)
    }
}

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

package io.legado.app.shared

import io.legado.app.help.http.BackstageWebView
import io.legado.shared.rule.RuleWebViewRequest
import io.legado.shared.rule.RuleWebViewRuntime

class AndroidWebViewRuleRuntime : RuleWebViewRuntime {
    override suspend fun evaluate(request: RuleWebViewRequest): String {
        return BackstageWebView(
            url = request.baseUrl,
            html = request.html,
            tag = request.sourceKey,
            headerMap = request.headers.takeIf { it.isNotEmpty() }?.let(::HashMap),
            javaScript = request.script,
            timeout = request.timeoutMillis,
            isRule = true
        ).getStrResponse().body.orEmpty()
    }
}

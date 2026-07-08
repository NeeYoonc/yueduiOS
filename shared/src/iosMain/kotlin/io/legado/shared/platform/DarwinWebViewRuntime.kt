package io.legado.shared.platform

import io.legado.shared.rule.RuleWebViewRequest
import io.legado.shared.rule.RuleWebViewRuntime
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DarwinWebViewRuntime : RuleWebViewRuntime {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun evaluate(request: RuleWebViewRequest): String {
        return withTimeout(request.timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                var webView: WKWebView? = null
                var navigationDelegate: RuleNavigationDelegate? = null
                fun cleanup() {
                    webView?.navigationDelegate = null
                    navigationDelegate = null
                    webView = null
                }

                dispatch_async(dispatch_get_main_queue()) {
                    val view = WKWebView(
                        frame = CGRectMake(0.0, 0.0, 1.0, 1.0),
                        configuration = WKWebViewConfiguration()
                    )
                    webView = view
                    val delegate = RuleNavigationDelegate(
                        onFinish = {
                            view.evaluateJavaScript(request.script) { result, error ->
                                if (!continuation.isActive) {
                                    cleanup()
                                    return@evaluateJavaScript
                                }
                                if (error != null) {
                                    continuation.resumeWithException(error.toException())
                                } else {
                                    continuation.resume(result?.toString().orEmpty())
                                }
                                cleanup()
                            }
                        },
                        onFailure = { error ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(error.toException())
                            }
                            cleanup()
                        }
                    )
                    navigationDelegate = delegate
                    view.navigationDelegate = delegate
                    view.loadHTMLString(
                        string = request.html,
                        baseURL = request.baseUrl?.let { NSURL.URLWithString(it) }
                    )
                }

                continuation.invokeOnCancellation {
                    dispatch_async(dispatch_get_main_queue()) {
                        webView?.stopLoading()
                        cleanup()
                    }
                }
            }
        }
    }

    private class RuleNavigationDelegate(
        private val onFinish: () -> Unit,
        private val onFailure: (NSError) -> Unit
    ) : NSObject(), WKNavigationDelegateProtocol {
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            onFinish()
        }

        @ObjCSignatureOverride
        override fun webView(
            webView: WKWebView,
            didFailNavigation: WKNavigation?,
            withError: NSError
        ) {
            onFailure(withError)
        }

        @ObjCSignatureOverride
        override fun webView(
            webView: WKWebView,
            didFailProvisionalNavigation: WKNavigation?,
            withError: NSError
        ) {
            onFailure(withError)
        }
    }

    private fun NSError.toException(): Throwable {
        val message = localizedDescription.takeIf { it.isNotBlank() }
            ?: "WKWebView rule evaluation failed"
        return IllegalStateException(message)
    }
}

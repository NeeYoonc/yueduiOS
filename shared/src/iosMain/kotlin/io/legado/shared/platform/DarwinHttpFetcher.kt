package io.legado.shared.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class DarwinHttpFetcher(
    private val session: NSURLSession = NSURLSession.sharedSession
) : HttpFetcher {

    override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
        val url = NSURL.URLWithString(request.url)
            ?: throw IllegalArgumentException("Invalid URL: ${request.url}")
        val urlRequest = NSMutableURLRequest.requestWithURL(url).apply {
            setHTTPMethod(request.method.name)
            request.headers.forEach { (name, value) ->
                setValue(value, forHTTPHeaderField = name)
            }
            request.body?.let { body ->
                setHTTPBody(body.toNSData())
            }
            request.timeoutSeconds()?.let { timeoutSeconds ->
                setTimeoutInterval(timeoutSeconds)
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val task = session.dataTaskWithRequest(
                request = urlRequest,
                completionHandler = { data, response, error ->
                if (error != null) {
                    continuation.resumeWithException(error.toException())
                    return@dataTaskWithRequest
                }
                val httpResponse = response as? NSHTTPURLResponse
                val body = if (request.method == SharedHttpMethod.HEAD) {
                    ""
                } else {
                    data?.toUtf8String().orEmpty()
                }
                continuation.resume(
                    SharedHttpResponse(
                        finalUrl = response.finalUrlOr(request.url),
                        statusCode = httpResponse?.statusCode?.toInt() ?: 0,
                        headers = httpResponse.headersMap(),
                        body = body
                    )
                )
                }
            )
            continuation.invokeOnCancellation {
                task.cancel()
            }
            task.resume()
        }
    }

    private fun SharedHttpRequest.timeoutSeconds(): Double? {
        val timeoutMillis = readTimeoutMillis ?: connectTimeoutMillis ?: return null
        return timeoutMillis.toDouble() / 1000.0
    }

    private fun String.toNSData(): NSData {
        val bytes = encodeToByteArray()
        if (bytes.isEmpty()) {
            return NSData()
        }
        return bytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = bytes.size.toULong()
            )
        }
    }

    private fun NSData.toUtf8String(): String {
        return NSString.create(
            data = this,
            encoding = NSUTF8StringEncoding
        )?.toString().orEmpty()
    }

    private fun NSURLResponse?.finalUrlOr(fallback: String): String {
        return this?.URL?.absoluteString ?: fallback
    }

    private fun NSHTTPURLResponse?.headersMap(): Map<String, String> {
        return this?.allHeaderFields
            ?.entries
            ?.associate { (key, value) -> key.toString() to value.toString() }
            .orEmpty()
    }

    private fun NSError.toException(): Throwable {
        val message = localizedDescription.takeIf { it.isNotBlank() }
            ?: "NSURLSession request failed"
        return IllegalStateException(message)
    }
}

package io.legado.shared.backup

import io.legado.shared.model.SharedServer
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpMethod
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class WebDavBackupService(
    private val httpFetcher: HttpFetcher
) {
    suspend fun uploadBackup(
        server: SharedServer,
        fileName: String,
        backupJson: String
    ): SharedHttpResponse {
        val config = server.webDavConfig()
        return httpFetcher.fetch(
            SharedHttpRequest(
                url = config.resolve(fileName),
                method = SharedHttpMethod.PUT,
                headers = config.authHeaders() + ("Content-Type" to "application/json; charset=utf-8"),
                body = backupJson
            )
        )
    }

    suspend fun downloadBackup(
        server: SharedServer,
        fileName: String
    ): String {
        val config = server.webDavConfig()
        val response = httpFetcher.fetch(
            SharedHttpRequest(
                url = config.resolve(fileName),
                method = SharedHttpMethod.GET,
                headers = config.authHeaders()
            )
        )
        require(response.isSuccess) { "WebDAV download failed: HTTP ${response.statusCode}" }
        return response.body
    }

    private fun SharedServer.webDavConfig(): WebDavConfig {
        require(type.equals("WEBDAV", ignoreCase = true)) { "Server is not a WebDAV server" }
        val rawConfig = config?.trim().orEmpty()
        require(rawConfig.isNotEmpty()) { "WebDAV server config is empty" }
        val jsonObject = json.parseToJsonElement(rawConfig) as? JsonObject
            ?: throw IllegalArgumentException("WebDAV server config must be a JSON object")
        val url = jsonObject.string("url")
            ?: jsonObject.string("host")
            ?: throw IllegalArgumentException("WebDAV server config requires url")
        return WebDavConfig(
            baseUrl = url,
            username = jsonObject.string("username") ?: jsonObject.string("user"),
            password = jsonObject.string("password") ?: jsonObject.string("pass")
        )
    }

    private data class WebDavConfig(
        val baseUrl: String,
        val username: String?,
        val password: String?
    ) {
        fun resolve(fileName: String): String {
            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            return base + fileName.split("/")
                .filter { it.isNotBlank() }
                .joinToString("/") { encodePathSegment(it) }
        }

        fun authHeaders(): Map<String, String> {
            val user = username?.takeIf { it.isNotEmpty() } ?: return emptyMap()
            val pass = password.orEmpty()
            return mapOf("Authorization" to "Basic ${"$user:$pass".base64()}")
        }

        private fun encodePathSegment(value: String): String {
            return value.encodeToByteArray().joinToString("") { byte ->
                val code = byte.toInt() and 0xff
                val char = code.toChar()
                when {
                    char in 'a'..'z' -> char.toString()
                    char in 'A'..'Z' -> char.toString()
                    char in '0'..'9' -> char.toString()
                    char == '-' || char == '_' || char == '.' || char == '~' -> char.toString()
                    else -> "%" + code.toString(16).uppercase().padStart(2, '0')
                }
            }
        }

        private fun String.base64(): String {
            val bytes = encodeToByteArray()
            if (bytes.isEmpty()) {
                return ""
            }
            val out = StringBuilder(((bytes.size + 2) / 3) * 4)
            var index = 0
            while (index < bytes.size) {
                val b0 = bytes[index++].toInt() and 0xff
                val b1 = if (index < bytes.size) bytes[index++].toInt() and 0xff else -1
                val b2 = if (index < bytes.size) bytes[index++].toInt() and 0xff else -1
                out.append(base64Alphabet[b0 shr 2])
                out.append(base64Alphabet[((b0 and 0x03) shl 4) or ((if (b1 >= 0) b1 else 0) shr 4)])
                out.append(if (b1 >= 0) base64Alphabet[((b1 and 0x0f) shl 2) or ((if (b2 >= 0) b2 else 0) shr 6)] else '=')
                out.append(if (b2 >= 0) base64Alphabet[b2 and 0x3f] else '=')
            }
            return out.toString()
        }

        private companion object {
            const val base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        }
    }

    private fun JsonObject.string(name: String): String? {
        return get(name)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

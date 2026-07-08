package io.legado.shared.backup

import io.legado.shared.model.SharedServer
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpMethod
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class WebDavBackupServiceTest {
    @Test
    fun uploadsBackupJsonWithBasicAuth() = runBlocking {
        val fetcher = RecordingFetcher(SharedHttpResponse("https://dav.test/root/backup.json", 201, body = ""))
        val service = WebDavBackupService(fetcher)

        val response = service.uploadBackup(
            server = server(),
            fileName = "backup.json",
            backupJson = """{"schemaVersion":1}"""
        )

        assertEquals(201, response.statusCode)
        assertEquals("https://dav.test/root/backup.json", fetcher.request.url)
        assertEquals(SharedHttpMethod.PUT, fetcher.request.method)
        assertEquals("""{"schemaVersion":1}""", fetcher.request.body)
        assertEquals("application/json; charset=utf-8", fetcher.request.headers["Content-Type"])
        assertEquals("Basic dXNlcjpwYXNz", fetcher.request.headers["Authorization"])
    }

    @Test
    fun downloadsBackupJson() = runBlocking {
        val fetcher = RecordingFetcher(
            SharedHttpResponse(
                finalUrl = "https://dav.test/root/backup%202.json",
                statusCode = 200,
                body = """{"schemaVersion":1}"""
            )
        )
        val service = WebDavBackupService(fetcher)

        val json = service.downloadBackup(server(), "backup 2.json")

        assertEquals("""{"schemaVersion":1}""", json)
        assertEquals("https://dav.test/root/backup%202.json", fetcher.request.url)
        assertEquals(SharedHttpMethod.GET, fetcher.request.method)
    }

    private fun server(): SharedServer {
        return SharedServer(
            id = 1,
            name = "WebDAV",
            type = "WEBDAV",
            config = """{"url":"https://dav.test/root/","username":"user","password":"pass"}"""
        )
    }

    private class RecordingFetcher(
        private val response: SharedHttpResponse
    ) : HttpFetcher {
        lateinit var request: SharedHttpRequest

        override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
            this.request = request
            return response
        }
    }
}

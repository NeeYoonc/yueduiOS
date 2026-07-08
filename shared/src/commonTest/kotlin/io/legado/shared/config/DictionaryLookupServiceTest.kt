package io.legado.shared.config

import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DictionaryLookupServiceTest {
    @Test
    fun looksUpEnabledRulesAndParsesJsonShowRule() = runBlocking {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = DictRuleRepository(store)
        repository.importJson(
            """
            [
              {"name":"Json Dict","urlRule":"https://dict.test/lookup?q={{key}}","showRule":"$.definition","enabled":true,"sortNumber":1},
              {"name":"Disabled","urlRule":"https://disabled.test/{{key}}","showRule":"$.definition","enabled":false,"sortNumber":2}
            ]
            """.trimIndent(),
            replace = true
        )
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://dict.test/lookup?q=metal%20max", request.url)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = """{"definition":"tank adventure"}"""
                )
            }
        }
        val service = DictionaryLookupService(fetcher, repository, AnalyzeRuleEngine())

        val results = service.lookup("metal max")

        assertEquals(listOf("Json Dict"), results.map { it.ruleName })
        assertEquals("metal max", results.single().word)
        assertEquals("tank adventure", results.single().content)
        assertEquals("https://dict.test/lookup?q=metal%20max", results.single().url)
        assertEquals(200, results.single().statusCode)
        assertNull(results.single().errorMessage)
    }

    @Test
    fun evaluatesScriptUrlAndScriptShowRuleWithKeyBinding() = runBlocking {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = DictRuleRepository(store)
        repository.importJson(
            """
            {
              "name":"Script Dict",
              "urlRule":"@js:return 'https://dict.test/' + key + ',{\"headers\":{\"X-Key\":\"' + key + '\"}}'",
              "showRule":"@js:return key + ':' + result",
              "enabled":true
            }
            """.trimIndent(),
            replace = true
        )
        val engine = AnalyzeRuleEngine(
            scriptRuntime = object : ScriptRuntime {
                override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
                    val key = bindings["key"].toString()
                    return when {
                        script.contains("https://dict.test") -> """https://dict.test/$key,{"headers":{"X-Key":"$key"}}"""
                        script.contains("key + ':' + result") -> "$key:${bindings["result"]}"
                        else -> error("Unexpected script: $script")
                    }
                }
            }
        )
        val service = DictionaryLookupService(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    assertEquals("https://dict.test/metal", request.url)
                    assertEquals("metal", request.headers["X-Key"])
                    return SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = "definition"
                    )
                }
            },
            dictRuleRepository = repository,
            ruleEngine = engine
        )

        val results = service.lookup("metal")

        assertEquals("metal:definition", results.single().content)
    }

    @Test
    fun keepsRuleErrorsSeparateFromSuccessfulResults() = runBlocking {
        val store = SharedLibraryStore(InMemoryCacheStore())
        val repository = DictRuleRepository(store)
        repository.importJson(
            """
            [
              {"name":"Broken","urlRule":"https://broken.test/{{key}}","enabled":true,"sortNumber":1},
              {"name":"Plain","urlRule":"https://plain.test/{{key}}","enabled":true,"sortNumber":2}
            ]
            """.trimIndent(),
            replace = true
        )
        val service = DictionaryLookupService(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    if (request.url.startsWith("https://broken.test")) {
                        error("network down")
                    }
                    return SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = "<p>plain definition</p>"
                    )
                }
            },
            dictRuleRepository = repository,
            ruleEngine = AnalyzeRuleEngine()
        )

        val results = service.lookup("metal")

        assertEquals(listOf("Broken", "Plain"), results.map { it.ruleName })
        assertEquals("network down", results[0].errorMessage)
        assertEquals("", results[0].content)
        assertEquals("<p>plain definition</p>", results[1].content)
        assertNull(results[1].errorMessage)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

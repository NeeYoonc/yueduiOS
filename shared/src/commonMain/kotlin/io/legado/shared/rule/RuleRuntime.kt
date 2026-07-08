package io.legado.shared.rule

import io.legado.shared.platform.ScriptRuntime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class RuleEvaluationContext(
    val sourceKey: String? = null,
    val baseUrl: String? = null,
    val variables: Map<String, String> = emptyMap()
)

data class RuleWebViewRequest(
    val sourceKey: String? = null,
    val baseUrl: String? = null,
    val html: String,
    val script: String,
    val headers: Map<String, String> = emptyMap(),
    val timeoutMillis: Long = 10000L
)

interface RuleWebViewRuntime {
    suspend fun evaluate(request: RuleWebViewRequest): String
}

class RuleJavaBridge(
    private val variables: MutableMap<String, String>
) {
    fun put(key: String, value: Any?): String {
        val text = value?.toString().orEmpty()
        variables[key] = text
        return text
    }

    fun get(key: String): String {
        return variables[key].orEmpty()
    }
}

class UnsupportedRuleRuntimeException(message: String) : IllegalStateException(message)

class AnalyzeRuleEngine(
    private val scriptRuntime: ScriptRuntime? = null,
    private val webViewRuntime: RuleWebViewRuntime? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val putRuleRegex = Regex("""@put:(\{[^}]+})""", RegexOption.IGNORE_CASE)
    private val getRuleRegex = Regex("""^@get:\{([^}]+)}$""", RegexOption.IGNORE_CASE)

    suspend fun evaluateString(
        content: String,
        rule: String?,
        context: RuleEvaluationContext = RuleEvaluationContext()
    ): String? {
        val trimmed = rule?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val variables = context.variables.toMutableMap()
        val ruleWithoutPut = applyPutRules(content, trimmed, variables)
        val segments = splitExecutableSegments(ruleWithoutPut)
        var result: String? = null
        for (segment in segments) {
            result = when (segment) {
                is RuleSegment.Extract -> evaluateExtractor(
                    content = result ?: content,
                    rule = segment.rule,
                    variables = variables
                )
                is RuleSegment.Script -> evaluateScript(
                    content = result ?: content,
                    script = segment.script,
                    context = context,
                    variables = variables
                )
                is RuleSegment.WebScript -> evaluateWebJs(
                    html = content,
                    script = segment.script,
                    context = context,
                    headers = emptyMap()
                )
            }
        }
        return result
    }

    suspend fun evaluateWebJs(
        html: String,
        script: String?,
        context: RuleEvaluationContext = RuleEvaluationContext(),
        headers: Map<String, String> = emptyMap()
    ): String? {
        val javaScript = script?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val runtime = webViewRuntime
            ?: throw UnsupportedRuleRuntimeException("webJs requires a platform WebView runtime")
        return runtime.evaluate(
            RuleWebViewRequest(
                sourceKey = context.sourceKey,
                baseUrl = context.baseUrl,
                html = html,
                script = javaScript,
                headers = headers
            )
        )
    }

    private suspend fun evaluateScript(
        content: String,
        script: String,
        context: RuleEvaluationContext,
        variables: MutableMap<String, String>
    ): String? {
        val runtime = scriptRuntime
            ?: throw UnsupportedRuleRuntimeException("@js rule requires a platform script runtime")
        return runtime.evaluate(
            script = script,
            bindings = variables + mapOf(
                "java" to RuleJavaBridge(variables),
                "result" to content,
                "baseUrl" to context.baseUrl,
                "sourceKey" to context.sourceKey,
                "variables" to variables
            )
        )?.toString()
    }

    private fun evaluateExtractor(
        content: String,
        rule: String,
        variables: Map<String, String>
    ): String? {
        val trimmed = rule.trim()
        getRuleRegex.matchEntire(trimmed)?.let { match ->
            return variables[match.groupValues[1]].orEmpty()
        }
        return RuleAnalyzer.getString(content, trimmed)
    }

    private fun applyPutRules(
        content: String,
        rule: String,
        variables: MutableMap<String, String>
    ): String {
        var updatedRule = rule
        putRuleRegex.findAll(rule).forEach { match ->
            val putMap = parsePutMap(match.groupValues[1])
            putMap.forEach { (key, valueRule) ->
                variables[key] = RuleAnalyzer.getString(content, valueRule).orEmpty()
            }
            updatedRule = updatedRule.replace(match.value, "")
        }
        return updatedRule.trim()
    }

    private fun parsePutMap(rawJson: String): Map<String, String> {
        val element = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonObject
            ?: return emptyMap()
        return element.jsonObject.entries.associate { (key, value) ->
            key to (runCatching { value.jsonPrimitive.contentOrNull }.getOrNull() ?: value.toString())
        }
    }

    private fun splitExecutableSegments(rule: String): List<RuleSegment> {
        val segments = mutableListOf<RuleSegment>()
        var cursor = 0
        while (cursor < rule.length) {
            val next = nextExecutableMarker(rule, cursor)
            if (next == null) {
                rule.substring(cursor).trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { segments.add(RuleSegment.Extract(it)) }
                break
            }
            if (next.index > cursor) {
                rule.substring(cursor, next.index).trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { segments.add(RuleSegment.Extract(it)) }
            }
            when (next.marker) {
                Marker.JsPrefix -> {
                    segments.add(RuleSegment.Script(rule.substring(next.index + "@js:".length)))
                    cursor = rule.length
                }
                Marker.WebJsPrefix -> {
                    segments.add(RuleSegment.WebScript(rule.substring(next.index + "@webjs:".length)))
                    cursor = rule.length
                }
                Marker.JsTag -> {
                    val scriptStart = next.index + "<js>".length
                    val scriptEnd = rule.indexOf("</js>", scriptStart, ignoreCase = true)
                    if (scriptEnd < 0) {
                        segments.add(RuleSegment.Script(rule.substring(scriptStart)))
                        cursor = rule.length
                    } else {
                        segments.add(RuleSegment.Script(rule.substring(scriptStart, scriptEnd)))
                        cursor = scriptEnd + "</js>".length
                    }
                }
            }
        }
        return segments
    }

    private fun nextExecutableMarker(rule: String, startIndex: Int): MarkerMatch? {
        return listOfNotNull(
            rule.indexOf("@js:", startIndex, ignoreCase = true)
                .takeIf { it >= 0 }
                ?.let { MarkerMatch(it, Marker.JsPrefix) },
            rule.indexOf("@webjs:", startIndex, ignoreCase = true)
                .takeIf { it >= 0 }
                ?.let { MarkerMatch(it, Marker.WebJsPrefix) },
            rule.indexOf("<js>", startIndex, ignoreCase = true)
                .takeIf { it >= 0 }
                ?.let { MarkerMatch(it, Marker.JsTag) }
        ).minByOrNull { it.index }
    }

    private sealed class RuleSegment {
        data class Extract(val rule: String) : RuleSegment()
        data class Script(val script: String) : RuleSegment()
        data class WebScript(val script: String) : RuleSegment()
    }

    private enum class Marker {
        JsPrefix,
        WebJsPrefix,
        JsTag
    }

    private data class MarkerMatch(
        val index: Int,
        val marker: Marker
    )
}

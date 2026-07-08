package io.legado.shared.config

import io.legado.shared.model.SharedDictRule
import io.legado.shared.model.SharedDictionaryLookupResult
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleEvaluationContext
import io.legado.shared.service.SharedRequestBuilder

class DictionaryLookupService(
    private val httpFetcher: HttpFetcher,
    private val dictRuleRepository: DictRuleRepository,
    private val ruleEngine: AnalyzeRuleEngine
) {
    suspend fun lookup(word: String): List<SharedDictionaryLookupResult> {
        val trimmedWord = word.trim()
        if (trimmedWord.isEmpty()) {
            return emptyList()
        }
        return dictRuleRepository.list()
            .filter { it.enabled }
            .map { rule -> lookupRule(rule, trimmedWord) }
    }

    private suspend fun lookupRule(rule: SharedDictRule, word: String): SharedDictionaryLookupResult {
        var requestUrl = ""
        return try {
            val request = buildRequest(rule, word)
            requestUrl = request.url
            val response = httpFetcher.fetch(request)
            val content = parseContent(rule, word, response.finalUrl, response.body)
            SharedDictionaryLookupResult(
                ruleName = rule.name,
                word = word,
                url = response.finalUrl,
                content = content,
                statusCode = response.statusCode
            )
        } catch (error: Throwable) {
            SharedDictionaryLookupResult(
                ruleName = rule.name,
                word = word,
                url = requestUrl,
                content = "",
                errorMessage = error.message ?: error.toString()
            )
        }
    }

    private suspend fun buildRequest(rule: SharedDictRule, word: String): SharedHttpRequest {
        val variables = mutableMapOf(
            "key" to word,
            "word" to word
        )
        val template = if (rule.urlRule.hasExecutableRule()) {
            ruleEngine.evaluateString(
                content = "",
                rule = rule.urlRule,
                context = RuleEvaluationContext(
                    sourceKey = rule.name,
                    variables = variables
                ),
                variables = variables
            ).orEmpty()
        } else {
            rule.urlRule
        }
        require(template.isNotBlank()) { "Dictionary rule ${rule.name} produced an empty url" }
        return SharedRequestBuilder.build(
            template = template,
            context = SharedRequestBuilder.SharedRequestContext(key = word)
        )
    }

    private suspend fun parseContent(
        rule: SharedDictRule,
        word: String,
        baseUrl: String,
        body: String
    ): String {
        if (rule.showRule.isBlank()) {
            return body
        }
        return ruleEngine.evaluateString(
            content = body,
            rule = rule.showRule,
            context = RuleEvaluationContext(
                sourceKey = rule.name,
                baseUrl = baseUrl,
                variables = mapOf(
                    "key" to word,
                    "word" to word
                )
            ),
            variables = mutableMapOf(
                "key" to word,
                "word" to word
            )
        ).orEmpty()
    }

    private fun String.hasExecutableRule(): Boolean {
        return contains("@js:", ignoreCase = true) ||
            contains("<js>", ignoreCase = true) ||
            contains("@webjs:", ignoreCase = true)
    }
}

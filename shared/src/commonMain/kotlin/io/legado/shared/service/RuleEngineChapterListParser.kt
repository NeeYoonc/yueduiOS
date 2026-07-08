package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleAnalyzer
import io.legado.shared.rule.RuleEvaluationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RuleEngineChapterListParser(
    private val ruleEngine: AnalyzeRuleEngine,
    private val fallbackParser: ChapterListParser = RegexChapterListParser
) : SuspendChapterListParser {
    private val json = Json {
        explicitNulls = false
    }
    private val groupReferenceRegex = Regex("""^\$\d+$""")
    private val falseRegex = Regex("""^(?:false|no|not|0|0\.0)$""", RegexOption.IGNORE_CASE)

    override suspend fun parse(
        source: SharedBookSource,
        book: SharedBook,
        body: String
    ): List<SharedBookChapter> {
        val rule = source.ruleToc ?: return fallbackParser.parse(source, body)
        val chapterListRule = rule.chapterList
            ?.trim()
            ?.removePrefix("-")
            ?.removePrefix("+")
            ?.takeIf { it.isNotEmpty() }
            ?: return fallbackParser.parse(source, body)
        if (usesRegexMatchGroups(chapterListRule, rule.chapterName, rule.chapterUrl)) {
            return fallbackParser.parse(source, body)
        }
        val blocks = RuleAnalyzer.selectBlocks(body, chapterListRule)
        if (blocks.isEmpty()) {
            return fallbackParser.parse(source, body)
        }
        val context = RuleEvaluationContext(
            sourceKey = source.key,
            baseUrl = source.bookSourceUrl,
            variables = book.variableMap
        )
        return blocks.mapNotNull { block ->
            val variables = context.variables.toMutableMap()
            val title = ruleEngine.evaluateString(block, rule.chapterName, context, variables).orEmpty()
            if (title.isBlank()) {
                null
            } else {
                val url = ruleEngine.evaluateString(block, rule.chapterUrl, context, variables).orEmpty()
                val isVolume = ruleEngine.evaluateString(block, rule.isVolume, context, variables).toRuleBoolean()
                val isVip = ruleEngine.evaluateString(block, rule.isVip, context, variables).toRuleBoolean()
                val isPay = ruleEngine.evaluateString(block, rule.isPay, context, variables).toRuleBoolean()
                val tag = ruleEngine.evaluateString(block, rule.updateTime, context, variables)
                val variableMap = variables.toMap()
                SharedBookChapter(
                    title = title,
                    url = url,
                    isVolume = isVolume,
                    isVip = isVip,
                    isPay = isPay,
                    tag = tag,
                    variable = variableMap.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
                    variableMap = variableMap
                )
            }
        }.mapIndexed { index, chapter ->
            chapter.copy(index = index)
        }
    }

    override suspend fun parseNextUrls(
        source: SharedBookSource,
        book: SharedBook,
        body: String
    ): List<String> {
        val rule = source.ruleToc?.nextTocUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return fallbackParser.parseNextUrls(source, body)
        val context = RuleEvaluationContext(
            sourceKey = source.key,
            baseUrl = source.bookSourceUrl,
            variables = book.variableMap
        )
        if (!rule.contains("@js:", ignoreCase = true) &&
            !rule.contains("@webjs:", ignoreCase = true) &&
            !rule.contains("<js>", ignoreCase = true)
        ) {
            RuleAnalyzer.getStrings(body, rule).takeIf { it.isNotEmpty() }?.let { return it }
        }
        val variables = context.variables.toMutableMap()
        return ruleEngine.evaluateString(body, rule, context, variables)
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toList()
            ?.takeIf { it.isNotEmpty() }
            ?: fallbackParser.parseNextUrls(source, body)
    }

    private fun String?.toRuleBoolean(): Boolean {
        if (isNullOrBlank() || this == "null") {
            return false
        }
        return !falseRegex.matches(trim())
    }

    private fun usesRegexMatchGroups(chapterListRule: String, vararg fieldRules: String?): Boolean {
        if (chapterListRule.trim().startsWith("$") ||
            chapterListRule.trim().startsWith("/") ||
            RuleAnalyzer.looksLikeHtmlRule(chapterListRule)
        ) {
            return false
        }
        return fieldRules.any { rule ->
            groupReferenceRegex.matches(rule?.trim().orEmpty())
        }
    }
}

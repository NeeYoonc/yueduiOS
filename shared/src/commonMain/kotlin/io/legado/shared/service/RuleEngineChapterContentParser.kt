package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleAnalyzer
import io.legado.shared.rule.RuleEvaluationContext

class RuleEngineChapterContentParser(
    private val ruleEngine: AnalyzeRuleEngine,
    private val fallbackParser: ChapterContentParser = RegexChapterContentParser
) : SuspendChapterContentParser {

    override suspend fun parse(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter,
        body: String
    ): SharedChapterContent {
        val rule = source.ruleContent ?: return fallbackParser.parse(source, book, chapter, body)
        val seedVariables = book.variableMap.toMutableMap().apply {
            putAll(chapter.variableMap)
        }
        val context = RuleEvaluationContext(
            sourceKey = source.key,
            baseUrl = source.bookSourceUrl,
            variables = seedVariables
        )
        val variables = seedVariables.toMutableMap()
        val contentBody = ruleEngine.evaluateWebJs(body, rule.webJs, context) ?: body
        val rawContent = ruleEngine.evaluateString(contentBody, rule.content, context, variables).normalized().orEmpty()
        return SharedChapterContent(
            content = applyReplaceRegex(rawContent, rule.replaceRegex),
            title = ruleEngine.evaluateString(contentBody, rule.title, context, variables).normalized(),
            subContent = ruleEngine.evaluateString(contentBody, rule.subContent, context, variables).normalized(),
            nextContentUrls = parseNextContentUrls(contentBody, rule.nextContentUrl, context, variables)
        )
    }

    private suspend fun parseNextContentUrls(
        body: String,
        rule: String?,
        context: RuleEvaluationContext,
        variables: MutableMap<String, String>
    ): List<String> {
        val trimmed = rule?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        if (!trimmed.contains("@js:", ignoreCase = true) &&
            !trimmed.contains("@webjs:", ignoreCase = true) &&
            !trimmed.contains("<js>", ignoreCase = true)
        ) {
            RuleAnalyzer.getStrings(body, trimmed).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return ruleEngine.evaluateString(body, trimmed, context, variables)
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toList()
            .orEmpty()
    }

    private fun applyReplaceRegex(content: String, rule: String?): String {
        if (content.isEmpty()) {
            return content
        }
        val replacementRule = parseReplaceRule(rule) ?: return content
        val regex = runCatching { Regex(replacementRule.pattern) }.getOrNull()
        val replaced = if (regex != null) {
            content.replace(regex, replacementRule.replacement)
        } else {
            content.replace(replacementRule.pattern, replacementRule.replacement)
        }
        return replaced.normalized().orEmpty()
    }

    private fun parseReplaceRule(rule: String?): ReplacementRule? {
        val trimmedRule = rule?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!trimmedRule.startsWith("##")) {
            return null
        }
        val parts = trimmedRule.split("##")
        if (parts.size < 3 || parts[1].isEmpty()) {
            return null
        }
        return ReplacementRule(
            pattern = parts[1],
            replacement = parts.getOrNull(2).orEmpty()
        )
    }

    private fun String?.normalized(): String? {
        return this
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.joinToString("\n")
            ?.takeIf { it.isNotEmpty() }
    }

    private data class ReplacementRule(
        val pattern: String,
        val replacement: String
    )
}

package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedContentRule
import io.legado.shared.rule.RuleAnalyzer

interface ChapterContentParser {
    fun parse(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter,
        body: String
    ): SharedChapterContent
}

fun interface SuspendChapterContentParser {
    suspend fun parse(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter,
        body: String
    ): SharedChapterContent
}

object RegexChapterContentParser : ChapterContentParser {

    override fun parse(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter,
        body: String
    ): SharedChapterContent {
        val rule = source.ruleContent ?: return SharedChapterContent()
        val content = extractSingle(body, rule.content).orEmpty()
        return SharedChapterContent(
            content = applyReplaceRegex(content, rule.replaceRegex),
            title = extractSingle(body, rule.title),
            subContent = extractSingle(body, rule.subContent),
            nextContentUrls = extractAll(body, rule.nextContentUrl)
        )
    }

    private fun parseJson(rule: SharedContentRule, body: String): SharedChapterContent {
        val root = SimpleJsonPath.parse(body) ?: return SharedChapterContent()
        val content = normalizeContent(SimpleJsonPath.text(root, rule.content).orEmpty())
        return SharedChapterContent(
            content = applyReplaceRegex(content, rule.replaceRegex),
            title = SimpleJsonPath.text(root, rule.title)?.let(::normalizeContent),
            subContent = SimpleJsonPath.text(root, rule.subContent)?.let(::normalizeContent),
            nextContentUrls = SimpleJsonPath.texts(root, rule.nextContentUrl)
        )
    }

    private fun SharedContentRule.hasJsonPathRule(): Boolean {
        return listOf(content, title, subContent, nextContentUrl)
            .any { it?.trim()?.startsWith("$") == true }
    }

    private fun extractSingle(body: String, rule: String?): String? {
        return RuleAnalyzer.getString(body, rule)?.let(::normalizeContent)
    }

    private fun extractAll(body: String, rule: String?): List<String> {
        return RuleAnalyzer.getStrings(body, rule)
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
        return normalizeContent(replaced)
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

    private fun normalizeContent(value: String): String {
        return value.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    private data class ReplacementRule(
        val pattern: String,
        val replacement: String
    )
}

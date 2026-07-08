package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleEvaluationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RuleEngineBookInfoParser(
    private val ruleEngine: AnalyzeRuleEngine,
    private val fallbackParser: BookInfoParser = RegexBookInfoParser
) : SuspendBookInfoParser {
    private val json = Json {
        explicitNulls = false
    }

    override suspend fun parse(
        source: SharedBookSource,
        book: SharedBook,
        body: String
    ): SharedBook {
        val rule = source.ruleBookInfo ?: return fallbackParser.parse(source, book, body)
        val context = RuleEvaluationContext(
            sourceKey = source.key,
            baseUrl = source.bookSourceUrl,
            variables = book.variableMap
        )
        val variables = context.variables.toMutableMap()
        val content = ruleEngine.evaluateString(body, rule.init, context, variables) ?: body
        val parsed = book.copy(
            name = ruleEngine.evaluateString(content, rule.name, context, variables).normalized()
                ?: book.name,
            author = ruleEngine.evaluateString(content, rule.author, context, variables).normalized()
                ?: book.author,
            kind = ruleEngine.evaluateString(content, rule.kind, context, variables).normalized()
                ?: book.kind,
            latestChapterTitle = ruleEngine.evaluateString(content, rule.lastChapter, context, variables).normalized()
                ?: book.latestChapterTitle,
            intro = ruleEngine.evaluateString(content, rule.intro, context, variables).normalized()
                ?: book.intro,
            coverUrl = ruleEngine.evaluateString(content, rule.coverUrl, context, variables).normalized()
                ?: book.coverUrl,
            tocUrl = ruleEngine.evaluateString(content, rule.tocUrl, context, variables).normalized()
                ?: book.tocUrl.ifBlank { book.bookUrl },
            wordCount = ruleEngine.evaluateString(content, rule.wordCount, context, variables).normalized()
                ?: book.wordCount
        )
        val variableMap = variables.toMap()
        return parsed.copy(
            variable = variableMap.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
            variableMap = variableMap
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
}

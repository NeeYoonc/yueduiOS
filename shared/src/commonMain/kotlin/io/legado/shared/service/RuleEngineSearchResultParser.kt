package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleAnalyzer
import io.legado.shared.rule.RuleEvaluationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun interface SuspendSearchResultParser {
    suspend fun parse(source: SharedBookSource, body: String): List<SharedSearchBook>
}

class RuleEngineSearchResultParser(
    private val ruleEngine: AnalyzeRuleEngine,
    private val fallbackParser: SearchResultParser = RuleAwareSearchResultParser
) : SuspendSearchResultParser {
    private val json = Json {
        explicitNulls = false
    }

    override suspend fun parse(source: SharedBookSource, body: String): List<SharedSearchBook> {
        val rule = source.ruleSearch ?: return fallbackParser.parse(source, body)
        val bookListRule = rule.bookList?.takeIf { it.isNotBlank() }
            ?: return fallbackParser.parse(source, body)
        val context = RuleEvaluationContext(
            sourceKey = source.key,
            baseUrl = source.bookSourceUrl
        )
        val books = mutableListOf<SharedSearchBook>()
        RuleAnalyzer.selectBlocks(body, bookListRule).forEach { block ->
            val variables = context.variables.toMutableMap()
            val book = SharedSearchBook(
                name = ruleEngine.evaluateString(block, rule.name, context, variables).orEmpty(),
                author = ruleEngine.evaluateString(block, rule.author, context, variables).orEmpty(),
                bookUrl = ruleEngine.evaluateString(block, rule.bookUrl, context, variables).orEmpty(),
                origin = source.bookSourceUrl,
                kind = ruleEngine.evaluateString(block, rule.kind, context, variables),
                latestChapterTitle = ruleEngine.evaluateString(block, rule.lastChapter, context, variables),
                intro = ruleEngine.evaluateString(block, rule.intro, context, variables),
                coverUrl = ruleEngine.evaluateString(block, rule.coverUrl, context, variables),
                wordCount = ruleEngine.evaluateString(block, rule.wordCount, context, variables),
                variable = variables.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) },
                variableMap = variables.toMap()
            )
            if (book.name.isNotBlank() || book.bookUrl.isNotBlank()) {
                books.add(book)
            }
        }
        return books
    }
}

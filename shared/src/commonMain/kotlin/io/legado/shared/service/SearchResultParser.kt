package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.rule.RuleAnalyzer
import kotlinx.serialization.json.JsonElement

interface SearchResultParser {
    fun parse(source: SharedBookSource, body: String): List<SharedSearchBook>
}

object LineSearchResultParser : SearchResultParser {

    override fun parse(source: SharedBookSource, body: String): List<SharedSearchBook> {
        return body.splitToSequence(Regex("\\n\\s*\\n"))
            .mapNotNull { block -> parseSearchBook(source, block) }
            .toList()
    }

    private fun parseSearchBook(source: SharedBookSource, block: String): SharedSearchBook? {
        val values = block.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && "=" in it }
            .map {
                val key = it.substringBefore("=").trim()
                val value = it.substringAfter("=").trim()
                key to value
            }
            .toMap()
        val name = values["name"].orEmpty()
        val bookUrl = values["bookUrl"] ?: values["url"] ?: ""
        if (name.isBlank() && bookUrl.isBlank()) {
            return null
        }
        return SharedSearchBook(
            name = name,
            author = values["author"].orEmpty(),
            bookUrl = bookUrl,
            origin = source.bookSourceUrl,
            kind = values["kind"],
            latestChapterTitle = values["lastChapter"] ?: values["latestChapterTitle"],
            intro = values["intro"],
            coverUrl = values["coverUrl"]
        )
    }
}

object RuleAwareSearchResultParser : SearchResultParser {
    override fun parse(source: SharedBookSource, body: String): List<SharedSearchBook> {
        return if (source.ruleSearch != null) {
            RegexSearchResultParser.parse(source, body)
        } else {
            LineSearchResultParser.parse(source, body)
        }
    }
}

object RegexSearchResultParser : SearchResultParser {

    private val groupReferenceRegex = Regex("""^\$(\d+)$""")

    override fun parse(source: SharedBookSource, body: String): List<SharedSearchBook> {
        val rule = source.ruleSearch ?: return emptyList()
        val bookListRule = rule.bookList?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (bookListRule.trim().startsWith("$")) {
            return parseJson(source, body, bookListRule)
        }
        if (RuleAnalyzer.looksLikeHtmlRule(bookListRule)) {
            return RuleAnalyzer.selectBlocks(body, bookListRule)
                .mapNotNull { block ->
                    val book = SharedSearchBook(
                        name = RuleAnalyzer.getString(block, rule.name).orEmpty(),
                        author = RuleAnalyzer.getString(block, rule.author).orEmpty(),
                        bookUrl = RuleAnalyzer.getString(block, rule.bookUrl).orEmpty(),
                        origin = source.bookSourceUrl,
                        kind = RuleAnalyzer.getString(block, rule.kind),
                        latestChapterTitle = RuleAnalyzer.getString(block, rule.lastChapter),
                        intro = RuleAnalyzer.getString(block, rule.intro),
                        coverUrl = RuleAnalyzer.getString(block, rule.coverUrl),
                        wordCount = RuleAnalyzer.getString(block, rule.wordCount)
                    )
                    book.takeIf { it.name.isNotBlank() || it.bookUrl.isNotBlank() }
                }
        }
        val bookRegex = runCatching { Regex(bookListRule) }.getOrNull() ?: return emptyList()
        return bookRegex.findAll(body)
            .mapNotNull { match ->
                val book = SharedSearchBook(
                    name = extractField(match, rule.name).orEmpty(),
                    author = extractField(match, rule.author).orEmpty(),
                    bookUrl = extractField(match, rule.bookUrl).orEmpty(),
                    origin = source.bookSourceUrl,
                    kind = extractField(match, rule.kind),
                    latestChapterTitle = extractField(match, rule.lastChapter),
                    intro = extractField(match, rule.intro),
                    coverUrl = extractField(match, rule.coverUrl),
                    wordCount = extractField(match, rule.wordCount)
                )
                book.takeIf { it.name.isNotBlank() || it.bookUrl.isNotBlank() }
            }
            .toList()
    }

    private fun parseJson(
        source: SharedBookSource,
        body: String,
        bookListRule: String
    ): List<SharedSearchBook> {
        val rule = source.ruleSearch ?: return emptyList()
        val root = SimpleJsonPath.parse(body) ?: return emptyList()
        val items = SimpleJsonPath.elements(root, bookListRule)
        return items.mapNotNull { item ->
            val book = SharedSearchBook(
                name = extractJsonField(item, rule.name).orEmpty(),
                author = extractJsonField(item, rule.author).orEmpty(),
                bookUrl = extractJsonField(item, rule.bookUrl).orEmpty(),
                origin = source.bookSourceUrl,
                kind = extractJsonField(item, rule.kind),
                latestChapterTitle = extractJsonField(item, rule.lastChapter),
                intro = extractJsonField(item, rule.intro),
                coverUrl = extractJsonField(item, rule.coverUrl),
                wordCount = extractJsonField(item, rule.wordCount)
            )
            book.takeIf { it.name.isNotBlank() || it.bookUrl.isNotBlank() }
        }
    }

    private fun extractJsonField(element: JsonElement, rule: String?): String? {
        return SimpleJsonPath.text(element, rule)
    }

    private fun extractField(match: MatchResult, rule: String?): String? {
        val trimmedRule = rule?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val groupReference = groupReferenceRegex.matchEntire(trimmedRule)
        if (groupReference != null) {
            val groupIndex = groupReference.groupValues[1].toInt()
            return match.groups.getOrNull(groupIndex)?.value?.trim()
        }
        return RuleAnalyzer.getString(match.value, trimmedRule)
    }

    private fun MatchGroupCollection.getOrNull(index: Int): MatchGroup? {
        return if (index in 0 until size) {
            get(index)
        } else {
            null
        }
    }
}

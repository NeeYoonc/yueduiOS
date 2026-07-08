package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import kotlinx.serialization.json.JsonElement

interface BookInfoParser {
    fun parse(source: SharedBookSource, book: SharedBook, body: String): SharedBook
}

object RegexBookInfoParser : BookInfoParser {
    override fun parse(source: SharedBookSource, book: SharedBook, body: String): SharedBook {
        val rule = source.ruleBookInfo ?: return book
        if (rule.hasJsonPathRule()) {
            return parseJson(rule, book, body)
        }
        return book.copy(
            name = extractField(body, rule.name) ?: book.name,
            author = extractField(body, rule.author) ?: book.author,
            kind = extractField(body, rule.kind) ?: book.kind,
            latestChapterTitle = extractField(body, rule.lastChapter) ?: book.latestChapterTitle,
            intro = extractField(body, rule.intro) ?: book.intro,
            coverUrl = extractField(body, rule.coverUrl) ?: book.coverUrl,
            tocUrl = extractField(body, rule.tocUrl) ?: book.tocUrl.ifBlank { book.bookUrl }
        )
    }

    private fun parseJson(rule: SharedBookInfoRule, book: SharedBook, body: String): SharedBook {
        val root = SimpleJsonPath.parse(body) ?: return book
        return book.copy(
            name = extractJsonField(root, rule.name) ?: book.name,
            author = extractJsonField(root, rule.author) ?: book.author,
            kind = extractJsonField(root, rule.kind) ?: book.kind,
            latestChapterTitle = extractJsonField(root, rule.lastChapter) ?: book.latestChapterTitle,
            intro = extractJsonField(root, rule.intro) ?: book.intro,
            coverUrl = extractJsonField(root, rule.coverUrl) ?: book.coverUrl,
            tocUrl = extractJsonField(root, rule.tocUrl) ?: book.tocUrl.ifBlank { book.bookUrl }
        )
    }

    private fun SharedBookInfoRule.hasJsonPathRule(): Boolean {
        return listOf(name, author, kind, lastChapter, intro, coverUrl, tocUrl)
            .any { it?.trim()?.startsWith("$") == true }
    }

    private fun extractJsonField(root: JsonElement, rule: String?): String? {
        return SimpleJsonPath.text(root, rule)
            ?.let(::normalizeField)
            ?.takeIf { it.isNotEmpty() }
    }

    private fun extractField(body: String, rule: String?): String? {
        val trimmedRule = rule?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val regex = runCatching { Regex(trimmedRule) }.getOrNull() ?: return null
        val match = regex.find(body) ?: return null
        val value = if (match.groupValues.size > 1) {
            match.groupValues[1]
        } else {
            match.value
        }
        return normalizeField(value).takeIf { it.isNotEmpty() }
    }

    private fun normalizeField(value: String): String {
        return value.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}

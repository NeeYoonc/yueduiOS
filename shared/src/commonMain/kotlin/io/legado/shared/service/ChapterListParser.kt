package io.legado.shared.service

import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import kotlinx.serialization.json.JsonElement

interface ChapterListParser {
    fun parse(source: SharedBookSource, body: String): List<SharedBookChapter>

    fun parseNextUrls(source: SharedBookSource, body: String): List<String> = emptyList()
}

object RegexChapterListParser : ChapterListParser {

    private val groupReferenceRegex = Regex("""^\$(\d+)$""")
    private val falseRegex = Regex("""^(?:false|no|not|0|0\.0)$""", RegexOption.IGNORE_CASE)

    override fun parse(source: SharedBookSource, body: String): List<SharedBookChapter> {
        val rule = source.ruleToc ?: return emptyList()
        val chapterListRule = rule.chapterList
            ?.trim()
            ?.removePrefix("-")
            ?.removePrefix("+")
            ?.takeIf { it.isNotEmpty() }
            ?: return emptyList()
        if (chapterListRule.startsWith("$")) {
            return parseJson(source, body, chapterListRule)
        }
        val chapterRegex = runCatching { Regex(chapterListRule) }.getOrNull() ?: return emptyList()
        return chapterRegex.findAll(body)
            .mapNotNull { match ->
                val title = extractField(match, rule.chapterName).orEmpty()
                if (title.isBlank()) {
                    null
                } else {
                    SharedBookChapter(
                        title = title,
                        url = extractField(match, rule.chapterUrl).orEmpty(),
                        isVolume = extractFlag(match, rule.isVolume),
                        isVip = extractFlag(match, rule.isVip),
                        isPay = extractFlag(match, rule.isPay),
                        tag = extractField(match, rule.updateTime)
                    )
                }
            }
            .mapIndexed { index, chapter -> chapter.copy(index = index) }
            .toList()
    }

    private fun parseJson(
        source: SharedBookSource,
        body: String,
        chapterListRule: String
    ): List<SharedBookChapter> {
        val rule = source.ruleToc ?: return emptyList()
        val root = SimpleJsonPath.parse(body) ?: return emptyList()
        val items = SimpleJsonPath.elements(root, chapterListRule)
        return items.mapNotNull { item ->
            val title = SimpleJsonPath.text(item, rule.chapterName).orEmpty()
            if (title.isBlank()) {
                null
            } else {
                SharedBookChapter(
                    title = title,
                    url = SimpleJsonPath.text(item, rule.chapterUrl).orEmpty(),
                    isVolume = extractJsonFlag(item, rule.isVolume),
                    isVip = extractJsonFlag(item, rule.isVip),
                    isPay = extractJsonFlag(item, rule.isPay),
                    tag = SimpleJsonPath.text(item, rule.updateTime)
                )
            }
        }
            .mapIndexed { index, chapter -> chapter.copy(index = index) }
    }

    override fun parseNextUrls(source: SharedBookSource, body: String): List<String> {
        val nextTocRule = source.ruleToc?.nextTocUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return emptyList()
        val nextRegex = runCatching { Regex(nextTocRule) }.getOrNull() ?: return emptyList()
        return nextRegex.findAll(body)
            .map { match ->
                if (match.groupValues.size > 1) {
                    match.groupValues[1].trim()
                } else {
                    match.value.trim()
                }
            }
            .filter { it.isNotEmpty() }
            .toList()
    }

    private fun extractJsonFlag(element: JsonElement, rule: String?): Boolean {
        val value = SimpleJsonPath.text(element, rule)
        if (value.isNullOrBlank() || value == "null") {
            return false
        }
        return !falseRegex.matches(value.trim())
    }

    private fun extractFlag(match: MatchResult, rule: String?): Boolean {
        val value = extractField(match, rule)
        if (value.isNullOrBlank() || value == "null") {
            return false
        }
        return !falseRegex.matches(value.trim())
    }

    private fun extractField(match: MatchResult, rule: String?): String? {
        val trimmedRule = rule?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val groupReference = groupReferenceRegex.matchEntire(trimmedRule)
        if (groupReference != null) {
            val groupIndex = groupReference.groupValues[1].toInt()
            return match.groups.getOrNull(groupIndex)?.value?.trim()
        }
        val fieldRegex = runCatching { Regex(trimmedRule) }.getOrNull() ?: return null
        val fieldMatch = fieldRegex.find(match.value) ?: return null
        return if (fieldMatch.groupValues.size > 1) {
            fieldMatch.groupValues[1].trim()
        } else {
            fieldMatch.value.trim()
        }
    }

    private fun MatchGroupCollection.getOrNull(index: Int): MatchGroup? {
        return if (index in 0 until size) {
            get(index)
        } else {
            null
        }
    }
}

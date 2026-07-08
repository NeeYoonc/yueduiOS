package io.legado.shared.rss

import io.legado.shared.model.SharedRssArticle
import io.legado.shared.model.SharedRssSource
import io.legado.shared.rule.RuleAnalyzer
import io.legado.shared.service.SharedUrlResolver

class RssRuleParser {
    fun parseArticles(
        source: SharedRssSource,
        body: String,
        baseUrl: String,
        sort: String = "default"
    ): List<SharedRssArticle> {
        val blocks = source.ruleArticles
            ?.takeIf { it.isNotBlank() }
            ?.let { RuleAnalyzer.selectBlocks(body, it) }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultXmlBlocks(body)
        return blocks.mapIndexedNotNull { index, block ->
            val title = extract(block, source.ruleTitle) ?: defaultXmlValue(block, "title")
            val link = extract(block, source.ruleLink) ?: defaultXmlValue(block, "link")
            if (title.isNullOrBlank() && link.isNullOrBlank()) {
                null
            } else {
                SharedRssArticle(
                    origin = source.sourceUrl,
                    sort = sort,
                    title = title.orEmpty(),
                    order = index.toLong(),
                    link = link.orEmpty()
                        .takeIf { it.isNotBlank() }
                        ?.let { SharedUrlResolver.resolve(baseUrl, it) }
                        .orEmpty(),
                    pubDate = extract(block, source.rulePubDate) ?: defaultXmlValue(block, "pubDate"),
                    description = extract(block, source.ruleDescription) ?: defaultXmlValue(block, "description"),
                    image = extract(block, source.ruleImage)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { SharedUrlResolver.resolve(baseUrl, it) },
                    content = extract(block, source.ruleContent)
                )
            }
        }
    }

    fun parseContent(
        source: SharedRssSource,
        article: SharedRssArticle,
        body: String
    ): SharedRssArticle {
        val content = extract(body, source.ruleContent)
            ?: defaultXmlValue(body, "content:encoded")
            ?: defaultXmlValue(body, "content")
        return article.copy(content = content ?: article.content)
    }

    private fun extract(body: String, rule: String?): String? {
        return RuleAnalyzer.getString(body, rule)
            ?.takeIf { it.isNotBlank() }
    }

    private fun defaultXmlBlocks(body: String): List<String> {
        val itemRegex = Regex("""<(item|entry)\b[\s\S]*?</\1>""", RegexOption.IGNORE_CASE)
        return itemRegex.findAll(body).map { it.value }.toList()
    }

    private fun defaultXmlValue(body: String, tag: String): String? {
        val regex = Regex(
            """<${Regex.escape(tag)}\b[^>]*>([\s\S]*?)</${Regex.escape(tag)}>""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(body)?.groupValues?.getOrNull(1)
            ?.replace(Regex("""<!\[CDATA\[([\s\S]*?)]]>"""), "$1")
            ?.replace(Regex("""<[^>]+>"""), " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}

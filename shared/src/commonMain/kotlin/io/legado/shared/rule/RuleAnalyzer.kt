package io.legado.shared.rule

import io.legado.shared.service.SimpleJsonPath

object RuleAnalyzer {
    private val groupReferenceRegex = Regex("""^\$(\d+)$""")
    private val htmlTagRegex = Regex("""^<\/?([A-Za-z][A-Za-z0-9_-]*)""")
    private val htmlAttrNameRegex = Regex("""^[A-Za-z_:][A-Za-z0-9_:\-]*$""")
    private val compositeOperators = listOf("||", "%%", "&&")

    fun getString(content: String, rule: String?): String? {
        return getStrings(content, rule).firstOrNull()
    }

    fun getStrings(content: String, rule: String?): List<String> {
        val parsed = ParsedRule.parse(rule) ?: return emptyList()
        val values = valuesForRule(content, parsed.extractRule)
        return values
            .map { parsed.applyReplacement(it) }
            .map { normalizeText(it) }
            .filter { it.isNotEmpty() }
    }

    fun selectBlocks(content: String, rule: String?): List<String> {
        val parsed = ParsedRule.parse(rule) ?: return emptyList()
        splitCompositeRule(parsed.extractRule)?.let { composite ->
            return mergeComposite(composite) { part -> selectBlocks(content, part) }
        }
        return when {
            parsed.extractRule.normalizedRule().startsWith("$") -> {
                val root = SimpleJsonPath.parse(content) ?: return emptyList()
                SimpleJsonPath.elements(root, parsed.extractRule.normalizedRule()).map { it.toString() }
            }
            parsed.extractRule.isXPathRule() -> xpathElements(content, parsed.extractRule)
            looksLikeHtmlRule(parsed.extractRule) -> htmlChainElements(content, parsed.extractRule)
            else -> regexBlocks(content, parsed.extractRule)
        }
    }

    fun looksLikeHtmlRule(rule: String?): Boolean {
        val trimmed = rule?.trim()?.removeModePrefix("@css:") ?: return false
        val selector = trimmed.substringBefore("@").trim()
        return selector.startsWith(".") ||
            selector.startsWith("#") ||
            htmlTagRegex.matches(selector) ||
            selector.matches(Regex("""^[A-Za-z][A-Za-z0-9_-]*(\s+[.#A-Za-z][A-Za-z0-9_-]*)*$"""))
    }

    private fun valuesForRule(content: String, rule: String): List<String> {
        splitCompositeRule(rule)?.let { composite ->
            return mergeComposite(composite) { part -> valuesForRule(content, part) }
        }
        val normalized = rule.normalizedRule()
        return when {
            normalized.startsWith("$") -> jsonValues(content, normalized)
            rule.isXPathRule() -> xpathValues(content, rule)
            looksLikeHtmlRule(rule) -> htmlChainValues(content, rule)
            else -> regexValues(content, normalized)
        }
    }

    private fun jsonValues(content: String, rule: String): List<String> {
        val root = SimpleJsonPath.parse(content) ?: return emptyList()
        return SimpleJsonPath.texts(root, rule)
    }

    private fun regexValues(content: String, rule: String): List<String> {
        val groupReference = groupReferenceRegex.matchEntire(rule)
        if (groupReference != null) {
            return emptyList()
        }
        val regex = runCatching { Regex(rule, setOf(RegexOption.DOT_MATCHES_ALL)) }.getOrNull()
            ?: return emptyList()
        return regex.findAll(content)
            .map { match ->
                if (match.groupValues.size > 1) {
                    match.groupValues[1]
                } else {
                    match.value
                }
            }
            .toList()
    }

    private fun regexBlocks(content: String, rule: String): List<String> {
        val regex = runCatching { Regex(rule, setOf(RegexOption.DOT_MATCHES_ALL)) }.getOrNull()
            ?: return emptyList()
        return regex.findAll(content).map { it.value }.toList()
    }

    private fun htmlValues(content: String, rule: String): List<String> {
        val htmlRule = HtmlRule.parse(rule)
        return htmlElements(content, htmlRule.selector).mapNotNull { element ->
            elementValue(element, htmlRule.attr)
        }
    }

    private fun htmlChainValues(content: String, rule: String): List<String> {
        val normalized = rule.trim().removeModePrefix("@css:")
        val parts = splitTopLevel(normalized, "@")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size <= 1) {
            return htmlValues(content, normalized)
        }
        var elements = listOf(content)
        for (index in 0 until parts.lastIndex) {
            elements = elements.flatMap { htmlElements(it, parts[index]) }
            if (elements.isEmpty()) {
                return emptyList()
            }
        }
        val last = parts.last()
        return if (isTerminalSelector(last)) {
            elements.flatMap { htmlElements(it, last) }
                .mapNotNull { elementValue(it, "text") }
        } else {
            elements.mapNotNull { elementValue(it, last) }
        }
    }

    private fun htmlElements(content: String, rule: String): List<String> {
        val selector = HtmlRule.parse(rule).selector
            .split(Regex("""\s+"""))
            .last()
            .trim()
        if (selector.isEmpty()) {
            return emptyList()
        }
        return when {
            selector.startsWith(".") -> elementsByClass(content, selector.removePrefix("."))
            selector.startsWith("#") -> elementsByAttribute(content, "id", selector.removePrefix("#"))
            else -> elementsByTag(content, selector)
        }
    }

    private fun htmlChainElements(content: String, rule: String): List<String> {
        val normalized = rule.trim().removeModePrefix("@css:")
        val parts = splitTopLevel(normalized, "@")
            .map { it.trim() }
            .filter { it.isNotEmpty() && isHtmlSelector(it) }
        if (parts.isEmpty()) {
            return emptyList()
        }
        return parts.fold(listOf(content)) { elements, part ->
            elements.flatMap { htmlElements(it, part) }
        }
    }

    private fun xpathValues(content: String, rule: String): List<String> {
        val parts = xpathParts(rule)
        if (parts.isEmpty()) {
            return emptyList()
        }
        var elements = listOf(content)
        for (part in parts) {
            when {
                part == "text()" -> return elements.map { stripTags(innerHtml(it)) }
                part.startsWith("@") -> return elements.mapNotNull { attrValue(it, part.removePrefix("@")) }
                else -> {
                    elements = elements.flatMap { htmlElements(it, part) }
                    if (elements.isEmpty()) {
                        return emptyList()
                    }
                }
            }
        }
        return elements.map { stripTags(innerHtml(it)) }
    }

    private fun xpathElements(content: String, rule: String): List<String> {
        val parts = xpathParts(rule)
            .takeWhile { it != "text()" && !it.startsWith("@") }
        if (parts.isEmpty()) {
            return emptyList()
        }
        return parts.fold(listOf(content)) { elements, part ->
            elements.flatMap { htmlElements(it, part) }
        }
    }

    private fun xpathParts(rule: String): List<String> {
        return rule.trim()
            .removeModePrefix("@XPath:")
            .removePrefix("//")
            .removePrefix("/")
            .split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun elementsByTag(content: String, tag: String): List<String> {
        val cleanTag = tag.substringBefore("@").trim()
        if (!htmlAttrNameRegex.matches(cleanTag)) {
            return emptyList()
        }
        val regex = Regex(
            """<($cleanTag)\b[^>]*>.*?</\1>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val paired = regex.findAll(content).map { it.value }.toList()
        if (paired.isNotEmpty()) {
            return paired
        }
        val selfClosing = Regex(
            """<$cleanTag\b[^>]*(?:/?)>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return selfClosing.findAll(content).map { it.value }.toList()
    }

    private fun elementsByClass(content: String, className: String): List<String> {
        val regex = Regex(
            """<([A-Za-z][A-Za-z0-9_-]*)\b(?=[^>]*\bclass\s*=\s*["'][^"']*${Regex.escape(className)}[^"']*["'])[^>]*>.*?</\1>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.findAll(content).map { it.value }.toList()
    }

    private fun elementsByAttribute(content: String, attr: String, value: String): List<String> {
        val regex = Regex(
            """<([A-Za-z][A-Za-z0-9_-]*)\b(?=[^>]*\b${Regex.escape(attr)}\s*=\s*["']${Regex.escape(value)}["'])[^>]*>.*?</\1>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.findAll(content).map { it.value }.toList()
    }

    private fun attrValue(element: String, attr: String): String? {
        if (!htmlAttrNameRegex.matches(attr)) {
            return null
        }
        val regex = Regex("""\b${Regex.escape(attr)}\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        return regex.find(element)?.groupValues?.getOrNull(1)
    }

    private fun elementValue(element: String, attr: String): String? {
        return when (attr.lowercase()) {
            "text" -> stripTags(innerHtml(element))
            "textnodes", "owntext" -> stripTags(innerHtml(element))
            "html", "all" -> innerHtml(element)
            else -> attrValue(element, attr)
        }
    }

    private fun innerHtml(element: String): String {
        return element.substringAfter(">", "").substringBeforeLast("<", "")
    }

    private fun stripTags(value: String): String {
        return value
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</?(p|div|article|section)\b[^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), " ")
    }

    private fun normalizeText(value: String): String {
        return value.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .replace(Regex("""[ \t]{2,}"""), " ")
            .trim()
    }

    private fun splitCompositeRule(rule: String): CompositeRule? {
        val trimmed = rule.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val operator = firstTopLevelOperator(trimmed) ?: return null
        val parts = splitTopLevel(trimmed, operator)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (parts.size > 1) CompositeRule(operator, parts) else null
    }

    private fun firstTopLevelOperator(rule: String): String? {
        var index = 0
        var squareDepth = 0
        var curlyDepth = 0
        var parenDepth = 0
        var quote: Char? = null
        while (index < rule.length) {
            val char = rule[index]
            if (quote != null) {
                if (char == quote && rule.getOrNull(index - 1) != '\\') {
                    quote = null
                }
                index++
                continue
            }
            when (char) {
                '\'', '"' -> quote = char
                '[' -> squareDepth++
                ']' -> if (squareDepth > 0) squareDepth--
                '{' -> curlyDepth++
                '}' -> if (curlyDepth > 0) curlyDepth--
                '(' -> parenDepth++
                ')' -> if (parenDepth > 0) parenDepth--
                else -> if (squareDepth == 0 && curlyDepth == 0 && parenDepth == 0) {
                    compositeOperators.firstOrNull { rule.startsWith(it, index) }?.let {
                        return it
                    }
                }
            }
            index++
        }
        return null
    }

    private fun splitTopLevel(rule: String, delimiter: String): List<String> {
        val parts = mutableListOf<String>()
        var start = 0
        var index = 0
        var squareDepth = 0
        var curlyDepth = 0
        var parenDepth = 0
        var quote: Char? = null
        while (index < rule.length) {
            val char = rule[index]
            if (quote != null) {
                if (char == quote && rule.getOrNull(index - 1) != '\\') {
                    quote = null
                }
                index++
                continue
            }
            when (char) {
                '\'', '"' -> quote = char
                '[' -> squareDepth++
                ']' -> if (squareDepth > 0) squareDepth--
                '{' -> curlyDepth++
                '}' -> if (curlyDepth > 0) curlyDepth--
                '(' -> parenDepth++
                ')' -> if (parenDepth > 0) parenDepth--
                else -> if (
                    squareDepth == 0 &&
                    curlyDepth == 0 &&
                    parenDepth == 0 &&
                    rule.startsWith(delimiter, index)
                ) {
                    parts.add(rule.substring(start, index))
                    index += delimiter.length
                    start = index
                    continue
                }
            }
            index++
        }
        parts.add(rule.substring(start))
        return parts
    }

    private fun <T> mergeComposite(
        composite: CompositeRule,
        evaluator: (String) -> List<T>
    ): List<T> {
        val results = composite.parts.map(evaluator)
        return when (composite.operator) {
            "||" -> results.firstOrNull { it.isNotEmpty() }.orEmpty()
            "%%" -> interleave(results)
            else -> results.flatten()
        }
    }

    private fun <T> interleave(results: List<List<T>>): List<T> {
        val maxSize = results.maxOfOrNull { it.size } ?: return emptyList()
        val merged = mutableListOf<T>()
        for (index in 0 until maxSize) {
            results.forEach { result ->
                if (index < result.size) {
                    merged.add(result[index])
                }
            }
        }
        return merged
    }

    private fun String.removeModePrefix(prefix: String): String {
        return if (startsWith(prefix, ignoreCase = true)) {
            substring(prefix.length)
        } else {
            this
        }
    }

    private fun String.normalizedRule(): String {
        return trim()
            .removeModePrefix("@Json:")
            .removeModePrefix("@CSS:")
    }

    private fun String.isXPathRule(): Boolean {
        val trimmed = trim()
        return trimmed.startsWith("@XPath:", ignoreCase = true) || trimmed.startsWith("/")
    }

    private fun isHtmlSelector(rule: String): Boolean {
        val selector = rule.trim().substringBefore("@").trim()
        return selector.startsWith(".") ||
            selector.startsWith("#") ||
            htmlTagRegex.matches(selector) ||
            selector.matches(Regex("""^[A-Za-z][A-Za-z0-9_-]*$"""))
    }

    private fun isTerminalSelector(rule: String): Boolean {
        val selector = rule.trim().substringBefore("@").trim()
        return selector.startsWith(".") || selector.startsWith("#")
    }

    private data class HtmlRule(
        val selector: String,
        val attr: String
    ) {
        companion object {
            fun parse(rule: String): HtmlRule {
                val normalized = rule.trim().removePrefix("@css:")
                val selector = normalized.substringBefore("@").trim()
                val attr = normalized.substringAfter("@", "text").trim().ifEmpty { "text" }
                return HtmlRule(selector, attr)
            }
        }
    }

    private data class CompositeRule(
        val operator: String,
        val parts: List<String>
    )

    private data class ParsedRule(
        val extractRule: String,
        val replacementPattern: String? = null,
        val replacement: String = ""
    ) {
        fun applyReplacement(value: String): String {
            val pattern = replacementPattern ?: return value
            val regex = runCatching { Regex(pattern) }.getOrNull()
            return if (regex != null) {
                value.replace(regex, replacement)
            } else {
                value.replace(pattern, replacement)
            }
        }

        companion object {
            fun parse(rule: String?): ParsedRule? {
                val trimmed = rule?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                val parts = trimmed.split("##")
                return if (parts.size >= 3) {
                    ParsedRule(
                        extractRule = parts[0].ifBlank { "." },
                        replacementPattern = parts[1],
                        replacement = parts[2]
                    )
                } else {
                    ParsedRule(extractRule = trimmed)
                }
            }
        }
    }
}

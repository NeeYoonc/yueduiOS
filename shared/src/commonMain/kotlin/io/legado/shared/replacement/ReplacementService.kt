package io.legado.shared.replacement

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedReplaceRule
import io.legado.shared.storage.SharedLibraryStore

class ReplacementService(
    private val libraryStore: SharedLibraryStore
) {
    fun applyToChapterContent(
        book: SharedBook,
        chapter: SharedBookChapter,
        content: SharedChapterContent
    ): SharedChapterContent {
        if (book.readConfig?.useReplaceRule == false) {
            return content
        }
        return content.copy(
            title = content.title?.let { applyRules(book, it, title = true) },
            content = applyRules(book, content.content, title = false)
        )
    }

    fun applyToChapterTitle(book: SharedBook, title: String): String {
        if (book.readConfig?.useReplaceRule == false) {
            return title
        }
        return applyRules(book, title, title = true)
    }

    private fun applyRules(book: SharedBook, input: String, title: Boolean): String {
        return enabledRules(book, title).fold(input) { current, rule ->
            if (rule.pattern.isBlank()) {
                current
            } else {
                runCatching {
                    if (rule.isRegex) {
                        Regex(rule.pattern).replace(current, rule.replacement)
                    } else {
                        current.replace(rule.pattern, rule.replacement)
                    }
                }.getOrElse { current }
            }
        }
    }

    private fun enabledRules(book: SharedBook, title: Boolean): List<SharedReplaceRule> {
        return libraryStore.loadDataSnapshot().replaceRules
            .asSequence()
            .filter { it.isEnabled }
            .filter { if (title) it.scopeTitle else it.scopeContent }
            .filter { it.matchesScope(book) }
            .sortedWith(compareBy<SharedReplaceRule> { it.order }.thenBy { it.name }.thenBy { it.id })
            .toList()
    }

    private fun SharedReplaceRule.matchesScope(book: SharedBook): Boolean {
        val includes = scope.isNullOrBlank() ||
            scope.contains(book.name) ||
            scope.contains(book.origin)
        val excludes = !excludeScope.isNullOrBlank() &&
            (excludeScope.contains(book.name) || excludeScope.contains(book.origin))
        return includes && !excludes
    }
}

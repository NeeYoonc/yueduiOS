package io.legado.shared.service

import io.legado.shared.model.SharedReaderSearchResult

class ReaderSearchService {
    fun search(
        content: String,
        query: String,
        contextChars: Int = DEFAULT_CONTEXT_CHARS
    ): List<SharedReaderSearchResult> {
        val term = query.trim()
        if (term.isEmpty() || content.isEmpty()) {
            return emptyList()
        }
        val results = mutableListOf<SharedReaderSearchResult>()
        var start = 0
        while (start <= content.length - term.length) {
            val index = content.indexOf(term, startIndex = start, ignoreCase = true)
            if (index < 0) {
                break
            }
            val end = index + term.length
            val snippetStart = (index - contextChars).coerceAtLeast(0)
            val snippetEnd = (end + contextChars).coerceAtMost(content.length)
            results += SharedReaderSearchResult(
                startIndex = index,
                endIndex = end,
                snippet = content.substring(snippetStart, snippetEnd).trim()
            )
            start = end
        }
        return results
    }

    private companion object {
        const val DEFAULT_CONTEXT_CHARS = 40
    }
}

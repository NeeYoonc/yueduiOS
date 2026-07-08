package io.legado.shared.local

import io.legado.shared.book.BookshelfService
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.model.SharedTxtTocRule
import io.legado.shared.storage.SharedLibraryStore

class LocalTextBookService(
    private val libraryStore: SharedLibraryStore,
    private val bookshelfService: BookshelfService = BookshelfService(libraryStore)
) {
    fun importTextFile(
        fileName: String,
        text: String,
        nowMillis: Long = 0L
    ): LocalTextImportResult {
        val normalizedText = text.normalizeLineEndings()
        require(normalizedText.isNotBlank()) { "Text file is empty" }

        val (name, author) = parseNameAuthor(fileName)
        val bookUrl = "local://txt/${stableHash("$fileName\n${normalizedText.length}\n${normalizedText.take(4096)}")}"
        val parsedChapters = splitChapters(bookUrl, name, normalizedText)
        val chapters = parsedChapters.map { it.chapter }
        val book = SharedBook(
            name = name,
            author = author,
            bookUrl = bookUrl,
            tocUrl = bookUrl,
            origin = LOCAL_ORIGIN,
            originName = fileName,
            latestChapterTitle = chapters.lastOrNull()?.title,
            latestChapterTime = nowMillis,
            totalChapterNum = chapters.size,
            durChapterTitle = chapters.firstOrNull()?.title,
            type = LOCAL_TEXT_TYPE,
            wordCount = normalizedText.length.toString(),
            canUpdate = false
        )
        val savedBook = bookshelfService.upsertBook(book)
        libraryStore.saveBookChapters(savedBook, chapters)
        parsedChapters.forEach { parsed ->
            libraryStore.saveChapterContent(
                savedBook,
                parsed.chapter,
                SharedChapterContent(
                    title = parsed.chapter.title,
                    content = parsed.content
                )
            )
        }
        return LocalTextImportResult(savedBook, chapters)
    }

    private fun splitChapters(
        bookUrl: String,
        bookName: String,
        text: String
    ): List<ParsedLocalChapter> {
        val matches = selectChapterRegex(text)
            ?.findAll(text)
            ?.filter { it.value.trim().isNotEmpty() }
            ?.toList()
            .orEmpty()
        if (matches.isEmpty()) {
            return listOf(
                ParsedLocalChapter(
                    chapter = SharedBookChapter(
                        title = bookName,
                        url = "$bookUrl#0",
                        index = 0,
                        bookUrl = bookUrl,
                        start = 0,
                        end = text.length.toLong(),
                        wordCount = text.length.toString()
                    ),
                    content = text.trimLineBreaks()
                )
            )
        }

        val parsed = mutableListOf<ParsedLocalChapter>()
        val firstStart = matches.first().range.first
        if (firstStart > 0) {
            val preface = text.substring(0, firstStart).trimLineBreaks()
            if (preface.isNotBlank()) {
                parsed += ParsedLocalChapter(
                    chapter = SharedBookChapter(
                        title = "Preface",
                        url = "$bookUrl#${parsed.size}",
                        index = parsed.size,
                        bookUrl = bookUrl,
                        start = 0,
                        end = firstStart.toLong(),
                        wordCount = preface.length.toString()
                    ),
                    content = preface
                )
            }
        }

        matches.forEachIndexed { matchIndex, match ->
            val nextStart = matches.getOrNull(matchIndex + 1)?.range?.first ?: text.length
            val contentStart = (match.range.last + 1).coerceAtMost(nextStart)
            val content = text.substring(contentStart, nextStart).trimLineBreaks()
            parsed += ParsedLocalChapter(
                chapter = SharedBookChapter(
                    title = match.value.toChapterTitle(),
                    url = "$bookUrl#${parsed.size}",
                    index = parsed.size,
                    bookUrl = bookUrl,
                    start = contentStart.toLong(),
                    end = nextStart.toLong(),
                    wordCount = content.length.toString()
                ),
                content = content
            )
        }
        return parsed
    }

    private fun selectChapterRegex(text: String): Regex? {
        val configured = libraryStore.loadDataSnapshot().txtTocRules
            .filter { it.enable && it.rule.isNotBlank() }
            .sortedWith(compareBy<SharedTxtTocRule> { it.serialNumber }.thenBy { it.id })
            .mapNotNull { rule ->
                runCatching { Regex(rule.rule, RegexOption.MULTILINE) }.getOrNull()
            }
        return (configured + fallbackChapterRegexes)
            .map { regex -> regex to regex.findAll(text).take(MAX_RULE_SCAN_MATCHES).count() }
            .filter { (_, count) -> count > 0 }
            .maxWithOrNull(compareBy<Pair<Regex, Int>> { it.second })
            ?.first
    }

    private fun parseNameAuthor(fileName: String): Pair<String, String> {
        val baseName = fileName.substringBeforeLast(".").trim().ifBlank { fileName.trim() }
        for (pattern in nameAuthorPatterns) {
            val match = pattern.matchEntire(baseName) ?: continue
            val name = match.groupValues.getOrNull(1).orEmpty().trim()
            val author = match.groupValues.getOrNull(2).orEmpty().trim()
            if (name.isNotBlank()) {
                return name to author
            }
        }
        return baseName to ""
    }

    private fun String.normalizeLineEndings(): String {
        return replace("\r\n", "\n").replace('\r', '\n')
    }

    private fun String.trimLineBreaks(): String {
        return trim('\n', '\r')
    }

    private fun String.toChapterTitle(): String {
        return lineSequence().firstOrNull()
            ?.trim()
            ?.take(MAX_TITLE_LENGTH)
            ?.ifBlank { null }
            ?: "Untitled"
    }

    private fun stableHash(input: String): String {
        var hash = 0xcbf29ce484222325UL
        input.encodeToByteArray().forEach { byte ->
            hash = hash xor byte.toUByte().toULong()
            hash *= 0x100000001b3UL
        }
        return hash.toString(16)
    }

    private data class ParsedLocalChapter(
        val chapter: SharedBookChapter,
        val content: String
    )

    companion object {
        const val LOCAL_ORIGIN = "loc_book"
        const val LOCAL_TEXT_TYPE = 0b1000 or 0b100000000

        private const val MAX_TITLE_LENGTH = 120
        private const val MAX_RULE_SCAN_MATCHES = 500

        private val nameAuthorPatterns = listOf(
            Regex("""^(.+?)\s+[Bb][Yy]\s+(.+)$"""),
            Regex("""^(.+?)\s*作者[:：]\s*(.+)$"""),
            Regex("""^(.+?)\s*[-_－—]\s*(.+)$""")
        )

        private val fallbackChapterRegexes = listOf(
            Regex(
                """^\s*(?:第[\d零〇一二三四五六七八九十百千万两]+[章节回卷集部篇].{0,60}|[Cc]hapter\s+\d+.{0,80}|[Ss]ection\s+\d+.{0,80}|[Pp]art\s+\d+.{0,80})\s*$""",
                RegexOption.MULTILINE
            )
        )
    }
}

data class LocalTextImportResult(
    val book: SharedBook,
    val chapters: List<SharedBookChapter>
)

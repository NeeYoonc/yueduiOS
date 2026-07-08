package io.legado.shared.source

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.service.BookInfoResult
import io.legado.shared.service.ChapterContentResult
import io.legado.shared.service.ChapterListResult
import io.legado.shared.service.SearchPageResult

class SourceDebugService(
    private val client: LegadoSharedClient
) {
    suspend fun debugSearch(
        source: SharedBookSource,
        key: String,
        page: Int = 1
    ): SourceDebugResult {
        val steps = mutableListOf<SourceDebugStep>()
        val search = client.search(source, key, page)
        steps.add(
            SourceDebugStep(
                stage = "search",
                message = "${search.books.size} result(s)",
                url = search.response.finalUrl
            )
        )
        return SourceDebugResult(source = source, search = search, steps = steps)
    }

    suspend fun debugFirstContent(
        source: SharedBookSource,
        key: String,
        page: Int = 1
    ): SourceDebugResult {
        val steps = mutableListOf<SourceDebugStep>()
        val search = client.search(source, key, page)
        steps.add(
            SourceDebugStep(
                stage = "search",
                message = "${search.books.size} result(s)",
                url = search.response.finalUrl
            )
        )
        val searchBook = search.books.firstOrNull()
            ?: return SourceDebugResult(source = source, search = search, steps = steps)
        val inputBook = searchBook.toBook()
        val bookInfo = if (source.ruleBookInfo != null) {
            client.getBookInfo(source, inputBook).also { result ->
                steps.add(
                    SourceDebugStep(
                        stage = "detail",
                        message = result.book.name.ifBlank { inputBook.name },
                        url = result.response.finalUrl
                    )
                )
            }
        } else {
            steps.add(SourceDebugStep(stage = "detail", message = "skipped"))
            null
        }
        val book = bookInfo?.book ?: inputBook
        val chapterList = client.getChapterList(source, book)
        steps.add(
            SourceDebugStep(
                stage = "toc",
                message = "${chapterList.chapters.size} chapter(s)",
                url = chapterList.response.finalUrl
            )
        )
        val chapter = chapterList.chapters.firstOrNull()
            ?: return SourceDebugResult(
                source = source,
                search = search,
                searchBook = searchBook,
                book = book,
                bookInfo = bookInfo,
                chapterList = chapterList,
                steps = steps
            )
        val content = client.getContent(source, book, chapter)
        steps.add(
            SourceDebugStep(
                stage = "content",
                message = "${content.content.content.length} character(s)",
                url = content.response.finalUrl
            )
        )
        return SourceDebugResult(
            source = source,
            search = search,
            searchBook = searchBook,
            book = book,
            bookInfo = bookInfo,
            chapterList = chapterList,
            chapter = chapter,
            content = content,
            steps = steps
        )
    }
}

data class SourceDebugResult(
    val source: SharedBookSource,
    val search: SearchPageResult,
    val searchBook: SharedSearchBook? = null,
    val book: SharedBook? = null,
    val bookInfo: BookInfoResult? = null,
    val chapterList: ChapterListResult? = null,
    val chapter: SharedBookChapter? = null,
    val content: ChapterContentResult? = null,
    val steps: List<SourceDebugStep> = emptyList()
)

data class SourceDebugStep(
    val stage: String,
    val message: String,
    val url: String? = null
)

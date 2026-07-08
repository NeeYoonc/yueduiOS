package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse

class BookContentService(
    private val httpFetcher: HttpFetcher,
    private val chapterContentParser: ChapterContentParser = RegexChapterContentParser,
    private val maxContentPages: Int = 20
) {
    suspend fun getContent(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter
    ): ChapterContentResult {
        val requestUrl = chapter.url.ifBlank { book.bookUrl }
        val visitedUrls = linkedSetOf<String>()
        val discoveredNextUrls = mutableListOf<String>()
        val pendingUrls = ArrayDeque<String>()
        val firstResponse = httpFetcher.fetch(SharedHttpRequest(url = requestUrl))
        visitedUrls.add(requestUrl)
        val firstContent = parseAndNormalize(source, book, chapter, firstResponse)
        val contentParts = mutableListOf<String>()
        if (firstContent.content.isNotBlank()) {
            contentParts.add(firstContent.content)
        }
        enqueueNextUrls(
            urls = firstContent.nextContentUrls,
            visitedUrls = visitedUrls,
            pendingUrls = pendingUrls,
            discoveredNextUrls = discoveredNextUrls
        )
        while (pendingUrls.isNotEmpty() && visitedUrls.size < maxContentPages) {
            val nextUrl = pendingUrls.removeFirst()
            if (!visitedUrls.add(nextUrl)) {
                continue
            }
            val nextResponse = httpFetcher.fetch(SharedHttpRequest(url = nextUrl))
            val nextContent = parseAndNormalize(source, book, chapter, nextResponse)
            if (nextContent.content.isNotBlank()) {
                contentParts.add(nextContent.content)
            }
            enqueueNextUrls(
                urls = nextContent.nextContentUrls,
                visitedUrls = visitedUrls,
                pendingUrls = pendingUrls,
                discoveredNextUrls = discoveredNextUrls
            )
        }
        return ChapterContentResult(
            source = source,
            book = book,
            chapter = chapter,
            response = firstResponse,
            content = firstContent.copy(
                content = contentParts.joinToString("\n"),
                nextContentUrls = discoveredNextUrls
            )
        )
    }

    private fun parseAndNormalize(
        source: SharedBookSource,
        book: SharedBook,
        chapter: SharedBookChapter,
        response: SharedHttpResponse
    ): SharedChapterContent {
        val content = chapterContentParser.parse(source, book, chapter, response.body)
        return content.copy(
            nextContentUrls = SharedUrlResolver.resolveAll(
                response.finalUrl,
                content.nextContentUrls
            )
        )
    }

    private fun enqueueNextUrls(
        urls: List<String>,
        visitedUrls: Set<String>,
        pendingUrls: ArrayDeque<String>,
        discoveredNextUrls: MutableList<String>
    ) {
        urls.forEach { url ->
            if (url.isBlank()) {
                return@forEach
            }
            if (url !in discoveredNextUrls) {
                discoveredNextUrls.add(url)
            }
            if (url !in visitedUrls && url !in pendingUrls) {
                pendingUrls.addLast(url)
            }
        }
    }
}

data class ChapterContentResult(
    val source: SharedBookSource,
    val book: SharedBook,
    val chapter: SharedBookChapter,
    val response: SharedHttpResponse,
    val content: SharedChapterContent
)

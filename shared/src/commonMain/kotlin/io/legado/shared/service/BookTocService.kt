package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse

class BookTocService(
    private val httpFetcher: HttpFetcher,
    private val chapterListParser: ChapterListParser = RegexChapterListParser,
    private val suspendChapterListParser: SuspendChapterListParser? = null,
    private val maxTocPages: Int = 20
) {
    suspend fun getChapterList(
        source: SharedBookSource,
        book: SharedBook
    ): ChapterListResult {
        val requestUrl = book.tocUrl.ifBlank { book.bookUrl }
        val visitedUrls = linkedSetOf<String>()
        val pendingUrls = ArrayDeque<String>()
        val allChapters = mutableListOf<SharedBookChapter>()
        val firstResponse = httpFetcher.fetch(SharedRequestBuilder.build(requestUrl))
        visitedUrls.add(requestUrl)
        val firstPage = parseAndNormalize(source, book, firstResponse)
        allChapters.addAll(firstPage.chapters)
        enqueueNextUrls(firstPage.nextTocUrls, visitedUrls, pendingUrls)
        while (pendingUrls.isNotEmpty() && visitedUrls.size < maxTocPages) {
            val nextUrl = pendingUrls.removeFirst()
            if (!visitedUrls.add(nextUrl)) {
                continue
            }
            val nextResponse = httpFetcher.fetch(SharedRequestBuilder.build(nextUrl))
            val nextPage = parseAndNormalize(source, book, nextResponse)
            allChapters.addAll(nextPage.chapters)
            enqueueNextUrls(nextPage.nextTocUrls, visitedUrls, pendingUrls)
        }
        return ChapterListResult(
            source = source,
            book = book,
            response = firstResponse,
            chapters = allChapters.mapIndexed { index, chapter -> chapter.copy(index = index) }
        )
    }

    private suspend fun parseAndNormalize(
        source: SharedBookSource,
        book: SharedBook,
        response: SharedHttpResponse
    ): ChapterListPage {
        return ChapterListPage(
            chapters = (suspendChapterListParser?.parse(source, book, response.body)
                ?: chapterListParser.parse(source, response.body))
                .map { chapter ->
                    chapter.copy(
                        url = chapter.url
                            .takeIf { it.isNotBlank() }
                            ?.let { SharedUrlResolver.resolve(response.finalUrl, it) }
                            ?: chapter.url
                    )
                },
            nextTocUrls = SharedUrlResolver.resolveAll(
                response.finalUrl,
                suspendChapterListParser?.parseNextUrls(source, book, response.body)
                    ?: chapterListParser.parseNextUrls(source, response.body)
            )
        )
    }

    private fun enqueueNextUrls(
        urls: List<String>,
        visitedUrls: Set<String>,
        pendingUrls: ArrayDeque<String>
    ) {
        urls.forEach { url ->
            if (url.isNotBlank() && url !in visitedUrls && url !in pendingUrls) {
                pendingUrls.addLast(url)
            }
        }
    }

    private data class ChapterListPage(
        val chapters: List<SharedBookChapter>,
        val nextTocUrls: List<String>
    )
}

data class ChapterListResult(
    val source: SharedBookSource,
    val book: SharedBook,
    val response: SharedHttpResponse,
    val chapters: List<SharedBookChapter>
)

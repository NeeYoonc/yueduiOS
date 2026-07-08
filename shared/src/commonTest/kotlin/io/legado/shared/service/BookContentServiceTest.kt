package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedChapterContent
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BookContentServiceTest {
    @Test
    fun fetchesChapterUrlAndDelegatesBodyToParser() = runBlocking {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )
        val book = SharedBook(
            name = "Metal Story",
            bookUrl = "https://source.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(
            title = "Chapter 1",
            url = "https://source.test/book/1/chapter/1.html"
        )
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://source.test/book/1/chapter/1.html", request.url)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = "raw content body"
                )
            }
        }
        val parser = object : ChapterContentParser {
            override fun parse(
                source: SharedBookSource,
                book: SharedBook,
                chapter: SharedBookChapter,
                body: String
            ): SharedChapterContent {
                assertEquals("https://source.test", source.bookSourceUrl)
                assertEquals("Metal Story", book.name)
                assertEquals("Chapter 1", chapter.title)
                assertEquals("raw content body", body)
                return SharedChapterContent(
                    content = "Parsed content",
                    nextContentUrls = emptyList()
                )
            }
        }

        val result = BookContentService(fetcher, parser).getContent(source, book, chapter)

        assertEquals(source, result.source)
        assertEquals(book, result.book)
        assertEquals(chapter, result.chapter)
        assertEquals("https://source.test/book/1/chapter/1.html", result.response.finalUrl)
        assertEquals("Parsed content", result.content.content)
        assertEquals(emptyList(), result.content.nextContentUrls)
    }

    @Test
    fun fallsBackToBookUrlWhenChapterUrlIsBlank() = runBlocking {
        val source = SharedBookSource(
            bookSourceUrl = "https://fallback.test",
            bookSourceName = "Fallback"
        )
        val book = SharedBook(
            name = "Fallback Book",
            bookUrl = "https://fallback.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(title = "Chapter 1")
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://fallback.test/book/1", request.url)
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = ""
                )
            }
        }

        val result = BookContentService(fetcher).getContent(source, book, chapter)

        assertEquals("", result.content.content)
        assertEquals(emptyList(), result.content.nextContentUrls)
    }

    @Test
    fun followsNextContentUrlsAndMergesPageContent() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )
        val book = SharedBook(
            name = "Metal Story",
            bookUrl = "https://source.test/book/1",
            origin = source.bookSourceUrl
        )
        val chapter = SharedBookChapter(
            title = "Chapter 1",
            url = "https://source.test/book/1/chapter/1.html"
        )
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                val body = when (request.url) {
                    "https://source.test/book/1/chapter/1.html" -> "Page 1|next=page-2.html"
                    "https://source.test/book/1/chapter/page-2.html" -> "Page 2|next=page-3.html"
                    "https://source.test/book/1/chapter/page-3.html" -> "Page 3"
                    else -> error("Unexpected request URL: ${request.url}")
                }
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = body
                )
            }
        }
        val parser = object : ChapterContentParser {
            override fun parse(
                source: SharedBookSource,
                book: SharedBook,
                chapter: SharedBookChapter,
                body: String
            ): SharedChapterContent {
                val content = body.substringBefore("|next=").trim()
                val next = body.substringAfter("|next=", missingDelimiterValue = "")
                    .trim()
                    .takeIf { it.isNotEmpty() }
                return SharedChapterContent(
                    content = content,
                    title = if (content == "Page 1") "Parsed Chapter 1" else null,
                    subContent = if (content == "Page 1") "Side note" else null,
                    nextContentUrls = listOfNotNull(next)
                )
            }
        }

        val result = BookContentService(fetcher, parser).getContent(source, book, chapter)

        assertEquals(
            listOf(
                "https://source.test/book/1/chapter/1.html",
                "https://source.test/book/1/chapter/page-2.html",
                "https://source.test/book/1/chapter/page-3.html"
            ),
            requestedUrls
        )
        assertEquals("Page 1\nPage 2\nPage 3", result.content.content)
        assertEquals(
            listOf(
                "https://source.test/book/1/chapter/page-2.html",
                "https://source.test/book/1/chapter/page-3.html"
            ),
            result.content.nextContentUrls
        )
        assertEquals("Parsed Chapter 1", result.content.title)
        assertEquals("Side note", result.content.subContent)
    }
}

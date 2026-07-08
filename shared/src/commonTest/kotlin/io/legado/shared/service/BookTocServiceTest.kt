package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BookTocServiceTest {
    @Test
    fun fetchesBookTocUrlAndDelegatesBodyToParser() = runBlocking {
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )
        val book = SharedBook(
            name = "Metal Story",
            author = "Tester",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1/catalog",
            origin = source.bookSourceUrl
        )
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                assertEquals("https://source.test/book/1/catalog", request.url)
                return SharedHttpResponse(
                    finalUrl = "https://source.test/book/1/catalog/index.html",
                    statusCode = 200,
                    body = "raw toc body"
                )
            }
        }
        val parser = object : ChapterListParser {
            override fun parse(
                source: SharedBookSource,
                body: String
            ): List<SharedBookChapter> {
                assertEquals("https://source.test", source.bookSourceUrl)
                assertEquals("raw toc body", body)
                return listOf(
                    SharedBookChapter(
                        title = "Chapter From Parser",
                        url = "chapter/1.html",
                        index = 0
                    )
                )
            }
        }

        val result = BookTocService(fetcher, parser).getChapterList(source, book)

        assertEquals(source, result.source)
        assertEquals(book, result.book)
        assertEquals("https://source.test/book/1/catalog/index.html", result.response.finalUrl)
        assertEquals(1, result.chapters.size)
        assertEquals("Chapter From Parser", result.chapters.single().title)
        assertEquals("https://source.test/book/1/catalog/chapter/1.html", result.chapters.single().url)
    }

    @Test
    fun fallsBackToBookUrlWhenTocUrlIsBlank() = runBlocking {
        val source = SharedBookSource(
            bookSourceUrl = "https://fallback.test",
            bookSourceName = "Fallback"
        )
        val book = SharedBook(
            name = "Fallback Book",
            bookUrl = "https://fallback.test/book/1",
            tocUrl = "",
            origin = source.bookSourceUrl
        )
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

        val result = BookTocService(fetcher).getChapterList(source, book)

        assertEquals(emptyList(), result.chapters)
    }

    @Test
    fun followsNextTocUrlsAndMergesChapters() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source"
        )
        val book = SharedBook(
            name = "Metal Story",
            bookUrl = "https://source.test/book/1",
            tocUrl = "https://source.test/book/1/catalog/index.html",
            origin = source.bookSourceUrl
        )
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                requestedUrls.add(request.url)
                val body = when (request.url) {
                    "https://source.test/book/1/catalog/index.html" -> "Chapter 1|next=page-2.html"
                    "https://source.test/book/1/catalog/page-2.html" -> "Chapter 2|next=page-3.html"
                    "https://source.test/book/1/catalog/page-3.html" -> "Chapter 3"
                    else -> error("Unexpected request URL: ${request.url}")
                }
                return SharedHttpResponse(
                    finalUrl = request.url,
                    statusCode = 200,
                    body = body
                )
            }
        }
        val parser = object : ChapterListParser {
            override fun parse(
                source: SharedBookSource,
                body: String
            ): List<SharedBookChapter> {
                return listOf(
                    SharedBookChapter(
                        title = body.substringBefore("|next=").trim(),
                        url = "${body.substringBefore("|next=").lowercase().replace(" ", "-")}.html"
                    )
                )
            }

            override fun parseNextUrls(source: SharedBookSource, body: String): List<String> {
                return body.substringAfter("|next=", missingDelimiterValue = "")
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { listOf(it) }
                    ?: emptyList()
            }
        }

        val result = BookTocService(fetcher, parser).getChapterList(source, book)

        assertEquals(
            listOf(
                "https://source.test/book/1/catalog/index.html",
                "https://source.test/book/1/catalog/page-2.html",
                "https://source.test/book/1/catalog/page-3.html"
            ),
            requestedUrls
        )
        assertEquals(3, result.chapters.size)
        assertEquals("Chapter 1", result.chapters[0].title)
        assertEquals(0, result.chapters[0].index)
        assertEquals("https://source.test/book/1/catalog/chapter-1.html", result.chapters[0].url)
        assertEquals("Chapter 2", result.chapters[1].title)
        assertEquals(1, result.chapters[1].index)
        assertEquals("https://source.test/book/1/catalog/chapter-2.html", result.chapters[1].url)
        assertEquals("Chapter 3", result.chapters[2].title)
        assertEquals(2, result.chapters[2].index)
        assertEquals("https://source.test/book/1/catalog/chapter-3.html", result.chapters[2].url)
    }
}

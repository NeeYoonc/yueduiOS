package io.legado.shared.source

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.model.SharedTocRule
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceDebugServiceTest {
    @Test
    fun debugsSearchDetailTocAndContentSteps() = runBlocking {
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return when (request.url) {
                    "https://debug.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """{"items":[{"title":"Debug Book","url":"/book/1"}]}"""
                    )
                    "https://debug.test/book/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """{"book":{"toc":"/book/1/toc"}}"""
                    )
                    "https://debug.test/book/1/toc" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """{"chapters":[{"title":"Chapter 1","url":"/book/1/1"}]}"""
                    )
                    "https://debug.test/book/1/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """{"content":{"text":"Debug content."}}"""
                    )
                    else -> error("Unexpected ${request.url}")
                }
            }
        }
        val service = SourceDebugService(LegadoSharedClient(fetcher))
        val source = SharedBookSource(
            bookSourceUrl = "https://debug.test",
            bookSourceName = "Debug",
            searchUrl = "https://debug.test/search?q={{key}}&page={{page}}",
            ruleSearch = SharedSearchRule(
                bookList = "$.items",
                name = "$.title",
                bookUrl = "$.url"
            ),
            ruleBookInfo = SharedBookInfoRule(tocUrl = "$.book.toc"),
            ruleToc = SharedTocRule(
                chapterList = "$.chapters",
                chapterName = "$.title",
                chapterUrl = "$.url"
            ),
            ruleContent = SharedContentRule(content = "$.content.text")
        )

        val result = service.debugFirstContent(source, "metal")

        assertEquals("Debug Book", result.search.books.single().name)
        assertEquals("Chapter 1", result.chapterList?.chapters?.single()?.title)
        assertEquals("Debug content.", result.content?.content?.content)
        assertEquals(
            listOf("search", "detail", "toc", "content"),
            result.steps.map { it.stage }
        )
    }
}

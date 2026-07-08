package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpRequest
import io.legado.shared.platform.SharedHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LegadoRuntimeTest {
    @Test
    fun importsSourcesOpensFirstResultAndCachesContent() = runBlocking {
        val cache = InMemoryCacheStore()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return when (request.url) {
                    "https://runtime.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            name=Metal Runtime
                            author=Tester
                            bookUrl=https://runtime.test/book/1
                        """.trimIndent()
                    )

                    "https://runtime.test/book/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<a class="toc" href="/book/1/toc.html">toc</a>"""
                    )

                    "https://runtime.test/book/1/toc.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<a class="chapter" href="/book/1/chapter/1.html">Chapter 1</a>"""
                    )

                    "https://runtime.test/book/1/chapter/1.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<main>Cached line.</main>"""
                    )

                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val runtime = LegadoRuntime(fetcher, cache)

        val sources = runtime.importAndSaveBookSources(
            """
            {
              "bookSourceUrl": "https://runtime.test",
              "bookSourceName": "Runtime",
              "searchUrl": "https://runtime.test/search?q={{key}}&page={{page}}",
              "ruleBookInfo": {
                "tocUrl": "<a class=\"toc\" href=\"([^\"]+)\">"
              },
              "ruleToc": {
                "chapterList": "<a class=\"chapter\" href=\"([^\"]+)\">([^<]+)</a>",
                "chapterUrl": "${'$'}1",
                "chapterName": "${'$'}2"
              },
              "ruleContent": {
                "content": "<main>([\\s\\S]*?)</main>"
              }
            }
            """.trimIndent()
        )
        val result = runtime.openFirstSearchResult(sources.single(), "metal")

        assertEquals(sources, runtime.loadBookSources())
        assertEquals("Metal Runtime", result.selectedBook?.name)
        assertEquals("Cached line.", result.content?.content?.content)
        assertEquals("Metal Runtime", runtime.loadBooks().single().name)
        assertEquals("Chapter 1", runtime.loadBooks().single().durChapterTitle)
        assertEquals(
            result.content?.content,
            runtime.libraryStore.loadChapterContent(
                result.selectedBook!!,
                result.selectedChapter!!
            )
        )
    }

    @Test
    fun opensSelectedSearchResultAndLoadsChosenChapter() = runBlocking {
        val cache = InMemoryCacheStore()
        val fetcher = object : HttpFetcher {
            override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                return when (request.url) {
                    "https://runtime-select.test/search?q=metal&page=1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            name=Metal Select
                            author=Tester
                            bookUrl=https://runtime-select.test/book/1
                        """.trimIndent()
                    )

                    "https://runtime-select.test/book/1" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<a class="toc" href="/book/1/toc.html">toc</a>"""
                    )

                    "https://runtime-select.test/book/1/toc.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """
                            <a class="chapter" href="/book/1/chapter/1.html">Chapter 1</a>
                            <a class="chapter" href="/book/1/chapter/2.html">Chapter 2</a>
                        """.trimIndent()
                    )

                    "https://runtime-select.test/book/1/chapter/1.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<main>First chapter.</main>"""
                    )

                    "https://runtime-select.test/book/1/chapter/2.html" -> SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """<main>Second chapter.</main>"""
                    )

                    else -> error("Unexpected request URL: ${request.url}")
                }
            }
        }
        val runtime = LegadoRuntime(fetcher, cache)
        val source = runtime.importAndSaveBookSources(
            """
            {
              "bookSourceUrl": "https://runtime-select.test",
              "bookSourceName": "Runtime Select",
              "searchUrl": "https://runtime-select.test/search?q={{key}}&page={{page}}",
              "ruleBookInfo": {
                "tocUrl": "<a class=\"toc\" href=\"([^\"]+)\">"
              },
              "ruleToc": {
                "chapterList": "<a class=\"chapter\" href=\"([^\"]+)\">([^<]+)</a>",
                "chapterUrl": "${'$'}1",
                "chapterName": "${'$'}2"
              },
              "ruleContent": {
                "content": "<main>([\\s\\S]*?)</main>"
              }
            }
            """.trimIndent()
        ).single()

        val search = runtime.client.search(source, "metal")
        val detail = runtime.openSearchBook(source, search.books.single(), nowMillis = 11L)
        val read = runtime.loadChapter(source, detail.book, chapterIndex = 1, nowMillis = 22L)

        assertEquals(listOf("Chapter 1", "Chapter 2"), runtime.loadBookChapters(detail.book).map { it.title })
        assertEquals("Second chapter.", read.content.content)
        assertEquals("Chapter 2", runtime.loadBooks().single().durChapterTitle)
        assertEquals(1, runtime.loadBooks().single().durChapterIndex)
        assertEquals(22L, runtime.loadBooks().single().durChapterTime)
    }

    @Test
    fun managesBookSourcesThroughRuntimeRepository() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.importAndSaveBookSources(
            """
            [
              {"bookSourceUrl":"https://one.test","bookSourceName":"One"},
              {"bookSourceUrl":"https://two.test","bookSourceName":"Two"}
            ]
            """.trimIndent()
        )

        runtime.setBookSourceEnabled("https://two.test", false)
        assertEquals(false, runtime.loadBookSources().first { it.bookSourceUrl == "https://two.test" }.enabled)

        val exported = runtime.exportBookSourcesJson()
        assertEquals(true, exported.contains("https://one.test"))

        runtime.deleteBookSource("https://one.test")
        assertEquals(listOf("https://two.test"), runtime.loadBookSources().map { it.bookSourceUrl })
    }

    @Test
    fun managesBookGroupsThroughRuntimeRepository() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.saveBooks(
            listOf(
                SharedBook(name = "A", bookUrl = "a", group = 0L),
                SharedBook(name = "B", bookUrl = "b", group = 0L)
            )
        )

        val group = runtime.upsertBookGroup(SharedBookGroup(groupId = 0L, groupName = "Favorites"))
        runtime.setBookGroupEnabled(runtime.loadBooks().first(), group.groupId, enabled = true)

        assertEquals("All", runtime.loadBookGroups().first().groupName)
        assertEquals(listOf("A"), runtime.loadBooksForGroup(group.groupId).map { it.name })

        runtime.deleteBookGroup(group.groupId)
        assertEquals(0L, runtime.loadBooks().first { it.name == "A" }.group)
    }

    @Test
    fun managesSearchKeywordsThroughRuntimeRepository() = runBlocking {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    return SharedHttpResponse(finalUrl = request.url, statusCode = 200, body = "")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.importAndSaveBookSources(
            """
            {"bookSourceUrl":"https://one.test","bookSourceName":"One","searchUrl":"https://one.test/search?q={{key}}"}
            """.trimIndent()
        )

        runtime.searchEnabledSources("first", nowMillis = 1L)
        runtime.searchEnabledSources("second", nowMillis = 2L)
        runtime.recordSearchKeyword("manual", nowMillis = 3L)

        assertEquals(listOf("manual", "second", "first"), runtime.loadSearchKeywords().map { it.word })

        runtime.deleteSearchKeyword("second")
        assertEquals(listOf("manual", "first"), runtime.loadSearchKeywords().map { it.word })

        runtime.clearSearchKeywords()
        assertEquals(emptyList(), runtime.loadSearchKeywords())
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }
}

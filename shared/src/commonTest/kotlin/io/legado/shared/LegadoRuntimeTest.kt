package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedCookie
import io.legado.shared.model.SharedRuleSub
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
    fun importsBookSourcesFromRemoteUrl() = runBlocking {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    assertEquals("https://remote-source.test/bookSources.json", request.url)
                    return SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """[{"bookSourceUrl":"https://remote-source.test","bookSourceName":"Remote Source"}]"""
                    )
                }
            },
            cacheStore = InMemoryCacheStore()
        )

        val imported = runtime.importBookSourcesFromUrl("https://remote-source.test/bookSources.json")

        assertEquals(listOf("Remote Source"), imported.map { it.bookSourceName })
        assertEquals(imported, runtime.loadBookSources())
    }

    @Test
    fun upsertsSingleBookSourceJsonWithoutReplacingOtherSources() {
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
              {"bookSourceUrl":"https://keep.test","bookSourceName":"Keep"},
              {"bookSourceUrl":"https://edit.test","bookSourceName":"Old"}
            ]
            """.trimIndent()
        )

        val saved = runtime.upsertBookSourceJson(
            """
            {
              "bookSourceUrl":"https://edit.test",
              "bookSourceName":"Edited",
              "bookSourceGroup":"Group A",
              "enabled":false,
              "enabledExplore":false,
              "searchUrl":"https://edit.test/search?q={{key}}",
              "ruleSearch":{"bookList":"$.data","name":"$.title"},
              "ruleToc":{"chapterList":"$.chapters","chapterName":"$.name"},
              "ruleContent":{"content":"$.content","nextContentUrl":"$.next"}
            }
            """.trimIndent()
        )

        assertEquals("Edited", saved.bookSourceName)
        assertEquals(false, saved.enabled)
        assertEquals(listOf("Keep", "Edited"), runtime.loadBookSources().map { it.bookSourceName })
        assertEquals("$.data", runtime.loadBookSources().first { it.bookSourceUrl == "https://edit.test" }.ruleSearch?.bookList)
        assertEquals("$.content", runtime.loadBookSources().first { it.bookSourceUrl == "https://edit.test" }.ruleContent?.content)
    }

    @Test
    fun importsRssSourcesAndReplaceRulesFromRemoteUrl() = runBlocking {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    return when (request.url) {
                        "https://remote-source.test/rssSources.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"sourceUrl":"https://remote-rss.test/feed","sourceName":"Remote RSS"}]"""
                        )

                        "https://remote-source.test/replaceRules.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"id":10,"name":"Remote Replace","pattern":"x","replacement":"y"}]"""
                        )

                        else -> error("Unexpected ${request.url}")
                    }
                }
            },
            cacheStore = InMemoryCacheStore()
        )

        val rss = runtime.importRssSourcesFromUrl("https://remote-source.test/rssSources.json")
        val replace = runtime.importReplaceRulesFromUrl("https://remote-source.test/replaceRules.json")

        assertEquals(listOf("Remote RSS"), rss.map { it.sourceName })
        assertEquals(listOf("Remote Replace"), replace.map { it.name })
        assertEquals(rss, runtime.loadRssSources())
        assertEquals(replace, runtime.loadReplaceRules())
    }

    @Test
    fun importsConfigRulesCookiesAndCachesFromRemoteUrl() = runBlocking {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    return when (request.url) {
                        "https://remote-config.test/dict.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"name":"Remote Dict","urlRule":"https://dict.test/{{word}}","showRule":"text"}]"""
                        )

                        "https://remote-config.test/tts.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"id":2,"name":"Remote TTS","url":"https://tts.test/speak?text={{text}}"}]"""
                        )

                        "https://remote-config.test/txt-toc.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"id":3,"name":"Remote TXT","rule":"^第.+章"}]"""
                        )

                        "https://remote-config.test/servers.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"id":4,"name":"Remote WebDAV","type":"WEBDAV","config":"{\"url\":\"https://dav.test\"}"}]"""
                        )

                        "https://remote-config.test/keyboard.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"type":1,"key":"volumeUp","value":"next"}]"""
                        )

                        "https://remote-config.test/subs.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"id":5,"name":"Remote Sub","url":"https://sub.test/books.json","type":0}]"""
                        )

                        "https://remote-config.test/raw.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """{"theme":"dark","readAloud":"on"}"""
                        )

                        "https://remote-config.test/cookies.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"url":"https://cookie.test","cookie":"sid=1"}]"""
                        )

                        "https://remote-config.test/cache.json" -> SharedHttpResponse(
                            finalUrl = request.url,
                            statusCode = 200,
                            body = """[{"key":"remote-cache","value":"cached","deadline":99}]"""
                        )

                        else -> error("Unexpected ${request.url}")
                    }
                }
            },
            cacheStore = InMemoryCacheStore()
        )

        val dictRules = runtime.importDictRulesFromUrl("https://remote-config.test/dict.json")
        val httpTts = runtime.importHttpTtsFromUrl("https://remote-config.test/tts.json")
        val txtTocRules = runtime.importTxtTocRulesFromUrl("https://remote-config.test/txt-toc.json")
        val servers = runtime.importServersFromUrl("https://remote-config.test/servers.json")
        val keyboardAssists = runtime.importKeyboardAssistsFromUrl("https://remote-config.test/keyboard.json")
        val ruleSubs = runtime.importRuleSubsFromUrl("https://remote-config.test/subs.json")
        val rawConfigs = runtime.importRawConfigsFromUrl("https://remote-config.test/raw.json")
        val cookies = runtime.importCookiesFromUrl("https://remote-config.test/cookies.json")
        val cacheEntries = runtime.importCacheEntriesFromUrl("https://remote-config.test/cache.json")

        assertEquals(listOf("Remote Dict"), dictRules.map { it.name })
        assertEquals(listOf("Remote TTS"), httpTts.map { it.name })
        assertEquals(listOf("Remote TXT"), txtTocRules.map { it.name })
        assertEquals(listOf("Remote WebDAV"), servers.map { it.name })
        assertEquals(listOf("volumeUp"), keyboardAssists.map { it.key })
        assertEquals(listOf("Remote Sub"), ruleSubs.map { it.name })
        assertEquals(listOf("readAloud", "theme"), rawConfigs.map { it.key })
        assertEquals(listOf("https://cookie.test"), cookies.map { it.url })
        assertEquals(listOf("remote-cache"), cacheEntries.map { it.key })
        assertEquals(dictRules, runtime.loadDictRules())
        assertEquals(httpTts, runtime.loadHttpTts())
        assertEquals(txtTocRules, runtime.loadTxtTocRules())
        assertEquals(servers, runtime.loadServers())
        assertEquals(keyboardAssists, runtime.loadKeyboardAssists())
        assertEquals(ruleSubs, runtime.loadRuleSubs())
        assertEquals(rawConfigs, runtime.loadRawConfigs())
        assertEquals(cookies, runtime.loadCookies())
        assertEquals(cacheEntries, runtime.loadCacheEntries())
    }

    @Test
    fun exposesSourceWebLoginRequestAndStoresSourceCookie() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        val source = SharedBookSource(
            bookSourceUrl = "https://runtime-login.test",
            bookSourceName = "Runtime Login",
            header = """{"User-Agent":"RuntimeUA"}""",
            loginUrl = "/login",
            enabledCookieJar = true
        )
        runtime.upsertCookie(SharedCookie(url = source.bookSourceUrl, cookie = "sid=stored"))

        val request = runtime.buildSourceWebLoginRequest(source)
        runtime.saveSourceWebLoginCookie(source, "token=web")

        assertEquals("https://runtime-login.test/login", request?.url)
        assertEquals("RuntimeUA", request?.headers?.value("User-Agent"))
        assertEquals("sid=stored", request?.headers?.value("Cookie"))
        assertEquals("token=web", runtime.loadCookies().single { it.url == source.bookSourceUrl }.cookie)
    }

    @Test
    fun exposesStructuredSourceLoginFieldsAndPersistsLoginInfo() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        val source = SharedBookSource(
            bookSourceUrl = "https://runtime-login-form.test",
            bookSourceName = "Runtime Login Form",
            loginUrl = "function login() {}",
            loginUi = """
            [
              {"name":"telephone","type":"text"},
              {"name":"password","type":"password","default":"123456"}
            ]
            """.trimIndent()
        )

        val fields = runtime.loadSourceLoginFields(source)
        val defaultInfo = runtime.loadSourceLoginInfoJson(source)
        val saved = runtime.saveSourceLoginInfoJson(
            source,
            """{"telephone":"13800000000","password":"pw"}"""
        )

        assertEquals(listOf("telephone", "password"), fields.map { it.name })
        assertEquals("""{"telephone":"","password":"123456"}""", defaultInfo.replace(Regex("\\s"), ""))
        assertEquals("userInfo_${source.bookSourceUrl}", saved.key)
        assertEquals("""{"telephone":"13800000000","password":"pw"}""", runtime.loadSourceLoginInfoJson(source).replace(Regex("\\s"), ""))

        runtime.clearSourceLoginInfo(source)
        assertEquals(defaultInfo.replace(Regex("\\s"), ""), runtime.loadSourceLoginInfoJson(source).replace(Regex("\\s"), ""))
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

    @Test
    fun updatesRuleSubscriptionsThroughRuntime() = runBlocking {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    assertEquals("https://runtime-sub.test/books.json", request.url)
                    return SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """[{"bookSourceUrl":"https://runtime-sub.test","bookSourceName":"Runtime Sub"}]"""
                    )
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        val sub = runtime.upsertRuleSub(
            SharedRuleSub(
                id = 1,
                name = "Runtime Books",
                url = "https://runtime-sub.test/books.json",
                type = 0,
                autoUpdate = true
            )
        )

        val result = runtime.updateRuleSub(sub, nowMillis = 10L)
        val autoResults = runtime.updateAutoRuleSubs(nowMillis = 11L)

        assertEquals(1, result.importedCount)
        assertEquals("Runtime Sub", runtime.loadBookSources().single().bookSourceName)
        assertEquals(listOf(11L), autoResults.map { it.ruleSub.update })
    }

    @Test
    fun loadsExploreKindsAndExplorePageThroughRuntime() = runBlocking {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    assertEquals("https://explore.test/rank?page=1", request.url)
                    return SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = "book|Explore Book|/book/1"
                    )
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        val source = runtime.importAndSaveBookSources(
            """
            {
              "bookSourceUrl":"https://explore.test",
              "bookSourceName":"Explore",
              "exploreUrl":"Rank::/rank?page={{page}}",
              "ruleExplore":{
                "bookList":"book\\|([^|]+)\\|([^\\n]+)",
                "name":"${'$'}1",
                "bookUrl":"${'$'}2"
              }
            }
            """.trimIndent()
        ).single()

        val kind = runtime.loadExploreKinds(source).single()
        val page = runtime.loadExplorePage(source, kind, page = 1)

        assertEquals(listOf("Explore"), runtime.loadExploreSources().map { it.bookSourceName })
        assertEquals("Rank", kind.title)
        assertEquals("Explore Book", page.books.single().name)
    }

    private class InMemoryCacheStore : CacheStorePort {
        private val values = mutableMapOf<String, String>()

        override fun getText(key: String): String? = values[key]

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }

    private fun Map<String, String>.value(name: String): String? {
        return entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }
}

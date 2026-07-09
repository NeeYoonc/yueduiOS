package io.legado.shared

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedCookie
import io.legado.shared.model.SharedHttpTts
import io.legado.shared.model.SharedRuleSub
import io.legado.shared.platform.CacheStorePort
import io.legado.shared.platform.HttpFetcher
import io.legado.shared.platform.SharedHttpMethod
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
    fun buildsHttpTtsAudioRequestWithSpeakTextSpeedHeadersAndCookies() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.upsertCookie(SharedCookie(url = "httpTts:7", cookie = "sid=tts"))
        val engine = SharedHttpTts(
            id = 7,
            name = "Remote TTS",
            url = """
                https://tts.test/say?text={{speakText}}&speed={{speakSpeed}},
                {"method":"POST","headers":{"Accept":"audio/mpeg"},"body":{"q":"{{speakText}}","speed":"{{speakSpeed}}"}}
            """.trimIndent(),
            header = """{"User-Agent":"LegadoTTS"}""",
            enabledCookieJar = true
        )

        val request = runtime.buildHttpTtsAudioRequest(engine, "你好 world", speechRate = 17)

        assertEquals(SharedHttpMethod.POST, request.method)
        assertEquals("https://tts.test/say?text=%E4%BD%A0%E5%A5%BD%20world&speed=17", request.url)
        assertEquals("audio/mpeg", request.headers.value("Accept"))
        assertEquals("LegadoTTS", request.headers.value("User-Agent"))
        assertEquals("sid=tts", request.headers.value("Cookie"))
        assertEquals("""{"q":"你好 world","speed":"17"}""", request.body)
    }

    @Test
    fun upsertsSingleReplaceRuleJsonWithoutReplacingOtherRules() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.importAndSaveReplaceRules(
            """
            [
              {"id":1,"name":"Keep","pattern":"a","replacement":"b"},
              {"id":2,"name":"Old","pattern":"x","replacement":"y"}
            ]
            """.trimIndent()
        )

        val saved = runtime.upsertReplaceRuleJson(
            """
            {
              "id":2,
              "name":"Edited",
              "group":"Clean",
              "pattern":"old",
              "replacement":"new",
              "scopeTitle":true,
              "scopeContent":false,
              "isRegex":false,
              "isEnabled":false,
              "timeoutMillisecond":99,
              "order":7
            }
            """.trimIndent()
        )

        assertEquals("Edited", saved.name)
        assertEquals(false, saved.enabled)
        assertEquals(listOf("Keep", "Edited"), runtime.loadReplaceRules().map { it.name })
        assertEquals(false, runtime.loadReplaceRules().first { it.id == 2L }.regex)
        assertEquals(true, runtime.loadReplaceRules().first { it.id == 2L }.scopeTitle)
    }

    @Test
    fun upsertsSingleHttpTtsJsonWithoutReplacingOtherEngines() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.importAndSaveHttpTts(
            """
            [
              {"id":1,"name":"Keep","url":"https://keep.test/tts?text={{speakText}}"},
              {"id":2,"name":"Old","url":"https://old.test/tts?text={{speakText}}"}
            ]
            """.trimIndent()
        )

        val saved = runtime.upsertHttpTtsJson(
            """
            {
              "id":2,
              "name":"Edited TTS",
              "url":"https://tts.test/api?text={{speakText}}&speed={{speakSpeed}}",
              "contentType":"audio/.*",
              "concurrentRate":"2",
              "loginUrl":"https://tts.test/login",
              "loginUi":"[{\"name\":\"token\"}]",
              "header":"{\"User-Agent\":\"TTS\"}",
              "jsLib":"function sign(){}",
              "enabledCookieJar":true,
              "loginCheckJs":"result"
            }
            """.trimIndent()
        )

        assertEquals("Edited TTS", saved.name)
        assertEquals("audio/.*", saved.contentType)
        assertEquals(listOf("Edited TTS", "Keep"), runtime.loadHttpTts().map { it.name })
        assertEquals("TTS", runtime.buildHttpTtsAudioRequest(saved, "hello").headers.value("User-Agent"))
    }

    @Test
    fun upsertsSingleDictRuleJsonWithoutReplacingOtherRules() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.importAndSaveDictRules(
            """
            [
              {"name":"Keep","urlRule":"https://keep.test/{{word}}","showRule":"text"},
              {"name":"Old","urlRule":"https://old.test/{{word}}","showRule":"old"}
            ]
            """.trimIndent()
        )

        val saved = runtime.upsertDictRuleJson(
            """
            {
              "name":"Old",
              "urlRule":"https://dict.test/{{word}}",
              "showRule":"$.content",
              "enabled":false,
              "sortNumber":8
            }
            """.trimIndent()
        )

        assertEquals("$.content", saved.showRule)
        assertEquals(false, saved.enabled)
        assertEquals(listOf("Keep", "Old"), runtime.loadDictRules().map { it.name })
        assertEquals(8, runtime.loadDictRules().first { it.name == "Old" }.sortNumber)
    }

    @Test
    fun upsertsSingleConfigJsonWithoutReplacingOtherEntries() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )

        runtime.importAndSaveTxtTocRules(
            """
            [
              {"id":1,"name":"Keep TXT","rule":"^keep$","serialNumber":1},
              {"id":2,"name":"Old TXT","rule":"^old$","serialNumber":2}
            ]
            """.trimIndent()
        )
        val txt = runtime.upsertTxtTocRuleJson("""{"id":2,"name":"Old TXT","rule":"^chapter (\\d+)$","replacement":"Chapter $1","enable":false,"serialNumber":9}""")
        assertEquals("^chapter (\\d+)$", txt.rule)
        assertEquals(false, txt.enable)
        assertEquals(listOf("Keep TXT", "Old TXT"), runtime.loadTxtTocRules().map { it.name })

        runtime.importAndSaveServers(
            """
            [
              {"id":1,"name":"Keep Server","type":"WEBDAV","config":"{}","sortNumber":1},
              {"id":2,"name":"Old Server","type":"WEBDAV","config":"{}","sortNumber":2}
            ]
            """.trimIndent()
        )
        val server = runtime.upsertServerJson("""{"id":2,"name":"Old Server","type":"SFTP","config":"{\"host\":\"example.test\"}","sortNumber":7}""")
        assertEquals("SFTP", server.type)
        assertEquals(listOf("Keep Server", "Old Server"), runtime.loadServers().map { it.name })

        runtime.importAndSaveKeyboardAssists(
            """
            [
              {"type":1,"key":"keep","value":"Keep","serialNo":1},
              {"type":1,"key":"old","value":"Old","serialNo":2}
            ]
            """.trimIndent()
        )
        val assist = runtime.upsertKeyboardAssistJson("""{"type":1,"key":"old","value":"Updated","serialNo":8}""")
        assertEquals("Updated", assist.value)
        assertEquals(listOf("keep", "old"), runtime.loadKeyboardAssists().map { it.key })

        runtime.importAndSaveRuleSubs(
            """
            [
              {"id":1,"name":"Keep Sub","url":"https://keep.test/rules.json","customOrder":1},
              {"id":2,"name":"Old Sub","url":"https://old.test/rules.json","customOrder":2}
            ]
            """.trimIndent()
        )
        val sub = runtime.upsertRuleSubJson("""{"id":2,"name":"Old Sub","url":"https://new.test/rules.json","type":1,"autoUpdate":true,"customOrder":6}""")
        assertEquals("https://new.test/rules.json", sub.url)
        assertEquals(true, sub.autoUpdate)
        assertEquals(listOf("Keep Sub", "Old Sub"), runtime.loadRuleSubs().map { it.name })

        runtime.importAndSaveRawConfigs("""{"keep":"one","theme":"old"}""")
        val raw = runtime.upsertRawConfigJson("""{"key":"theme","value":"new"}""")
        assertEquals("theme", raw.key)
        assertEquals("new", raw.value)
        assertEquals(listOf("keep", "theme"), runtime.loadRawConfigs().map { it.key })

        runtime.importAndSaveCookies("""[{"url":"https://keep.test","cookie":"k=v"},{"url":"https://old.test","cookie":"old=v"}]""")
        val cookie = runtime.upsertCookieJson("""{"url":"https://old.test","cookie":"new=v"}""")
        assertEquals("new=v", cookie.cookie)
        assertEquals(listOf("https://keep.test", "https://old.test"), runtime.loadCookies().map { it.url })

        runtime.importAndSaveCacheEntries("""[{"key":"keep","value":"one","deadline":0},{"key":"old","value":"old","deadline":0}]""")
        val cache = runtime.upsertCacheEntryJson("""{"key":"old","value":"new","deadline":1234}""")
        assertEquals("new", cache.value)
        assertEquals(1234L, cache.deadline)
        assertEquals(listOf("keep", "old"), runtime.loadCacheEntries().map { it.key })
    }

    @Test
    fun upsertsSingleRssSourceJsonWithoutReplacingOtherSources() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )
        runtime.importAndSaveRssSources(
            """
            [
              {"sourceUrl":"https://keep.test/rss","sourceName":"Keep RSS","customOrder":1},
              {"sourceUrl":"https://old.test/rss","sourceName":"Old RSS","ruleArticles":"$.old","customOrder":2}
            ]
            """.trimIndent()
        )

        val saved = runtime.upsertRssSourceJson(
            """
            {
              "sourceUrl":"https://old.test/rss",
              "sourceName":"Old RSS",
              "sourceGroup":"News",
              "ruleArticles":"$.items",
              "ruleTitle":"$.title",
              "enableJs":false,
              "cacheFirst":true,
              "customOrder":8
            }
            """.trimIndent()
        )

        assertEquals("$.items", saved.ruleArticles)
        assertEquals("News", saved.sourceGroup)
        assertEquals(false, saved.enableJs)
        assertEquals(true, saved.cacheFirst)
        assertEquals(listOf("Keep RSS", "Old RSS"), runtime.loadRssSources().map { it.sourceName })
        assertEquals(8, runtime.loadRssSources().first { it.sourceUrl == "https://old.test/rss" }.customOrder)
    }

    @Test
    fun importsSmartConfigJsonByAndroidLegadoShape() {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    error("No network expected")
                }
            },
            cacheStore = InMemoryCacheStore()
        )

        val sourceSummary = runtime.importAnyConfigJson(
            """
            [
              {"bookSourceUrl":"https://source.test","bookSourceName":"Source","searchUrl":"https://source.test/search?q={{key}}"}
            ]
            """.trimIndent()
        )
        assertEquals(1, sourceSummary.bookSources)
        assertEquals(listOf("Source"), runtime.loadBookSources().map { it.bookSourceName })

        val rssSummary = runtime.importAnyConfigJson("""{"sourceUrl":"https://rss.test","sourceName":"RSS","ruleArticles":"$.items"}""")
        assertEquals(1, rssSummary.rssSources)
        assertEquals(listOf("RSS"), runtime.loadRssSources().map { it.sourceName })

        val replaceSummary = runtime.importAnyConfigJson("""{"pattern":"bad","replacement":"good","name":"Replace"}""")
        assertEquals(1, replaceSummary.replaceRules)
        assertEquals(listOf("Replace"), runtime.loadReplaceRules().map { it.name })

        val ttsSummary = runtime.importAnyConfigJson("""{"name":"TTS","url":"https://tts.test?s={{speakText}}","contentType":"audio/.*"}""")
        assertEquals(1, ttsSummary.httpTts)
        assertEquals(listOf("TTS"), runtime.loadHttpTts().map { it.name })

        val ruleSubSummary = runtime.importAnyConfigJson("""{"name":"Sub","url":"https://rules.test/all.json","autoUpdate":true}""")
        assertEquals(1, ruleSubSummary.ruleSubs)
        assertEquals(listOf("Sub"), runtime.loadRuleSubs().map { it.name })

        val rawSummary = runtime.importAnyConfigJson("""{"readConfig":"{\"fontSize\":20}"}""")
        assertEquals(1, rawSummary.rawConfigs)
        assertEquals(listOf("readConfig"), runtime.loadRawConfigs().map { it.key })

        val backupSummary = runtime.importAnyConfigJson(
            """
            {
              "bookSources":[{"bookSourceUrl":"https://backup-source.test","bookSourceName":"Backup Source"}],
              "rssSources":[{"sourceUrl":"https://backup-rss.test","sourceName":"Backup RSS"}],
              "cookies":[{"url":"https://backup-source.test","cookie":"a=b"}]
            }
            """.trimIndent(),
            replace = true
        )
        assertEquals(1, backupSummary.bookSources)
        assertEquals(1, backupSummary.rssSources)
        assertEquals(1, backupSummary.cookies)
        assertEquals(listOf("Backup Source"), runtime.loadBookSources().map { it.bookSourceName })
        assertEquals(listOf("Backup RSS"), runtime.loadRssSources().map { it.sourceName })
    }

    @Test
    fun importsSmartConfigJsonFromRemoteUrl() = runBlocking {
        val runtime = LegadoRuntime(
            httpFetcher = object : HttpFetcher {
                override suspend fun fetch(request: SharedHttpRequest): SharedHttpResponse {
                    assertEquals("https://share.test/source.json", request.url)
                    return SharedHttpResponse(
                        finalUrl = request.url,
                        statusCode = 200,
                        body = """{"bookSourceUrl":"https://remote-source.test","bookSourceName":"Remote Source"}"""
                    )
                }
            },
            cacheStore = InMemoryCacheStore()
        )

        val summary = runtime.importAnyConfigFromUrl("https://share.test/source.json")

        assertEquals(1, summary.bookSources)
        assertEquals(listOf("Remote Source"), runtime.loadBookSources().map { it.bookSourceName })
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

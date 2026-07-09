package io.legado.shared.rule

import io.legado.shared.platform.ScriptRuntime
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuleAnalyzerTest {
    @Test
    fun extractsJsonValues() {
        val body = """{"content":{"items":[{"name":"One"},{"name":"Two"}]}}"""

        assertEquals(listOf("One", "Two"), RuleAnalyzer.getStrings(body, "$.content.items.name"))
    }

    @Test
    fun extractsHtmlTextAndAttributes() {
        val html = """
            <div class="book"><a href="/one"><span class="name">One</span></a></div>
            <div class="book"><a href="/two"><span class="name">Two</span></a></div>
        """.trimIndent()

        val blocks = RuleAnalyzer.selectBlocks(html, ".book")

        assertEquals(2, blocks.size)
        assertEquals("One", RuleAnalyzer.getString(blocks[0], ".name@text"))
        assertEquals("/two", RuleAnalyzer.getString(blocks[1], "a@href"))
    }

    @Test
    fun extractsLegacyTagSelectorHtml() {
        val html = "<html><body><h1>Metal</h1><p>tank adventure</p></body></html>"

        assertEquals("<h1>Metal</h1><p>tank adventure</p>", RuleAnalyzer.getString(html, "tag.body@all"))
    }

    @Test
    fun extractsLegacyIndexedJsoupSelectors() {
        val html = """
            <section class="book"><a href="/one"><span id="first-title">One</span></a></section>
            <section class="book"><a href="/two"><span id="second-title">Two</span></a></section>
        """.trimIndent()

        assertEquals("Two", RuleAnalyzer.getString(html, "class.book.1@text"))
        assertEquals("/two", RuleAnalyzer.getString(html, "tag.a.1@href"))
        assertEquals("One", RuleAnalyzer.getString(html, "id.first-title@text"))
    }

    @Test
    fun evaluatesChainedHtmlRules() {
        val html = """
            <section class="book"><div class="meta"><a href="/one"><span class="name">One</span></a></div></section>
            <section class="book"><div class="meta"><a href="/two"><span class="name">Two</span></a></div></section>
        """.trimIndent()

        assertEquals(listOf("One", "Two"), RuleAnalyzer.getStrings(html, ".book@.meta@.name@text"))
        assertEquals(listOf("/one", "/two"), RuleAnalyzer.getStrings(html, ".book@.meta@a@href"))
    }

    @Test
    fun evaluatesCompositeRuleOperators() {
        val body = """{"content":{"names":["One","Two"],"fallback":"Fallback","authors":["A","B"]}}"""

        assertEquals("Fallback", RuleAnalyzer.getString(body, "$.content.missing||$.content.fallback"))
        assertEquals(listOf("One", "Two", "A", "B"), RuleAnalyzer.getStrings(body, "$.content.names&&$.content.authors"))
        assertEquals(listOf("One", "A", "Two", "B"), RuleAnalyzer.getStrings(body, "$.content.names%%$.content.authors"))
    }

    @Test
    fun evaluatesBasicXPathLikeRules() {
        val html = """<html><body><article><h1>Title</h1><a href="/next">Next</a></article></body></html>"""

        assertEquals("Title", RuleAnalyzer.getString(html, "@XPath://article/h1/text()"))
        assertEquals("/next", RuleAnalyzer.getString(html, "@XPath://article/a/@href"))
    }

    @Test
    fun extractsRegexAndAppliesReplacement() {
        val body = "title=<b>Before</b>"

        assertEquals("Before", RuleAnalyzer.getString(body, "title=<b>(.*?)</b>"))
        assertEquals("After", RuleAnalyzer.getString(body, "title=<b>(.*?)</b>##Before##After"))
    }

    @Test
    fun evaluatesJsThroughPlatformRuntime() {
        runBlocking {
            val engine = AnalyzeRuleEngine(
                scriptRuntime = object : ScriptRuntime {
                    override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
                        return "${bindings["result"]}:$script"
                    }
                }
            )

            assertEquals("body:return result", engine.evaluateString("body", "@js:return result"))
        }
    }

    @Test
    fun evaluatesExtractorThenJsRuleChain() {
        runBlocking {
            val engine = AnalyzeRuleEngine(
                scriptRuntime = object : ScriptRuntime {
                    override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
                        return "${bindings["result"]}:$script"
                    }
                }
            )
            val body = """{"content":{"id":"book-1"}}"""

            assertEquals("book-1:return result", engine.evaluateString(body, "$.content.id@js:return result"))
            assertEquals("book-1:return result", engine.evaluateString(body, "$.content.id<js>return result</js>"))
        }
    }

    @Test
    fun evaluatesPutAndGetRules() {
        runBlocking {
            val engine = AnalyzeRuleEngine()
            val body = """{"content":{"id":"book-1"}}"""

            assertEquals("book-1", engine.evaluateString(body, """@put:{"id":"$.content.id"}@get:{id}"""))
        }
    }

    @Test
    fun requiresRuntimeForJsAndWebJs() {
        runBlocking {
            val engine = AnalyzeRuleEngine()

            assertFailsWith<UnsupportedRuleRuntimeException> {
                engine.evaluateString("body", "@js:return result")
            }
            assertFailsWith<UnsupportedRuleRuntimeException> {
                engine.evaluateWebJs("<html></html>", "document.body.innerText")
            }
        }
    }

    @Test
    fun evaluatesWebJsThroughPlatformRuntime() {
        runBlocking {
            val engine = AnalyzeRuleEngine(
                webViewRuntime = object : RuleWebViewRuntime {
                    override suspend fun evaluate(request: RuleWebViewRequest): String {
                        return "${request.baseUrl}:${request.script}:${request.html.length}"
                    }
                }
            )

            assertEquals(
                "https://source.test:document.title:13",
                engine.evaluateWebJs(
                    html = "<html></html>",
                    script = "document.title",
                    context = RuleEvaluationContext(baseUrl = "https://source.test")
                )
            )
        }
    }
}

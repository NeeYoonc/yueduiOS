package io.legado.shared.service

import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.rule.AnalyzeRuleEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleEngineSearchResultParserTest {
    @Test
    fun parsesJsonSearchFieldsThroughAnalyzeRuleEngine() = runBlocking {
        val engine = AnalyzeRuleEngine(
            scriptRuntime = object : ScriptRuntime {
                override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
                    return "${bindings["result"]}/detail"
                }
            }
        )
        val parser = RuleEngineSearchResultParser(engine)
        val source = SharedBookSource(
            bookSourceUrl = "https://api.source.test",
            bookSourceName = "API",
            ruleSearch = SharedSearchRule(
                bookList = "$.content.content",
                name = "$.title",
                author = "$.author",
                bookUrl = "$.id@js:return result + '/detail'",
                coverUrl = "$.cover"
            )
        )
        val body = """
            {"content":{"content":[
              {"id":"book-1","title":"Metal Max","author":"Tester","cover":"/cover.jpg"}
            ]}}
        """.trimIndent()

        val books = parser.parse(source, body)

        assertEquals(1, books.size)
        assertEquals("Metal Max", books.single().name)
        assertEquals("Tester", books.single().author)
        assertEquals("book-1/detail", books.single().bookUrl)
        assertEquals("/cover.jpg", books.single().coverUrl)
    }
}

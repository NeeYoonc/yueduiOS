package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleJavaBridge
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleEngineBookInfoParserTest {
    @Test
    fun parsesBookInfoWithBookVariablesAndJs() = runBlocking {
        val engine = AnalyzeRuleEngine(
            scriptRuntime = object : ScriptRuntime {
                override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
                    val java = bindings["java"] as RuleJavaBridge
                    return if (script.contains("java.put")) {
                        java.put("detailId", bindings["result"])
                    } else {
                        "${java.get("bookId")}/${bindings["result"]}"
                    }
                }
            }
        )
        val parser = RuleEngineBookInfoParser(engine)
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleBookInfo = SharedBookInfoRule(
                name = "$.content.title",
                author = "$.content.id@js:java.put('detailId', result)",
                tocUrl = "$.content.catalogUrl@js:java.get('bookId') + '/' + result",
                coverUrl = "$.content.cover"
            )
        )
        val book = SharedBook(
            name = "Old",
            bookUrl = "https://source.test/book",
            origin = "https://source.test",
            variableMap = mapOf("bookId" to "book-1")
        )
        val body = """
            {"content":{"id":"detail-1","title":"Book Title","catalogUrl":"catalog","cover":"/cover.jpg"}}
        """.trimIndent()

        val parsed = parser.parse(source, book, body)

        assertEquals("Book Title", parsed.name)
        assertEquals("detail-1", parsed.author)
        assertEquals("book-1/catalog", parsed.tocUrl)
        assertEquals("/cover.jpg", parsed.coverUrl)
        assertEquals("book-1", parsed.variableMap["bookId"])
        assertEquals("detail-1", parsed.variableMap["detailId"])
    }
}

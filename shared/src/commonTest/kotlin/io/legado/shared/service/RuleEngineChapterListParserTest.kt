package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedTocRule
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleJavaBridge
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleEngineChapterListParserTest {
    @Test
    fun parsesChapterUrlWithBookVariablesAndJs() = runBlocking {
        val engine = AnalyzeRuleEngine(
            scriptRuntime = object : ScriptRuntime {
                override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
                    val java = bindings["java"] as RuleJavaBridge
                    return "${java.get("bookId")}/${bindings["result"]}"
                }
            }
        )
        val parser = RuleEngineChapterListParser(engine)
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleToc = SharedTocRule(
                chapterList = "$.content.content",
                chapterName = "$.chapterTitle",
                chapterUrl = "$.id@js:java.get('bookId') + '/' + result"
            )
        )
        val book = SharedBook(
            name = "Book",
            bookUrl = "https://source.test/book",
            origin = "https://source.test",
            variableMap = mapOf("bookId" to "book-1")
        )
        val body = """
            {"content":{"content":[
              {"id":"chapter-1","chapterTitle":"Chapter 1"}
            ]}}
        """.trimIndent()

        val chapters = parser.parse(source, book, body)

        assertEquals(1, chapters.size)
        assertEquals("Chapter 1", chapters.single().title)
        assertEquals("book-1/chapter-1", chapters.single().url)
    }
}

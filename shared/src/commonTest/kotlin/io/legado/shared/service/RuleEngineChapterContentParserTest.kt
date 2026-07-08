package io.legado.shared.service

import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import io.legado.shared.platform.ScriptRuntime
import io.legado.shared.rule.AnalyzeRuleEngine
import io.legado.shared.rule.RuleJavaBridge
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleEngineChapterContentParserTest {
    @Test
    fun parsesContentWithChapterVariablesJsAndReplaceRegex() = runBlocking {
        val engine = AnalyzeRuleEngine(
            scriptRuntime = object : ScriptRuntime {
                override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
                    val java = bindings["java"] as RuleJavaBridge
                    return "${java.get("chapterToken")}:${bindings["result"]}"
                }
            }
        )
        val parser = RuleEngineChapterContentParser(engine)
        val source = SharedBookSource(
            bookSourceUrl = "https://source.test",
            bookSourceName = "Source",
            ruleContent = SharedContentRule(
                title = "$.content.title",
                content = "$.content.text@js:java.get('chapterToken') + ':' + result",
                nextContentUrl = "$.content.next",
                replaceRegex = "##Line##Text"
            )
        )
        val book = SharedBook(
            name = "Book",
            bookUrl = "https://source.test/book",
            origin = "https://source.test"
        )
        val chapter = SharedBookChapter(
            title = "Chapter",
            url = "https://source.test/chapter",
            variableMap = mapOf("chapterToken" to "chapter-1")
        )
        val body = """
            {"content":{"title":"Chapter 1","text":"Line 1","next":"next.html"}}
        """.trimIndent()

        val content = parser.parse(source, book, chapter, body)

        assertEquals("Chapter 1", content.title)
        assertEquals("chapter-1:Text 1", content.content)
        assertEquals(listOf("next.html"), content.nextContentUrls)
    }
}

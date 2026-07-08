package io.legado.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceJsonImporterTest {
    @Test
    fun importsSingleSourceObject() {
        val sources = SourceJsonImporter.importBookSources(
            """
            {
              "bookSourceUrl": "https://example.test",
              "bookSourceName": "Example",
              "searchUrl": "https://example.test/search?q={{key}}"
            }
            """.trimIndent()
        )

        assertEquals(1, sources.size)
        assertEquals("https://example.test", sources.single().bookSourceUrl)
        assertEquals("Example", sources.single().bookSourceName)
    }

    @Test
    fun importsSourceArrayAndDropsBlankEntries() {
        val sources = SourceJsonImporter.importBookSources(
            """
            [
              {
                "bookSourceUrl": "https://one.test",
                "bookSourceName": "One"
              },
              {
                "bookSourceUrl": "",
                "bookSourceName": "Broken"
              }
            ]
            """.trimIndent()
        )

        assertEquals(1, sources.size)
        assertEquals("One", sources.single().bookSourceName)
    }

    @Test
    fun importsNestedRuleFieldsWithoutDroppingKnownAndroidRuleProperties() {
        val sources = SourceJsonImporter.importBookSources(
            """
            {
              "bookSourceUrl": "https://rules.test",
              "bookSourceName": "Rules",
              "ruleSearch": {
                "bookList": ".book",
                "updateTime": ".updated",
                "wordCount": ".words"
              },
              "ruleBookInfo": {
                "init": ".detail",
                "updateTime": ".updated",
                "wordCount": ".words",
                "canReName": "true",
                "downloadUrls": ".download@href"
              },
              "ruleToc": {
                "chapterList": ".chapter",
                "formatJs": "format"
              },
              "ruleContent": {
                "content": ".content",
                "subContent": ".sub",
                "title": "h2",
                "imageStyle": "FULL",
                "imageDecode": "decode",
                "payAction": "pay",
                "callBackJs": "callback"
              }
            }
            """.trimIndent()
        )

        val source = sources.single()

        assertEquals(".updated", source.ruleSearch?.updateTime)
        assertEquals(".words", source.ruleSearch?.wordCount)
        assertEquals(".detail", source.ruleBookInfo?.init)
        assertEquals(".updated", source.ruleBookInfo?.updateTime)
        assertEquals(".words", source.ruleBookInfo?.wordCount)
        assertEquals("true", source.ruleBookInfo?.canReName)
        assertEquals(".download@href", source.ruleBookInfo?.downloadUrls)
        assertEquals("format", source.ruleToc?.formatJs)
        assertEquals(".sub", source.ruleContent?.subContent)
        assertEquals("h2", source.ruleContent?.title)
        assertEquals("FULL", source.ruleContent?.imageStyle)
        assertEquals("decode", source.ruleContent?.imageDecode)
        assertEquals("pay", source.ruleContent?.payAction)
        assertEquals("callback", source.ruleContent?.callBackJs)
    }

    @Test
    fun rejectsJsonThatDoesNotContainUsableSources() {
        assertFailsWith<IllegalArgumentException> {
            SourceJsonImporter.importBookSources("""{"bookSourceName":"Missing URL"}""")
        }
    }
}

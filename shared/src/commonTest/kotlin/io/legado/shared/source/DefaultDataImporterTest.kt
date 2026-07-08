package io.legado.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultDataImporterTest {
    @Test
    fun importsStructuredDefaultDataIntoSnapshot() {
        val snapshot = DefaultDataImporter.importSnapshot(
            DefaultDataPayload(
                bookSourcesJson = """
                    [
                      {
                        "bookSourceUrl": "https://source.test",
                        "bookSourceName": "Source",
                        "customOrder": 2,
                        "jsLib": "function lib(){}",
                        "ruleExplore": { "bookList": "$.items", "name": "$.name" },
                        "ruleSearch": { "bookList": "$.items", "name": "$.name" }
                      }
                    ]
                """.trimIndent(),
                rssSourcesJson = """
                    [
                      {
                        "sourceUrl": "https://rss.test",
                        "sourceName": "RSS",
                        "customOrder": 3,
                        "singleUrl": true
                      }
                    ]
                """.trimIndent(),
                httpTtsJson = """
                    [
                      {
                        "id": -29,
                        "name": "TTS",
                        "url": "https://tts.test",
                        "contentType": "audio/mpeg",
                        "loginUi": [{ "name": "AppKey", "type": "text" }]
                      }
                    ]
                """.trimIndent(),
                dictRulesJson = """
                    [
                      { "name": "Dict", "urlRule": "https://dict.test?q={{key}}", "showRule": "$.text" }
                    ]
                """.trimIndent(),
                txtTocRulesJson = """
                    [
                      { "id": -1, "name": "TOC", "rule": "^Chapter", "serialNumber": 0 }
                    ]
                """.trimIndent(),
                keyboardAssistsJson = """
                    [
                      { "type": 0, "key": "@css:", "value": "@css:", "serialNo": 1 }
                    ]
                """.trimIndent(),
                readConfigJson = """{"pageAnim":1}""",
                themeConfigJson = """{"name":"default"}""",
                coverRuleJson = """{"rule":"cover"}""",
                directLinkUploadJson = """{"url":"https://upload.test"}"""
            )
        )

        assertEquals("Source", snapshot.bookSources.single().bookSourceName)
        assertEquals("$.items", snapshot.bookSources.single().ruleExplore?.bookList)
        assertEquals("RSS", snapshot.rssSources.single().sourceName)
        assertEquals("TTS", snapshot.httpTts.single().name)
        assertTrue(snapshot.httpTts.single().loginUi!!.contains("AppKey"))
        assertEquals("Dict", snapshot.dictRules.single().name)
        assertEquals("TOC", snapshot.txtTocRules.single().name)
        assertEquals("@css:", snapshot.keyboardAssists.single().key)
        assertEquals("""{"pageAnim":1}""", snapshot.rawConfigs.getValue("readConfig"))
        assertEquals("""{"name":"default"}""", snapshot.rawConfigs.getValue("themeConfig"))
    }

    @Test
    fun treatsMissingDefaultFilesAsEmptyLists() {
        val snapshot = DefaultDataImporter.importSnapshot(DefaultDataPayload())

        assertEquals(emptyList(), snapshot.bookSources)
        assertEquals(emptyList(), snapshot.rssSources)
        assertEquals(emptyMap(), snapshot.rawConfigs)
    }
}


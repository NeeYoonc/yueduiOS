package io.legado.shared.source

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DefaultDataAssetImportJvmTest {
    @Test
    fun importsRealAndroidDefaultDataFiles() {
        val root = File("..").canonicalFile
        val defaultData = File(root, "app/src/main/assets/defaultData")
        val snapshot = DefaultDataImporter.importSnapshot(
            DefaultDataPayload(
                bookSourcesJson = defaultData.readText("bookSources.json"),
                rssSourcesJson = defaultData.readText("rssSources.json"),
                httpTtsJson = defaultData.readText("httpTTS.json"),
                dictRulesJson = defaultData.readText("dictRules.json"),
                txtTocRulesJson = defaultData.readText("txtTocRule.json"),
                keyboardAssistsJson = defaultData.readText("keyboardAssists.json"),
                readConfigJson = defaultData.readText("readConfig.json"),
                themeConfigJson = defaultData.readText("themeConfig.json"),
                coverRuleJson = defaultData.readText("coverRule.json"),
                directLinkUploadJson = defaultData.readText("directLinkUpload.json")
            )
        )

        assertTrue(snapshot.bookSources.isNotEmpty(), "bookSources should import")
        assertTrue(snapshot.rssSources.isNotEmpty(), "rssSources should import")
        assertTrue(snapshot.httpTts.isNotEmpty(), "httpTTS should import")
        assertTrue(snapshot.dictRules.isNotEmpty(), "dictRules should import")
        assertTrue(snapshot.txtTocRules.isNotEmpty(), "txtTocRule should import")
        assertTrue(snapshot.keyboardAssists.isNotEmpty(), "keyboardAssists should import")
        assertTrue(snapshot.rawConfigs.keys.containsAll(listOf("readConfig", "themeConfig", "coverRule", "directLinkUpload")))
    }

    private fun File.readText(name: String): String {
        val file = File(this, name)
        require(file.isFile) { "Missing default data file: ${file.absolutePath}" }
        return file.readText(Charsets.UTF_8)
    }
}


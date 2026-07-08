package io.legado.shared.source

import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.model.SharedDictRule
import io.legado.shared.model.SharedHttpTts
import io.legado.shared.model.SharedKeyboardAssist
import io.legado.shared.model.SharedRssSource
import io.legado.shared.model.SharedTxtTocRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class DefaultDataPayload(
    val bookSourcesJson: String? = null,
    val rssSourcesJson: String? = null,
    val httpTtsJson: String? = null,
    val dictRulesJson: String? = null,
    val txtTocRulesJson: String? = null,
    val keyboardAssistsJson: String? = null,
    val readConfigJson: String? = null,
    val themeConfigJson: String? = null,
    val coverRuleJson: String? = null,
    val directLinkUploadJson: String? = null
)

object DefaultDataImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun importSnapshot(payload: DefaultDataPayload): SharedDataSnapshot {
        return SharedDataSnapshot(
            bookSources = payload.bookSourcesJson.importBookSourcesOrEmpty(),
            rssSources = payload.rssSourcesJson.decodeListOrEmpty(),
            httpTts = payload.httpTtsJson.importHttpTtsOrEmpty(),
            dictRules = payload.dictRulesJson.decodeListOrEmpty(),
            txtTocRules = payload.txtTocRulesJson.decodeListOrEmpty(),
            keyboardAssists = payload.keyboardAssistsJson.decodeListOrEmpty(),
            rawConfigs = payload.rawConfigMap()
        )
    }

    private fun String?.importBookSourcesOrEmpty() = this?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { raw ->
            runCatching { SourceJsonImporter.importBookSources(raw) }.getOrElse { emptyList() }
        }
        ?: emptyList()

    private inline fun <reified T> String?.decodeListOrEmpty(): List<T> {
        val raw = this?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        return runCatching {
            if (raw.startsWith("[")) {
                json.decodeFromString<List<T>>(raw)
            } else {
                listOf(json.decodeFromString<T>(raw))
            }
        }.getOrElse {
            emptyList()
        }
    }

    private fun String?.importHttpTtsOrEmpty(): List<SharedHttpTts> {
        val raw = this?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        return runCatching {
            val root = json.parseToJsonElement(raw)
            val items = when (root) {
                is JsonArray -> root
                else -> JsonArray(listOf(root))
            }
            items.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                SharedHttpTts(
                    id = obj.long("id") ?: 0L,
                    name = obj.string("name").orEmpty(),
                    url = obj.string("url").orEmpty(),
                    contentType = obj.string("contentType"),
                    concurrentRate = obj.string("concurrentRate") ?: "0",
                    loginUrl = obj.string("loginUrl"),
                    loginUi = obj.storageString("loginUi"),
                    header = obj.storageString("header"),
                    jsLib = obj.string("jsLib"),
                    enabledCookieJar = obj.boolean("enabledCookieJar") ?: false,
                    loginCheckJs = obj.string("loginCheckJs"),
                    lastUpdateTime = obj.long("lastUpdateTime") ?: 0L
                )
            }
        }.getOrElse {
            emptyList()
        }
    }

    private fun DefaultDataPayload.rawConfigMap(): Map<String, String> {
        return listOf(
            "readConfig" to readConfigJson,
            "themeConfig" to themeConfigJson,
            "coverRule" to coverRuleJson,
            "directLinkUpload" to directLinkUploadJson
        ).mapNotNull { (key, value) ->
            value?.trim()?.takeIf { it.isNotEmpty() }?.let { key to it }
        }.toMap()
    }

    private fun JsonObject.string(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.long(key: String): Long? {
        return this[key]?.jsonPrimitive?.longOrNull
    }

    private fun JsonObject.boolean(key: String): Boolean? {
        return this[key]?.jsonPrimitive?.booleanOrNull
    }

    private fun JsonObject.storageString(key: String): String? {
        val element = this[key] ?: return null
        return when (element) {
            is JsonPrimitive -> element.contentOrNull
            else -> json.encodeToString(JsonElement.serializer(), element)
        }
    }
}


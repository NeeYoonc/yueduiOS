package io.legado.shared.model

import kotlinx.serialization.Serializable

const val SHARED_DATA_SCHEMA_VERSION = 1

@Serializable
data class SharedDataSnapshot(
    val schemaVersion: Int = SHARED_DATA_SCHEMA_VERSION,
    val exportedAtMillis: Long = 0L,
    val bookSources: List<SharedBookSource> = emptyList(),
    val books: List<SharedBook> = emptyList(),
    val bookGroups: List<SharedBookGroup> = emptyList(),
    val chapters: List<SharedBookChapter> = emptyList(),
    val chapterContents: List<SharedChapterContentRecord> = emptyList(),
    val bookmarks: List<SharedBookmark> = emptyList(),
    val replaceRules: List<SharedReplaceRule> = emptyList(),
    val searchBooks: List<SharedSearchBook> = emptyList(),
    val searchKeywords: List<SharedSearchKeyword> = emptyList(),
    val cookies: List<SharedCookie> = emptyList(),
    val rssSources: List<SharedRssSource> = emptyList(),
    val rssArticles: List<SharedRssArticle> = emptyList(),
    val rssReadRecords: List<SharedRssReadRecord> = emptyList(),
    val rssStars: List<SharedRssStar> = emptyList(),
    val txtTocRules: List<SharedTxtTocRule> = emptyList(),
    val readRecords: List<SharedReadRecord> = emptyList(),
    val httpTts: List<SharedHttpTts> = emptyList(),
    val caches: List<SharedCacheEntry> = emptyList(),
    val ruleSubs: List<SharedRuleSub> = emptyList(),
    val dictRules: List<SharedDictRule> = emptyList(),
    val keyboardAssists: List<SharedKeyboardAssist> = emptyList(),
    val servers: List<SharedServer> = emptyList(),
    val readerPreferences: SharedReaderPreferences = SharedReaderPreferences(),
    val rawConfigs: Map<String, String> = emptyMap()
)

@Serializable
data class SharedRawConfigEntry(
    val key: String = "",
    val value: String = ""
)

@Serializable
data class SharedBookGroup(
    val groupId: Long = 1L,
    val groupName: String = "",
    val cover: String? = null,
    val order: Int = 0,
    val enableRefresh: Boolean = true,
    val show: Boolean = true,
    val bookSort: Int = -1,
    val onlyUpdateRead: Boolean = false
)

@Serializable
data class SharedBookmark(
    val time: Long = 0L,
    val bookName: String = "",
    val bookAuthor: String = "",
    val chapterIndex: Int = 0,
    val chapterPos: Int = 0,
    val chapterName: String = "",
    val bookText: String = "",
    val content: String = ""
)

@Serializable
data class SharedChapterContentRecord(
    val bookUrl: String = "",
    val chapterUrl: String = "",
    val content: SharedChapterContent = SharedChapterContent()
)

@Serializable
data class SharedReplaceRule(
    val id: Long = 0L,
    val name: String = "",
    val group: String? = null,
    val pattern: String = "",
    val replacement: String = "",
    val scope: String? = null,
    val scopeTitle: Boolean = false,
    val scopeContent: Boolean = true,
    val excludeScope: String? = null,
    val isEnabled: Boolean = true,
    val isRegex: Boolean = true,
    val timeoutMillisecond: Long = 3000L,
    val order: Int = Int.MIN_VALUE
) {
    val enabled: Boolean
        get() = isEnabled

    val regex: Boolean
        get() = isRegex
}

@Serializable
data class SharedSearchKeyword(
    val word: String = "",
    val usage: Int = 1,
    val lastUseTime: Long = 0L
)

@Serializable
data class SharedCookie(
    val url: String = "",
    val cookie: String = ""
)

@Serializable
data class SharedRssSource(
    val sourceUrl: String = "",
    val sourceName: String = "",
    val sourceIcon: String = "",
    val sourceGroup: String? = null,
    val sourceComment: String? = null,
    val enabled: Boolean = true,
    val variableComment: String? = null,
    val jsLib: String? = null,
    val enabledCookieJar: Boolean? = true,
    val concurrentRate: String? = null,
    val header: String? = null,
    val loginUrl: String? = null,
    val loginUi: String? = null,
    val loginCheckJs: String? = null,
    val coverDecodeJs: String? = null,
    val sortUrl: String? = null,
    val singleUrl: Boolean = false,
    val articleStyle: Int = 0,
    val ruleArticles: String? = null,
    val ruleNextPage: String? = null,
    val ruleTitle: String? = null,
    val rulePubDate: String? = null,
    val ruleDescription: String? = null,
    val ruleImage: String? = null,
    val ruleLink: String? = null,
    val ruleContent: String? = null,
    val contentWhitelist: String? = null,
    val contentBlacklist: String? = null,
    val shouldOverrideUrlLoading: String? = null,
    val style: String? = null,
    val enableJs: Boolean = true,
    val loadWithBaseUrl: Boolean = true,
    val injectJs: String? = null,
    val preloadJs: String? = null,
    val startHtml: String? = null,
    val startStyle: String? = null,
    val startJs: String? = null,
    val showWebLog: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val customOrder: Int = 0,
    val type: Int = 0,
    val preload: Boolean = false,
    val cacheFirst: Boolean = false,
    val searchUrl: String? = null
)

@Serializable
data class SharedRssArticle(
    val origin: String = "",
    val sort: String = "",
    val title: String = "",
    val order: Long = 0L,
    val link: String = "",
    val pubDate: String? = null,
    val description: String? = null,
    val content: String? = null,
    val image: String? = null,
    val group: String = "default",
    val read: Boolean = false,
    val variable: String? = null,
    val type: Int = 0,
    val durPos: Int = 0
) {
    val summary: String
        get() = description.orEmpty()

    val readableContent: String
        get() = content ?: summary
}

@Serializable
data class SharedRssReadRecord(
    val record: String = "",
    val title: String? = null,
    val readTime: Long? = null,
    val read: Boolean = true,
    val origin: String = "",
    val sort: String = "",
    val image: String? = null,
    val type: Int = 0,
    val durPos: Int = 0,
    val pubDate: String? = null
)

@Serializable
data class SharedRssStar(
    val origin: String = "",
    val sort: String = "",
    val title: String = "",
    val starTime: Long = 0L,
    val link: String = "",
    val pubDate: String? = null,
    val description: String? = null,
    val content: String? = null,
    val image: String? = null,
    val group: String = "default",
    val variable: String? = null,
    val type: Int = 0,
    val durPos: Int = 0
)

@Serializable
data class SharedTxtTocRule(
    val id: Long = 0L,
    val name: String = "",
    val rule: String = "",
    val replacement: String = "",
    val example: String? = null,
    val serialNumber: Int = -1,
    val enable: Boolean = true
)

@Serializable
data class SharedReadRecord(
    val deviceId: String = "",
    val bookName: String = "",
    val readTime: Long = 0L,
    val lastRead: Long = 0L
)

@Serializable
data class SharedHttpTts(
    val id: Long = 0L,
    val name: String = "",
    val url: String = "",
    val contentType: String? = null,
    val concurrentRate: String? = "0",
    val loginUrl: String? = null,
    val loginUi: String? = null,
    val header: String? = null,
    val jsLib: String? = null,
    val enabledCookieJar: Boolean? = false,
    val loginCheckJs: String? = null,
    val lastUpdateTime: Long = 0L
)

@Serializable
data class SharedCacheEntry(
    val key: String = "",
    val value: String? = null,
    val deadline: Long = 0L
)

@Serializable
data class SharedRuleSub(
    val id: Long = 0L,
    val name: String = "",
    val url: String = "",
    val type: Int = 0,
    val customOrder: Int = 0,
    val autoUpdate: Boolean = false,
    val update: Long = 0L,
    val updateInterval: Int = 0,
    val silentUpdate: Boolean = false,
    val js: String? = null,
    val showRule: String? = null,
    val sourceUrl: String? = null
)

@Serializable
data class SharedDictRule(
    val name: String = "",
    val urlRule: String = "",
    val showRule: String = "",
    val enabled: Boolean = true,
    val sortNumber: Int = 0
)

@Serializable
data class SharedDictionaryLookupResult(
    val ruleName: String = "",
    val word: String = "",
    val url: String = "",
    val content: String = "",
    val statusCode: Int = 0,
    val errorMessage: String? = null
)

@Serializable
data class SharedReaderSearchResult(
    val startIndex: Int = 0,
    val endIndex: Int = 0,
    val snippet: String = ""
)

@Serializable
data class SharedReaderPreferences(
    val fontSize: Double = 18.0,
    val lineSpacing: Double = 8.0,
    val contentPadding: Double = 20.0,
    val theme: String = "system"
)

@Serializable
data class SharedKeyboardAssist(
    val type: Int = 0,
    val key: String = "",
    val value: String = "",
    val serialNo: Int = 0
)

@Serializable
data class SharedServer(
    val id: Long = 0L,
    val name: String = "",
    val type: String = "WEBDAV",
    val config: String? = null,
    val sortNumber: Int = 0
)

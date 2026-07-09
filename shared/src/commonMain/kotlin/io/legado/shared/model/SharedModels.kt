package io.legado.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedBookSource(
    val bookSourceUrl: String = "",
    val bookSourceName: String = "",
    val bookSourceGroup: String? = null,
    val bookSourceType: Int = 0,
    val bookUrlPattern: String? = null,
    val customOrder: Int = 0,
    val enabled: Boolean = true,
    val enabledExplore: Boolean = true,
    val jsLib: String? = null,
    val enabledCookieJar: Boolean? = true,
    val concurrentRate: String? = null,
    val header: String? = null,
    val loginUrl: String? = null,
    val loginUi: String? = null,
    val loginCheckJs: String? = null,
    val coverDecodeJs: String? = null,
    val bookSourceComment: String? = null,
    val variableComment: String? = null,
    val lastUpdateTime: Long = 0,
    val respondTime: Long = 180000L,
    val weight: Int = 0,
    val exploreUrl: String? = null,
    val exploreScreen: String? = null,
    val searchUrl: String? = null,
    val ruleExplore: SharedSearchRule? = null,
    val ruleSearch: SharedSearchRule? = null,
    val ruleBookInfo: SharedBookInfoRule? = null,
    val ruleToc: SharedTocRule? = null,
    val ruleContent: SharedContentRule? = null,
    val ruleReview: SharedReviewRule? = null,
    val eventListener: Boolean = false,
    val customButton: Boolean = false
) {
    val key: String
        get() = bookSourceUrl

    val displayName: String
        get() = if (bookSourceGroup.isNullOrBlank()) {
            bookSourceName
        } else {
            "$bookSourceName ($bookSourceGroup)"
        }
}

@Serializable
data class SharedExploreKind(
    val title: String = "",
    val url: String? = null,
    val type: String = "url",
    val action: String? = null,
    val chars: List<String> = emptyList(),
    val default: String? = null,
    val viewName: String? = null
)

@Serializable
data class SharedSearchRule(
    val bookList: String? = null,
    val name: String? = null,
    val author: String? = null,
    val kind: String? = null,
    val lastChapter: String? = null,
    val updateTime: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val bookUrl: String? = null,
    val wordCount: String? = null,
    val checkKeyWord: String? = null
)

@Serializable
data class SharedBookInfoRule(
    val init: String? = null,
    val name: String? = null,
    val author: String? = null,
    val kind: String? = null,
    val lastChapter: String? = null,
    val updateTime: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val tocUrl: String? = null,
    val wordCount: String? = null,
    val canReName: String? = null,
    val downloadUrls: String? = null
)

@Serializable
data class SharedTocRule(
    val chapterList: String? = null,
    val chapterName: String? = null,
    val chapterUrl: String? = null,
    val formatJs: String? = null,
    val nextTocUrl: String? = null,
    val updateTime: String? = null,
    val isVolume: String? = null,
    val isVip: String? = null,
    val isPay: String? = null,
    val preUpdateJs: String? = null
)

@Serializable
data class SharedContentRule(
    val content: String? = null,
    val subContent: String? = null,
    val title: String? = null,
    val nextContentUrl: String? = null,
    val replaceRegex: String? = null,
    val webJs: String? = null,
    val sourceRegex: String? = null,
    val imageStyle: String? = null,
    val imageDecode: String? = null,
    val payAction: String? = null,
    val callBackJs: String? = null
)

@Serializable
data class SharedReviewRule(
    val reviewUrl: String? = null,
    val avatarRule: String? = null,
    val contentRule: String? = null,
    val postTimeRule: String? = null,
    val reviewQuoteUrl: String? = null,
    val voteUpUrl: String? = null,
    val voteDownUrl: String? = null,
    val postReviewUrl: String? = null,
    val postQuoteUrl: String? = null,
    val deleteUrl: String? = null
)

@Serializable
data class SharedBook(
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val tocUrl: String = bookUrl,
    val origin: String = "",
    val originName: String = "",
    val kind: String? = null,
    val customTag: String? = null,
    val latestChapterTitle: String? = null,
    val latestChapterTime: Long = 0,
    val lastCheckTime: Long = 0,
    val lastCheckCount: Int = 0,
    val totalChapterNum: Int = 0,
    val durChapterTitle: String? = null,
    val durChapterIndex: Int = 0,
    val durVolumeIndex: Int = 0,
    val chapterInVolumeIndex: Int = 0,
    val durChapterPos: Int = 0,
    val durChapterTime: Long = 0,
    val intro: String? = null,
    val customIntro: String? = null,
    val coverUrl: String? = null,
    val customCoverUrl: String? = null,
    val charset: String? = null,
    val type: Int = 0,
    val group: Long = 0,
    val wordCount: String? = null,
    val canUpdate: Boolean = true,
    val order: Int = 0,
    val originOrder: Int = 0,
    val variable: String? = null,
    val readConfig: SharedReadConfig? = null,
    val syncTime: Long = 0L,
    val variableMap: Map<String, String> = emptyMap()
)

@Serializable
data class SharedReadConfig(
    val reverseToc: Boolean = false,
    val pageAnim: Int? = null,
    val reSegment: Boolean = false,
    val imageStyle: String? = null,
    val useReplaceRule: Boolean? = null,
    val delTag: Long = 0L,
    val ttsEngine: String? = null,
    val splitLongChapter: Boolean = true,
    val readSimulating: Boolean = false,
    val startDate: String? = null,
    val startChapter: Int? = null,
    val dailyChapters: Int = 3,
    val openCredits: Int = 0,
    val closeCredits: Int = 0,
    val playMode: Int = 0,
    val playSpeed: Float = 1.0f
)

@Serializable
data class SharedSearchBook(
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val origin: String = "",
    val originName: String = "",
    val kind: String? = null,
    val latestChapterTitle: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val tocUrl: String = "",
    val type: Int = 0,
    val wordCount: String? = null,
    val time: Long = 0,
    val variable: String? = null,
    val variableMap: Map<String, String> = emptyMap(),
    val originOrder: Int = 0,
    val chapterWordCountText: String? = null,
    val chapterWordCount: Int = -1,
    val respondTime: Int = -1
) {
    fun toBook(): SharedBook = SharedBook(
        name = name,
        author = author,
        bookUrl = bookUrl,
        tocUrl = tocUrl.ifBlank { bookUrl },
        origin = origin,
        originName = originName,
        kind = kind,
        latestChapterTitle = latestChapterTitle,
        intro = intro,
        coverUrl = coverUrl,
        type = type,
        wordCount = wordCount,
        originOrder = originOrder,
        variable = variable,
        variableMap = variableMap
    )
}

@Serializable
data class SharedBookChapter(
    val title: String = "",
    val url: String = "",
    val index: Int = 0,
    val isVolume: Boolean = false,
    val isVip: Boolean = false,
    val isPay: Boolean = false,
    val baseUrl: String = "",
    val bookUrl: String = "",
    val resourceUrl: String? = null,
    val wordCount: String? = null,
    val start: Long? = null,
    val end: Long? = null,
    val startFragmentId: String? = null,
    val endFragmentId: String? = null,
    val imgUrl: String? = null,
    val tag: String? = null,
    val variable: String? = null,
    val variableMap: Map<String, String> = emptyMap()
)

@Serializable
data class SharedChapterContent(
    val content: String = "",
    val title: String? = null,
    val subContent: String? = null,
    val nextContentUrls: List<String> = emptyList()
)

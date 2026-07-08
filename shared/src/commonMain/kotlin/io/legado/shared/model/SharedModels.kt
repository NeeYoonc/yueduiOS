package io.legado.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedBookSource(
    val bookSourceUrl: String = "",
    val bookSourceName: String = "",
    val bookSourceGroup: String? = null,
    val bookSourceType: Int = 0,
    val bookUrlPattern: String? = null,
    val enabled: Boolean = true,
    val enabledExplore: Boolean = true,
    val enabledCookieJar: Boolean? = true,
    val concurrentRate: String? = null,
    val header: String? = null,
    val loginUrl: String? = null,
    val loginUi: String? = null,
    val loginCheckJs: String? = null,
    val coverDecodeJs: String? = null,
    val bookSourceComment: String? = null,
    val variableComment: String? = null,
    val exploreUrl: String? = null,
    val searchUrl: String? = null,
    val ruleSearch: SharedSearchRule? = null,
    val ruleBookInfo: SharedBookInfoRule? = null,
    val ruleToc: SharedTocRule? = null,
    val ruleContent: SharedContentRule? = null
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
data class SharedBook(
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val tocUrl: String = bookUrl,
    val origin: String = "",
    val kind: String? = null,
    val latestChapterTitle: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val variableMap: Map<String, String> = emptyMap()
)

@Serializable
data class SharedSearchBook(
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val origin: String = "",
    val kind: String? = null,
    val latestChapterTitle: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null
) {
    fun toBook(): SharedBook = SharedBook(
        name = name,
        author = author,
        bookUrl = bookUrl,
        tocUrl = bookUrl,
        origin = origin,
        kind = kind,
        latestChapterTitle = latestChapterTitle,
        intro = intro,
        coverUrl = coverUrl
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
    val tag: String? = null,
    val variableMap: Map<String, String> = emptyMap()
)

@Serializable
data class SharedChapterContent(
    val content: String = "",
    val title: String? = null,
    val subContent: String? = null,
    val nextContentUrls: List<String> = emptyList()
)

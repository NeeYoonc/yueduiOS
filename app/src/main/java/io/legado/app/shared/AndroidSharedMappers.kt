package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.model.SharedTocRule

fun BookSource.toSharedBookSource(): SharedBookSource = SharedBookSource(
    bookSourceUrl = bookSourceUrl,
    bookSourceName = bookSourceName,
    bookSourceGroup = bookSourceGroup,
    bookSourceType = bookSourceType,
    bookUrlPattern = bookUrlPattern,
    enabled = enabled,
    enabledExplore = enabledExplore,
    enabledCookieJar = enabledCookieJar,
    concurrentRate = concurrentRate,
    header = header,
    loginUrl = loginUrl,
    loginUi = loginUi,
    loginCheckJs = loginCheckJs,
    coverDecodeJs = coverDecodeJs,
    bookSourceComment = bookSourceComment,
    variableComment = variableComment,
    exploreUrl = exploreUrl,
    searchUrl = searchUrl,
    ruleSearch = ruleSearch?.toSharedSearchRule(),
    ruleBookInfo = ruleBookInfo?.toSharedBookInfoRule(),
    ruleToc = ruleToc?.toSharedTocRule(),
    ruleContent = ruleContent?.toSharedContentRule()
)

fun SearchRule.toSharedSearchRule(): SharedSearchRule = SharedSearchRule(
    bookList = bookList,
    name = name,
    author = author,
    kind = kind,
    lastChapter = lastChapter,
    updateTime = updateTime,
    intro = intro,
    coverUrl = coverUrl,
    bookUrl = bookUrl,
    wordCount = wordCount,
    checkKeyWord = checkKeyWord
)

fun BookInfoRule.toSharedBookInfoRule(): SharedBookInfoRule = SharedBookInfoRule(
    init = init,
    name = name,
    author = author,
    kind = kind,
    lastChapter = lastChapter,
    updateTime = updateTime,
    intro = intro,
    coverUrl = coverUrl,
    tocUrl = tocUrl,
    wordCount = wordCount,
    canReName = canReName,
    downloadUrls = downloadUrls
)

fun TocRule.toSharedTocRule(): SharedTocRule = SharedTocRule(
    chapterList = chapterList,
    chapterName = chapterName,
    chapterUrl = chapterUrl,
    formatJs = formatJs,
    nextTocUrl = nextTocUrl,
    updateTime = updateTime,
    isVolume = isVolume,
    isVip = isVip,
    isPay = isPay,
    preUpdateJs = preUpdateJs
)

fun ContentRule.toSharedContentRule(): SharedContentRule = SharedContentRule(
    content = content,
    subContent = subContent,
    title = title,
    nextContentUrl = nextContentUrl,
    replaceRegex = replaceRegex,
    webJs = webJs,
    sourceRegex = sourceRegex,
    imageStyle = imageStyle,
    imageDecode = imageDecode,
    payAction = payAction,
    callBackJs = callBackJs
)

fun Book.toSharedBook(): SharedBook = SharedBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    tocUrl = tocUrl,
    origin = origin,
    kind = kind,
    latestChapterTitle = latestChapterTitle,
    intro = intro,
    coverUrl = coverUrl,
    variableMap = variableMap
)

fun SearchBook.toSharedSearchBook(): SharedSearchBook = SharedSearchBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    origin = origin,
    kind = kind,
    latestChapterTitle = latestChapterTitle,
    intro = intro,
    coverUrl = coverUrl
)

fun BookChapter.toSharedBookChapter(): SharedBookChapter = SharedBookChapter(
    title = title,
    url = url,
    index = index,
    isVolume = isVolume,
    isVip = isVip,
    isPay = isPay,
    tag = tag,
    variableMap = variableMap
)

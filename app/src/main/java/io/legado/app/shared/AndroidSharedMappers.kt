package io.legado.app.shared

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.ReviewRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.shared.model.SharedBook
import io.legado.shared.model.SharedBookChapter
import io.legado.shared.model.SharedBookGroup
import io.legado.shared.model.SharedBookInfoRule
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedContentRule
import io.legado.shared.model.SharedReadConfig
import io.legado.shared.model.SharedReviewRule
import io.legado.shared.model.SharedSearchBook
import io.legado.shared.model.SharedSearchKeyword
import io.legado.shared.model.SharedSearchRule
import io.legado.shared.model.SharedTocRule

fun BookSource.toSharedBookSource(): SharedBookSource = SharedBookSource(
    bookSourceUrl = bookSourceUrl,
    bookSourceName = bookSourceName,
    bookSourceGroup = bookSourceGroup,
    bookSourceType = bookSourceType,
    bookUrlPattern = bookUrlPattern,
    customOrder = customOrder,
    enabled = enabled,
    enabledExplore = enabledExplore,
    jsLib = jsLib,
    enabledCookieJar = enabledCookieJar,
    concurrentRate = concurrentRate,
    header = header,
    loginUrl = loginUrl,
    loginUi = loginUi,
    loginCheckJs = loginCheckJs,
    coverDecodeJs = coverDecodeJs,
    bookSourceComment = bookSourceComment,
    variableComment = variableComment,
    lastUpdateTime = lastUpdateTime,
    respondTime = respondTime,
    weight = weight,
    exploreUrl = exploreUrl,
    exploreScreen = exploreScreen,
    searchUrl = searchUrl,
    ruleExplore = ruleExplore?.toSharedSearchRule(),
    ruleSearch = ruleSearch?.toSharedSearchRule(),
    ruleBookInfo = ruleBookInfo?.toSharedBookInfoRule(),
    ruleToc = ruleToc?.toSharedTocRule(),
    ruleContent = ruleContent?.toSharedContentRule(),
    ruleReview = ruleReview?.toSharedReviewRule(),
    eventListener = eventListener,
    customButton = customButton
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

fun ExploreRule.toSharedSearchRule(): SharedSearchRule = SharedSearchRule(
    bookList = bookList,
    name = name,
    author = author,
    kind = kind,
    lastChapter = lastChapter,
    updateTime = updateTime,
    intro = intro,
    coverUrl = coverUrl,
    bookUrl = bookUrl,
    wordCount = wordCount
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

fun ReviewRule.toSharedReviewRule(): SharedReviewRule = SharedReviewRule(
    reviewUrl = reviewUrl,
    avatarRule = avatarRule,
    contentRule = contentRule,
    postTimeRule = postTimeRule,
    reviewQuoteUrl = reviewQuoteUrl,
    voteUpUrl = voteUpUrl,
    voteDownUrl = voteDownUrl,
    postReviewUrl = postReviewUrl,
    postQuoteUrl = postQuoteUrl,
    deleteUrl = deleteUrl
)

fun Book.toSharedBook(): SharedBook = SharedBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    tocUrl = tocUrl,
    origin = origin,
    originName = originName,
    kind = kind,
    customTag = customTag,
    latestChapterTitle = latestChapterTitle,
    latestChapterTime = latestChapterTime,
    lastCheckTime = lastCheckTime,
    lastCheckCount = lastCheckCount,
    totalChapterNum = totalChapterNum,
    durChapterTitle = durChapterTitle,
    durChapterIndex = durChapterIndex,
    durVolumeIndex = durVolumeIndex,
    chapterInVolumeIndex = chapterInVolumeIndex,
    durChapterPos = durChapterPos,
    durChapterTime = durChapterTime,
    intro = intro,
    customIntro = customIntro,
    coverUrl = coverUrl,
    customCoverUrl = customCoverUrl,
    charset = charset,
    type = type,
    group = group,
    wordCount = wordCount,
    canUpdate = canUpdate,
    order = order,
    originOrder = originOrder,
    variable = variable,
    readConfig = readConfig?.toSharedReadConfig(),
    syncTime = syncTime,
    variableMap = variableMap
)

fun BookGroup.toSharedBookGroup(): SharedBookGroup = SharedBookGroup(
    groupId = groupId,
    groupName = groupName,
    cover = cover,
    order = order,
    enableRefresh = enableRefresh,
    show = show,
    bookSort = bookSort,
    onlyUpdateRead = onlyUpdateRead
)

fun Book.ReadConfig.toSharedReadConfig(): SharedReadConfig = SharedReadConfig(
    reverseToc = reverseToc,
    pageAnim = pageAnim,
    reSegment = reSegment,
    imageStyle = imageStyle,
    useReplaceRule = useReplaceRule,
    delTag = delTag,
    ttsEngine = ttsEngine,
    splitLongChapter = splitLongChapter,
    readSimulating = readSimulating,
    startDate = startDate?.toString(),
    startChapter = startChapter,
    dailyChapters = dailyChapters,
    openCredits = openCredits,
    closeCredits = closeCredits,
    playMode = playMode,
    playSpeed = playSpeed
)

fun SearchBook.toSharedSearchBook(): SharedSearchBook = SharedSearchBook(
    name = name,
    author = author,
    bookUrl = bookUrl,
    origin = origin,
    originName = originName,
    kind = kind,
    latestChapterTitle = latestChapterTitle,
    intro = intro,
    coverUrl = coverUrl,
    tocUrl = tocUrl,
    type = type,
    wordCount = wordCount,
    time = time,
    variable = variable,
    originOrder = originOrder,
    chapterWordCountText = chapterWordCountText,
    chapterWordCount = chapterWordCount,
    respondTime = respondTime
)

fun SearchKeyword.toSharedSearchKeyword(): SharedSearchKeyword = SharedSearchKeyword(
    word = word,
    usage = usage,
    lastUseTime = lastUseTime
)

fun BookChapter.toSharedBookChapter(): SharedBookChapter = SharedBookChapter(
    title = title,
    url = url,
    index = index,
    isVolume = isVolume,
    isVip = isVip,
    isPay = isPay,
    baseUrl = baseUrl,
    bookUrl = bookUrl,
    resourceUrl = resourceUrl,
    wordCount = wordCount,
    start = start,
    end = end,
    startFragmentId = startFragmentId,
    endFragmentId = endFragmentId,
    imgUrl = imgUrl,
    tag = tag,
    variable = variable,
    variableMap = variableMap
)

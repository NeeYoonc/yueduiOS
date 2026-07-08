# Legado KMP iOS Prototype Design

## Context

当前 `legado-beta` 是原生 Android/Kotlin 工程，主应用在 `app` 模块，另有 `modules:book`、`modules:rhino` 和 `modules:web`。仓库没有现成 iOS、Swift、Compose Multiplatform 或 Kotlin Multiplatform 目标。

代码检查显示，第一阶段跨端重构的核心入口主要集中在：

- `app/src/main/java/io/legado/app/data/entities/BookSource.kt`
- `app/src/main/java/io/legado/app/data/entities/Book.kt`
- `app/src/main/java/io/legado/app/data/entities/BookChapter.kt`
- `app/src/main/java/io/legado/app/data/entities/SearchBook.kt`
- `app/src/main/java/io/legado/app/model/webBook/WebBook.kt`
- `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`
- `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt`

这些代码同时依赖 Android Room、Parcelable、Android WebView、OkHttp、Rhino、全局配置、CookieStore、CacheManager 和 UI 层日志。直接复制到 iOS 不可行，需要先抽象平台能力，再迁移可共享核心。

## Goal

第一阶段交付一个可阅读 iOS 原型，同时保持 Android 现有工程可构建、可继续运行。

原型闭环包括：

- 导入普通文本书源 JSON。
- 使用书源搜索小说。
- 获取书籍详情和目录。
- 解析章节正文。
- iOS SwiftUI 原型展示搜索结果、目录、正文阅读页。
- Android 侧通过适配层继续调用共享核心中的新模型或服务，不破坏现有主流程。

## Non Goals

第一阶段不迁移以下能力：

- 漫画、音频、视频、RSS。
- 本地 TXT/EPUB/PDF/MOBI/UMD 导入。
- TTS、WebDAV、备份恢复、远程书架同步。
- Android 后台 WebView 执行的复杂 `webJs`。
- 登录 UI、验证码、二维码、浏览器式人工登录流程。
- 完整 Room 数据库迁移。
- 完整 UI 复刻。

这些能力后续分阶段迁移，不能阻塞 iOS 最小阅读闭环。

## Architecture

新增 `:shared` Kotlin Multiplatform 模块。

### commonMain

`commonMain` 只放跨平台可共享代码：

- 纯 Kotlin 数据模型：`SharedBookSource`、`SharedBook`、`SharedBookChapter`、`SharedSearchBook`。
- 书源规则模型：搜索、详情、目录、正文规则。
- 规则解析流程：URL 规则处理、HTML/JSON/正则解析、正文组装。
- 平台接口：`HttpFetcher`、`ScriptRuntime`、`CookieStorePort`、`CacheStorePort`、`RuleLogger`、`Clock`。
- 服务入口：`BookSearchService`、`BookInfoService`、`ChapterListService`、`ChapterContentService`。

`commonMain` 不能引用 Android、Room、Parcelable、OkHttp、Rhino、WebView、Glide、Media3、NanoHTTPD。

### androidMain

`androidMain` 提供 Android 适配：

- `AndroidHttpFetcher` 可先包现有 OkHttp 请求能力。
- `AndroidScriptRuntime` 可先包现有 Rhino 执行。
- `AndroidCookieStorePort` 和 `AndroidCacheStorePort` 可桥接现有 CookieStore/CacheManager。
- Android 现有 `BookSource`、`Book`、`BookChapter` 与 shared 模型之间提供 mapper。

第一阶段 Android 不强制切主流程到 shared，但 shared 测试和 Android 适配必须能编译，避免迁移方向只服务 iOS。

### iosMain

`iosMain` 提供 iOS 适配：

- HTTP 使用 Ktor Darwin engine。
- JS 执行使用 JavaScriptCore 封装成 `ScriptRuntime`。
- Cookie 和缓存先使用内存或文件实现。
- Swift 通过 shared framework 调用服务入口。

第一阶段 iOS 存储不做完整数据库。搜索结果、目录和正文可以先用内存状态驱动 SwiftUI 原型。后续再引入 SQLDelight 管理书架和缓存。

## Data Flow

搜索流程：

1. iOS UI 输入关键词。
2. Swift 调用 `BookSearchService.search(source, keyword, page)`。
3. shared 根据 `searchUrl` 构造请求，调用 `HttpFetcher`。
4. shared 使用规则解析 HTML/JSON。
5. 返回 `SharedSearchBook` 列表。

详情与目录流程：

1. UI 选择搜索结果。
2. shared 获取详情页并解析 `SharedBook`。
3. shared 获取目录页并解析 `SharedBookChapter` 列表。
4. UI 展示章节列表。

正文流程：

1. UI 选择章节。
2. shared 根据章节 URL 获取页面。
3. shared 按 `ruleContent` 解析正文。
4. UI 展示纯文本正文。

## Rule Compatibility

第一阶段支持规则能力：

- `@CSS` / 默认 JSoup 类选择器等价能力。
- `@Json` / JSONPath 等价能力。
- `@XPath` 中可用的基础 XPath 能力。
- 正则提取和 `##` 替换。
- `{{ }}` 中简单 JS 表达式。
- `@js:` 中不访问 Android 对象的 JS。
- 基础 GET/POST URL option。
- 基础 header、charset、page、key 替换。

第一阶段明确降级：

- `webJs` 返回明确错误，提示当前 iOS 原型不支持 WebView 规则。
- 访问 Android 专属对象的 JS 返回明确错误。
- 登录检测和登录 UI 不作为原型验收项。

## Dependency Direction

共享模块优先选择跨平台库：

- Kotlin Multiplatform Gradle plugin。
- kotlinx.coroutines。
- kotlinx.serialization-json，用于 shared 模型序列化。
- Ktor Client，用于跨平台 HTTP 抽象。
- SQLDelight 后续用于共享数据库，第一阶段只预留接口。

现有 Android 依赖保留在 Android 模块或 `androidMain` 适配层，不进入 `commonMain`。

## Migration Strategy

按小步迁移，不一次性搬 800 多个源文件。

第一步新增 `:shared` 骨架，先让 Gradle 能识别 Android 和 iOS framework target。

第二步迁移纯模型。不要直接搬带注解的 Room/Parcelable 实体，而是新建 shared 模型，再写 mapper：

- `BookSource` -> `SharedBookSource`
- `Book` -> `SharedBook`
- `BookChapter` -> `SharedBookChapter`
- `SearchBook` -> `SharedSearchBook`

第三步抽平台端口。先定义接口和 fake 实现，让 common tests 可以不联网运行。

第四步迁移规则解析中最小可用部分。先覆盖搜索结果解析，再覆盖目录和正文。

第五步接 Android 适配器，保证 Android 编译不被破坏。

第六步创建 iOS SwiftUI 原型，调用 shared framework 完成一条真实阅读链路。

## Testing

测试按风险分层：

- `commonTest`：模型序列化、URL option 解析、规则拆分、CSS/JSON/XPath/Regex 解析、正文清洗。
- `commonTest`：用 fake HTTP 响应验证搜索、目录、正文服务。
- `androidTest` 或 JVM test：验证 Android mapper 和 Rhino 适配器。
- iOS 手动 smoke：打开 SwiftUI 原型，导入一个普通文本书源，搜索一本书，打开目录，阅读一章。

实现时遵守红绿重构。新增共享逻辑前先写失败测试，再写最小实现。

## Verification

每阶段完成前至少运行：

- `.\gradlew :shared:allTests`
- `.\gradlew :shared:assemble`
- `.\gradlew :app:assembleDebug`

iOS 侧在有 macOS/Xcode 环境时运行：

- `.\gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- Xcode 打开 iOS 原型并在 simulator smoke test。

当前机器是 Windows，无法本机运行 iOS Simulator。Windows 上只能验证 KMP 配置、common tests、Android 编译和 shared framework 任务配置；iOS 真机或模拟器验证需要 macOS。

## Risks

主要风险是规则引擎对 Android 运行时的隐性依赖。`AnalyzeUrl` 和 `AnalyzeRule` 当前混合了请求、JS、Cookie、缓存、WebView、日志和实体读写。解决方式是先抽端口，再迁移纯算法。

第二个风险是 JS 兼容性。Android 当前使用 Rhino，iOS 计划使用 JavaScriptCore。两者对 Java/Kotlin 对象桥接不同。第一阶段只支持简单 JS 和明确的 shared bridge，复杂 Android 对象调用延后。

第三个风险是规则覆盖率。书源生态很杂，不能声称兼容所有书源。第一阶段验收以普通文本书源闭环为准。

## Acceptance Criteria

第一阶段完成条件：

- `:shared` 模块存在，并包含 Android/iOS target。
- `commonMain` 不引用 Android-only 包。
- 至少一个普通文本书源可通过 fake fixture 完成搜索、目录、正文解析测试。
- Android app debug 构建仍通过。
- iOS SwiftUI 原型能调用 shared API，完成搜索、目录、正文展示。
- 不支持的规则能力有明确错误信息，而不是崩溃或静默返回空。

## References

- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
- Ktor Client engines: https://ktor.io/docs/client-engines.html
- SQLDelight: https://sqldelight.github.io/sqldelight/

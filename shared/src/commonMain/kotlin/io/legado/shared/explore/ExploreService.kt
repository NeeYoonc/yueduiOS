package io.legado.shared.explore

import io.legado.shared.LegadoSharedClient
import io.legado.shared.model.SharedBookSource
import io.legado.shared.model.SharedExploreKind
import io.legado.shared.service.SearchPageResult
import io.legado.shared.service.SharedUrlResolver
import io.legado.shared.storage.SharedLibraryStore
import kotlinx.serialization.json.Json

class ExploreService(
    private val client: LegadoSharedClient,
    private val libraryStore: SharedLibraryStore
) {
    fun listExploreSources(): List<SharedBookSource> {
        return libraryStore.loadBookSources()
            .filter { it.enabled && it.enabledExplore && !it.exploreUrl.isNullOrBlank() }
            .sortedWith(compareBy<SharedBookSource> { it.customOrder }.thenBy { it.bookSourceName })
    }

    fun listKinds(source: SharedBookSource): List<SharedExploreKind> {
        val rawExploreUrl = source.exploreUrl?.trim().orEmpty()
        if (rawExploreUrl.isBlank()) {
            return emptyList()
        }
        if (rawExploreUrl.startsWith("@js:", ignoreCase = true) || rawExploreUrl.startsWith("<js>", ignoreCase = true)) {
            return listOf(
                SharedExploreKind(
                    title = "Unsupported JS explore",
                    type = "error",
                    action = rawExploreUrl
                )
            )
        }
        if (rawExploreUrl.startsWith("[")) {
            return runCatching {
                json.decodeFromString<List<SharedExploreKind>>(rawExploreUrl)
            }.getOrElse { error ->
                listOf(SharedExploreKind(title = "ERROR:${error.message}", type = "error", action = rawExploreUrl))
            }
        }
        return rawExploreUrl
            .split(Regex("""(&&|\n)+"""))
            .mapNotNull { item ->
                val trimmed = item.trim()
                if (trimmed.isEmpty()) {
                    return@mapNotNull null
                }
                val title = trimmed.substringBefore("::").trim()
                val url = trimmed.substringAfter("::", missingDelimiterValue = "").trim().ifBlank { null }
                SharedExploreKind(title = title, url = url)
            }
    }

    suspend fun loadExplore(
        source: SharedBookSource,
        kind: SharedExploreKind,
        page: Int = 1
    ): SearchPageResult {
        val exploreUrl = requireNotNull(kind.url?.takeIf { it.isNotBlank() }) {
            "Explore kind ${kind.title} has no url"
        }
        val resolvedUrl = SharedUrlResolver.resolve(source.bookSourceUrl, exploreUrl)
        val exploreSource = source.copy(
            searchUrl = resolvedUrl,
            ruleSearch = source.ruleExplore ?: source.ruleSearch
        )
        return client.search(exploreSource, key = "", page = page)
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}

package io.legado.shared.service

object SharedUrlResolver {
    fun resolve(baseUrl: String, target: String): String {
        val trimmedTarget = target.trim()
        if (trimmedTarget.isEmpty() || isAbsoluteHttpUrl(trimmedTarget)) {
            return trimmedTarget
        }
        val base = ParsedHttpUrl.parse(baseUrl) ?: return trimmedTarget
        if (trimmedTarget.startsWith("//")) {
            return "${base.scheme}:$trimmedTarget"
        }
        if (trimmedTarget.startsWith("?") || trimmedTarget.startsWith("#")) {
            return "${base.origin}${base.path}$trimmedTarget"
        }
        val (targetPath, suffix) = splitSuffix(trimmedTarget)
        val resolvedPath = if (targetPath.startsWith("/")) {
            normalizePath(targetPath)
        } else {
            normalizePath("${base.directoryPath}$targetPath")
        }
        return "${base.origin}$resolvedPath$suffix"
    }

    fun resolveAll(baseUrl: String, targets: List<String>): List<String> {
        return targets.map { resolve(baseUrl, it) }
    }

    private fun isAbsoluteHttpUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
    }

    private fun splitSuffix(value: String): Pair<String, String> {
        val queryIndex = value.indexOf('?').takeIf { it >= 0 } ?: Int.MAX_VALUE
        val fragmentIndex = value.indexOf('#').takeIf { it >= 0 } ?: Int.MAX_VALUE
        val suffixStart = minOf(queryIndex, fragmentIndex)
        return if (suffixStart == Int.MAX_VALUE) {
            value to ""
        } else {
            value.substring(0, suffixStart) to value.substring(suffixStart)
        }
    }

    private fun normalizePath(path: String): String {
        val segments = ArrayDeque<String>()
        path.split('/').forEach { segment ->
            when {
                segment.isEmpty() || segment == "." -> Unit
                segment == ".." -> if (segments.isNotEmpty()) segments.removeLast()
                else -> segments.addLast(segment)
            }
        }
        val normalized = segments.joinToString("/")
        return if (normalized.isEmpty()) "/" else "/$normalized"
    }

    private data class ParsedHttpUrl(
        val scheme: String,
        val authority: String,
        val path: String
    ) {
        val origin: String
            get() = "$scheme://$authority"

        val directoryPath: String
            get() = if (path.endsWith("/")) {
                path
            } else {
                path.substringBeforeLast("/", missingDelimiterValue = "/") + "/"
            }

        companion object {
            fun parse(url: String): ParsedHttpUrl? {
                val schemeEnd = url.indexOf("://")
                if (schemeEnd <= 0) return null
                val scheme = url.substring(0, schemeEnd).lowercase()
                if (scheme != "http" && scheme != "https") return null
                val rest = url.substring(schemeEnd + 3)
                val authorityEnd = rest.indexOf('/').takeIf { it >= 0 } ?: rest.length
                val authority = rest.substring(0, authorityEnd)
                if (authority.isBlank()) return null
                val rawPath = rest.substring(authorityEnd).ifBlank { "/" }
                val path = splitSuffix(rawPath).first.ifBlank { "/" }
                return ParsedHttpUrl(scheme, authority, path)
            }
        }
    }
}

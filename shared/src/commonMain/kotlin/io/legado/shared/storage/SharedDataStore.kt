package io.legado.shared.storage

import io.legado.shared.model.SharedDataSnapshot
import io.legado.shared.platform.CacheStorePort
import kotlinx.serialization.json.Json

class SharedDataStore(
    private val cacheStore: CacheStorePort,
    private val snapshotKey: String = DEFAULT_SNAPSHOT_KEY
) {
    fun save(snapshot: SharedDataSnapshot) {
        cacheStore.putText(snapshotKey, json.encodeToString(snapshot))
    }

    fun load(): SharedDataSnapshot {
        val rawJson = cacheStore.getText(snapshotKey)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return SharedDataSnapshot()
        return runCatching {
            json.decodeFromString<SharedDataSnapshot>(rawJson)
        }.getOrElse {
            SharedDataSnapshot()
        }
    }

    fun clear() {
        cacheStore.removeText(snapshotKey)
    }

    companion object {
        const val DEFAULT_SNAPSHOT_KEY = "legado.sharedDataSnapshot"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}


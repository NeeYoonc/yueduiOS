package io.legado.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class DarwinUserDefaultsCacheStoreCompileTest {
    @Test
    fun exposesUserDefaultsCacheStoreAsCacheStorePort() {
        val cacheStore: CacheStorePort = DarwinUserDefaultsCacheStore()

        assertEquals("DarwinUserDefaultsCacheStore", cacheStore::class.simpleName)
    }
}

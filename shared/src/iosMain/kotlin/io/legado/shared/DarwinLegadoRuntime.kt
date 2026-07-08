package io.legado.shared

import io.legado.shared.platform.DarwinHttpFetcher
import io.legado.shared.platform.DarwinUserDefaultsCacheStore

class DarwinLegadoRuntime : LegadoRuntime(
    httpFetcher = DarwinHttpFetcher(),
    cacheStore = DarwinUserDefaultsCacheStore()
)

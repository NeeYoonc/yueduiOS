package io.legado.shared

import io.legado.shared.platform.DarwinHttpFetcher
import io.legado.shared.platform.DarwinScriptRuntime
import io.legado.shared.platform.DarwinUserDefaultsCacheStore
import io.legado.shared.platform.DarwinWebViewRuntime

class DarwinLegadoRuntime : LegadoRuntime(
    httpFetcher = DarwinHttpFetcher(),
    cacheStore = DarwinUserDefaultsCacheStore(),
    scriptRuntime = DarwinScriptRuntime(),
    webViewRuntime = DarwinWebViewRuntime()
)

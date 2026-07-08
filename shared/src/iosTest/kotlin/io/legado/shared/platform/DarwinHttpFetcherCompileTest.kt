package io.legado.shared.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class DarwinHttpFetcherCompileTest {
    @Test
    fun exposesDarwinHttpFetcherAsHttpFetcher() {
        val fetcher: HttpFetcher = DarwinHttpFetcher()

        assertEquals("DarwinHttpFetcher", fetcher::class.simpleName)
    }
}

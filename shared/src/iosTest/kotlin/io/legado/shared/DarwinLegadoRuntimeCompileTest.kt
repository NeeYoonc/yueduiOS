package io.legado.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class DarwinLegadoRuntimeCompileTest {
    @Test
    fun exposesDarwinRuntimeAsLegadoRuntime() {
        val runtime: LegadoRuntime = DarwinLegadoRuntime()

        assertEquals("DarwinLegadoRuntime", runtime::class.simpleName)
    }
}

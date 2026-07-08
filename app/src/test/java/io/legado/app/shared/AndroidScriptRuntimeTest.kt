package io.legado.app.shared

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidScriptRuntimeTest {

    @Test
    fun evaluatesJavaScriptWithBindings() = runBlocking {
        val runtime = AndroidScriptRuntime()

        val result = runtime.evaluate(
            script = "result + ':' + baseUrl",
            bindings = mapOf(
                "result" to "body",
                "baseUrl" to "https://source.test"
            )
        )

        assertEquals("body:https://source.test", result)
    }
}

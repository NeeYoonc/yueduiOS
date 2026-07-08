package io.legado.app.shared

import kotlinx.coroutines.runBlocking
import io.legado.shared.rule.RuleJavaBridge
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

    @Test
    fun exposesJavaPutGetBridgeToRhino() = runBlocking {
        val variables = mutableMapOf<String, String>()
        val runtime = AndroidScriptRuntime()

        val result = runtime.evaluate(
            script = "java.put('bookId', result); java.get('bookId')",
            bindings = mapOf(
                "result" to "book-1",
                "java" to RuleJavaBridge(variables)
            )
        )

        assertEquals("book-1", result)
        assertEquals("book-1", variables["bookId"])
    }
}

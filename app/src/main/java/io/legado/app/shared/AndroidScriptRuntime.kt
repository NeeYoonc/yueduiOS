package io.legado.app.shared

import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.shared.platform.ScriptRuntime

class AndroidScriptRuntime : ScriptRuntime {
    override suspend fun evaluate(script: String, bindings: Map<String, Any?>): Any? {
        val scriptBindings = ScriptBindings().apply {
            bindings.forEach { (key, value) ->
                put(key, value)
            }
        }
        return RhinoScriptEngine.eval(script, scriptBindings)
    }
}

package ai.tambo.tmodeler.core

import ai.tambo.tmodeler.scheduler.TmsScope

open class Tattr(
    var name: String,
    private val onGet: () -> Any?,
    private val onSet: (Any?) -> Unit
) : TobGb(){
    // Explicit delegation avoids an unbound inherited context-receiver symbol on Wasm.
    context(TmsScope)
    override fun resolveDeepLink() = super.resolveDeepLink()

    open fun getValue(): Any? = onGet()
    open fun setValue(v: Any?) = onSet(v)

    var isJson = false
}
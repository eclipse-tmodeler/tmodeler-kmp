package ai.tambo.tmodeler.ms.utils

import ai.tambo.tmodeler.core.TobGb

class Ttuple(val values: List<Any?>) {
    inline fun <reified T> get(): T? = values.firstOrNull { it is T } as? T
    inline fun <reified T> getAll(): List<T> = values.filterIsInstance<T>()
    fun getAt(index: Int): Any? = values.getOrNull(index)
    fun toList(): List<Any?> = values
    fun deepLinkResolve(enabled: Boolean) {
        values.forEach {
            if(it is TobGb) it.deepLinkResolve(enabled)
        }
    }
}
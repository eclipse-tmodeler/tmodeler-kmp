package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.operation.Top.Equal
import ai.tambo.tmodeler.field.operation.Top.Like
import ai.tambo.tmodeler.field.operation.Top.NotEqual
import ai.tambo.tmodeler.field.operation.Tx
import kotlin.js.JsName

open class EnumsField<E : Enum<E>>(
    private val values: Array<E>,
    private val codeSelector: (E) -> String = { it.name }
) : TField<List<E>, EnumsField<E>>(TFtype.TEXT) {

    var dep = JsonField()
    private var dep_ by dep

    override fun getValue(): Any? {
        try {
            return dep.getValue()
        }catch (_: Exception){}

        return null
    }

    override fun setValue(v: Any?) {
        when (v) {
            is List<*> -> {
                val list = v.filterIsInstance<Enum<*>>().map { it as E }
                _value = list
                //dep_ = list.map { toCode(it) }

                dep.set(
                    list.map { toCode(it) }
                )
            }
            is String -> {
                // v = '[ "GUEST", "USER" ]' ou 'GUEST,USER' selon le format
                val codes = v
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") } // supprime les guillemets si JSON

                _value = codes.mapNotNull { fromCode(it) } // liste d'enums

                // mise à jour du JsonField pour le délgué
                dep.set(
                    (_value as List<E>).map { toCode(it) } // transforme la liste d'enum en liste de codes
                )
            }
        }
    }


    private var codeMap: Map<String, E> = values.associateBy { codeSelector(it) }

    private fun fromCode(code: String): E? = codeMap[code]

    private fun toCode(e: E): String = codeSelector(e)




    init {
        isJson = true
        dep.resetAsArray()
        setValue(emptyList<E>())
    }



    // --- list utils ---
    private fun resolveIndex(index: Int): Int {
        return if (index >= 0) index else size + index
    }

    operator fun get(index: Int): E = get()!![resolveIndex(index)]

    operator fun set(index: Int, element: E) {
        val all = get()!!.toMutableList()
        all[index] = element
        set(all)
    }

    fun addValue(element: Any?): Boolean {
        if (element == null)
            return false

        return add(element as E)
    }

    fun add(element: E): Boolean {
        val all = get()!!.toMutableList()
        if(all.contains(element))
            return false
        val result = all.add(element)
        set(all)
        return result
    }

    fun add(index: Int, element: E) {
        val all = get()!!.toMutableList()
        if(all.contains(element))
            return
        all.add(index, element)
        set(all)
    }

    fun addAll(elements: Collection<E>): Boolean {
        val all = get()!!.toMutableList()
        val elts = mutableListOf<E>()
        elements.forEach {
            if(!all.contains(it))
                elts.add(it)
        }
        val result = all.addAll(elts)
        set(all)
        return result
    }

    fun remove(element: E): Boolean {
        val all = get()!!.toMutableList()
        if(!all.contains(element))
            return false
        val result = all.remove(element)
        if (result) set(all)
        return result
    }

    fun removeAt(index: Int): E {
        val all = get()!!.toMutableList()
        val result = all.removeAt(index)
        set(all)
        return result
    }

    fun clear() {
        set(emptyList())
    }

    fun first() : E = get(0)
    fun second() : E = get(1)
    fun last() : E = get(-1)


    // direct ops
    operator fun iterator(): Iterator<E> = get()!!.iterator()

    val size: Int
        get() {
            return dep.asList<Long>().size
        }

    val isNotEmpty: Boolean
        get() = !isEmpty

    val isEmpty: Boolean
        get() {
            return dep.asList<Long>().isEmpty()
        }

    operator fun plus(e: E) {
        add(e)
    }

    operator fun minus(e: E) {
        remove(e)
    }



    // tms ops
    infix fun contains(v: E): Tx {
        val code = toCode(v)
        return Tx(this, Like, "%[${code}]%") or
                Tx(this, Like, "%,${code}]%") or
                Tx(this, Like, "%[${code},%") or
                Tx(this, Like, "%,${code},%")
    }
    infix fun contains(v: List<E>): Tx {
        prepare()
        return v.map { this contains it }.reduce { acc, tx -> acc and tx }
    }
    infix fun notContains(v: E): Tx {
        prepare()
        return !contains(v)
    }
    infix fun notContains(v: List<E>): Tx {
        prepare()
        return v.map { this notContains it }.reduce { acc, tx -> acc and tx }
    }
    @JsName("jsIsEmptyTx")
    fun isEmpty(): Tx {
        prepare()
        return Tx(this, Equal, "[]")
    }
    @JsName("jsIsNotEmptyTx")
    fun isNotEmpty(): Tx {
        prepare()
        return Tx(this, NotEqual, "[]")
    }

}
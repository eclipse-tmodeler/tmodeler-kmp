package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.operation.Top.Equal
import ai.tambo.tmodeler.field.operation.Top.Like
import ai.tambo.tmodeler.field.operation.Top.NotEqual
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.utils.ThreadLocalBoolean
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.igetById
import ai.tambo.tmodeler.ms.ext.igetByIds
import ai.tambo.tmodeler.ms.ext.igetByKey
import ai.tambo.tmodeler.ms.ext.igetByKeys
import kotlin.js.JsName
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlin.reflect.KClass

open class ListFieldGb<T : TBaseModel>(
    val cls: KClass<T>
) : TField<List<T>, ListFieldGb<T>>(TFtype.TEXT){

    var dep = JsonField()

    override fun getValue(): Any? {
        try {
            return dep.getValue()
        }catch (_: Exception){}

        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun setValue(v: Any?) {
        prepare()

        when (v) {
            is String -> {
                dep.setValue(v)

                if (dep.isNotEmpty()) {
                    val list = dep.asList<Long>()  // contient négatifs et positifs

                    _value = getItems(list)
                }else{
                    val emptyTypedList = listOf<T>()
                    _value = emptyTypedList
                }
            }
            is List<*> -> {
                if(v.isNotEmpty()){
                    @Suppress("UNCHECKED_CAST")
                    val list = v.filter { _prototype!!::class.isInstance(it) }
                        .map { it as T }

                    dep.set(
                        list.mapNotNull {
                            if(it.isServed())
                                it.id_
                            else
                                -it._id_
                        }
                    )
                } else {
                    dep.set(emptyList<Long>())
                }

                _value = v
            }
            else -> {
                _value = null
            }
        }
    }

    fun getItems(list: List<Long>) : List<T> = getOrderedItems(list)
    private fun getOrderedItems(list: List<Long>) : List<T> {
        if(!isDeepLinkResolve)
            return emptyList()

        if(list.isEmpty()) return emptyList()

        val all = mutableListOf<T>()

        list.forEach {
            var v : T?
            if(it > 0){
                v = _tms?.igetById(it)
            }else{
                v = _tms?.igetByKey(-it)
            }
            if(v!=null) all.add(v)
        }

        return all.toList()
    }
    private fun getUnorderedItems(list: List<Long>) : List<T> {
        if(list.isEmpty()) return emptyList()

        val positiveIds = list.filter { it > 0 }
        val negativeIds = list.filter { it < 0 }.map { -it }

        val v1 = _tms?.igetByIds(positiveIds)!!
        val v2 = _tms?.igetByKeys(negativeIds)!!
        v1.addAll( v2)

        return v1
    }

    context(TmsScope)
    override fun resolveDeepLink() {
        val backup = isDeepLinkResolve
        deepLinkResolve(true)
        setValue(getValue())
        deepLinkResolve(backup)
    }

    protected var _tms :TmsGb<T>? = null

    fun tms() = _tms

    fun linkedTo(model: TmsGb<*>) : Boolean{
        return _tms == model
    }


    init {
        isJson = true
        dep.resetAsArray()
        setValue(emptyList<T>())
        //foreignKey()
        isPrepared = false
    }
    companion object {
        private val alreadyIn = ThreadLocalBoolean()
    }

    var _prototype : T? = null
    private var creator: ((KClass<T>) -> T)? = null

    open fun onInit(block: (KClass<T>) -> T): ListFieldGb<T> {
        this.creator = block
        //setupClassInfo()
        return this
    }

    fun createInstance(): T? {
        return creator?.invoke(cls)
    }

    fun prototype(p:  T) : ListFieldGb<T>{
        _prototype = p
        return this
    }

    open fun setupClassInfo() {

        if (!alreadyIn.get()) {
            val proto = if (creator == null) null else try { createInstance() } catch (_: Exception) { null }
            if (proto != null) prototype(proto)

            val p = _prototype
            if (p == null) return
            try {
                alreadyIn.set(true)

                _targetModel_ = p.clazz()
                _targetField = p.keyName()

                _tms = TmsGb(cls).onInit { cls -> p }
            } catch (_: Exception) {
                // silence
            } finally {
                alreadyIn.set(false)
            }
        }
    }

    override fun prepare() {
        if(isPrepared) return

        isPrepared = true
        setupClassInfo()
    }

    // ops
    infix fun contains(v: T): Tx { // marche
        prepare()
        return Tx(this, Like, "%[${v.key}]%") or
                Tx(this, Like, "%,${v.key}]%") or
                Tx(this, Like, "%[${v.key},%") or
                Tx(this, Like, "%,${v.key},%")
    }
    infix fun contains(v: List<T>): Tx {
        prepare()
        return v.map { this contains it }.reduce { acc, tx -> acc and tx }
    }
    infix fun notContains(v: T): Tx {
        prepare()
        return !contains(v)
    }
    infix fun notContains(v: List<T>): Tx {
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



    // --- list utils ---
    private fun resolveIndex(index: Int): Int {
        return if (index >= 0) index else size + index
    }

    operator fun get(index: Int): T = get()!![resolveIndex(index)]

    operator fun set(index: Int, element: T) {
        val all = get()!!.toMutableList()
        all[index] = element
        set(all)
    }

    fun addValue(element: TBaseModel?): Boolean {
        if (element == null)
            return false

        return add(element as T)
    }

    fun add(element: T): Boolean {
        if(element.isServed()){
            remove(element)
        }
        val all = get()!!.toMutableList()
        val result = all.add(element)
        set(all)
        return result
    }

    fun add(index: Int, element: T) {
        val all = get()!!.toMutableList()
        all.add(index, element)
        set(all)
    }

    fun addAll(elements: Collection<T>): Boolean {
        val all = get()!!.toMutableList()
        val result = all.addAll(elements)
        set(all)
        return result
    }

    fun remove(element: T): Boolean {
        val all = get()!!.toMutableList()
        val result = all.remove(element)
        if (result) set(all)
        return result
    }

    fun removeAt(index: Int): T {
        val all = get()!!.toMutableList()
        val result = all.removeAt(index)
        set(all)
        return result
    }

    fun clear() {
        set(emptyList())
    }


    operator fun iterator(): Iterator<T> = get()!!.iterator()

    override fun exists(): Boolean {
        return isNotEmpty
    }

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

    fun first() : T = get(0)
    fun second() : T = get(1)
    fun last() : T = get(-1)


    operator fun plus(e: T): ListFieldGb<T> {
        return apply {
            add(e)
        }
    }

    fun clearLocalLinks() {
        val list = dep.asList<Long>()
        dep.resetAsArray()
        list.forEach {
            if(it>0) dep.add(it)
        }
    }


    val ids: List<Long>
        get() = dep.asList<Long>()
    val min: Long?
        get() = ids.minOrNull()
    val inf: Long?
        get() = min?.minus(1)
    val max: Long?
        get() = ids.minOrNull()
    val sup: Long?
        get() = max?.plus(1)


}
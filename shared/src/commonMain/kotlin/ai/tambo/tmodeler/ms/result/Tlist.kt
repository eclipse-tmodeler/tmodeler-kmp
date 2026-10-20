package ai.tambo.tmodeler.ms.result

import ai.tambo.tmodeler.db.KeysType
import ai.tambo.tmodeler.feature.TFeature
import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TModelGb
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.build
import ai.tambo.tmodeler.ms.ext.build4server
import ai.tambo.tmodeler.ms.ext.collectAsFlow
import ai.tambo.tmodeler.ms.ext.coreBuild
import ai.tambo.tmodeler.ms.ext.delete
import ai.tambo.tmodeler.ms.ext.executeAsFeature
import ai.tambo.tmodeler.ms.ext.filter
import ai.tambo.tmodeler.ms.ext.getByKeys
import ai.tambo.tmodeler.ms.ext.group
import ai.tambo.tmodeler.ms.ext.merge
import ai.tambo.tmodeler.ms.ext.order
import ai.tambo.tmodeler.ms.ext.save
import ai.tambo.tmodeler.ms.server.serveRefresh
import ai.tambo.tmodeler.ms.ext.sub
import ai.tambo.tmodeler.ms.observer.ResultItemsCallback
import ai.tambo.tmodeler.ms.server.serveDelete
import ai.tambo.tmodeler.ms.server.serveSave
import ai.tambo.tmodeler.ms.utils.Ttuple
import ai.tambo.tmodeler.scheduler.TmsScope
import ai.tambo.tmodeler.scheduler.TFlow
import ai.tambo.tmodeler.extension.synchronization.core.RequestConfigBase

@Suppress("UNCHECKED_CAST")
class Tlist<T : TBaseModel>(
    val tms: TmsGb<T>,
    internal val rows: MutableList<Ttuple> = mutableListOf()
) : AbstractMutableList<T>() {

    companion object {
        fun <T : TBaseModel> fromList(tms: TmsGb<T>, list: List<T>): Tlist<T> {
            val rows = list.map { Ttuple(listOf(it)) }.toMutableList()
            return Tlist(tms, rows)
        }

        fun <T : TBaseModel> fromTuples(tms: TmsGb<T>, tupleList: List<Ttuple>): Tlist<T> {
            return Tlist(tms, tupleList.toMutableList())
        }

        fun <T : TBaseModel> of(first: T, vararg others: T): Tlist<T> {
            val list = listOf(first) + others.asList()
            return of(list)
        }

        fun <T : TBaseModel> of(list: List<T>): Tlist<T> {
            val first = list[0]
            val tms = (first as TModelGb<*>).tms() as TmsGb<T>
            return fromList(tms, list)
        }
    }

    override fun isEmpty(): Boolean { return size==0 }
    fun isNotEmpty(): Boolean { return size>0 }

    private fun resolveIndex(index: Int): Int {
        return if (index >= 0) index else size + index
    }
    //operator fun get(index: Int): T = get()!![resolveIndex(index)]
    //override fun get(index: Int): T = rows[index].getAt(0) as? T ?: throw IndexOutOfBoundsException("No element of type T at index $index")
    operator override fun get(index: Int): T = rows[resolveIndex(index)].getAt(0) as? T ?:
        throw IndexOutOfBoundsException("No element of type T at index $index")

    override val size: Int get() = rows.size
    override fun add(element: T): Boolean  { rows.add(Ttuple(listOf(element))); return true }
    override fun addAll(elements: Collection<T>): Boolean { elements.forEach { add(it) }; return elements.isNotEmpty() }
    fun addAll(elements: Tlist<T>) = rows.addAll(elements.rows)
    override fun add(index: Int, element: T) {
        rows.add(index, Ttuple(listOf(element)))
    }
    override fun set(index: Int, element: T): T {
        val old = rows[index].getAt(0) as? T ?: throw IndexOutOfBoundsException("No element of type T at index $index")
        rows[index] = Ttuple(listOf(element))
        return old
    }
    override fun removeAt(index: Int): T {
        val tuple = rows.removeAt(index)
        val value = tuple.getAt(0) as? T
        return value ?: throw IndexOutOfBoundsException()
    }

    fun getTuple(index: Int): Ttuple = rows[index]
    fun add(tuple: Ttuple) = rows.add(tuple)
    fun add(index: Int, tuple: Ttuple) = rows.add(index, tuple)
    fun set(index: Int, tuple: Ttuple) { rows[index] = tuple }
    fun remove(index: Int): Ttuple = rows.removeAt(index)

    override fun remove(element: T): Boolean {
        val iterator = rows.iterator()
        while (iterator.hasNext()) {
            val t = iterator.next().getAt(0) as? T
            if (t == element) {
                iterator.remove()
                return true
            }
        }
        return false
    }
    override fun contains(element: T): Boolean {
        return rows.any { it.getAt(0) as? T == element }
    }
    override fun indexOf(element: T): Int {
        return rows.indexOfFirst { it.getAt(0) as? T == element }
    }
    override fun lastIndexOf(element: T): Int {
        return rows.indexOfLast { it.getAt(0) as? T == element }
    }

    fun clearAll() = rows.clear()
    fun isEmptyList(): Boolean = rows.isEmpty()

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        val sub = Tlist(tms)
        sub.rows.addAll(rows.subList(fromIndex, toIndex))
        return sub
    }

    fun keys(): KeysType = emptyList()

    /*inline fun <reified U : TBaseModel> filter(predicate: (Ttuple) -> Boolean): Tlist<U> {
        val filteredTuples = rows.filter(predicate).toMutableList()
        return Tlist.fromList(Tms(U::class), filteredTuples)
    }*/

    fun merge(x: Tx) = tms.merge(x)
    fun filter(x: Tx) = tms.filter(x)
    fun order(x: Tx) = tms.order(x)
    fun order(x: Tx, x2: Tx) = tms.order(x)
    fun group(vararg fields: BaseField) = tms.group(*fields) //passe pas
    fun sub(size:Int, offset:Int=0) = tms.sub(size, offset)

    context(TmsScope)
    fun build() = tms.build()
    fun coreBuild() = tms.coreBuild()
    fun build4server() = tms.build4server()

    fun first() = get(0)

    fun second() = get(1)
    fun last() = get(size - 1)
    fun <Sp> firstTyped() = get(0) as Sp
    fun <Sp> lastTyped() = get(size - 1) as Sp

    inline fun <reified U : TBaseModel> select(): Tlist<U> {
        val result = mutableListOf<U>()
        for (i in 0 until size) {
            getTuple(i).get<U>()?.let { result += it }
        }

        val tms = TmsGb(U::class)
        return fromList(tms, result)
    }

    // server
    fun serveRefresh(items: List<T> = emptyList(), config: RequestConfigBase?=null, result: ResultItemsCallback<T>?=null) =
        tms.serveRefresh(
            items = items,
            config = config,
            result = result
        )
    fun serveDelete(items: List<T> = emptyList(), result: ResultItemsCallback<T>?=null) = tms.serveDelete(items, result)
    operator fun invoke(result: ResultItemsCallback<T>) {
        tms.clearBuilder()
        tms.serveRefresh(items = this, result = result)
    }
    context(TmsScope)
    fun refresh(result: ResultItemsCallback<T>?=null) : Tlist<T> {
        tms.clearBuilder()
        tms.serveRefresh(items = this, result = result)

        return tms.getByKeys(map { it._id_ })
    }
    context(TmsScope)
    fun delete(fromServer: Boolean=false) : Int = tms.delete(fromServer, this)
    context(TmsScope)
    fun save(fromServer: Boolean=false,result: ResultItemsCallback<T>?=null) : Int {
        val res = tms.save(fromServer, this)

        if(result!=null) serve(result)

        return res
    }
    fun serve(result: ResultItemsCallback<T>) : Tlist<T> {
        tms.serveSave(this, result)
        return this
    }

    fun asFlow(flow: TFlow<T> = TFlow(tms, TFlow.Mode.MANUAL, 25)) : Tlist<T>
            = collectAsFlow(flow)
    fun collectAsFlow(flow: TFlow<T> = TFlow(tms, TFlow.Mode.MANUAL, 25)) : Tlist<T>
            = tms.collectAsFlow(flow, this)

    fun asFeature(feature: TFeature<T>) : Tlist<T>
            = executeAsFeature(feature)
    fun executeAsFeature(feature: TFeature<T>) : Tlist<T>
            = tms.executeAsFeature(feature, this)






    inline fun <reified U : TBaseModel> merge(field: BaseField?=null): Tlist<T> {
        //join(JoinType.INNER)
        return tms.merge<U>(field)
    }



    public operator fun plus(elements: Tlist<out TBaseModel>): Tlist<T> {
        if(tms!=elements.tms) return this
        addAll(elements as Tlist<T>)
        return this
    }
}
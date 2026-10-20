package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.common.IntField
import ai.tambo.tmodeler.field.operation.Top.Equal
import ai.tambo.tmodeler.field.operation.Top.*
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.operation.listToString
import ai.tambo.tmodeler.field.utils.ThreadLocalBoolean
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.igetById
import ai.tambo.tmodeler.ms.ext.igetByKey
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlin.reflect.KClass

open class ModelFieldGb<T : TBaseModel>(
    private val cls: KClass<T>
) : TField<T, ModelFieldGb<T>>(TFtype.INTEGER) {
    var dep = IntField()
    private var dep_ by dep

    // `get()` ne lève pas quand la valeur est absente (une exception coûte cher en wasm)
    override fun getValue(): Any? = dep.get()

    // Garantit qu'un champ déclaré avec .onInit{} (ex: _state, state, linkers) n'est jamais
    // null quand il est lu via le délégué `by` avant d'avoir été explicitement assigné.
    /*override fun get(): T? {
        if (_value == null) {
            createInstance()?.let { setValue(it) }
        }
        return _value as T?
    }*/

    override fun setValue(v: Any?) {
        prepare()

        if(v is Long) {
            dep_ = v

            try {
                _value = getItem(v)
            }catch (e: Exception){
                val v = e
            }
        }
        else if(v is TBaseModel) {
            if(v.isServed())
                dep_ = v.id_
            else
                dep_ = -v._id_
            _value = v
        }
    }

    private fun getItem(v: Long): T? {
        if(!isDeepLinkResolve)
            return null

        return if(v > 0){
            _tms?.igetById(v)
        }else{
            _tms?.igetByKey(-v)
        }
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
        foreignKey()
        isPrepared = false
    }
    companion object {
        private val alreadyIn = ThreadLocalBoolean()
    }

    var _prototype : T? = null
    private var creator: ((KClass<T>) -> T)? = null

    open fun onInit(block: (KClass<T>) -> T): ModelFieldGb<T> {
        this.creator = block
        //setupClassInfo()
        return this
    }

    fun createInstance(): T? {
        return creator?.invoke(cls)
    }

    fun prototype(p:  T) : ModelFieldGb<T>{
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

    public override fun prepare() {
        if(isPrepared) return

        isPrepared = true
        setupClassInfo()
    }


    fun refresh() {

    }



    // ops
    override fun ieq(v: T?): Tx {
        if(v==null){
            return super.ieq(v)
        }
        prepare()
        return Tx(this, Equal, v.sKey)
    }
    override fun ine(v: T?): Tx {
        if(v==null){
            return super.ine(v)
        }
        prepare()
        return Tx(this, NotEqual, v.sKey)
    }
    override fun iinto(values: List<T>): Tx {
        prepare()
        return Tx(this, In, listToString(values.map { it.sKey }))
    }
    operator fun plus(v: T): Tx {
        prepare()
        val s = if(v.isServed()) "" else "-"
        return Tx(this, Equal, "$s${_tms?.name()}.${v.keyName}")
    }


    override fun exists(): Boolean {
        return dep.exists()
    }
    fun available() = _value!=null

    fun isLocalLink() : Boolean {
        prepare()

        try {
            val v = getValue() as Long
            if(v>0) return false
        }catch (_: Exception){}

        return true
    }

    fun clearLocalLink() {
        if(!isLocalLink()) return

        dep.set(null)
    }

}

infix fun <T : TBaseModel> ModelFieldGb<T>.and(v: T) = this + v
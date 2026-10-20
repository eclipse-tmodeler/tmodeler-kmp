package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.create
import ai.tambo.tmodeler.ms.ext.emit
import ai.tambo.tmodeler.ms.ext.get
import ai.tambo.tmodeler.ms.ext.idelete
import ai.tambo.tmodeler.ms.ext.igetByKey
import ai.tambo.tmodeler.ms.ext.isave
import ai.tambo.tmodeler.ms.ext.onChange
import ai.tambo.tmodeler.ms.ext.onDelete
import ai.tambo.tmodeler.ms.ext.onUpdate
import ai.tambo.tmodeler.ms.server.serveRefresh
import ai.tambo.tmodeler.ms.ext.update
import ai.tambo.tmodeler.ms.observer.ResultItemsCallback
import ai.tambo.tmodeler.ms.server.serveCreate
import ai.tambo.tmodeler.ms.server.serveSave
import ai.tambo.tmodeler.ms.server.serveUpdate
import ai.tambo.tmodeler.scheduler.TmsScope
import ai.tambo.tmodeler.scheduler.TGate
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KClass


@Suppress("UNCHECKED_CAST")
open class TModel0<T : TModel0<T>> : TBaseModel(){
    private var _tms :TmsGb<T>? = null

    fun tms(): TmsGb<T> {
        if(_tms==null) _tms = TmsGb(this::class as KClass<T>).onInit { cls -> this as T }
        return _tms!!
    }

    /*override fun save(): Int {
        return if (isCreated()) update() else create()
    }*/
    override fun create(): Int {
        return tms().create(this as T)
    }
    override fun update(): Int {
        return tms().update(this as T)
    }
    context(TmsScope)
    fun del() = idelete()
    context(TmsScope)
    override fun delete() = idelete()
    override fun idelete(): Int {
        return tms().idelete(this as T)
    }
    /*override fun serve(): Int {
        return tms().serve(this as T)
    }*/
    context(TmsScope)
    override fun save(onServed: Any?) = isave(onServed)
    override fun isave(onServed: Any?): Int = isave(false, onServed)
    override fun isave(fromServer: Boolean, onServed: Any?): Int {
        val callback = onServed as? ResultItemsCallback<T>
        return tms().isave(fromServer,listOf(this as T), callback)
    }
    override fun idelete(fromServer: Boolean): Int {
        return tms().idelete(fromServer, this as T)
    }




    override fun init(): T? {
        return this as T
    }
    open fun applyUpload(): T {
        return this as T
    }

    fun <T : TBaseModel?> updates(instances: List<T?>?, data: String?) {
        /*try {
            updates<TBaseModel?>(instances, JSONArray(data))
        } catch (e: java.lang.Exception) {
        }*/
    }

    fun <T : TBaseModel?> updates(instances: List<T?>?) {//, array: JSONArray?
        /*if (instances == null || instances.size() === 0 || array == null || array.length() === 0) return

        for (i in 0..<instances.size()) {
            try {
                instances.get(i)!!.parse(array.getString(i))
            } catch (e: java.lang.Exception) {
            }
        }*/
    }



    fun onUpdate(c: (e: T) -> Unit) {
        tms().onUpdate { keys ->
            if (isCreated() && _id_ in keys) {
                // il y a pas d'interference thread avant d'arriver ici, donc,
                // c'est sur, celui qui a muté était dans TGd, on y est encore
                val inst = tms().igetByKey(_id_)
                c(inst ?: this as T)
            }
        }
    }
    fun onDelete(c: (e: T) -> Unit) {
        tms().onDelete { keys ->
            if (isCreated() && _id_ in keys) {
                c(this as T)
            }
        }
    }


    fun onChange(c: (e: T) -> Unit) {
        onCommitted(c)
        onEmit(c)
    }
    fun <T0, D : TField<T0, D>> onChange(field: TField<T0, D>, callback: (v: T0) -> Unit) {
        onCommitted(field, callback)
        onEmit(field, callback)
    }
    fun onChange(callback: (field: TField<*, *>, value: Any?) -> Unit) {
        onCommitted(callback)
        onEmit(callback)
    }

    // parce que onchange reel est devenu un generic, on utilise ce nom pour remplacer la vraie version du changement
    fun onCommitted(c: (e: T) -> Unit) {
        tms().onChange { keys ->
            if (isCreated() && _id_ in keys) {
                // il y a pas d'interference thread avant d'arriver ici, donc,
                // c'est sur, celui qui a muté était dans TGd, on y est encore
                val inst = tms().igetByKey(_id_)
                c(inst ?: this as T)
            }
        }
    }
    fun <T0, D : TField<T0, D>> onCommitted(field: TField<T0, D>, callback: (v: T0) -> Unit) {
        val key = field.name
        var oldValue: JsonElement? = null

        run {
            val jsonObj = try { Json.parseToJsonElement(data()).jsonObject } catch (e: Exception) { null }
            oldValue = jsonObj?.get(key)
        }

        onCommitted { instance: T ->
            val jsonObj = try { Json.parseToJsonElement(instance.data()).jsonObject } catch (e: Exception) { return@onCommitted }
            val newValue = jsonObj[key]

            if (newValue != oldValue) {
                oldValue = newValue
                callback((instance.clazz().getAttrSafe(key) as TField<T0, D>).get() as T0)
            }
        }
    }
    fun onCommitted(callback: (field: TField<*, *>, value: Any?) -> Unit) {
        val oldValues = mutableMapOf<String, JsonElement?>()

        run {
            val jsonObj = try { Json.parseToJsonElement(data()).jsonObject } catch (e: Exception) { null }
            jsonObj?.forEach { (key, value) -> oldValues[key] = value }
        }

        onCommitted { instance: T ->
            val jsonObj = try { Json.parseToJsonElement(instance.data()).jsonObject } catch (e: Exception) { return@onCommitted }

            for ((key, newValue) in jsonObj) {
                val oldValue = oldValues[key]

                if (newValue != oldValue) {
                    oldValues[key] = newValue

                    val field = instance.clazz().getAttrSafe(key) as? TField<*, *> ?: continue
                    val value = field.get()
                    callback(field, value)
                }
            }
        }
    }

    fun onEmit(onEmit: (e: T) -> Unit) {
        val self = this

        TGate.scope.launch {
            TmEventBus.messages.collect { emitted ->
                if(emitted == self)
                    onEmit(emitted as T)
            }
        }
    }
    fun <T0, D : TField<T0, D>> onEmit(field: TField<T0, D>, callback: (v: T0) -> Unit) {
        val key = field.name
        var oldValue: JsonElement? = null

        run {
            val jsonObj = try { Json.parseToJsonElement(data()).jsonObject } catch (e: Exception) { null }
            oldValue = jsonObj?.get(key)
        }

        onEmit { instance: T ->
            val jsonObj = try { Json.parseToJsonElement(instance.data()).jsonObject } catch (e: Exception) { return@onEmit }
            val newValue = jsonObj[key]

            if (newValue != oldValue) {
                oldValue = newValue
                callback((instance.clazz().getAttrSafe(key) as TField<T0, D>).get() as T0)
            }
        }
    }
    fun onEmit(callback: (field: TField<*, *>, value: Any?) -> Unit) {
        val oldValues = mutableMapOf<String, JsonElement?>()

        run {
            val jsonObj = try { Json.parseToJsonElement(data()).jsonObject } catch (e: Exception) { null }
            jsonObj?.forEach { (key, value) -> oldValues[key] = value }
        }

        onEmit { instance: T ->
            val jsonObj = try { Json.parseToJsonElement(instance.data()).jsonObject } catch (e: Exception) { return@onEmit }

            for ((key, newValue) in jsonObj) {
                val oldValue = oldValues[key]

                if (newValue != oldValue) {
                    oldValues[key] = newValue

                    val field = instance.clazz().getAttrSafe(key) as? TField<*, *> ?: continue
                    val value = field.get()
                    callback(field, value)
                }
            }
        }
    }

    context(TmsScope)
    fun save(onServed: (T?) -> Unit, block: T.() -> Unit) = isave(onServed, block)
    fun isave(onServed: (T?) -> Unit, block: T.() -> Unit): T {
        val cb : ResultItemsCallback<T> = { items->
            if(items!=null && items.isNotEmpty()) onServed(items.first())
            else onServed(null)
        }

        (this as T).block()
        this.isave(cb)
        return this as T
    }

    fun emit(block: T.() -> Unit): T {
        (this as T).block()
        tms().emit(listOf(this))
        return this as T
    }
    context(TmsScope)
    fun save(block: T.() -> Unit) = isave(block)
    fun isave(block: T.() -> Unit): T {
        (this as T).block()
        this.isave()
        return this as T
    }
    context(TmsScope)
    fun notify(block: T.() -> Unit) = inotify(block)
    fun inotify(block: T.() -> Unit): T = isave(block)


    // server
    operator fun invoke(result: (T?) -> Unit){
        tms().clearBuilder()
        tms().serveRefresh(items = listOf(this as T)) { items->
            if(items!=null && items.isNotEmpty()) result(items.first())
            else result(null)
        }
    }
    context(TmsScope)
    fun refresh(result: ((T?) -> Unit)?=null): T {
        if(result!=null){
            tms().clearBuilder()
            tms().serveRefresh(items = listOf(this as T)) { items->
                if(items!=null && items.isNotEmpty()) result(items.first())
                else result(null)
            }
        }

        return (if(isCreated()) {
            tms().get(key)?:this
        } else {
            this
        }) as T
    }
    fun serve(result: ((T?) -> Unit)?=null): T {
        if(result!=null) {
            tms().serveSave(listOf(this as T)) { items ->
                if (items != null && items.isNotEmpty()) result(items.first())
                else result(null)
            }
        }else{
            tms().serveSave(listOf(this as T))
        }
        return this as T
    }
    fun serveCreate(result: ((T?) -> Unit)?=null): T {
        if(result!=null) {
            tms().serveCreate(listOf(this as T)) { items ->
                if (items != null && items.isNotEmpty()) result(items.first())
                else result(null)
            }
        }else{
            tms().serveCreate(listOf(this as T))
        }
        return this as T
    }
    fun serveUpdate(result: ((T?) -> Unit)?=null): T {
        if(result!=null){
            tms().serveUpdate(listOf(this as T)) { items->
                if(items!=null && items.isNotEmpty()) result(items.first())
                else result(null)
            }
        }else{
            tms().serveUpdate(listOf(this as T))
        }
        return this as T
    }
    
    override fun onSync(): TServerConfigBase? = TModeler.getInstance().syncEngine().newServerConfig()
}
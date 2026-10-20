package ai.tambo.tmodeler.db

import ai.tambo.tmodeler.ms.observer.ResultCallback
import ai.tambo.tmodeler.extension.synchronization.core.RequestConfigBase

class Ttable(
    name: String,
    fields: List<TtableField> = listOf(),
    primaryKeys: List<String> = listOf()
) {
    /*
    var driver: TtableDriver = TtableDriver(name, fields.toMutableList(), primaryKeys.toMutableList())
    private val observers: MutableList<TModelObserver> = mutableListOf()

    fun addObserver(observer: TModelObserver) { observers.add(observer) }
    fun removeObserver(observer: TModelObserver) { observers.remove(observer) }
    fun notifyChange(keys: KeysType) {}
    fun notifySave(keys: KeysType) {}
    fun notifyCreate(keys: KeysType) {}
    fun notifyUpdate(keys: KeysType) {}
    fun notifyDelete(keys: KeysType) {}
    fun notifyChange(keys: KeysType, onCreate: Boolean, onUpdate: Boolean, onDelete: Boolean) {}
    */

    private val _observers = mutableListOf<TModelObserver>()
    val driver = TtableDriver(name, fields.toMutableList(), primaryKeys.toMutableList())

    fun addObserver(observer: TModelObserver) {
        _observers.add(observer)
    }

    fun removeObserver(observer: TModelObserver) {
        _observers.remove(observer)
    }

    fun notifyChange(keys: KeysType) = _observers.forEach { it.onModelChange(keys) }
    fun notifySave(keys: KeysType) = _observers.forEach { it.onSave(keys) }

    fun notifyCreate(keys: KeysType) = _observers.toList().forEach {
        it.onCreate(keys); it.onSave(keys); it.onModelChange(keys)
    }

    fun notifyUpdate(keys: KeysType) = _observers.toList().forEach {
        it.onUpdate(keys); it.onSave(keys); it.onModelChange(keys)
    }

    fun notifyDelete(keys: KeysType) = _observers.toList().forEach {
        it.onDelete(keys); it.onModelChange(keys)
    }

    fun notifyChange(keys: KeysType, onCreate: Boolean, onUpdate: Boolean, onDelete: Boolean) {
        if (onCreate) notifyCreate(keys)
        if (onUpdate) notifyUpdate(keys)
        if (onDelete) notifyDelete(keys)
    }


    fun notifyServeCreate(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null) = _observers.toList().forEach { it.onServeCreate(ids, res, config) }
    fun notifyServeUpdate(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null) = _observers.toList().forEach { it.onServeUpdate(ids, res, config) }
    fun notifyServeDelete(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null) = _observers.toList().forEach { it.onServeDelete(ids, res, config) }
    fun notifyServeRefresh(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null) = _observers.toList().forEach { it.onServeRefresh(ids, res, config) }

    fun notifyServer(
        ids: KeysType,
        res: ResultCallback?,
        config: RequestConfigBase?=null,
        onServeCreate: Boolean,
        onServeUpdate: Boolean,
        onServeDelete: Boolean,
        onServeRefresh: Boolean
    ) {
        if (onServeCreate) notifyServeCreate(ids, res, config)
        if (onServeUpdate) notifyServeUpdate(ids, res, config)
        if (onServeDelete) notifyServeDelete(ids, res, config)
        if (onServeRefresh) notifyServeRefresh(ids, res, config)
    }
}
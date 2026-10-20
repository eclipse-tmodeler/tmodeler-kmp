package ai.tambo.tmodeler.ms.observer

import ai.tambo.tmodeler.db.KeysType
import ai.tambo.tmodeler.db.TModelObserver
import ai.tambo.tmodeler.scheduler.TFlow
import ai.tambo.tmodeler.extension.synchronization.core.RequestConfigBase


typealias ServerCallback = (ids: KeysType, res: ResultCallback?, config: RequestConfigBase?) -> Unit
typealias ModelCallback = (KeysType) -> Unit
typealias ResultCallback = (KeysType?) -> Unit
typealias ResultItemsCallback<T> = (List<T>?) -> Unit
typealias ResultItemFlowCallback<T> = (TFlow<T>?) -> Unit


class TModelCallback : TModelObserver {
    private val _onChangeCallbacks = mutableListOf<ModelCallback>()
    private val _onSaveCallbacks = mutableListOf<ModelCallback>()
    private val _onCreateCallbacks = mutableListOf<ModelCallback>()
    private val _onUpdateCallbacks = mutableListOf<ModelCallback>()
    private val _onDeleteCallbacks = mutableListOf<ModelCallback>()

    private val _onServeCreateCallbacks = mutableListOf<ServerCallback>()
    private val _onServeUpdateCallbacks = mutableListOf<ServerCallback>()
    private val _onServeDeleteCallbacks = mutableListOf<ServerCallback>()
    private val _onServeRefreshCallbacks = mutableListOf<ServerCallback>()

    fun addOnChangeCallback(cb: ModelCallback) { _onChangeCallbacks.add(cb) }
    fun addOnSaveCallback(cb: ModelCallback) { _onSaveCallbacks.add(cb) }
    fun addOnCreateCallback(cb: ModelCallback) { _onCreateCallbacks.add(cb) }
    fun addOnUpdateCallback(cb: ModelCallback) { _onUpdateCallbacks.add(cb) }
    fun addOnDeleteCallback(cb: ModelCallback) { _onDeleteCallbacks.add(cb) }

    fun addOnServeCreateCallback(cb: ServerCallback) { _onServeCreateCallbacks.add(cb) }
    fun addOnServeUpdateCallback(cb: ServerCallback) { _onServeUpdateCallbacks.add(cb) }
    fun addOnServeDeleteCallback(cb: ServerCallback) { _onServeDeleteCallbacks.add(cb) }
    fun addOnServeRefreshCallback(cb: ServerCallback) { _onServeRefreshCallbacks.add(cb) }

    override fun onModelChange(keys: KeysType) {
        _onChangeCallbacks.forEach { it(keys) }
    }

    override fun onSave(keys: KeysType) {
        _onSaveCallbacks.forEach { it(keys) }
    }

    override fun onCreate(keys: KeysType) {
        _onCreateCallbacks.forEach { it(keys) }
    }

    override fun onUpdate(keys: KeysType) {
        _onUpdateCallbacks.forEach { it(keys) }
    }

    override fun onDelete(keys: KeysType) {
        _onDeleteCallbacks.forEach { it(keys) }
    }


    override fun onServeCreate(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?) {
        _onServeCreateCallbacks.forEach { it(ids, res, config) }
    }

    override fun onServeUpdate(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?) {
        _onServeUpdateCallbacks.forEach { it(ids, res, config) }
    }

    override fun onServeDelete(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?) {
        _onServeDeleteCallbacks.forEach { it(ids, res, config) }
    }

    override fun onServeRefresh(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?) {
        _onServeRefreshCallbacks.forEach { it(ids, res, config) }
    }
}
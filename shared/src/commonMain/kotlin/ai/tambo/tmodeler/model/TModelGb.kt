package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.field.advanced.ListFieldGb
import ai.tambo.tmodeler.field.advanced.ModelFieldGb
import ai.tambo.tmodeler.field.advanced.TimeField
import ai.tambo.tmodeler.field.base.TF
import ai.tambo.tmodeler.ms.server.ProcessType
import ai.tambo.tmodeler.ms.server.serveDelete
import ai.tambo.tmodeler.scheduler.TmsScope

/**
 * @TModelGb for TModel Global
 * */
open class TModelGb<T : TModelGb<T>> : TModel0<T>(){

    var _state = ModelFieldGb(Tstate::class).onDelete(TF.CASCADE).onInit { cls -> Tstate() }
    var state = ModelFieldGb(Tstate::class).onDelete(TF.CASCADE).onInit { cls -> Tstate() }
    var linkers = ListFieldGb(Tlinker::class).onDelete(TF.CASCADE).onInit { cls -> Tlinker() }


    @TMignore var _state_ by _state
    @TMignore var state_ by state
    @TMignore var linkers_ by linkers


    override fun clazz(): Tclass {
        _clazz = null
        if (_clazz == null) _clazz = Tclass(TModelGb::class, super.clazz()).fields(
            state.apply { name = "state" },
            _state.apply { name = "_state" },
            linkers.apply { name = "linkers" },
        )
        return _clazz!!
    }

    @Suppress("UNCHECKED_CAST")
    override fun clazz(inst : TobGb): Tclass {
        inst as T

        if (_clazz == null) _clazz = Tclass(TModelGb::class, super.clazz(inst)).fields(
            inst.state.apply { name = "state" },
            inst._state.apply { name = "_state" },
            inst.linkers.apply { name = "linkers" },
        )
        return _clazz!!
    }




    @Suppress("UNCHECKED_CAST")
    override fun init(): T {
        super.init()
        initStates()
        return this as T
    }

    override fun initStates() {
        if (_state.empty()) {
            val lstate = Tstate()
            val time = TimeField.now()
            lstate.created_ = time
            lstate.updated_ = time
            lstate.isave()
            this._state_ = lstate
        }
    }


    override fun update(): Int {
        return isave()
    }
    override fun create(): Int {
        return isave()
    }
    override fun saveX(fromServer: Boolean, manualServe: Boolean): ProcessType {
        var result = ProcessType.NONE

        if (_state.exists()) {
            if (fromServer)
                _state_.uploaded_ = TimeField.now()
            else {
                _state_.updated_ = TimeField.now()
                if((autoServe() || manualServe)) result = ProcessType.SERVE
            }
            _state_.isave()
        }

        if((autoServe() || manualServe) && onCreate()){
            result = ProcessType.SERVE
        }

        return result
    }
    override fun deleteX(fromServer: Boolean, manualServe: Boolean): ProcessType {
        var result = ProcessType.NONE

        if(!isSaved() || onActionDelete()) return result

        if (fromServer)
            result = ProcessType.DIRECT
        else {
            if(isServed() && _state.exists()){
                _state_.isave {
                    deleted_ = TimeField.now()
                }

                if((autoServe() || manualServe)){
                    result = ProcessType.SERVE
                }
            }else{
                result = ProcessType.DIRECT
            }
        }

        return result
    }
    override fun stop() {
        if(exists()){
            _state_.isave {
                stopped_ = TimeField.now()
            }
        }
    }


    fun contentLink(t: TBaseModel, field: String): Boolean {
        linkers_.forEach {
            if(it.model_ == t.getModelName() && it.field_ == field && it.fk_ == t._id_)
                return true
        }
        return false
    }

    context(TmsScope)
    fun notifyLinks() {
        val target = this
        linkers_.forEach {

            // Bind the inherited context-receiver method to its declaring type for Wasm.
            val item: TModel0<*> = it.item
            item.save {
                val v = field(it.field_)
                if(v!=null){
                    if(v is ModelFieldGb<*>)
                        v.setValue(target)
                    if(v is ListFieldGb<*>)
                        v.addValue(target)
                }
            }
        }
    }


    override fun applyUpload(): T {
        _state_.uploaded_ = TimeField.now()
        _state_.isave(true)
        return super.applyUpload()
    }

    override fun onUpdate(): Boolean {
        val v = isServed() &&
                (_state.exists() && (_state_.updated.exists() && _state_.uploaded.exists()) && _state_.updated_ > _state_.uploaded_ )
        return v
    }

    override fun onDelete(): Boolean {
        return isServed() && onActionDelete()
    }

    override fun onActionDelete(): Boolean {
        return _state.exists() && _state_.deleted.exists()
    }

    override fun onStopped(): Boolean {
        _state.refresh()
        return _state.exists() && _state_.stopped.exists()
    }





    override fun autoServe(): Boolean {
        //return tms().db.autoServe && onSync()?.isConfig()?:false
        return tms().baseServerConfig.autoSync && onSync()?.isConfig()?:false
    }



    // server
    fun delete(result: (T?) -> Unit): T = serveDelete(result)
    fun serveDelete(result: ((T?) -> Unit)?=null): T {
        if(result!=null) {
            tms().serveDelete(listOf(this as T)) { items->
                if(items!=null && items.isNotEmpty()) result(items.first())
                else result(null)
            }
        }else{
            tms().serveDelete(listOf(this as T))
        }
        return this as T
    }



}
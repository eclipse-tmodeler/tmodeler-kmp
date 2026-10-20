package ai.tambo.tmodeler.db

import ai.tambo.tmodeler.ms.observer.ResultCallback
import ai.tambo.tmodeler.extension.synchronization.core.RequestConfigBase

typealias KeysType = List<Long>

interface TModelObserver {
    fun onModelChange(keys: KeysType)
    fun onSave(keys: KeysType)
    fun onCreate(keys: KeysType)
    fun onUpdate(keys: KeysType)
    fun onDelete(keys: KeysType)


    fun onServeCreate(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null)
    fun onServeUpdate(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null)
    fun onServeDelete(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null)
    fun onServeRefresh(ids: KeysType, res: ResultCallback?, config: RequestConfigBase?=null)
}
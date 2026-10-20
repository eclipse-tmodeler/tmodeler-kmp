package ai.tambo.tmodeler.ms.data

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TmEventBus
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.TmsDeep
import ai.tambo.tmodeler.ms.filter.TFilterBuilder

object TdataCreator {
    fun <T : TBaseModel> create(tms: TmsGb<T>, _builder: TFilterBuilder?, instances: List<T>): Int {
        if(instances.isEmpty()) return 0

        val jsonData = TobGb.data(instances)

        val result = TmsDeep.insertDataSafe(tms.modelClass(), jsonData)
        val lastId = TmsDeep.lastInsertRowId(tms.modelClass())

        val lastIndex = instances.size - 1
        for (i in instances.indices) {
            instances[i].key = lastId - lastIndex + i
        }

        if (result) {
            val keys = instances.map { it.key }
            TmsDeep.notifyChange(tms.modelClass(), keys, true, false, false)
            return instances.size
        }

        return 0
    }

    fun <T : TBaseModel> emitCreate(instances: List<T>): Int {
        TmEventBus.publish(instances)
        return instances.size
    }
}
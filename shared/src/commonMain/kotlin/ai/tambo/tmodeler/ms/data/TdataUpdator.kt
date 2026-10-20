package ai.tambo.tmodeler.ms.data

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TmEventBus
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.TmsDeep
import ai.tambo.tmodeler.ms.filter.TFilterBuilder

object TdataUpdator {
    fun <T : TBaseModel> update(tms: TmsGb<T>, _builder: TFilterBuilder?, instances: List<T>): Int {
        if(instances.isEmpty()) return 0

        val jsonData = TobGb.data(instances)

        val result = TmsDeep.updateDataSafe(tms.modelClass(), jsonData, instances[0].keyName())

        if (result) {
            val keys = instances.map { it.key }
            TmsDeep.notifyChange(tms.modelClass(), keys, false, true, false)
            return instances.size
        }

        return 0
    }

    fun <T : TBaseModel> emitUpdate(instances: List<T>): Int {
        TmEventBus.publish(instances)
        return instances.size
    }
}
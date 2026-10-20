package ai.tambo.tmodeler.ms.data

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.TmsDeep
import ai.tambo.tmodeler.ms.filter.TFilterBuilder

class TdataDeletor {
    companion object {
        fun <T : TBaseModel> del(tms: TmsGb<T>, _builder: TFilterBuilder?, instances: List<T>): Int {
            if(instances.isEmpty()) return 0

            val jsonData = TobGb.data(instances)

            val result = TmsDeep.deleteData(tms.modelClass(), jsonData, instances[0].keyName())

            if (result) {
                val keys = instances.map { it.key }
                TmsDeep.notifyChange(tms.modelClass(), keys, false, false, true)
                return instances.size
            }

            return 0
        }
    }
}
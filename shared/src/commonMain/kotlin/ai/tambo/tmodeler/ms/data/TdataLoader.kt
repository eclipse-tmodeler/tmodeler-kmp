package ai.tambo.tmodeler.ms.data

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.TmsDeep
import ai.tambo.tmodeler.ms.filter.TFilterBuilder
import ai.tambo.tmodeler.ms.result.Tlist
import ai.tambo.tmodeler.ms.utils.Ttuple

class TdataLoader {
    companion object {
        fun <T : TBaseModel> get(tms: TmsGb<T>, _builder: TFilterBuilder): Tlist<T> {
            _builder.tableFields = tms.tableAllFields()
            //_builder.fieldsKeys = tms.allFieldsKeys()

            val data = TmsDeep.read(tms.modelClass(), _builder)

            return if (tms._tuplet!!.values.size == 1) {
                val list: List<T> = TobGb.parse(tms._prototype!!, data, _builder.isDeepLinkResolve)
                Tlist.fromList(tms, list)
            } else {
                val list: List<Ttuple> = TobGb.parseTuples(tms._tuplet!!, data, _builder.isDeepLinkResolve)
                Tlist.fromTuples(tms, list)
            }
        }
    }
}
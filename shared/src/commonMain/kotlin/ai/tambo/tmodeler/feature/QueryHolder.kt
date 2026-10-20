package ai.tambo.tmodeler.feature

import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.result.Tlist

data class QueryHolder<K : TBaseModel>(
    val model: TmsGb<K>,
    val lambda: K,
    val query: Tlist<K>
)
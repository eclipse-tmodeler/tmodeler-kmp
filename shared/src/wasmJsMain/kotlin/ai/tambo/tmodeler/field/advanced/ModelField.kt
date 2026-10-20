package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TModelRegistry
import kotlin.reflect.KClass

class ModelField<T : TBaseModel>(cls: KClass<T>) : ModelFieldGb<T>(cls) {
    init {
        onInit { TModelRegistry.instanceFor(it) }
    }
}

package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.model.TBaseModel
import kotlin.reflect.KClass

public class ModelField<T : TBaseModel>(cls: KClass<T>) : ModelFieldGb<T>(cls) {
    init {
        onInit({ cls -> cls.constructors.first().call() })
    }
}
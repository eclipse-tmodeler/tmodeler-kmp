package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.model.TBaseModel
import kotlin.reflect.KClass

public class ListField<T : TBaseModel>(cls: KClass<T>) : ListFieldGb<T>(cls) {
    init {
        onInit({ cls -> cls.constructors.first().call() })
    }
}
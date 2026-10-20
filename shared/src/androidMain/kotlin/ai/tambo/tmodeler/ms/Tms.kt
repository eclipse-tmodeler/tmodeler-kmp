package ai.tambo.tmodeler.ms

import ai.tambo.tmodeler.model.TBaseModel
import kotlin.reflect.KClass

class Tms<T : TBaseModel>(
    private val cls: KClass<T>
) : TmsGb<T>(cls) {
    init {
        onInit { cls -> cls.constructors.first().call() }
    }
}
package ai.tambo.tmodeler.ms

import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TModelRegistry
import kotlin.reflect.KClass

class Tms<T : TBaseModel>(private val cls: KClass<T>) : TmsGb<T>(cls) {
    init {
        // Validate outside setupClassInfo(), whose legacy implementation catches errors.
        val initial = TModelRegistry.instanceFor(cls)
        initial.clazz()
        onInit { TModelRegistry.instanceFor(it) }
    }
}

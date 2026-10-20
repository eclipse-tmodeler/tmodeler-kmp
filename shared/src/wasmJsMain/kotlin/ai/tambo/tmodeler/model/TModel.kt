package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Current_class
import ai.tambo.tmodeler.core.Tclass
import kotlin.reflect.KClass

open class TModel<T : TModel<T>> : TModelGb<T>() {
    @Suppress("UNCHECKED_CAST")
    override fun instance(): T = TModelRegistry.instanceFor(this::class as KClass<T>)

    @Suppress("UNCHECKED_CAST")
    override fun clazz(): Tclass {
        if (_clazz == null) {
            val registered = TModelRegistry.clazzFor(this::class as KClass<T>, this as T, null)
            _clazz = registered.sup(super.clazz())
        }
        return _clazz!!
    }

    override fun clazz(inst: Current_class): Tclass {
        require(inst is TModel<*> && inst::class == this::class) {
            "TModel.clazz: expected an instance of ${this::class.simpleName}"
        }
        return inst.clazz()
    }

    override fun clone(): T = clone(instance())
}

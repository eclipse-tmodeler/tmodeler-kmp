package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Current_class
import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.Treflector

open class TModel<T : TModel<T>> : TModelGb<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun instance(): T {
        return this::class.constructors.first().call() as T
    }
    override fun clazz(): Tclass {
        return Treflector.clazz(this, this)
    }
    override fun clazz(inst : Current_class): Tclass {
        return Treflector.clazz(this, inst)
    }
    override fun clone(): T {
        return clone(instance())
    }

}
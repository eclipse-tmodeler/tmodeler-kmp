package ai.tambo.tmodeler.core

import ai.tambo.tmodeler.model.TBaseModel

open class Tob : TobGb() {

    // Sur wasmJs, pas de constructors via réflexion.
    // Chaque sous-classe doit surcharger instance() manuellement (via TModel.instance()).
    override fun instance(): Tob = Tob()

    override fun clazz(): Tclass {
        if (_clazz == null) _clazz = Tclass(TobGb::class, null)
        return _clazz!!
    }

    override fun clazz(inst: TobGb): Tclass = clazz()

    override fun clone(): Tob = clone(instance())

    companion object {
        inline fun <reified T : TobGb> parse(json: String): List<T> {
            // Nécessite que le prototype soit fourni explicitement côté appelant
            return emptyList()
        }

        fun <E : TBaseModel> newInstance(model: Any?): E? = null
    }
}
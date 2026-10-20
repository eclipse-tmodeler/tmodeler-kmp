package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.field.common.IntField
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.find
import ai.tambo.tmodeler.ms.ext.iget

class Tlinker : TModel0<Tlinker>() {

    override fun instance(): Tlinker {
        return Tlinker()
    }

    var model = TextField()
    var field = TextField()
    var fk = IntField()

    @TMignore var model_ by model
    @TMignore var field_ by field
    @TMignore var fk_ by fk

    companion object {
        val tms by lazy { TmsGb(Tlinker::class).onInit { cls -> Tlinker() } }
    }

    override fun clazz(): Tclass {
        if (_clazz == null) _clazz = Tclass(Tlinker::class, super.clazz()).fields(
            model.apply { name = "model" },
            field.apply { name = "field" },
            fk.apply { name = "fk" },
        )
        return _clazz!!
    }

    @Suppress("UNCHECKED_CAST")
    override fun clazz(inst: TobGb): Tclass {
        inst as Tlinker

        if (_clazz == null) _clazz = Tclass(Tlinker::class, super.clazz()).fields(
            inst.model.apply { name = "model" },
            inst.field.apply { name = "field" },
            inst.fk.apply { name = "fk" },
        )
        return _clazz!!
    }

    val item by lazy {
        tms().find(model_)?.iget(fk_) as TModelGb<*>
    }
}
package ai.tambo.tmodeler.extension.security.core

import ai.tambo.tmodeler.field.advanced.JsonField
import ai.tambo.tmodeler.field.advanced.ModelFieldGb
import ai.tambo.tmodeler.field.advanced.TimeField
import ai.tambo.tmodeler.model.TModelGb


class TNodeToken : TModelGb<TNodeToken>() {
    var public_k: JsonField = JsonField()
    var private_k: JsonField = JsonField()
    var from: TimeField = TimeField()
    var to: TimeField = TimeField()

    // en metant à jour un token t0, on cree t1 et t0.private.from(t1).to(t1).set(real_t0_private_k)
    var parent = ModelFieldGb(TNodeToken::class)

    fun generate(parent: TNodeToken?): TNodeToken {
        return this
    }
}

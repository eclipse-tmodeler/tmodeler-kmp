package ai.tambo.tmodeler.extension.security.core

import ai.tambo.tmodeler.field.advanced.ModelFieldGb
import ai.tambo.tmodeler.model.TModelGb


class TNode<T : TNode<T>> : TModelGb<T>() {
    var token = ModelFieldGb(TNodeToken::class)
}

package ai.tambo.tmodeler.scheduler

import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb

/**
 * Accès Tms séquentiel via coroutine
 */
suspend fun <T : TBaseModel> TmsGb<T>.gated(
    block: suspend TmsGb<T>.() -> Unit
) {
    TGate.data {
        this.block()
    }
}

/**
 * Accès Tms séquentiel avec résultat
 */
suspend fun <T : TBaseModel, R> TmsGb<T>.gatedResult(
    default: R?=null,
    block: suspend TmsGb<T>.() -> R
): R {
    return TGate.dataResult(default) {
        this.block()
    }
}


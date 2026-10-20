package ai.tambo.tmodeler.support

import ai.tambo.tmodeler.scheduler.TGate
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlinx.coroutines.runBlocking

/**
 * Exécute [block] dans `TGate.data` et ATTEND le résultat.
 *
 * En production on écrit `TGate.data { ... }` (asynchrone). Dans un test on veut le résultat tout de suite :
 * c'est le rôle de `TGate.dataResult`. Les opérations sur les modèles (`save()`, `delete()`, `Tms.all()`, ...) ne sont
 * disponibles que dans ce bloc (ou dans une fonction `context(TmsScope)`), car elles ont besoin du « contexte de
 * données » (`TmsScope`) qui garantit le bon thread.
 *
 * Cette fonction est volontairement au niveau fichier (et non dans un `object`) pour éviter tout
 * interblocage d'initialisation de classe entre le thread de test et le thread de données.
 */
fun <R> inData(block: suspend context(TmsScope) () -> R): R = runBlocking {
    TGate.dataResult { relay(block) }
}

context(TmsScope)
private suspend fun <R> relay(block: suspend context(TmsScope) () -> R): R = block(this@TmsScope)

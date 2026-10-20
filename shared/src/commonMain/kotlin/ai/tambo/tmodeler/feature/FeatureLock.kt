package ai.tambo.tmodeler.feature

import ai.tambo.tmodeler.ms.ext.TmsDeep
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex

object FeatureLock {


    private val mutex = Mutex()

    suspend fun controlled(block: (done: () -> Unit) -> Unit) {
        if (!TmsDeep.synchronizer.concurrencyReadWrite) {
            mutex.lock()
            var released = false
            val deferred = CompletableDeferred<Unit>()
            val done = {
                if (!released) {
                    released = true
                    mutex.unlock()
                    deferred.complete(Unit)
                }
            }
            try {
                block(done)
                deferred.await()
            } catch (e: Throwable) {
                done()
                throw e
            }
        } else {
            block {}
        }
    }
}
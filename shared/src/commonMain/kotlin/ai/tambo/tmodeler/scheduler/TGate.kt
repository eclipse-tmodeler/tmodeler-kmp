package ai.tambo.tmodeler.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/* =========================
   CONTEXT SCOPES
========================= */

class TmsScope internal constructor()
class MainScope internal constructor()
class BackScope internal constructor()


//@OptIn(ExperimentalContextReceivers::class)
object TGate {

    /* =========================
       SCOPE
    ========================== */

    var scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + TGateDispatcher.io
    )

    /* =========================
       MUTEX (KMP SAFE)
    ========================== */

    private val globalDbMutex = Mutex()
    private val mapMutex = Mutex()
    private val dbMutexes = mutableMapOf<String, Mutex>()

    private suspend fun getMutex(dbName: String?): Mutex {
        if (dbName == null) return globalDbMutex

        return mapMutex.withLock {
            dbMutexes.getOrPut(dbName) { Mutex() }
        }
    }

    /* =========================
       CORE FUNCTIONS
    ========================== */


    fun data(
        dbName: String? = null,
        rw: RW = RW.WRITE,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend context(TmsScope) () -> Unit
    ) {
        scope.launch(TGateDispatcher.io) {
            dataAwait(dbName, rw, onError, block)
        }
    }

    suspend fun dataAwait(
        dbName: String? = null,
        rw: RW = RW.WRITE,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend context(TmsScope) () -> Unit
    ) {
        val mutex = getMutex(dbName)
        withContext(TGateDispatcher.io) {
            mutex.withLock {
                val dataScope = TmsScope()
                if (onError == null) {
                    with(dataScope) {  // ← Établit le context receiver automatiquement
                        block(dataScope)
                    }
                } else {
                    try {
                        with(dataScope) {
                            block(dataScope)
                        }
                    } catch (t: Throwable) {
                        onError(t)
                    }
                }
            }
        }
    }

    suspend fun <R> dataResult(
        default: R? = null,
        dbName: String? = null,
        rw: RW = RW.WRITE,
        block: suspend context(TmsScope) () -> R
    ): R {
        val mutex = getMutex(dbName)
        return withContext(TGateDispatcher.io) {
            mutex.withLock {
                val dataScope = TmsScope()
                if (default == null) {
                    with(dataScope) {
                        block(dataScope)
                    }
                } else {
                    try {
                        with(dataScope) {
                            block(dataScope)
                        }
                    } catch (t: Throwable) {
                        default
                    }
                }
            }
        }
    }
    /*fun data(
        dbName: String? = null,
        rw: RW = RW.WRITE,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend (TDataScope) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            dataAwait(dbName, rw, onError, block)
        }
    }

    suspend fun dataAwait(
        dbName: String? = null,
        rw: RW = RW.WRITE,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend (TDataScope) -> Unit
    ) {
        val mutex = getMutex(dbName)
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val dataScope = TDataScope()
                if (onError == null) {
                    with(TDataScope()) {
                        block(dataScope)
                    }
                } else {
                    try {
                        with(TDataScope()) {
                            block(dataScope)
                        }
                    } catch (t: Throwable) {
                        onError(t)
                    }
                }
            }
        }
    }

    suspend fun <R> dataResult(
        default: R? = null,
        dbName: String? = null,
        rw: RW = RW.WRITE,
        block: suspend (TDataScope) -> R
    ): R {
        val mutex = getMutex(dbName)
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val dataScope = TDataScope()
                if (default == null) {
                    with(TDataScope()) {
                        block(dataScope)
                    }
                } else {
                    try {
                        with(TDataScope()) {
                            block(dataScope)
                        }
                    } catch (t: Throwable) {
                        default
                    }
                }
            }
        }
    }*/

    fun back(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        scope.launch(TGateDispatcher.io) {
            if (onError == null) {
                block()
            } else {
                try {
                    block()
                } catch (t: Throwable) {
                    onError(t)
                }
            }
        }
    }

    suspend fun backAwait(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        withContext(TGateDispatcher.io) {
            if (onError == null) {
                block()
            } else {
                try {
                    block()
                } catch (t: Throwable) {
                    onError(t)
                }
            }
        }
    }

    fun main(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        scope.launch(MainDispatcher.immediate) {
            if (onError == null) {
                block()
            } else {
                try {
                    block()
                } catch (t: Throwable) {
                    onError(t)
                }
            }
        }
    }

    suspend fun mainAwait(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        withContext(MainDispatcher.immediate) {
            if (onError == null) {
                block()
            } else {
                try {
                    block()
                } catch (t: Throwable) {
                    onError(t)
                }
            }
        }
    }
}

val DAE = "DATA_ACCESS_ERROR"
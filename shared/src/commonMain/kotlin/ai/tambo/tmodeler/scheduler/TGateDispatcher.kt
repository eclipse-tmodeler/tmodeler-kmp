package ai.tambo.tmodeler.scheduler

import kotlinx.coroutines.CoroutineDispatcher

// Dispatcher dedie aux I/O bloquantes (SQLite...), different par plateforme
// car Dispatchers.IO n'existe pas sur wasmJs.
expect object TGateDispatcher {
    val io: CoroutineDispatcher
}

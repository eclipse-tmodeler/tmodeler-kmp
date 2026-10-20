package ai.tambo.tmodeler.scheduler

import kotlinx.coroutines.Dispatchers

// Dispatchers.IO n'existe pas sur wasmJs : on retombe sur Default (pool CPU-bound).
actual object TGateDispatcher {
    actual val io = Dispatchers.Default
}

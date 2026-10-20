package ai.tambo.tmodeler.scheduler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual object TGateDispatcher {
    actual val io = Dispatchers.IO
}

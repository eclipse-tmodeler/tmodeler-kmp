package ai.tambo.tmodeler.scheduler

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object MainDispatcher {
    actual val immediate: CoroutineDispatcher =
        Dispatchers.Main.immediate
}

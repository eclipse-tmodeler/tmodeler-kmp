package ai.tambo.tmodeler.scheduler

import kotlinx.coroutines.Dispatchers

actual object MainDispatcher {
    actual val immediate = Dispatchers.Unconfined
}

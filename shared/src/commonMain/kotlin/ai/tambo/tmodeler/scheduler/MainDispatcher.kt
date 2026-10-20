package ai.tambo.tmodeler.scheduler


import kotlinx.coroutines.CoroutineDispatcher

expect object MainDispatcher {
    val immediate: CoroutineDispatcher
}
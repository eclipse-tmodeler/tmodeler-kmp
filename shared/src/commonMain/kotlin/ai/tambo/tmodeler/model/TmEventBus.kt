package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.scheduler.TGate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


object TmEventBus {

    private val _messages = MutableSharedFlow<TBaseModel>(
        replay = 0,
        extraBufferCapacity = 1024
    )

    val messages = _messages.asSharedFlow()

    fun publish(instances: List<TBaseModel>) {
        instances.forEach { publish(it) }
    }

    fun publish(instance: TBaseModel) {
        TGate.scope.launch {
            _messages.emit(instance)
        }
    }

}
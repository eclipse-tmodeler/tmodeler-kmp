package ai.tambo.tmodeler.extension.synchronization.core

import ai.tambo.tmodeler.feature.TFeature
import ai.tambo.tmodeler.scheduler.TFlow

open class RequestConfigBase(
    open var pull : Boolean = false,
    open var cmd : String? = null,
    open var loadTimer : String? = null,
    open var flow: TFlow<*>? = null,

    open var feature: TFeature<*>? = null,
) {
    open fun isFlow() = flow!=null
    open fun isFeature() = feature!=null
}
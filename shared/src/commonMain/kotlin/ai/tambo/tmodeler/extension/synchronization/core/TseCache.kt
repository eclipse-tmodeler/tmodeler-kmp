package ai.tambo.tmodeler.extension.synchronization.core

import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.scheduler.TmsScope

open class TseCache {
    context(TmsScope)
    open fun getLoadTimer(model: TmsGb<*>): String? { return null }
    context(TmsScope)
    open fun setLoadTimer(model: TmsGb<*>, value: String) {}
}
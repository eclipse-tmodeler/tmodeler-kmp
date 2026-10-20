package ai.tambo.tmodeler.extension.synchronization.core

import ai.tambo.tmodeler.ms.TmsGb
import kotlin.js.JsName

open class TServerConfigBase {
    open fun getModelName():String? { return null }
    @JsName("jsAutoSync")
    open fun autoSync(): Boolean? { return false }
    open fun isConfig(): Boolean? { return false }


    open fun with(link: String?): TServerConfigBase { return this }
    open fun with(
        model: String?,
        modelHost: String?,
        mediaHost: String?,
        syncListener: Any?=null
    ): TServerConfigBase { return this }
    open fun with(tms: TmsGb<*>): TServerConfigBase { return this }

    open var autoSync: Boolean = false
    open var supportsSpatial: Boolean = false
}
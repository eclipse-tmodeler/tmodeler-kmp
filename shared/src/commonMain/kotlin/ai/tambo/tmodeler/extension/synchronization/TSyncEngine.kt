package ai.tambo.tmodeler.extension.synchronization

import ai.tambo.tmodeler.extension.synchronization.core.TFeatureDeepBase
import ai.tambo.tmodeler.extension.synchronization.core.TncDeepBase
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase
import ai.tambo.tmodeler.extension.synchronization.core.TseCache
import ai.tambo.tmodeler.ms.TmsGb

/**
 * For T-Synchronization-Engine
 * TSY Engine
 * */
open class TSyncEngine {

    open fun observe(tms: TmsGb<*>) {
        println("🔄 Sync observation started for ${tms.name()}")
    }

    open fun listen(tms: TmsGb<*>) {
        println("👂 Listening for server updates on ${tms.name()}")
    }

    open fun newServerConfig(): TServerConfigBase = TServerConfigBase()

    open var featureDeep: TFeatureDeepBase = TFeatureDeepBase()

    /** Moteur profond des notifications instantanées (Tnc) ; inactif par défaut, fourni par la synchronisation. */
    open var tncDeep: TncDeepBase = TncDeepBase()
    open var cache: TseCache = TseCache()
    open var concurrencyReadWrite = false
}


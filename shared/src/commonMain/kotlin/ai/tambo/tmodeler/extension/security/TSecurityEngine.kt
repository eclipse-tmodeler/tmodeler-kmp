package ai.tambo.tmodeler.extension.security

import ai.tambo.tmodeler.model.TBaseModel

/**
 * For T-Security-Engine
 * TSE Engine
 * */
open class TSecurityEngine {
    open fun protect(model: TBaseModel) {
        println("🔐 Protecting ${model::class.simpleName}")
    }

    open fun authorize(model: TBaseModel) {
        println("✅ Checking access for ${model::class.simpleName}")
    }
}


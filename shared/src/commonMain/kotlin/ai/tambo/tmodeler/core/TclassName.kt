package ai.tambo.tmodeler.core

import kotlin.reflect.KClass

// KClass.qualifiedName n'est pas supporte a la compilation sur Kotlin/Wasm.
expect fun classQualifiedName(kClass: KClass<*>): String?

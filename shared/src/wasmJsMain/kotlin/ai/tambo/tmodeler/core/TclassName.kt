package ai.tambo.tmodeler.core

import kotlin.reflect.KClass

// Pas de qualifiedName sur Kotlin/Wasm : simpleName en repli (pas de package resolu).
actual fun classQualifiedName(kClass: KClass<*>): String? = kClass.simpleName

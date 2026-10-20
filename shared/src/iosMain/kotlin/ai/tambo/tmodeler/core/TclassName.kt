package ai.tambo.tmodeler.core

import kotlin.reflect.KClass

actual fun classQualifiedName(kClass: KClass<*>): String? = kClass.qualifiedName

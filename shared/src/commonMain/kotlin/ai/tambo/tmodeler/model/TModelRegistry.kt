package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tattr
import ai.tambo.tmodeler.core.Tclass
import kotlin.reflect.KClass

object TModelRegistry {
    private class RegistryEntry<T : TBaseModel>(
        val factory: () -> T,
        val fieldsOf: (T) -> List<Tattr>,
        val packageName: String?
    )

    private val entries = mutableMapOf<KClass<*>, RegistryEntry<*>>()

    fun <T : TBaseModel> register(
        cls: KClass<T>,
        factory: () -> T,
        fieldsOf: (T) -> List<Tattr>,
        packageName: String? = null
    ) {
        entries[cls] = RegistryEntry(factory, fieldsOf, packageName)
    }

    fun <T : TBaseModel> instanceFor(cls: KClass<T>): T {
        val instance = entryFor(cls).factory()
        check(instance::class == cls) { "TModelRegistry: factory returned a different class for ${cls.simpleName}" }
        return instance
    }

    fun <T : TBaseModel> clazzFor(cls: KClass<T>, inst: T, superClazz: Tclass?): Tclass {
        require(inst::class == cls) { "TModelRegistry: instance does not match ${cls.simpleName}" }
        val entry = entryFor(cls)
        return Tclass(cls, superClazz).fields(entry.fieldsOf(inst)).also { clazz ->
            entry.packageName?.let { clazz.packageName(it) }
        }
    }

    private fun <T : TBaseModel> entryFor(cls: KClass<T>): RegistryEntry<T> {
        val entry = entries[cls]
            ?: error("TModelRegistry: ${cls.simpleName} is not registered. Call the generated registerAllTModels() before using models.")
        // register() associates the same T with both the KClass key and its entry.
        @Suppress("UNCHECKED_CAST")
        return entry as RegistryEntry<T>
    }
}

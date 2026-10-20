package ai.tambo.tmodeler.core

import ai.tambo.tmodeler.model.TBaseModel

open class Tob : TobGb() {

    override fun instance() : Tob{
        return this::class.constructors.first().call()
    }

    //-----------------------------------------------------------------------

    override fun clazz(): Tclass {
        return Treflector.clazz(this, this)
    }
    override fun clazz(inst : Current_class): Tclass {
        return Treflector.clazz(this, inst)
    }

    //-----------------------------------------------------------------------

    override fun clone(): Tob {
        return clone(instance())
    }

    //-----------------------------------------------------------------------

    companion object {

        inline fun <reified T : Final_class> parse(json: String): List<T> {
            val prototype = T::class.constructors.first().call()
            return parse(prototype, json)
        }

        inline fun <reified T : Any> newInstance(): T? {
            return try {
                T::class.java.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                null
            }
        }

        fun <E : TBaseModel> newInstance(model: Class<E>): E? {
            return try {
                model.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                null
            }
        }
    }

    //-----------------------------------------------------------------------
}



/*
open fun clazz(inst : Current_class): Tclass {
    if (_clazz != null) return _clazz!!

    val kClass = this::class
    val superClass = kClass.supertypes.firstOrNull()?.classifier as? KClass<*>

    // Si une super classe existe, on crée son Tclass récursivement
    val superTclass = if (superClass != null && Current_class::class.java.isAssignableFrom(superClass.java)) {
        // Crée une instance fictive temporaire juste pour obtenir clazz()
        val instance = superClass.constructors.first().call() as Current_class
        instance.clazz(inst)
    } else {
        null
    }

    // On récupère seulement les propriétés **déclarées dans la classe courante**, pas héritées
    val declaredProps = kClass.memberProperties.filter { it.javaField?.declaringClass == kClass.java }

    // On mappe les propriétés déclarées vers les Tattr
    val fields = declaredProps.map { prop ->
        prop.isAccessible = true
        Tattr(
            name = prop.name,
            onGet = {
                inst::class.memberProperties
                    .firstOrNull { it.name == prop.name }
                    ?.apply { isAccessible = true }
                    ?.getter
                    ?.call(inst)
            },
            onSet = { newValue ->
                val prop = inst::class.memberProperties
                    .firstOrNull { it.name == prop.name }

                if (prop is KMutableProperty1 && prop.setter.parameters.size == 2) {
                    prop.isAccessible = true
                    prop.setter.call(inst, newValue)
                }
            }
        )
    }

    // On construit le Tclass courant, lié à celui du parent
    _clazz = Tclass(kClass, superTclass).fields(fields)
    return _clazz!!
}
*/

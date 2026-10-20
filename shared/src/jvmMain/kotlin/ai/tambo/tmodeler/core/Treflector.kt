package ai.tambo.tmodeler.core

import ai.tambo.tmodeler.model.TMignore
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

typealias Current_class = TobGb
typealias Final_class = TobGb

class Treflector {

    companion object {

        fun getFieldRecursive(clazz: Class<*>, fieldName: String): Field? {
            var current: Class<*>? = clazz
            while (current != null) {
                try {
                    return current.getDeclaredField(fieldName).apply { isAccessible = true }
                } catch (e: NoSuchFieldException) {}
                try {
                    return current.getDeclaredField("${fieldName}\$delegate").apply { isAccessible = true }
                } catch (e: NoSuchFieldException) {
                    current = current.superclass
                }
            }
            return null
        }

        fun getTattr(
            inst: Any,
            propName: String
        ): Tattr? {
            val kProp = inst::class.memberProperties
                .firstOrNull { it.name == propName }

            if (kProp is KMutableProperty1<*, *> && kProp.setter.parameters.size == 2) {
                kProp.isAccessible = true

                try {
                    val field = getFieldRecursive(inst::class.java, kProp.name)

                    if (field != null && Tattr::class.java.isAssignableFrom(field.type)) {
                        val value = field.get(inst)
                        if (value is Tattr) {
                            return value
                        }
                    }
                } catch (_: Exception) {}
            }

            return null
        }

        inline fun <reified R> interceptTattrField(
            inst: Any,
            propName: String,
            onTattr: (Tattr) -> R,
            onFallback: () -> R
        ): R {
            val kProp = inst::class.memberProperties
                .firstOrNull { it.name == propName }

            if (kProp is KMutableProperty1<*, *> && kProp.setter.parameters.size == 2) {
                kProp.isAccessible = true

                try {
                    val field = getFieldRecursive(inst::class.java, kProp.name)
                    if (field != null && Tattr::class.java.isAssignableFrom(field.type)) {
                        val value = field.get(inst)
                        if (value is Tattr) {
                            return onTattr(value)
                        }
                    }
                } catch (_: Exception) {
                }

                return onFallback()
            }

            return onFallback()
        }

        fun clazz(real: Current_class, inst: Current_class): Tclass {
            if (real.getClazz() != null) return real.getClazz()!!

            val kClass = real::class
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
            val fields = declaredProps
                .filter { prop ->
                    prop.findAnnotation<TMignore>() == null
                }
                .mapNotNull { prop ->
                prop.isAccessible = true

                var attr = getTattr(inst, prop.name)
                if(attr!=null){
                    attr.apply { name = prop.name }
                }else{
                    attr = Tattr(
                        name = prop.name,
                        onGet = {
                            inst::class.memberProperties
                                .firstOrNull { it.name == prop.name }
                                ?.apply { isAccessible = true }
                                ?.getter
                                ?.call(inst)
                        },
                        onSet = { newValue ->
                            val kProp = inst::class.memberProperties
                                .firstOrNull { it.name == prop.name }
                            if (kProp is KMutableProperty1<*, *>) {
                                kProp.isAccessible = true
                                kProp.setter.call(inst, newValue)
                            }
                        }
                    )
                }

                attr
            }

            real.setClazz(Tclass(kClass, superTclass).fields(fields))
            return real.getClazz()!!
        }
    }
}

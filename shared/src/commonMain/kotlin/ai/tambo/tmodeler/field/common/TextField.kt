package ai.tambo.tmodeler.field.common

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype

open class TextField : TField<String, TextField>(TFtype.TEXT) {

    /*override fun getValue(): Any? = super.getValue()?.let {
        //(it as? String)?.removeEscapeChar
    }

    override fun setValue(v: Any?) = super.setValue(
        (v as? String)?.withEscapeChar
    )

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return super.getValue(thisRef, property)//.removeEscapeChar
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) {
        val nv = newValue.withEscapeChar
        super.setValue(thisRef, property, nv)
    }*/
}




// Extension pour échapper les apostrophes pour SQL
val String.withEscapeChar: String
    get() = this.replace("'", "''")

// Extension pour restaurer les apostrophes depuis SQL
val String.removeEscapeChar: String
    get() = this.replace("''", "'")
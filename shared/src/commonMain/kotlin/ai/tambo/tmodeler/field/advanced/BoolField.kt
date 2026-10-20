package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.common.IntField

open class BoolField : TField<Boolean, BoolField>(TFtype.INTEGER) {

    private val depField = IntField()
    private var dep by depField

    override fun getValue(): Any? = depField.get()

    @Suppress("EQUALITY_NOT_APPLICABLE_WARNING")
    override fun setValue(v: Any?) {
        if(v is Long) {
            dep = v
            _value = v==1
        }
        else if(v is Boolean) {
            dep = if(v) 1 else 0
            _value = v
        }
    }
}

package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.field.operation.Top.Equal
import ai.tambo.tmodeler.field.operation.Top.In
import ai.tambo.tmodeler.field.operation.Top.NotEqual
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.operation.listToString

open class EnumField<E : Enum<E>>(
    private val values: Array<E>,
    private val codeSelector: (E) -> String = { it.name }
) : TField<E, EnumField<E>>(TFtype.TEXT) {

    var dep = TextField()
    private var dep_ by dep

    override fun getValue(): Any? = dep.get()

    override fun setValue(v: Any?) {
        when (v) {
            is String -> {
                dep_ = v
                _value = fromCode(v)
            }
            is Enum<*> -> {
                val enumValue = v as E
                dep_ = toCode(enumValue)
                _value = enumValue
            }
        }
    }

    private var codeMap: Map<String, E> =
        values.associateBy { codeSelector(it) }

    private fun fromCode(code: String): E? {
        return codeMap[code]
    }

    private fun toCode(e: E): String {
        return codeSelector(e)
    }



    // ops
    override fun ieq(v: E?): Tx {
        if(v==null){
            return super.ieq(v)
        }
        prepare()
        return Tx(this, Equal, toCode(v))
    }
    override fun ine(v: E?): Tx {
        if(v==null){
            return super.ine(v)
        }
        prepare()
        return Tx(this, NotEqual, toCode(v))
    }
    override fun iinto(values: List<E>): Tx {
        prepare()
        return Tx(this, In, listToString(values.map { toCode(it) }))
    }
}

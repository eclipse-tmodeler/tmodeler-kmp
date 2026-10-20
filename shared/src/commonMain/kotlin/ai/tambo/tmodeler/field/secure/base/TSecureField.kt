package ai.tambo.tmodeler.field.secure.base

import ai.tambo.tmodeler.extension.security.core.TNode
import ai.tambo.tmodeler.field.advanced.JsonField
import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import kotlinx.serialization.descriptors.StructureKind

class TSecureField : TField<String, TSecureField>(TFtype.TEXT) {

    private var dep by JsonField()

    override fun getValue(): Any? {
        try {
            return dep
        }catch (_: Exception){}

        return null
    }

    override fun setValue(v: Any?) {
        if(v is String) {
        }
        else if(v is StructureKind.OBJECT) {
        }
    }



    // s'il vient de jsonField : exact time field has been updated : to get specific token
    var content: String? = null
    var updated: String? = null
    var security_level: Int = 0


    fun from(node: TNode<*>?): TSecureField {
        return this
    }

    fun to(node: TNode<*>?): TSecureField {
        return this
    }


    fun read(): String? {
        return ""
    }

    fun secure(value: String?) {
    }

    fun transfert(node: TNode<*>?): String? {
        return ""
    }

    fun granted(node: TNode<*>?): Boolean {
        return true
    }

    fun transferable(v: Boolean) {
    }

    fun securityLevel(v: Int): TSecureField {
        return this
    } // Nb: lle taff de base se fait bien sur du String
}
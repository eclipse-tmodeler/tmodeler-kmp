package ai.tambo.tmodeler.field.secure.common

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.secure.base.TSecureField
import kotlinx.serialization.descriptors.StructureKind

class SecureTextField : TField<String, SecureTextField>(TFtype.TEXT) {

    private var dep by TSecureField()

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
}

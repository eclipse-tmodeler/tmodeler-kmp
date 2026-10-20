package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tattr
import ai.tambo.tmodeler.core.Tserial
import ai.tambo.tmodeler.field.base.BaseField
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

open class TmSerial : Tserial() {
    override fun serialize(key: String, attr: Tattr?): JsonObject {
        return if (attr is BaseField) {
            super.serialize(key, attr)
        } else {
            buildJsonObject {}
        }
    }

    override fun deserialize(json: JsonObject, key: String, attr: Tattr?) {
        if(attr is BaseField)
            super.deserialize(json, key, attr)
    }
}
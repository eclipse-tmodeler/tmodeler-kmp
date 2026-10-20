package ai.tambo.tmodeler.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

open class Tserial {

    open fun serialize(key: String, attr: Tattr?): JsonObject {
        if (attr == null) return buildJsonObject {}
        var value = attr.getValue() ?: return buildJsonObject {}

        if (attr.isJson && value is String) {
            value = try {
                Json.parseToJsonElement(value)
            } catch (e: Exception) {
                JsonNull
            }
        }

        val jsonValue = when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is JsonElement -> value
            else -> JsonPrimitive(value.toString())
        }

        return buildJsonObject {
            put(key, jsonValue)
        }
    }

    open fun deserialize(json: JsonObject, key: String, attr: Tattr?) {
        try {
            if (attr == null) return
            val value = json[key] ?: return

            when (value) {
                is JsonNull -> {
                    //attr.setValue(null)
                }
                is JsonPrimitive -> {
                    if (value.isString) attr.setValue(value.content)
                    else if (value.booleanOrNull != null) attr.setValue(value.boolean)
                    else if (value.longOrNull != null) attr.setValue(value.long)
                    else if (value.doubleOrNull != null) attr.setValue(value.double)
                    else attr.setValue(value.content)
                }
                else -> attr.setValue(value.toString())
            }
        } catch (_: Exception) {}
    }
}

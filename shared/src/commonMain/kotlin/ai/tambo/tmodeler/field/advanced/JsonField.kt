package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.common.TextField
import kotlinx.serialization.json.*

open class JsonField : TField<JsonElement, JsonField>(TFtype.TEXT) {
    private var dep by TextField()

    var _jsonElement: JsonElement = JsonNull

    init {
        isJson = true
        dep = "{}"
        loadFromDep()
    }

    override fun getValue(): Any? {
        try {
            return dep
        }catch (_: Exception){}

        return null
    }

    override fun setValue(v: Any?) {
        when (v) {
            is String -> {
                dep = v
                loadFromDep()
            }
            is JsonElement -> {
                _jsonElement = v
                _value = v
                dep = v.toString()/// Json.encodeToString(v)
            }
            else -> {
                dep = ""
                _jsonElement = JsonNull
                _value = JsonNull
            }
        }
    }

    private fun loadFromDep() {
        _jsonElement = try {
            Json.parseToJsonElement(dep ?: "{}")
        } catch (_: Exception) {
            JsonNull
        }
        _value = _jsonElement
    }

    fun containsValue(value: JsonElement): Boolean {
        return (_jsonElement as? JsonArray)?.contains(value) == true
    }

    // -- Operations Array (JsonArray) --

    fun add(value: JsonElement): JsonField {
        val arr = (_jsonElement as? JsonArray)?.toMutableList() ?: mutableListOf()
        arr.add(value)
        setValue(JsonArray(arr))
        return this
    }

    fun put(key: String, value: JsonElement): JsonField {
        val obj = (_jsonElement as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        obj[key] = value
        setValue(JsonObject(obj))
        return this
    }









    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(value.mapKeys { it.key.toString() }
            .mapValues { toJsonElement(it.value) })
        is List<*> -> JsonArray(value.map { toJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }

    // Put key-value into internal JsonObject
    fun put(key: String, value: Any?): JsonField {
        val obj = (_jsonElement as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        obj[key] = toJsonElement(value)
        setValue(JsonObject(obj))
        return this
    }

    // Get value by key (returns Any?)
    fun get(key: String): Any? {
        val obj = _jsonElement as? JsonObject ?: return null
        return fromJsonElement(obj[key])
    }

    fun containsKey(key: String): Boolean {
        val obj = _jsonElement as? JsonObject ?: return false
        return obj.containsKey(key)
    }

    fun remove(key: String): JsonField {
        val obj = (_jsonElement as? JsonObject)?.toMutableMap() ?: return this
        obj.remove(key)
        setValue(JsonObject(obj))
        return this
    }

    // -- Operations Array --

    inline fun <reified T : Any> asList(): List<T> {
        val arr = _jsonElement as? JsonArray ?: return emptyList()
        return arr.mapNotNull { element ->
            try {
                when (T::class) {
                    String::class -> element.jsonPrimitive.contentOrNull as T
                    Int::class -> element.jsonPrimitive.intOrNull as T
                    Long::class -> element.jsonPrimitive.longOrNull as T
                    Double::class -> element.jsonPrimitive.doubleOrNull as T
                    Boolean::class -> element.jsonPrimitive.booleanOrNull as T
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun set(v: List<*>) {
        val arr = v.map { toJsonElement(it) }
        setValue(JsonArray(arr))
    }

    fun add(value: Any?): JsonField {
        val arr = (_jsonElement as? JsonArray)?.toMutableList() ?: mutableListOf()
        arr.add(toJsonElement(value))
        setValue(JsonArray(arr))
        return this
    }

    fun addAll(vararg values: Any?): JsonField {
        val arr = (_jsonElement as? JsonArray)?.toMutableList() ?: mutableListOf()
        arr.addAll(values.map { toJsonElement(it) })
        setValue(JsonArray(arr))
        return this
    }

    fun addAll(values: List<Any?>): JsonField {
        val arr = (_jsonElement as? JsonArray)?.toMutableList() ?: mutableListOf()
        arr.addAll(values.map { toJsonElement(it) })
        setValue(JsonArray(arr))
        return this
    }

    inline fun <reified T> get(index: Int): T? {
        val arr = _jsonElement as? JsonArray ?: return null
        val element = arr.getOrNull(index) ?: return null
        return try {

            when (T::class) {
                String::class -> element.jsonPrimitive.contentOrNull as T
                Int::class -> element.jsonPrimitive.intOrNull as T
                Long::class -> element.jsonPrimitive.longOrNull as T
                Double::class -> element.jsonPrimitive.doubleOrNull as T
                Boolean::class -> element.jsonPrimitive.booleanOrNull as T
                else -> null
            }
        } catch (e: Exception) { null }
    }

    fun containsValue(value: Any?): Boolean {
        val arr = _jsonElement as? JsonArray ?: return false
        val je = toJsonElement(value)
        return arr.contains(je)
    }

    fun remove(index: Int): JsonField {
        val arr = (_jsonElement as? JsonArray)?.toMutableList() ?: return this
        if (index in arr.indices) {
            arr.removeAt(index)
            setValue(JsonArray(arr))
        }
        return this
    }

    fun resetAsObject(): JsonField {
        dep = "{}"
        try {
            loadFromDep()
        }catch (_: Exception){}
        return this
    }

    fun resetAsArray(): JsonField {
        dep = "[]"
        try {
            loadFromDep()
        }catch (_: Exception){}
        return this
    }

    fun isObject(): Boolean = _jsonElement is JsonObject
    fun isArray(): Boolean = _jsonElement is JsonArray
    @Suppress("UNCHECKED_CAST")
    fun isEmpty(): Boolean {
        return when (_jsonElement) {
            null, JsonNull -> true
            is JsonObject -> (_jsonElement as JsonObject).isEmpty()
            is JsonArray -> (_jsonElement as JsonArray).isEmpty()
            else -> false
        }
    }
    fun isNotEmpty(): Boolean = !isEmpty()

    private fun fromJsonElement(element: JsonElement?): Any? {
        return when (element) {
            null, JsonNull -> null
            is JsonPrimitive -> {
                if (element.isString) element.content
                else if (element.booleanOrNull != null) element.boolean
                else element.doubleOrNull ?: element.content
            }
            is JsonObject -> element
            is JsonArray -> element
        }
    }
}
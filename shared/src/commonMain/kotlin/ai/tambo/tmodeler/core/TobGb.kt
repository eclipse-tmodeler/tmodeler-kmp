package ai.tambo.tmodeler.core


import ai.tambo.tmodeler.ms.utils.Ttuple
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.*


private val JsonElement.jsonArrayOrNull: JsonArray?
    get() = try { this.jsonArray } catch (_: Exception) { null }

open class TobGb : Treflexion {
    protected var _serializer: Tserial? = null
    protected var _clazz: Tclass? = null

    //-----------------------------------------------------------------------

    override fun serial_(): Tserial {
        if (_serializer == null) _serializer = Tserial()
        return _serializer!!
    }

    override fun serial(): Tserial {
        if (_serializer == null) _serializer = Tserial()
        return _serializer!!
    }

    override fun clazz(): Tclass {
        if (_clazz == null) _clazz = Tclass(TobGb::class, null)
        return _clazz!!
    }

    //-----------------------------------------------------------------------

    open fun clazz(inst : TobGb): Tclass {
        return clazz()
    }

    //-----------------------------------------------------------------------


    fun getClazz(): Tclass? {
        return _clazz
    }
    fun setClazz(c : Tclass) {
        _clazz = c
    }

    //-----------------------------------------------------------------------

    open fun instance() : TobGb{
        return TobGb()
    }

    open fun clone() : TobGb{
        return clone(TobGb())
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : TobGb> clone(prototype: T): T {
        val instance = prototype.instance() as T
        instance.parse(data())
        return instance
    }


    // Sérialise en JSON String l'objet courant selon les fields
    fun data(): String {
        return try {
            val jsonObj = toJson(serial(), clazz().getFields())
            Json.encodeToString(JsonObject.serializer(), jsonObj)
        } catch (e: Exception) {
            "{}"
        }
    }
    fun data(
        includes: List<String>?,
        excludes: List<String>?
    ): String {
        return try {
            val jsonObj = toJson(serial(), clazz().getFields())

            // RAPIDE : 99% des cas → rien à filtrer
            if (includes == null && excludes == null) {
                return Json.encodeToString(JsonObject.serializer(), jsonObj)
            }

            // 1% DES CAS → filtrage
            val map = jsonObj.toMutableMap()

            // Includes → garder uniquement les clés
            includes?.let { inc ->
                map.keys.retainAll(inc.toSet())
            }

            // Excludes → enlever les clés
            excludes?.forEach { key ->
                map.remove(key)
            }

            Json.encodeToString(
                JsonObject.serializer(),
                JsonObject(map)
            )

        } catch (e: Exception) {
            "{}"
        }
    }

    // Parse à partir d'une String JSON
    fun parse(json: String) {
        try {
            val jsonObj = Json.parseToJsonElement(json).jsonObject
            fromJson(serial(), jsonObj, clazz().getFields())
        } catch (e: Exception) {}
    }

    fun parse(element: JsonElement) {
        val jsonObj = element.jsonObject
        fromJson(serial(), jsonObj, clazz().getFields())
    }

    //-----------------------------------------------------------------------

    context(TmsScope)
    open fun resolveDeepLink(){}
    open fun disableDeepLink(v: Boolean=true) = deepLinkResolve(!v)
    open fun deepLinkResolve(enabled: Boolean = true) : Any {
        isDeepLinkResolve = enabled
        return 0
    }
    open fun deepLinkMaxDepth(v:Int){
        deepLinkMaxDepth = v
    }
    var isDeepLinkResolve = true
    var deepLinkMaxDepth = -1

    //-----------------------------------------------------------------------

    fun toJson(serializer: Tserial, attrs: List<Tattr>): JsonObject {
        return buildJsonObject {
            try {
                for (attr in attrs) {
                    try {
                        val fieldJson = serializer.serialize(attr.name, attr)

                        for ((key, value) in fieldJson) {
                            put(key, value)
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    fun fromJson(serializer: Tserial, jsonObj: JsonObject, attrs: List<Tattr>) {
        try {
            for (attr in attrs) {
                try {
                    if (jsonObj.containsKey(attr.name)) {
                        serializer.deserialize(jsonObj, attr.name, attr)
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    //-----------------------------------------------------------------------

    companion object {
        fun <T : TobGb> data(
            list: List<T>,
            includes: List<String>? = null,
            excludes: List<String>? = null
        ): String {
            return list.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ","
            ) { it.data(
                includes = includes,
                excludes = excludes
            ) }
        }

        inline fun <reified T : TobGb> data(vararg args: T): String {
            return data(args.toList())
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : TobGb> parse(prototype : T, json: String, isDeepLinkResolve: Boolean=true): List<T> {
            return try {
                val jsonArray = Json.parseToJsonElement(json).jsonArray
                jsonArray.map { element ->
                    val instance = prototype.instance() as T
                    instance.deepLinkResolve(isDeepLinkResolve)
                    //instance.parse(element.toString())
                    instance.parse(element)
                    instance
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun parseTuples(prototype: Ttuple, json: String, isDeepLinkResolve: Boolean=true): List<Ttuple> {
            return try {
                val jsonArray = Json.parseToJsonElement(json).jsonArray
                val types = prototype.values

                jsonArray.mapNotNull { row ->
                    val rowArray = row.jsonArrayOrNull
                    if (rowArray == null || rowArray.size != types.size) return@mapNotNull null

                    val parsedValues = rowArray.mapIndexed { i, element ->
                        val proto = types[i] as? TobGb ?: return@mapNotNull null
                        val instance = proto.instance() as TobGb
                        instance.deepLinkResolve(isDeepLinkResolve)
                        //instance.parse(element.toString())
                        instance.parse(element)
                        instance
                    }

                    Ttuple(parsedValues)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        //-----------------------------------------------------------------------

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun <T : TobGb> parseSequence(
            factory: () -> T,
            jsonString: String
        ): Sequence<T> = sequence {

            val reader = JsonReader(jsonString)

            while (reader.hasNext()) {

                val elementString = reader.nextObject()

                val element = json.parseToJsonElement(elementString)

                val obj = factory()
                obj.parse(element)

                yield(obj)
            }
        }

        fun <T : TobGb> parseList(
            factory: () -> T,
            jsonString: String
        ): List<T> {
            return parseSequence(factory, jsonString).toList()
        }

        //-----------------------------------------------------------------------

        fun <T : TobGb> parseSequenceForKey(
            factory: () -> T,
            jsonString: String,
            key: String
        ): Sequence<T> = sequence {

            val reader = JsonKeyArrayReader(jsonString, key)

            while (reader.hasNext()) {

                val elementString = reader.nextObject()

                val element = json.parseToJsonElement(elementString)

                val obj = factory()
                obj.deepLinkResolve(false)
                obj.parse(element)

                yield(obj)
            }
        }

        //-----------------------------------------------------------------------

    }

    //-----------------------------------------------------------------------



    //-----------------------------------------------------------------------

    override fun toString(): String = data()
}
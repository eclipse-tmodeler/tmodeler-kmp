package ai.tambo.tmodeler.db

class TtableField(
    private var name: String,
    private var type: String,
    private var defaultValue: Any? = null,
    private var constraints: String = "",
    private var maxlen: Int = 0,
    private var targetTable: String = "",
    private var targetField: String = "",
    private var onDelete: String = ""
) {
    private var geoType: String = ""
    private var geoSrid: Int = 0
    private var geoDim: Int = 0
    private var geoMeasure: Boolean = false
    private var geoSpatialIndex: Boolean = false

    fun toScript(): String {
        val builder = StringBuilder()
        builder.append("$name $type")
        if (maxlen > 0) builder.append("($maxlen)")
        if (constraints.isNotEmpty()) builder.append(" $constraints")
        defaultValue?.let { builder.append(" DEFAULT ${formatDefaultValue(it)}") }
        return builder.toString()
    }

    fun toGeoScript(tname: String): String {
        val dim = when (geoDim) {
            2 -> "XY"
            3 -> "XYZ"
            else -> ""
        } + if (geoMeasure) "M" else ""
        return "SELECT AddGeometryColumn('$tname', '$name', $geoSrid, '$geoType', '$dim');\n"
    }

    fun toGeoSpatialIndexScript(tname: String): String {
        return "SELECT CreateSpatialIndex('$tname', '$name');\n"
    }

    fun toForeignScript(): String {
        return if (targetTable.isNotEmpty() && targetField.isNotEmpty()) {
            buildString {
                append("   FOREIGN KEY ($name) REFERENCES $targetTable($targetField)")
                if (onDelete.isNotEmpty()) append(" ON DELETE $onDelete")
            }
        } else ""
    }

    /*
    fun toScript(): String = ""
    fun toGeoScript(tname: String): String = ""
    fun toGeoSpatialIndexScript(tname: String): String = ""
    fun toForeignScript(): String = ""
    */

    fun isGeo(): Boolean = geoType.isNotEmpty()
    fun isSpatialIndex(): Boolean = geoSpatialIndex
    fun isTextField(): Boolean = type == "TEXT"
    /*
    fun isGeo(): Boolean = false
    fun isSpatialIndex(): Boolean = false
    */

    fun name(v: String) = apply { name = v }
    fun type(v: String) = apply { type = v }
    fun defaultValue(v: Any?) = apply { defaultValue = v }
    fun constraints(v: String) = apply { constraints = v }
    fun maxlen(v: Int) = apply { maxlen = v }
    fun targetTable(v: String) = apply { targetTable = v }
    fun targetField(v: String) = apply { targetField = v }
    fun onDelete(v: String) = apply { onDelete = v }
    fun geoType(v: String) = apply { geoType = v }
    fun geoSrid(v: Int) = apply { geoSrid = v }
    fun geoDim(v: Int) = apply { geoDim = v }
    fun geoMeasure(v: Boolean) = apply { geoMeasure = v }
    fun geoSpatialIndex(v: Boolean) = apply { geoSpatialIndex = v }
    /*
    private fun formatDefaultValue(value: Any?): String = ""
    */
    private fun formatDefaultValue(value: Any): String = when (value) {
        is String -> "'${value}'"
        is Int, is Long, is Float, is Double -> value.toString()
        is Boolean -> if (value) "TRUE" else "FALSE"
        else -> ""
    }
}
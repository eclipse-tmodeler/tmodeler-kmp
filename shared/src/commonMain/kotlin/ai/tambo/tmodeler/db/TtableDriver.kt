package ai.tambo.tmodeler.db

class TtableDriver(
    private var name: String = "",
    private var fields: MutableList<TtableField> = mutableListOf(),
    private var primaryKeys: MutableList<String> = mutableListOf()
) {

    fun addField(field: TtableField) { fields.add(field) }
    fun getName(): String = name

    private fun join(elements: List<String>, delimiter: String): String = elements.joinToString(delimiter)

    fun toScript(): String {
        val script = StringBuilder()
        script.append("CREATE TABLE $name (\n")

        val fieldScripts = fields.filterNot { it.isGeo() }.map { "   ${it.toScript()}" }
        script.append(fieldScripts.joinToString(",\n"))

        if (primaryKeys.size > 1) {
            script.append(",\n   PRIMARY KEY (${primaryKeys.joinToString(", ")})")
        }

        val foreignKeys = fields.mapNotNull { it.toForeignScript().takeIf { it.isNotEmpty() } }
        foreignKeys.forEach { script.append(",\n$it") }
        script.append("\n);\n")

        fields.filter { it.isGeo() }.forEach { script.append(it.toGeoScript(name)) }
        fields.filter { it.isSpatialIndex() }.forEach { script.append(it.toGeoSpatialIndexScript(name)) }

        return script.toString()
    }
}
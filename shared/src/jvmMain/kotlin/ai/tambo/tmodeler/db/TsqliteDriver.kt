package ai.tambo.tmodeler.db

import ai.tambo.tmodeler.ms.filter.TFilterBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import java.sql.Connection
import java.sql.DriverManager

import kotlin.text.Regex
import kotlin.text.RegexOption

import kotlinx.serialization.json.*
import java.sql.PreparedStatement

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class TsqliteDriver actual constructor() : TdbDriver {
    private var connection: Connection? = null

    actual override fun open(dbName: String): Boolean {
        return try {
            val url = "jdbc:sqlite:$dbName.db"
            connection = DriverManager.getConnection(url)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun close() {
        try {
            connection?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual override fun executeInserts(tableName: String, jsonArrayData: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        connection?.autoCommit = false
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                val columns = obj.keys.toList()
                val placeholders = columns.joinToString(",") { "?" }
                val sql = "INSERT INTO $tableName (${columns.joinToString(",")}) VALUES ($placeholders)"
                connection?.prepareStatement(sql).use { ps ->
                    if (ps != null) {
                        for ((index, col) in columns.withIndex()) {
                            val value = obj[col]
                            setPreparedStatementValue(ps, index + 1, value)
                        }
                        ps.executeUpdate()
                    }
                }
            }
            connection?.commit()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            connection?.rollback()
            false
        } finally {
            connection?.autoCommit = true
        }
    }

    actual override fun executeUpdates(tableName: String, jsonArrayData: String, key: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        connection?.autoCommit = false
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                if (!obj.containsKey(key)) continue
                val keyValue = obj[key]

                val columns = obj.keys.filter { it != key }
                if (columns.isEmpty()) continue

                val setPart = columns.joinToString(",") { "$it = ?" }
                val sql = "UPDATE $tableName SET $setPart WHERE $key = ?"

                connection?.prepareStatement(sql).use { ps ->
                    if (ps != null) {
                        for ((index, col) in columns.withIndex()) {
                            setPreparedStatementValue(ps, index + 1, obj[col])
                        }
                        // clé pour WHERE
                        setPreparedStatementValue(ps, columns.size + 1, keyValue)
                        ps.executeUpdate()
                    }
                }
            }
            connection?.commit()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            connection?.rollback()
            false
        } finally {
            connection?.autoCommit = true
        }
    }

    private fun setPreparedStatementValue(ps: PreparedStatement, index: Int, value: JsonElement?) {
        when {
            value == null || value is JsonNull -> ps.setObject(index, null)
            value is JsonPrimitive && value.isString -> ps.setString(index, value.content)
            value is JsonPrimitive && value.longOrNull != null -> ps.setLong(index, value.long)
            value is JsonPrimitive && value.doubleOrNull != null -> ps.setDouble(index, value.double)
            value is JsonPrimitive && value.booleanOrNull != null -> ps.setBoolean(index, value.boolean)
            else -> ps.setString(index, value.toString())
        }
    }

    actual override fun createTable(tableScript: String): Boolean {
        return executeSQL(tableScript)
    }

    actual override fun dropTable(tableName: String): Boolean {
        return executeSQL("DROP TABLE IF EXISTS $tableName")
    }

    actual override fun tableSize(tableName: String): Int {
        val query = "SELECT COUNT(*) FROM $tableName;"
        return try {
            connection?.createStatement().use { stmt ->
                val rs = stmt?.executeQuery(query)
                if (rs?.next() == true) {
                    rs.getInt(1)
                } else {
                    0
                }
            } ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    actual override fun tableNotEmpty(tableName: String): Boolean {
        val query = "SELECT EXISTS(SELECT 1 FROM $tableName LIMIT 1);"
        return try {
            connection?.createStatement().use { stmt ->
                val rs = stmt?.executeQuery(query)
                rs?.next() == true && rs.getInt(1) == 1
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun tableExists(tableName: String): Boolean {
        val query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
        return try {
            connection?.prepareStatement(query)?.use { stmt ->
                stmt.setString(1, tableName)
                stmt.executeQuery().use { rs ->
                    rs.next()
                }
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun executeSQL(sql: String): Boolean {
        return try {
            connection?.createStatement()?.use { stmt ->
                stmt.execute(sql)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun lastInsertRowId(): Long {
        return try {
            connection?.createStatement()?.use { stmt ->
                stmt.executeQuery("SELECT last_insert_rowid();").use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            } ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    actual override fun executeQuery(query: String): String {
        return try {
            connection?.createStatement()?.use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    val meta = rs.metaData
                    val sb = StringBuilder()
                    while (rs.next()) {
                        for (i in 1..meta.columnCount) {
                            sb.append("${meta.getColumnLabel(i)}=${rs.getString(i)} ")
                        }
                        sb.append("\n")
                    }
                    sb.toString()
                }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    actual override fun executeQuery(query: String, builder: Any): String {
        val realBuilder = builder as TFilterBuilder

        val tableFields = realBuilder.tableFields
        val groupedRows = mutableListOf<JsonElement>()

        try {
            val statement = connection?.createStatement()
            val rs = statement?.executeQuery(query)
            val meta = rs?.metaData ?: return "[]"

            while (/*rs != null && */rs.next()) {
                val rowVariants = mutableListOf<MutableMap<String, JsonElement>>()
                rowVariants.add(mutableMapOf()) // At least one object

                for (i in 1..meta.columnCount) {
                    val colName = meta.getColumnLabel(i) ?: continue
                    val rawValue = rs.getString(i)
                    if (colName.isEmpty()) continue

                    var index = 0
                    var offset = 0
                    if (tableFields.size > 1) {
                        var totalFields = 0
                        for ((g, group) in tableFields.withIndex()) {
                            totalFields += group.size
                            if (i - 1 < totalFields) {
                                index = g
                                offset = totalFields - group.size
                                break
                            }
                        }
                    }

                    if (index >= rowVariants.size) {
                        repeat(index - rowVariants.size + 1) {
                            rowVariants.add(mutableMapOf())
                        }
                    }

                    val j = i - 1 - offset
                    val targetRow = rowVariants[index]
                    val field = tableFields[index].getOrNull(j)

                    if (rawValue == null) {
                        // targetRow[colName] = JsonNull // Optionally skip or handle
                    } else {
                        val strVal = rawValue
                        val jsonValue: JsonElement = when {
                            field?.isTextField() == true -> JsonPrimitive(strVal)
                            strVal == "true" -> JsonPrimitive(true)
                            strVal == "false" -> JsonPrimitive(false)
                            strVal == "null" -> JsonNull
                            strVal.toLongOrNull() != null -> JsonPrimitive(strVal.toLong())
                            strVal.toDoubleOrNull() != null -> JsonPrimitive(strVal.toDouble())
                            else -> JsonPrimitive(strVal)
                        }
                        targetRow[colName] = jsonValue
                    }
                }

                val result: JsonElement = if (rowVariants.size == 1) {
                    JsonObject(rowVariants[0])
                } else {
                    buildJsonArray {
                        rowVariants.forEach { map ->
                            add(JsonObject(map))
                        }
                    }
                }

                groupedRows.add(result)
            }

            rs.close()
            statement.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return "{}"
        }

        return JsonArray(groupedRows).toString()
    }

    actual override fun deleteDatabase(dbName: String): Boolean {
        return try {
            val file = java.io.File("$dbName.db")
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    actual override fun tableSchema(tableName: String): String {
        val result = StringBuilder()
        try {
            val query = "PRAGMA table_info($tableName)"
            connection?.createStatement()?.use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("name")
                        val type = rs.getString("type")
                        result.append("$name $type\n")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.toString()
    }

    actual override fun context(v: Any) {}
    actual override fun context(): Any? = null








    /*actual override fun executeSQL(sql: String): Boolean {
        return try {
            if (connection == null) return false

            val trimmed = sql.trim().lowercase()

            when {
                trimmed.startsWith("insert") -> executeInsert(sql)
                trimmed.startsWith("update") -> executeUpdate(sql)
                else -> baseExecuteSQL(sql)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }*/

    private fun baseExecuteSQL(sql: String): Boolean {
        return try {
            connection?.createStatement()?.use { stmt ->
                stmt.execute(sql)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun executeInsert(sql: String): Boolean {
        // Exemple : INSERT INTO table (col1, col2) VALUES ('a','b'), ('c','d')
        val regex = Regex(
            """insert\s+into\s+(\w+)\s*\(([^)]+)\)\s*values\s*(.+)""",
            setOf(RegexOption.IGNORE_CASE , RegexOption.DOT_MATCHES_ALL)
        )
        val match = regex.find(sql) ?: throw IllegalArgumentException("Invalid INSERT SQL: $sql")

        val table = match.groupValues[1]
        val columns = match.groupValues[2].split(",").map { it.trim() }
        val valuesPart = match.groupValues[3].trim()

        // Découper les groupes VALUES (...)
        val tuples = Regex("""\(([^)]+)\)""").findAll(valuesPart).map { it.groupValues[1] }.toList()

        val placeholders = "(" + columns.joinToString(",") { "?" } + ")"
        val newSql = "INSERT INTO $table (${columns.joinToString(",")}) VALUES " +
                tuples.joinToString(",") { placeholders }

        connection?.prepareStatement(newSql).use { ps ->
            if (ps == null) return false
            var paramIndex = 1
            for (tuple in tuples) {
                val values = splitSQLValues(tuple)
                for (v in values) {
                    ps.setObject(paramIndex++, parseSQLValue(v))
                }
            }
            ps.executeUpdate()
        }
        return true
    }

    private fun executeUpdate(sql: String): Boolean {
        val regex = Regex(
            """update\s+(\w+)\s+set\s+(.+?)(\s+where\s+.+)?$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val match = regex.find(sql) ?: throw IllegalArgumentException("Invalid UPDATE SQL: $sql")

        val table = match.groupValues[1]
        val setPart = match.groupValues[2].trim()
        val wherePart = match.groupValues[3].takeIf { it.isNotBlank() } ?: ""

        // 🔹 Découpage SET en assignments en respectant les quotes
        val assignments = mutableListOf<Pair<String, String>>()
        var current = StringBuilder()
        var inQuotes = false
        for (c in setPart) {
            when (c) {
                '\'' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(c)
                    } else {
                        val part = current.toString().trim()
                        if (part.isNotEmpty()) {
                            val idx = part.indexOf('=')
                            if (idx == -1) throw IllegalArgumentException("Invalid assignment: $part")
                            val col = part.substring(0, idx).trim()
                            val value = part.substring(idx + 1).trim()
                            assignments += col to value
                        }
                        current = StringBuilder()
                    }
                }
                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) {
            val part = current.toString().trim()
            val idx = part.indexOf('=')
            if (idx == -1) throw IllegalArgumentException("Invalid assignment: $part")
            val col = part.substring(0, idx).trim()
            val value = part.substring(idx + 1).trim()
            assignments += col to value
        }

        // 🔹 Reconstruction du SQL avec placeholders
        val newSql = buildString {
            append("UPDATE $table SET ")
            append(assignments.joinToString(", ") { "${it.first} = ?" })
            append(" $wherePart")
        }

        connection?.prepareStatement(newSql).use { ps ->
            if (ps == null) return false
            var paramIndex = 1
            for ((_, value) in assignments) {
                ps.setObject(paramIndex++, parseSQLValue(value))
            }
            ps.executeUpdate()
        }
        return true
    }

    /**
     * Découpe les valeurs SQL en respectant les quotes
     */
    private fun splitSQLValues(tuple: String): List<String> {
        val values = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < tuple.length) {
            val c = tuple[i]
            if (c == '\'' && (i == 0 || tuple[i - 1] != '\\')) {
                inQuotes = !inQuotes
                sb.append(c)
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) values.add(sb.toString().trim())
        return values
    }

    /**
     * Convertit une valeur SQL brute → valeur Java/Kotlin
     */
    private fun parseSQLValue(value: String): Any? {
        if (value.equals("null", ignoreCase = true)) return null
        return if (value.startsWith("'") && value.endsWith("'")) {
            value.substring(1, value.length - 1).replace("''", "'")
        } else {
            value.toDoubleOrNull() ?: value // int/float sinon string brute
        }
    }

}

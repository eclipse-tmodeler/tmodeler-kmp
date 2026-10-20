package ai.tambo.tmodeler.db

import ai.tambo.tmodeler.ms.filter.TFilterBuilder
import kotlinx.serialization.json.*
import org.spatialite.database.SQLiteDatabase

actual class TspatialiteDriver : TdbDriver {

    private var context: Tcontext? = null
    private var db: SQLiteDatabase? = null
    private var dbName: String = ""

    //+
    init {
        try {
            System.loadLibrary("android_spatialite")
        } catch (e: Exception) {}
    }

    actual override fun createTable(tableScript: String): Boolean {
        db?.let { database ->
            return try {
                val statements = tableScript.split(";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                for (stmt in statements) {
                    when {
                        stmt.startsWith("CREATE TABLE", ignoreCase = true) ||
                                stmt.startsWith("DROP TABLE", ignoreCase = true) ||
                                stmt.startsWith("ALTER TABLE", ignoreCase = true) -> {
                            executeSQL(stmt)
                        }

                        stmt.startsWith("SELECT AddGeometryColumn", ignoreCase = true) ||
                                stmt.startsWith("SELECT CreateSpatialIndex", ignoreCase = true) -> {
                            database.rawQuery(stmt, null).use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val result = cursor.getInt(0)
                                    if (result == 1) {} else {}
                                } else {}
                            }
                        }

                        else -> {
                            executeSQL(stmt)
                        }
                    }
                }

                true
            } catch (e: Exception) {
                false
            }
        } ?: run {
            return false
        }
    }

    actual override fun dropTable(tableName: String): Boolean {
        db?.let { database ->
            return try {
                val geomCols = mutableListOf<String>()
                val cursor = database.rawQuery(
                    "SELECT f_geometry_column FROM geometry_columns WHERE f_table_name = ?;",
                    arrayOf(tableName.lowercase())
                )
                while (cursor.moveToNext()) {
                    geomCols.add(cursor.getString(0))
                }
                cursor.close()

                geomCols.forEach { col ->
                    try { database.execSQL("SELECT DisableSpatialIndex('$tableName', '$col');") } catch (_: Exception) {}
                    try { database.execSQL("DROP TABLE IF EXISTS idx_${tableName}_$col;") } catch (_: Exception) {}
                    val triggers = listOf(
                        "rtree_${tableName}_${col}_insert",
                        "rtree_${tableName}_${col}_update1",
                        "rtree_${tableName}_${col}_update2",
                        "rtree_${tableName}_${col}_delete"
                    )
                    triggers.forEach { t ->
                        try { database.execSQL("DROP TRIGGER IF EXISTS $t;") } catch (_: Exception) {}
                    }
                    try { database.execSQL("SELECT DiscardGeometryColumn('$tableName', '$col');") } catch (_: Exception) {}
                }

                database.execSQL("DROP TABLE IF EXISTS $tableName;")
                true
            } catch (e: Exception) {
                false
            }
        } ?: run {
            return false
        }
    }

    actual override fun open(dbName: String): Boolean {
        this.dbName = dbName

        val ctx = context?:return false

        return try {
            val dbPath = ctx.getDatabasePath(dbName).absolutePath
            db = SQLiteDatabase.openOrCreateDatabase(dbPath, null)

            if (!ensureSpatialMetadataInitialized()) {
                return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ensureSpatialMetadataInitialized(): Boolean {
        db?.let { database ->
            return try {
                val cursor = database.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='spatial_ref_sys';",
                    null
                )
                val alreadyInitialized = cursor.count > 0
                cursor.close()

                if (!alreadyInitialized) {
                    database.rawQuery("SELECT InitSpatialMetadata(1)", null).use { cursor ->
                        cursor.moveToFirst() // InitSpatialMetadata retourne 1 ligne
                    }
                } else {}

                true
            } catch (e: Exception) {
                false
            }
        } ?: run {
            return false
        }
    }

    //classic sqlite
    actual override fun context(v: Any) {
        if(v is Tcontext) this.context = v
    }

    actual override fun context(): Any? = this.context

    actual override fun close() {
        try {
            db?.close()
            db = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual override fun executeInserts(tableName: String, jsonArrayData: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        db?.beginTransaction()
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                val columns = obj.keys.toList()

                // séparer les valeurs géométriques (raw) et les bindables
                val rawColumns = mutableListOf<String>()
                val rawValues = mutableListOf<String>()
                val bindColumns = mutableListOf<String>()
                val bindValues = mutableListOf<Any?>()

                for (col in columns) {
                    val value = obj[col]
                    val strVal = if (value is JsonPrimitive && value.isString) {
                        value.content
                    } else {
                        value.toString()
                    }

                    if (value is JsonPrimitive && value.isString && strVal.trim().startsWith("ST_")) {
                        // valeur géométrique → raw SQL
                        rawColumns.add(col)
                        rawValues.add(strVal)
                    } else {
                        // valeur normale → placeholder
                        bindColumns.add(col)
                        bindValues.add(
                            when {
                                value is JsonPrimitive && value.isString -> value.content
                                value is JsonPrimitive && value.longOrNull != null -> value.long
                                value is JsonPrimitive && value.doubleOrNull != null -> value.double
                                value is JsonPrimitive && value.booleanOrNull != null -> if (value.boolean) 1 else 0
                                value is JsonNull -> null
                                else -> strVal
                            }
                        )
                    }
                }

                val allColumns = rawColumns + bindColumns
                val sql = buildString {
                    append("INSERT INTO $tableName (")
                    append(allColumns.joinToString(", "))
                    append(") VALUES (")
                    append(
                        rawValues.joinToString(", ") +
                                (if (rawValues.isNotEmpty() && bindColumns.isNotEmpty()) ", " else "") +
                                List(bindColumns.size) { "?" }.joinToString(", ")
                    )
                    append(")")
                }

                val stmt = db?.compileStatement(sql)
                if (stmt != null) {
                    bindValues.forEachIndexed { i, v ->
                        val index = i + 1
                        when (v) {
                            null -> stmt.bindNull(index)
                            is Long -> stmt.bindLong(index, v)
                            is Int -> stmt.bindLong(index, v.toLong())
                            is Double -> stmt.bindDouble(index, v)
                            is Float -> stmt.bindDouble(index, v.toDouble())
                            is String -> stmt.bindString(index, v)
                            else -> stmt.bindString(index, v.toString())
                        }
                    }
                    stmt.executeInsert()
                }
            }
            db?.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.endTransaction()
        }
    }

    actual override fun executeUpdates(tableName: String, jsonArrayData: String, key: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        db?.beginTransaction()
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                val whereValue = obj[key] ?: continue

                val rawAssignments = mutableListOf<String>()
                val bindAssignments = mutableListOf<String>()
                val bindValues = mutableListOf<Any?>()

                for (col in obj.keys.filter { it != key }) {
                    val value = obj[col]
                    val strVal = if (value is JsonPrimitive && value.isString) {
                        value.content
                    } else {
                        value.toString()
                    }

                    if (value is JsonPrimitive && value.isString && strVal.trim().startsWith("ST_")) {
                        rawAssignments.add("$col = $strVal")
                    } else {
                        bindAssignments.add("$col = ?")
                        bindValues.add(
                            when {
                                value is JsonPrimitive && value.isString -> value.content
                                value is JsonPrimitive && value.longOrNull != null -> value.long
                                value is JsonPrimitive && value.doubleOrNull != null -> value.double
                                value is JsonPrimitive && value.booleanOrNull != null -> if (value.boolean) 1 else 0
                                value is JsonNull -> null
                                else -> strVal
                            }
                        )
                    }
                }

                val setClause = (rawAssignments + bindAssignments).joinToString(", ")
                val sql = "UPDATE $tableName SET $setClause WHERE $key = ?"

                val stmt = db?.compileStatement(sql)
                if (stmt != null) {
                    // bind des valeurs normales
                    bindValues.forEachIndexed { i, v ->
                        val index = i + 1
                        when (v) {
                            null -> stmt.bindNull(index)
                            is Long -> stmt.bindLong(index, v)
                            is Int -> stmt.bindLong(index, v.toLong())
                            is Double -> stmt.bindDouble(index, v)
                            is Float -> stmt.bindDouble(index, v.toDouble())
                            is String -> stmt.bindString(index, v)
                            else -> stmt.bindString(index, v.toString())
                        }
                    }
                    // bind du WHERE
                    val whereBindIndex = bindValues.size + 1
                    val wv = when {
                        whereValue is JsonPrimitive && whereValue.isString -> whereValue.content
                        whereValue is JsonPrimitive && whereValue.longOrNull != null -> whereValue.long
                        whereValue is JsonPrimitive && whereValue.doubleOrNull != null -> whereValue.double
                        whereValue is JsonPrimitive && whereValue.booleanOrNull != null -> if (whereValue.boolean) 1 else 0
                        whereValue is JsonNull -> null
                        else -> whereValue.toString()
                    }
                    when (wv) {
                        null -> stmt.bindNull(whereBindIndex)
                        is Long -> stmt.bindLong(whereBindIndex, wv)
                        is Int -> stmt.bindLong(whereBindIndex, wv.toLong())
                        is Double -> stmt.bindDouble(whereBindIndex, wv)
                        is Float -> stmt.bindDouble(whereBindIndex, wv.toDouble())
                        is String -> stmt.bindString(whereBindIndex, wv)
                        else -> stmt.bindString(whereBindIndex, wv.toString())
                    }

                    stmt.executeUpdateDelete()
                }
            }
            db?.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.endTransaction()
        }
    }

    /*
    actual override fun executeInserts(tableName: String, jsonArrayData: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        db?.beginTransaction()
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                val columns = obj.keys.toList()
                val placeholders = List(columns.size) { "?" }
                val values = columns.map { col ->
                    val value = obj[col]
                    when {
                        value is JsonPrimitive && value.isString -> value.content
                        value is JsonPrimitive && value.longOrNull != null -> value.long
                        value is JsonPrimitive && value.doubleOrNull != null -> value.double
                        value is JsonPrimitive && value.booleanOrNull != null -> if (value.boolean) 1 else 0
                        value is JsonNull -> null
                        else -> value.toString()
                    }
                }

                val sql = "INSERT INTO $tableName (${columns.joinToString(", ")}) VALUES (${placeholders.joinToString(", ")})"
                val stmt = db?.compileStatement(sql)
                if (stmt != null) {
                    values.forEachIndexed { i, v ->
                        val index = i + 1
                        when (v) {
                            null -> stmt.bindNull(index)
                            is Long -> stmt.bindLong(index, v)
                            is Int -> stmt.bindLong(index, v.toLong())
                            is Double -> stmt.bindDouble(index, v)
                            is Float -> stmt.bindDouble(index, v.toDouble())
                            is String -> stmt.bindString(index, v)
                            else -> stmt.bindString(index, v.toString())
                        }
                    }
                    stmt.executeInsert()
                }
            }
            db?.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.endTransaction()
        }
    }

    actual override fun executeUpdates(tableName: String, jsonArrayData: String, key: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        db?.beginTransaction()
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                val whereValue = obj[key] ?: continue

                val columns = obj.keys.filter { it != key }
                if (columns.isEmpty()) continue

                val placeholders = columns.map { "$it = ?" }
                val values = columns.map { col ->
                    val value = obj[col]
                    when {
                        value is JsonPrimitive && value.isString -> value.content
                        value is JsonPrimitive && value.longOrNull != null -> value.long
                        value is JsonPrimitive && value.doubleOrNull != null -> value.double
                        value is JsonPrimitive && value.booleanOrNull != null -> if (value.boolean) 1 else 0
                        value is JsonNull -> null
                        else -> value.toString()
                    }
                }

                val sql = "UPDATE $tableName SET ${placeholders.joinToString(", ")} WHERE $key = ?"
                val stmt = db?.compileStatement(sql)
                if (stmt != null) {
                    values.forEachIndexed { i, v ->
                        val index = i + 1
                        when (v) {
                            null -> stmt.bindNull(index)
                            is Long -> stmt.bindLong(index, v)
                            is Int -> stmt.bindLong(index, v.toLong())
                            is Double -> stmt.bindDouble(index, v)
                            is Float -> stmt.bindDouble(index, v.toDouble())
                            is String -> stmt.bindString(index, v)
                            else -> stmt.bindString(index, v.toString())
                        }
                    }
                    // bind where value en dernier
                    val whereBindIndex = values.size + 1
                    val wv = when {
                        whereValue is JsonPrimitive && whereValue.isString -> whereValue.content
                        whereValue is JsonPrimitive && whereValue.longOrNull != null -> whereValue.long
                        whereValue is JsonPrimitive && whereValue.doubleOrNull != null -> whereValue.double
                        whereValue is JsonPrimitive && whereValue.booleanOrNull != null -> if (whereValue.boolean) 1 else 0
                        whereValue is JsonNull -> null
                        else -> whereValue.toString()
                    }
                    when (wv) {
                        null -> stmt.bindNull(whereBindIndex)
                        is Long -> stmt.bindLong(whereBindIndex, wv)
                        is Int -> stmt.bindLong(whereBindIndex, wv.toLong())
                        is Double -> stmt.bindDouble(whereBindIndex, wv)
                        is Float -> stmt.bindDouble(whereBindIndex, wv.toDouble())
                        is String -> stmt.bindString(whereBindIndex, wv)
                        else -> stmt.bindString(whereBindIndex, wv.toString())
                    }

                    stmt.executeUpdateDelete()
                }
            }

            db?.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.endTransaction()
        }
    }
    */

    /*
    actual override fun executeInserts(tableName: String, jsonArrayData: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        db?.beginTransaction()
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                val cv = ContentValues()
                for ((col, value) in obj) {
                    when {
                        value is JsonPrimitive && value.isString -> cv.put(col, value.content)
                        value is JsonPrimitive && value.longOrNull != null -> cv.put(col, value.long)
                        value is JsonPrimitive && value.doubleOrNull != null -> cv.put(col, value.double)
                        value is JsonPrimitive && value.booleanOrNull != null -> cv.put(col, if (value.boolean) 1 else 0)
                        value is JsonNull -> cv.putNull(col)
                        else -> cv.put(col, value.toString())
                    }
                }
                db?.insert(tableName, null, cv)
            }
            db?.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.endTransaction()
        }
    }

    actual override fun executeUpdates(tableName: String, jsonArrayData: String, key: String): Boolean {
        val jsonArray = try { Json.parseToJsonElement(jsonArrayData).jsonArray }
        catch (e: Exception) { e.printStackTrace(); return false }

        db?.beginTransaction()
        return try {
            for (element in jsonArray) {
                val obj = element.jsonObject
                if (!obj.containsKey(key)) continue
                val whereValue = obj[key]?.let {
                    if (it is JsonPrimitive && it.isString) it.content else it.toString()
                } ?: continue

                val cv = ContentValues()
                for ((col, value) in obj) {
                    if (col == key) continue
                    when {
                        value is JsonPrimitive && value.isString -> cv.put(col, value.content)
                        value is JsonPrimitive && value.longOrNull != null -> cv.put(col, value.long)
                        value is JsonPrimitive && value.doubleOrNull != null -> cv.put(col, value.double)
                        value is JsonPrimitive && value.booleanOrNull != null -> cv.put(col, if (value.boolean) 1 else 0)
                        value is JsonNull -> cv.putNull(col)
                        else -> cv.put(col, value.toString())
                    }
                }
                db?.update(tableName, cv, "$key = ?", arrayOf(whereValue))
            }
            db?.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.endTransaction()
        }
    }*/

    actual override fun deleteDatabase(dbName: String): Boolean {
        return context?.deleteDatabase("$dbName.db")?:false
    }

    actual override fun tableSize(tableName: String): Int {
        val query = "SELECT COUNT(*) FROM $tableName;"
        return try {
            val cursor = db?.rawQuery(query, null)
            val result = cursor?.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            } ?: 0
            result
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    actual override fun tableNotEmpty(tableName: String): Boolean {
        val query = "SELECT EXISTS(SELECT 1 FROM $tableName LIMIT 1);"
        return try {
            val cursor = db?.rawQuery(query, null)
            val result = cursor?.use {
                it.moveToFirst() && it.getInt(0) == 1
            } ?: false
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun tableExists(tableName: String): Boolean {
        val query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
        return try {
            val cursor = db?.rawQuery(query, arrayOf(tableName))
            val exists = cursor?.use {
                it.moveToFirst()
            } ?: false
            exists
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun executeSQL(sql: String): Boolean {
        return try {
            db?.execSQL(sql)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual override fun lastInsertRowId(): Long {
        return try {
            val cursor = db?.rawQuery("SELECT last_insert_rowid();", null)
            val id = cursor?.use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            } ?: 0L
            id
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    actual override fun executeQuery(query: String): String {
        return try {
            val cursor = db?.rawQuery(query, null)
            val sb = StringBuilder()

            cursor?.use {
                val columnCount = it.columnCount
                val columnNames = it.columnNames

                while (it.moveToNext()) {
                    for (i in 0 until columnCount) {
                        sb.append("${columnNames[i]}=${it.getString(i)} ")
                    }
                    sb.append("\n")
                }
            }

            sb.toString()
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
            val cursor = db?.rawQuery(query, null) ?: return "[]"
            val columnCount = cursor.columnCount
            val columnNames = cursor.columnNames

            while (cursor.moveToNext()) {
                val rowVariants = mutableListOf<MutableMap<String, JsonElement>>()
                rowVariants.add(mutableMapOf()) // At least one object

                for (i in 0 until columnCount) {
                    val colName = columnNames[i] ?: continue
                    val rawValue = cursor.getString(i)
                    if (colName.isEmpty()) continue

                    var index = 0
                    var offset = 0
                    if (tableFields.size > 1) {
                        var totalFields = 0
                        for ((g, group) in tableFields.withIndex()) {
                            totalFields += group.size
                            if (i < totalFields) {
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

                    val j = i - offset
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

            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return "{}"
        }

        return JsonArray(groupedRows).toString()
    }

    actual override fun tableSchema(tableName: String): String {
        val cursor = db?.rawQuery("PRAGMA table_info($tableName)", null)
        val sb = StringBuilder()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                sb.append("$name $type\n")
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return sb.toString()
    }








    /*actual override fun executeSQL(sql: String): Boolean {
        return try {
            if (db == null) return false

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
            db?.execSQL(sql)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun executeInsert(sql: String): Boolean {
        val regex = Regex(
            """insert\s+into\s+(\w+)\s*\(([^)]+)\)\s*values\s*(.+)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val match = regex.find(sql) ?: throw IllegalArgumentException("Invalid INSERT SQL: $sql")

        val table = match.groupValues[1]
        val columns = match.groupValues[2].split(",").map { it.trim() }
        val valuesPart = match.groupValues[3].trim()

        val tuples = Regex("""\(([^)]+)\)""").findAll(valuesPart).map { it.groupValues[1] }.toList()

        var success = false
        for (tuple in tuples) {
            val values = splitSQLValues(tuple)
            val cv = android.content.ContentValues()
            for (i in columns.indices) {
                val col = columns[i]
                val v = parseSQLValue(values[i])
                when (v) {
                    null -> cv.putNull(col)
                    is Int -> cv.put(col, v)
                    is Long -> cv.put(col, v)
                    is Double -> cv.put(col, v)
                    is Float -> cv.put(col, v)
                    else -> cv.put(col, v.toString())
                }
            }
            val rowId = db?.insert(table, null, cv) ?: -1
            if (rowId != -1L) success = true
        }

        return success
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

        // Découpage SET
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

        val cv = android.content.ContentValues()
        for ((col, rawValue) in assignments) {
            val v = parseSQLValue(rawValue)
            when (v) {
                null -> cv.putNull(col)
                is Int -> cv.put(col, v)
                is Long -> cv.put(col, v)
                is Double -> cv.put(col, v)
                is Float -> cv.put(col, v)
                else -> cv.put(col, v.toString())
            }
        }

        val rowsAffected = if (wherePart.isNotEmpty()) {
            val whereRegex = Regex("""where\s+(.+)""", RegexOption.IGNORE_CASE)
            val whereMatch = whereRegex.find(wherePart)
            val clause = whereMatch?.groupValues?.get(1)?.trim() ?: ""
            db?.update(table, cv, clause, null) ?: 0
        } else {
            db?.update(table, cv, null, null) ?: 0
        }

        return rowsAffected > 0
    }

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

    private fun parseSQLValue(value: String): Any? {
        if (value.equals("null", ignoreCase = true)) return null
        return if (value.startsWith("'") && value.endsWith("'")) {
            value.substring(1, value.length - 1).replace("''", "'")
        } else {
            value.toDoubleOrNull() ?: value
        }
    }


}


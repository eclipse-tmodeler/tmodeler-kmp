package ai.tambo.tmodeler.db

import kotlinx.serialization.json.*

/**
 * TsqliteDriver implémentation wasmJs
 *
 * Backend : stockage JSON en mémoire + persistance via localStorage.
 * Chaque "table" est une List<JsonObject> sérialisée sous la clé "tmodeler__<dbName>__<tableName>".
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class TsqliteDriver actual constructor() : TdbDriver {

    private var dbName: String = ""
    private var _context: Any? = null

    // Stockage en mémoire : table -> liste de rows (JsonObject)
    private val tables: MutableMap<String, MutableList<JsonObject>> = mutableMapOf()
    // Schémas déclarés : table -> CREATE TABLE script
    private val schemas: MutableMap<String, String> = mutableMapOf()
    private var lastRowId: Long = 0L

    private fun normTable(name: String): String = name.trim().lowercase()

    // ---- clé localStorage ----
    private fun storageKey(tableName: String) = "tmodeler__${dbName}__${normTable(tableName)}"
    private fun schemasKey() = "tmodeler__${dbName}____schemas__"

    // ---- persistance ----
    // Le dernier contenu localStorage vu par table est gardé côté JS (comparaison sans recopier la chaîne dans wasm).
    private fun loadTable(tableName: String): MutableList<JsonObject> {
        val key = storageKey(tableName)
        val raw = jsLocalStorageGetItem(key)
        if (raw == null) return mutableListOf()
        return try {
            val arr = Json.parseToJsonElement(raw).jsonArray
            val list = arr.map { it.jsonObject }.toMutableList()
            list.forEach { row ->
                val id = (row["_id_"] ?: row["id"])?.jsonPrimitive?.longOrNull ?: 0L
                if (id > lastRowId) lastRowId = id
            }
            list
        } catch (_: Exception) { mutableListOf() }
    }

    private fun saveTable(tableName: String) {
        val t = normTable(tableName)
        val key = storageKey(t)
        val list = tables[t] ?: return
        val json = buildJsonArray { list.forEach { add(it) } }
        val encoded = Json.encodeToString(JsonArray.serializer(), json)
        jsLocalStorageSetItem(key, encoded)
    }

    // Chargement paresseux + cohérence multi-onglets : si un autre onglet a réécrit la table, on la recharge
    // (et `lastRowId` est recalculé), pour ne jamais travailler sur un cache périmé ni écraser ses lignes.
    private fun ensureLoaded(tableName: String) {
        val t = normTable(tableName)
        if (!tables.containsKey(t)) {
            tables[t] = loadTable(t)
            invalidate(t)
            return
        }
        if (jsLocalStorageChanged(storageKey(t))) {
            tables[t] = loadTable(t)
            invalidate(t)
        }
    }

    private fun reloadSchemasIfMissing(t: String) {
        if (schemas.containsKey(t)) return
        val raw = jsLocalStorageGetItem(schemasKey()) ?: return
        try {
            Json.parseToJsonElement(raw).jsonObject.forEach { (k, v) -> schemas[normTable(k)] = v.jsonPrimitive.content }
        } catch (_: Exception) {}
    }

    // ---- TdbDriver impl ----

    actual override fun open(dbName: String): Boolean {
        this.dbName = dbName
        val raw = jsLocalStorageGetItem(schemasKey())
        if (raw != null) {
            try {
                val obj = Json.parseToJsonElement(raw).jsonObject
                obj.forEach { (k, v) -> schemas[normTable(k)] = v.jsonPrimitive.content }
            } catch (_: Exception) {}
        }
        return true
    }

    actual override fun close() {
        tables.clear()
        indexes.clear()
    }

    actual override fun createTable(tableScript: String): Boolean {
        val rawName = extractTableName(tableScript) ?: return false
        val name = normTable(rawName)
        schemas[name] = tableScript
        persistSchemas()
        if (!tables.containsKey(name)) tables[name] = mutableListOf()
        return true
    }

    actual override fun dropTable(tableName: String): Boolean {
        val t = normTable(tableName)
        tables.remove(t)
        invalidate(t)
        schemas.remove(t)
        jsLocalStorageRemoveItem(storageKey(t))
        persistSchemas()
        return true
    }

    actual override fun tableExists(tableName: String): Boolean {
        val t = normTable(tableName)
        reloadSchemasIfMissing(t)
        return schemas.containsKey(t)
    }

    actual override fun tableNotEmpty(tableName: String): Boolean {
        val t = normTable(tableName)
        ensureLoaded(t)
        return (tables[t]?.isNotEmpty()) == true
    }

    actual override fun tableSize(tableName: String): Int {
        val t = normTable(tableName)
        ensureLoaded(t)
        return tables[t]?.size ?: 0
    }

    actual override fun tableSchema(tableName: String): String {
        return schemas[normTable(tableName)] ?: ""
    }

    actual override fun executeInserts(tableName: String, jsonArrayData: String): Boolean {
        val t = normTable(tableName)
        ensureLoaded(t)
        invalidate(t)
        return try {
            val arr = Json.parseToJsonElement(jsonArrayData).jsonArray
            val rows = tables.getOrPut(t) { mutableListOf() }
            for (element in arr) {
                val obj = element.jsonObject.toMutableMap()
                val existingId = (obj["_id"] ?: obj["_id_"])?.jsonPrimitive?.longOrNull
                if (existingId == null || existingId == 0L) {
                    lastRowId++
                    obj["_id"] = JsonPrimitive(lastRowId)
                    obj["_id_"] = JsonPrimitive(lastRowId)
                } else {
                    if (existingId > lastRowId) lastRowId = existingId
                    obj["_id"] = JsonPrimitive(existingId)
                    obj["_id_"] = JsonPrimitive(existingId)
                }
                rows.add(JsonObject(obj))
            }
            saveTable(t)
            true
        } catch (_: Exception) { false }
    }

    actual override fun executeUpdates(
        tableName: String,
        jsonArrayData: String,
        key: String
    ): Boolean {
        val t = normTable(tableName)
        ensureLoaded(t)
        invalidate(t)
        return try {
            val arr = Json.parseToJsonElement(jsonArrayData).jsonArray
            val rows = tables.getOrPut(t) { mutableListOf() }
            for (element in arr) {
                val update = element.jsonObject
                val keyVal = update[key] ?: update["_id"] ?: update["_id_"] ?: continue
                val idx = rows.indexOfFirst { (it[key] ?: it["_id"] ?: it["_id_"]) == keyVal }
                if (idx >= 0) {
                    val merged = rows[idx].toMutableMap()
                    update.forEach { (k, v) -> merged[k] = v }
                    rows[idx] = JsonObject(merged)
                } else {
                    val obj = update.toMutableMap()
                    val existingId = (obj["_id"] ?: obj["_id_"])?.jsonPrimitive?.longOrNull
                    if (existingId == null || existingId == 0L) {
                        lastRowId++
                        obj["_id"] = JsonPrimitive(lastRowId)
                        obj["_id_"] = JsonPrimitive(lastRowId)
                    }
                    rows.add(JsonObject(obj))
                }
            }
            saveTable(t)
            true
        } catch (_: Exception) { false }
    }

    actual override fun executeSQL(sql: String): Boolean {
        val trimmed = sql.trim()
        val upper = trimmed.uppercase()
        return when {
            upper.startsWith("DELETE FROM") -> {
                val tableName = normTable(upper.removePrefix("DELETE FROM").trim().split(" ").first())
                ensureLoaded(tableName)
                invalidate(tableName)
                val whereIdx = upper.indexOf("WHERE")
                if (whereIdx >= 0) {
                    val condition = trimmed.substring(whereIdx + 5).trim().trimEnd(';')
                    val matches = compileCondition(condition)
                    tables[tableName]?.removeAll { row -> matches(row) }
                    saveTable(tableName)
                } else {
                    tables[tableName]?.clear()
                    saveTable(tableName)
                }
                true
            }
            upper.startsWith("UPDATE") -> {
                val afterUpdate = trimmed.substring(6).trim()
                val tableName = normTable(afterUpdate.split(" ", "\n", "\t").first())
                ensureLoaded(tableName)
                invalidate(tableName)
                val setIdx = upper.indexOf("SET")
                val whereIdx = upper.indexOf("WHERE")
                if (setIdx >= 0) {
                    val setClause = if (whereIdx >= 0) trimmed.substring(setIdx + 3, whereIdx).trim() else trimmed.substring(setIdx + 3).trim().trimEnd(';')
                    val whereClause = if (whereIdx >= 0) trimmed.substring(whereIdx + 5).trim().trimEnd(';') else ""
                    val assignments = setClause.split(",").map { it.trim() }
                    val rows = tables[tableName] ?: return true
                    val matches = if (whereClause.isEmpty()) null else compileCondition(whereClause)
                    for (i in rows.indices) {
                        val row = rows[i]
                        if (matches == null || matches(row)) {
                            val mod = row.toMutableMap()
                            for (assign in assignments) {
                                val c = assign.substringBefore("=").trim().substringAfterLast(".").trim('`', '"')
                                val v = assign.substringAfter("=").trim().trim('\'', '"')
                                mod[c] = JsonPrimitive(v)
                            }
                            rows[i] = JsonObject(mod)
                        }
                    }
                    saveTable(tableName)
                }
                true
            }
            else -> true
        }
    }

    actual override fun lastInsertRowId(): Long = lastRowId

    actual override fun executeQuery(query: String): String {
        return try {
            val upper = query.trim().uppercase()
            if (!upper.startsWith("SELECT")) return "[]"

            val fromIdx = upper.indexOf("FROM")
            if (fromIdx < 0) return "[]"

            val afterFrom = query.substring(fromIdx + 4).trim()
            val rawTable = afterFrom.split(" ", "\n", "\t").first().trim()
            val tableName = normTable(rawTable)
            ensureLoaded(tableName)

            val rows = tables[tableName] ?: return "[]"

            // WHERE condition
            val whereIdx = upper.indexOf("WHERE")
            val filtered = if (whereIdx >= 0) {
                val whereSection = query.substring(query.uppercase().indexOf("WHERE") + 5).trim()
                val cutSection = whereSection
                    .split(Regex("(?i)\\bORDER\\s+BY\\b|\\bLIMIT\\b"))
                    .first()
                    .trim()
                applyWhere(tableName, rows, cutSection)
            } else rows

            // ORDER BY
            val orderByIdx = upper.indexOf("ORDER BY")
            val sorted = if (orderByIdx >= 0) {
                val afterOrder = query.substring(orderByIdx + 8).trim()
                val orderClause = afterOrder.split(Regex("(?i)\\bLIMIT\\b")).first().trim()
                val isDesc = orderClause.uppercase().endsWith("DESC")
                val col = orderClause.split(" ", "\t").first().trim('`', '"').substringAfterLast(".").trim()
                filtered.sortedWith(Comparator { a, b ->
                    val va = (a[col] ?: a["_${col}_"] ?: a[col.trim('_')])?.jsonPrimitive?.content ?: ""
                    val vb = (b[col] ?: b["_${col}_"] ?: b[col.trim('_')])?.jsonPrimitive?.content ?: ""
                    val numA = va.toLongOrNull()
                    val numB = vb.toLongOrNull()
                    val cmp = if (numA != null && numB != null) numA.compareTo(numB) else va.compareTo(vb)
                    if (isDesc) -cmp else cmp
                })
            } else filtered

            // LIMIT [OFFSET]
            val limited = Regex("(?i)\\bLIMIT\\s+(\\d+)(?:\\s+OFFSET\\s+(\\d+))?\\s*;?\\s*$").find(query.trim())?.let { m ->
                val limit = m.groupValues[1].toIntOrNull() ?: sorted.size
                val offset = m.groupValues[2].toIntOrNull() ?: 0
                sorted.drop(offset).take(limit)
            } ?: sorted

            buildJsonArray { limited.forEach { add(it) } }.toString()
        } catch (_: Exception) { "[]" }
    }

    actual override fun executeQuery(query: String, builder: Any): String = executeQuery(query)

    actual override fun deleteDatabase(dbName: String): Boolean {
        tables.keys.toList().forEach { dropTable(it) }
        jsLocalStorageRemoveItem(schemasKey())
        return true
    }

    actual override fun context(v: Any) { _context = v }
    actual override fun context(): Any? = _context

    // ---- helpers ----

    private fun extractTableName(script: String): String? {
        val upper = script.trim().uppercase()
        val prefix = if (upper.startsWith("CREATE TABLE IF NOT EXISTS")) "CREATE TABLE IF NOT EXISTS"
                     else if (upper.startsWith("CREATE TABLE")) "CREATE TABLE"
                     else return null
        return script.substring(prefix.length).trim().split("(", " ").firstOrNull()?.trim()
    }

    private fun persistSchemas() {
        val obj = buildJsonObject { schemas.forEach { (k, v) -> put(k, v) } }
        jsLocalStorageSetItem(schemasKey(), Json.encodeToString(JsonObject.serializer(), obj))
    }

    private fun applyWhere(tableName: String, rows: List<JsonObject>, condition: String): List<JsonObject> {
        val andParts = condition.split(Regex("\\s+AND\\s+", RegexOption.IGNORE_CASE))
        if (andParts.size == 1) {
            val eq = parseEquality(andParts[0].trim())
            if (eq != null) return indexFor(tableName, rows, eq.first)[eq.second] ?: emptyList()
        }
        val predicates = andParts.map { compileCondition(it.trim()) }
        return rows.filter { row -> predicates.all { it(row) } }
    }

    // Les recherches `col = valeur` (résolution des liens par id/clé) étaient un balayage complet de la table
    // à chaque appel : un index par colonne, reconstruit paresseusement après toute écriture, les rend directes.
    private val indexes: MutableMap<String, MutableMap<String, Map<String, List<JsonObject>>>> = mutableMapOf()

    private fun invalidate(t: String) { indexes.remove(t) }

    private fun indexFor(t: String, rows: List<JsonObject>, col: String): Map<String, List<JsonObject>> {
        val perTable = indexes.getOrPut(t) { mutableMapOf() }
        return perTable.getOrPut(col) {
            val map = HashMap<String, MutableList<JsonObject>>()
            for (row in rows) {
                val content = (cellOf(row, col) as? JsonPrimitive)?.content ?: continue
                map.getOrPut(content) { ArrayList(1) }.add(row)
            }
            map
        }
    }

    private fun parseEquality(cond: String): Pair<String, String>? {
        var clean = cond.trim()
        while (clean.startsWith("(") && clean.endsWith(")")) {
            clean = clean.substring(1, clean.length - 1).trim()
        }
        if (!clean.contains("=") || clean.contains("!=") || clean.contains("<") || clean.contains(">")) return null
        if (clean.contains(" IS NOT NULL", ignoreCase = true) || clean.contains(" IS NULL", ignoreCase = true)) return null
        if (clean.contains(" IN ", ignoreCase = true) || clean.contains(" IN(", ignoreCase = true)) return null
        val col = columnOf(clean.substringBefore("="))
        val valRaw = clean.substringAfter("=").trim().trim('(', ')', '\'', '"', ';', ' ')
        return col to valRaw
    }

    private fun cellOf(row: JsonObject, col: String): JsonElement? = row[col] ?: row["_${col}_"] ?: row[col.trim('_')]

    private fun columnOf(raw: String): String =
        raw.trim().trim('(', ')', '`', '"', '[', ']').substringAfterLast(".").trim()

    // La condition est analysée une seule fois par requête, pas une fois par ligne.
    private fun compileCondition(cond: String): (JsonObject) -> Boolean {
        return try {
            var clean = cond.trim()
            while (clean.startsWith("(") && clean.endsWith(")")) {
                clean = clean.substring(1, clean.length - 1).trim()
            }
            when {
                clean.contains(" IS NOT NULL", ignoreCase = true) -> {
                    val col = columnOf(clean.substringBefore(" IS NOT NULL"))
                    ({ row -> val cell = cellOf(row, col); cell != null && cell !is JsonNull })
                }
                clean.contains(" IS NULL", ignoreCase = true) -> {
                    val col = columnOf(clean.substringBefore(" IS NULL"))
                    ({ row -> val cell = cellOf(row, col); cell == null || cell is JsonNull })
                }
                clean.contains(" IN ", ignoreCase = true) || clean.contains(" IN(", ignoreCase = true) -> {
                    val inIdx = clean.indexOf(" IN", ignoreCase = true)
                    val col = columnOf(clean.substring(0, inIdx))
                    val afterIn = clean.substring(inIdx + 3).trim().trim('(', ')', ';', ' ')
                    val inVals = afterIn.split(",").map { s -> s.trim().trim('\'', '"') }.toSet()
                    ({ row -> val cell = cellOf(row, col)?.jsonPrimitive?.content; cell != null && inVals.contains(cell) })
                }
                clean.contains("!=") || clean.contains("<>") -> {
                    val sep = if (clean.contains("!=")) "!=" else "<>"
                    val col = columnOf(clean.substringBefore(sep))
                    val valRaw = clean.substringAfter(sep).trim().trim('(', ')', '\'', '"', ';', ' ')
                    ({ row -> cellOf(row, col)?.jsonPrimitive?.content != valRaw })
                }
                clean.contains("=") -> {
                    val col = columnOf(clean.substringBefore("="))
                    val valRaw = clean.substringAfter("=").trim().trim('(', ')', '\'', '"', ';', ' ')
                    ({ row -> cellOf(row, col)?.jsonPrimitive?.content == valRaw })
                }
                else -> ({ _ -> true })
            }
        } catch (_: Exception) { { _ -> true } }
    }
}

// ---- JS interop localStorage ----
// Les modifications faites par un autre onglet sont signalées par l'événement `storage` (jamais déclenché pour
// l'onglet courant) : aucune relecture des (grosses) chaînes à chaque requête.
private fun jsLocalStorageGetItem(key: String): String? =
    js("(function(k){ var g = globalThis; if (!g.__tmDirty) { g.__tmDirty = {}; g.__tmKeys = {}; addEventListener('storage', function(e){ if (e.key === null) { for (var x in g.__tmKeys) g.__tmDirty[x] = true; } else g.__tmDirty[e.key] = true; }); } g.__tmKeys[k] = true; delete g.__tmDirty[k]; var v = localStorage.getItem(k); return v === null ? null : v; })(key)")

private fun jsLocalStorageChanged(key: String): Boolean =
    js("(function(k){ var g = globalThis; return !!(g.__tmDirty && g.__tmDirty[k] === true); })(key)")

private fun jsLocalStorageSetItem(key: String, value: String) {
    js("(function(k,v){ localStorage.setItem(k,v); if (globalThis.__tmDirty) delete globalThis.__tmDirty[k]; })(key, value)")
}

private fun jsLocalStorageRemoveItem(key: String) {
    js("(function(k){ localStorage.removeItem(k); if (globalThis.__tmDirty) delete globalThis.__tmDirty[k]; })(key)")
}

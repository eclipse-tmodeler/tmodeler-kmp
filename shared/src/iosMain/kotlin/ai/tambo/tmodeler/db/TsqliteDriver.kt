package ai.tambo.tmodeler.db

actual class TsqliteDriver actual constructor() : TdbDriver {
    actual override fun open(dbName: String): Boolean = false
    actual override fun close() {}
    actual override fun executeInserts(tableName: String, jsonArrayData: String): Boolean {
        return false
    }

    actual override fun executeUpdates(
        tableName: String,
        jsonArrayData: String,
        key: String
    ): Boolean {
        return false
    }

    actual override fun createTable(tableScript: String): Boolean = false
    actual override fun dropTable(tableName: String): Boolean = false
    actual override fun tableNotEmpty(tableName: String): Boolean {
        val query = "SELECT EXISTS(SELECT 1 FROM $tableName LIMIT 1);"
        return false
    }
    actual override fun tableExists(tableName: String): Boolean = false
    actual override fun tableSize(tableName: String): Int {
        return 0
    }

    actual override fun executeSQL(sql: String): Boolean = false
    actual override fun lastInsertRowId(): Long = 0L
    actual override fun executeQuery(query: String): String = "Not supported on wasm"
    actual override fun executeQuery(query: String, builder: Any): String = "Not supported"
    actual override fun deleteDatabase(dbName: String): Boolean = false
    actual override fun tableSchema(tableName: String): String = "Not supported"

    actual override fun context(v: Any) {}
    actual override fun context(): Any? {
        return false
    }
}

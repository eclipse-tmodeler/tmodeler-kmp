package ai.tambo.tmodeler.db

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class TspatialiteDriver actual constructor() : TdbDriver {

    actual override fun open(dbName: String): Boolean = false
    actual override fun close() {}

    actual override fun executeInserts(tableName: String, jsonArrayData: String): Boolean = false

    actual override fun executeUpdates(
        tableName: String,
        jsonArrayData: String,
        key: String
    ): Boolean = false

    actual override fun createTable(tableScript: String): Boolean = false
    actual override fun dropTable(tableName: String): Boolean = false
    actual override fun tableNotEmpty(tableName: String): Boolean = false
    actual override fun tableExists(tableName: String): Boolean = false
    actual override fun tableSize(tableName: String): Int = 0
    actual override fun executeSQL(sql: String): Boolean = false
    actual override fun lastInsertRowId(): Long = 0L
    actual override fun executeQuery(query: String): String = "[]"
    actual override fun executeQuery(query: String, builder: Any): String = "[]"
    actual override fun deleteDatabase(dbName: String): Boolean = false
    actual override fun tableSchema(tableName: String): String = ""
    actual override fun context(v: Any) {}
    actual override fun context(): Any? = null
}

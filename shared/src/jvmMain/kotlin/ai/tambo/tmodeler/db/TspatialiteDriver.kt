package ai.tambo.tmodeler.db

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class TspatialiteDriver actual constructor() : TdbDriver {
    actual override fun open(dbName: String): Boolean {
        return true
    }

    actual override fun close() {
    }

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

    actual override fun createTable(tableScript: String): Boolean {
        return true
    }

    actual override fun dropTable(tableName: String): Boolean {
        return true
    }

    actual override fun tableNotEmpty(tableName: String): Boolean {
        return true
    }

    actual override fun tableExists(tableName: String): Boolean {
        return true
    }

    actual override fun tableSize(tableName: String): Int {
        return 0
    }

    actual override fun executeSQL(sql: String): Boolean {
        return true
    }

    actual override fun lastInsertRowId(): Long {
        return 0
    }

    actual override fun executeQuery(query: String): String {
        return ""
    }

    actual override fun executeQuery(query: String, builder: Any): String {
        return ""
    }

    actual override fun deleteDatabase(dbName: String): Boolean {
        return true
    }

    actual override fun tableSchema(tableName: String): String {
        return ""
    }

    actual override fun context(v: Any) {
    }

    actual override fun context(): Any? {
        return null
    }
}
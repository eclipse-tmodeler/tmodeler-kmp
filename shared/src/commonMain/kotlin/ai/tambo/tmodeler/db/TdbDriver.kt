package ai.tambo.tmodeler.db

interface TdbDriver {
    fun open(dbName: String): Boolean
    fun close()
    fun executeInserts(tableName: String, jsonArrayData : String): Boolean
    fun executeUpdates(tableName: String, jsonArrayData : String, key : String): Boolean //each jsonobject content key, so key will be use like where condition
    fun executeSQL(sql: String): Boolean
    fun lastInsertRowId(): Long
    fun executeQuery(query: String): String
    fun executeQuery(query: String, builder: Any): String
    fun deleteDatabase(dbName: String): Boolean
    fun createTable(tableScript: String): Boolean
    fun dropTable(tableName: String): Boolean
    fun tableNotEmpty(tableName: String): Boolean
    fun tableExists(tableName: String): Boolean
    fun tableSize(tableName: String): Int
    fun tableSchema(tableName: String): String

    fun context(v: Any)
    fun context() : Any?
}
package ai.tambo.tmodeler.db


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class TspatialiteDriver() : TdbDriver {
    override fun open(dbName: String): Boolean
    override fun close()
    override fun executeInserts(tableName: String, jsonArrayData: String): Boolean
    override fun executeUpdates(
        tableName: String,
        jsonArrayData: String,
        key: String
    ): Boolean

    override fun createTable(tableScript: String): Boolean
    override fun dropTable(tableName: String): Boolean
    override fun tableNotEmpty(tableName: String): Boolean
    override fun tableExists(tableName: String): Boolean
    override fun tableSize(tableName: String): Int
    override fun executeSQL(sql: String): Boolean
    override fun lastInsertRowId(): Long
    override fun executeQuery(query: String): String
    override fun executeQuery(query: String, builder: Any): String
    override fun deleteDatabase(dbName: String): Boolean
    override fun tableSchema(tableName: String): String

    override fun context(v: Any)
    override fun context() : Any?
}
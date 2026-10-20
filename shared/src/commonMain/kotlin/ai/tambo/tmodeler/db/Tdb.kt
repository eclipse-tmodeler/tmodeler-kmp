package ai.tambo.tmodeler.db

import ai.tambo.tmodeler.ms.observer.ResultCallback
import ai.tambo.tmodeler.extension.synchronization.core.RequestConfigBase
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase

open class Tdb(private val driver: TdbDriver) {

    fun context() : Any? = driver.context()
    val tables: MutableMap<String, Ttable> = mutableMapOf()
    var dbDir: String = ""
    var dbName: String = ""
    var autoServe: Boolean = false
    var mediaDir: String = ""
    var serverConfig = TServerConfigBase()
    var packages: MutableList<String> = mutableListOf()

    open fun open(): Boolean = driver.open(dbName)
    open fun tableExists(name: String): Boolean = driver.tableExists(name)
    open fun createTable(table: Ttable): Boolean {
        if(tableExists(table.driver.getName())){
            if(!tableSaves(table.driver.getName())){
                tables[table.driver.getName()] = table
            }
            return true
        }
        val result = driver.createTable(table.driver.toScript())
        if(result){
            tables[table.driver.getName()] = table
        }
        return result
    }
    open fun dropTable(name: String, keys: KeysType): Boolean {
        val ret = driver.dropTable(name)
        if(ret){
            notifyChange(name, keys, false, false, true)
        }
        return ret
    }
    open fun tableNotEmpty(name: String): Boolean {
        return driver.tableNotEmpty(name)
    }
    open fun tableSize(name: String): Int {
        return driver.tableSize(name)
    }
    open fun tableSaves(name: String): Boolean = tables.contains(name)

    fun lastInsertRowId(): Long = driver.lastInsertRowId()
    fun executeSQL(sql: String): Boolean = driver.executeSQL(sql)
    fun executeInserts(tableName: String, jsonArrayData : String): Boolean = driver.executeInserts(tableName, jsonArrayData)
    fun executeUpdates(tableName: String, jsonArrayData : String, key : String): Boolean = driver.executeUpdates(tableName, jsonArrayData, key)
    fun executeQuery(query: String): String = driver.executeQuery(query)
    fun executeQuery(query: String, builder: Any): String = driver.executeQuery(query, builder)

    fun addObserver(modelName: String, observer: TModelObserver) {
        if(tables.contains(modelName)){
            tables[modelName]!!.addObserver(observer)
        }
    }
    fun removeObserver(modelName: String, observer: TModelObserver) {
        if(tables.contains(modelName)){
            tables[modelName]!!.removeObserver(observer)
        }
    }
    fun notifyChange(modelName: String, keys: KeysType, onCreate: Boolean = false, onUpdate: Boolean = false, onDelete: Boolean = false) {
        if(tables.contains(modelName)){
            tables[modelName]!!.notifyChange(keys, onCreate, onUpdate, onDelete)
        }
    }
    fun notifyServer(
        modelName: String,
        ids: KeysType,
        res: ResultCallback?,
        config: RequestConfigBase?=null,
        onServeCreate: Boolean,
        onServeUpdate: Boolean,
        onServeDelete: Boolean,
        onServeRefresh: Boolean
    ) {
        if(tables.contains(modelName)){
            tables[modelName]!!.notifyServer(ids, res, config, onServeCreate, onServeUpdate, onServeDelete, onServeRefresh)
        }
    }

    enum class Type { SQLITE, SPATIALITE, MYSQL, POSTGRESQL }

    class Builder {
        private var driver: TdbDriver? = null
        private var db: Tdb? = null

        fun type(v: Type): Builder {
            db = Tdb(createDriver(v))
            return this
        }
        fun context(v: Any): Builder {
            db?.driver?.context(v)
            return this
        }
        fun accept(pkg: String): Builder {
            db?.packages?.add(pkg)
            return this
        }
        fun dbDir(v: String): Builder {
            db?.dbDir = v
            return this
        }
        fun dbName(v: String): Builder {
            db?.dbName = v
            return this
        }
        fun autoServe(v: Boolean = true): Builder {
            db?.autoServe = v
            return this
        }
        fun mediaDir(v: String): Builder {
            db?.mediaDir = v
            return this
        }
        fun server(config: TServerConfigBase): Builder {
            db?.serverConfig = config
            return this
        }

        fun start() = get()

        fun get(): Tdb? = db

        fun createDriver(type : Type) : TdbDriver {
            return when(type){
                Type.SQLITE -> TsqliteDriver()
                Type.SPATIALITE -> TspatialiteDriver()
                else -> TsqliteDriver()
            }
        }
    }
}
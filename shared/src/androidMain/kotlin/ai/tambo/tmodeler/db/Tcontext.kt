package ai.tambo.tmodeler.db

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

class Tcontext : ContextWrapper {

    public var transformer: ((Any, String, String, String?) -> Boolean)? = null
    private var dbDir: String? = null

    constructor(base: Context) : super(base)

    constructor(base: Context, dir: String) : super(base) {
        this.dbDir = dir
    }

    fun setDBDir(dir: String) {
        this.dbDir = dir
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getDBDir(): String {
        return dbDir ?: getDatabaseDir().absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getDatabaseDir(): File {
        return dbDir?.let { File(it) } ?: File(dataDir, "databases")
    }

    override fun getDatabasePath(name: String): File {
        if (dbDir == null) return super.getDatabasePath(name)

        var dbFileName = "$dbDir${File.separator}$name"
        if (!dbFileName.endsWith(".db")) {
            dbFileName += ".db"
        }

        val dbFile = File(dbFileName)
        if (!dbFile.parentFile.exists()) {
            dbFile.parentFile.mkdirs()
        }

        return dbFile
    }

    override fun databaseList(): Array<String> {
        if (dbDir == null) return super.databaseList()

        val dirFile = File(dbDir!!)
        if (!dirFile.exists()) return emptyArray()

        return dirFile.listFiles()?.map { it.name }?.toTypedArray() ?: emptyArray()
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?
    ): SQLiteDatabase {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null)
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?,
        errorHandler: DatabaseErrorHandler?
    ): SQLiteDatabase {
        return openOrCreateDatabase(name, mode, factory)
    }
}

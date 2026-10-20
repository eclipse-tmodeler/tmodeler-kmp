package ai.tambo.tmodeler.modeler

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.db.Tdb
import ai.tambo.tmodeler.extension.security.TSecurityEngine
import ai.tambo.tmodeler.extension.intelligence.TAiEngine
import ai.tambo.tmodeler.extension.synchronization.TSyncEngine
import ai.tambo.tmodeler.field.media.utils.TFile
import ai.tambo.tmodeler.ms.TmsGb
import kotlin.js.JsName

class TModeler private constructor() {

    fun context() : Any?{
        return if(_dbs.values.isNotEmpty()){
            _dbs.values.first().context()
        }else null
    }

    var _dbType: TM.DbType = TM.DbType.SQLITE
    var _dbDir: String = ""
    var _dbName: String = ""
    var _mediaDir: String = ""
    var _serverHost: String = ""

    var _provider: TMProvider? = null

    private val _dbs: MutableMap<String, Tdb> = mutableMapOf()
    private val _db_pkgs: MutableMap<String, String> = mutableMapOf()
    private val _mutex = Mutex()

    private var _callback: (() -> Unit)? = null

    private val tmsRegistry = mutableMapOf<String, TmsGb<*>>()

    fun registerTms(tmsList: List<out TmsGb<*>>) {
        for (tms in tmsList) {
            val name = tms.name()
            tmsRegistry[name] = tms
        }
    }

    fun findTms(name: String): TmsGb<*>? {
        return tmsRegistry[name]
    }

    fun connect(db: Tdb) = init(db)

    fun init(db: Tdb): TModeler {
        start(db)
        return this
    }

    fun geTdb(id: String, rw: Boolean = false): Tdb? {
        return _mutex.read {
            _dbs[id] ?: null
        }
    }

    fun getDbByPkg(pkg: String, rw: Boolean = false): Tdb? {
        return _mutex.read {
            _dbs[_db_pkgs[pkg]] ?: null
        }
    }

    fun geTdb(cls: Tclass, rw: Boolean = false): Tdb {
        return _mutex.read {
            val v = geTdb(cls.getDb(), rw)
            if(v==null){
                val v2 = getDbByPkg(cls.getPackage(), rw)
                v2 ?: _dbs.values.firstOrNull()
            }else{
                v
            }
        } ?: error("No such db!!")
    }

    suspend fun geTsuspendb(cls: Tclass, rw: Boolean = false): Tdb {
        /**
         * @return one that can be suspend any  time by the same call on geTdb
         * if a the same call on geTsuspendb, just be suspend, and wait for all current to be end
         * */
        return geTdb(cls, rw)
        /*return withContext(Dispatchers.Default) {
            _mutex.read {
                val v = geTdb(cls.getDb(), rw)
                if (v == null) {
                    val v2 = getDbByPkg(cls.getPackage(), rw)
                    v2 ?: _dbs.values.firstOrNull()
                } else {
                    v
                }
            } ?: error("No such db!!")
        }*/
    }

    fun dbReady(cb: () -> Unit): TModeler {
        _callback = cb
        return this
    }

    private fun start(db: Tdb): TModeler {

        if(!db.open()){
            return this
        }

        _mutex.write {
            _dbs[db.dbName] = db
            for(pkg in db.packages){
                _db_pkgs[pkg] = db.dbName
            }
        }
        _callback?.invoke()
        try { ai.tambo.tmodeler.notifs.Tnc.getInstance().onModelerReady() } catch (_: Throwable) {}

        return this
    }

    companion object {
        private var _instance: TModeler? = null

        fun getInstance(): TModeler {
            if (_instance == null) {
                _instance = TModeler()
            }
            return _instance!!
        }

        fun start(): TModeler = getInstance()

        fun initialize(type: TM.DbType, dbDir: String, dbName: String): TM.Builder {
            return TM.Builder()
                .type(type)
                .dbDir(dbDir)
                .dbName(dbName)
        }
    }






    @JsName("jsContextVar")
    var context: Any? = null
        private set

    var defaultDb: Int = 0
    var bases: List<Tdb>? = null
        private set

    var listener: TModelerListener? = null
        private set

    var serverLink: Any? = null
        private set

    private var serverConnected: Boolean = false
    private var dbBaseDir: String? = null
    private var mediaBaseDir: String? = null
    private var mediaTmpDir: String? = null
    private var mediaBaseHost: String? = null
    private var serverBaseHost: String? = null
    private var serverBaseUrl: String? = null
    private var serverVersion: String? = null
    private var handler: Any? = null

    fun db(): Tdb {
        return bases!![defaultDb]
    }

    private fun with(ctx: Any?=null, autoControlServerAccess: Boolean, controlListener: TModelerListener) {

        context = ctx
        listener = controlListener

        mediaBaseDir = listener!!.onLoadBaseDir("")

        mediaBaseDir = listener!!.onLoadMediaDir("/Media/")
        mediaTmpDir = listener!!.onLoadMediaTmpDir()
        mediaBaseHost = listener!!.onLoadMediaHost()
        serverBaseHost = listener!!.onLoadServerHost()
        serverBaseUrl = listener!!.onLoadBaseUrl()
        serverVersion = listener!!.onLoadVersion()
        dbBaseDir = listener!!.onLoadDBdir("")

        bases = listener!!.onLoadBases(emptyList())

        if (autoControlServerAccess) listenServer()

        listener!!.onPrepareSync()
    }

    private fun listenServer() {}

    fun host(): String? {
        return listener!!.onLoadServerHost()
    }

    fun serverHost(): String {
        return host() + TFile.separator() + listener!!.onLoadBaseUrl() + TFile.separator() + listener!!.onLoadVersion()
    }

    fun mediaFullHost(): String {
        return host() + TFile.separator() + listener!!.onLoadMediaHost()
    }

    fun mediaHost(): String? {
        return listener!!.onLoadMediaHost()
    }

    fun baseDir(): String? {
        return listener!!.onLoadMediaDir("")
    }

    fun mediaDir(): String {
        return "/media/"
    }

    fun mediaAbsolute(): String {
        return baseDir() + mediaDir()
    }

    fun tmpDir(): String? {
        return listener!!.onLoadMediaTmpDir()
    }




    private var syncEngine : TSyncEngine = TSyncEngine()
    private var securityEngine : TSecurityEngine = TSecurityEngine()
    private var aiEngine : TAiEngine = TAiEngine()

    fun syncEngine() : TSyncEngine = syncEngine
    fun securityEngine() : TSecurityEngine = securityEngine
    fun aiEngine() : TAiEngine = aiEngine

    fun synchronization(syncEngine : TSyncEngine) : TModeler {
        this.syncEngine = syncEngine
        try { ai.tambo.tmodeler.notifs.Tnc.getInstance().resumeIfReady() } catch (_: Throwable) {}
        return this
    }
    fun security(securityEngine : TSecurityEngine) : TModeler { this.securityEngine = securityEngine; return this; }
    fun intelligence(aiEngine : TAiEngine) : TModeler { this.aiEngine = aiEngine; return this; }

    fun notifications(block: ai.tambo.tmodeler.notifs.TncConfig.() -> Unit): TModeler {
        ai.tambo.tmodeler.notifs.Tnc.config(block)
        return this
    }
}
package ai.tambo.tmodeler.modeler

class TM {
    enum class DbType { SQLITE, MYSQL, POSTGRESQL }

    class Builder {
        private var dbType: DbType = DbType.SQLITE
        private var dbDir: String = ""
        private var dbName: String = ""
        private var mediaDir: String = ""
        private var host: String = ""
        private var provider: TMProvider? = null

        fun type(v: DbType) = apply { dbType = v }
        fun dbDir(v: String) = apply { dbDir = v }
        fun dbName(v: String) = apply { dbName = v }
        fun mediaDir(v: String) = apply { mediaDir = v }
        fun host(v: String) = apply { host = v }
        fun provider(v: TMProvider) = apply { provider = v }

        fun getInstance(): TModeler {
            val instance = TModeler.getInstance()
            instance._dbType = dbType
            instance._dbDir = dbDir
            instance._dbName = dbName
            instance._mediaDir = mediaDir
            instance._serverHost = host
            instance._provider = provider
            return instance
        }
    }

    companion object {
        fun getInstance(): TModeler = TModeler.getInstance()
    }
}
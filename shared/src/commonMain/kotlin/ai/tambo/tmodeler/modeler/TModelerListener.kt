package ai.tambo.tmodeler.modeler

import ai.tambo.tmodeler.db.Tdb


interface TModelerListener {
    fun onLoadMediaTmpDir(): String?
    fun onLoadMediaDir(dir: String?): String?
    fun onLoadBaseDir(dir: String?): String?
    fun onLoadMediaHost(): String?
    fun onLoadServerHost(): String?
    fun onLoadBaseUrl(): String?
    fun onLoadVersion(): String?
    fun onLoadDBdir(dir: String?): String?
    fun onLoadBases(bases: List<Tdb>): List<Tdb>
    fun onServerAccessChange(connected: Boolean)
    fun onPrepareSync()
}

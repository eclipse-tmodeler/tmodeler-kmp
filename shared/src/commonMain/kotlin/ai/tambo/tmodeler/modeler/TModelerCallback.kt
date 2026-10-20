package ai.tambo.tmodeler.modeler

import ai.tambo.tmodeler.db.Tdb


class TModelerCallback : TModelerListener {
    override fun onLoadMediaTmpDir(): String? {
        return "tmp"
    }

    public override fun onLoadMediaDir(dir: String?): String? {
        return dir
    }

    public override fun onLoadBaseDir(dir: String?): String? {
        return dir
    }

    override fun onLoadMediaHost(): String? {
        return "media"
    }

    override fun onLoadServerHost(): String? {
        return null
    }

    override fun onLoadBaseUrl(): String? {
        return ""
    }

    override fun onLoadVersion(): String? {
        return ""
    }

    public override fun onLoadDBdir(dir: String?): String? {
        return dir
    }

    override fun onLoadBases(bases: List<Tdb>): List<Tdb> {
        return bases
    }

    override fun onServerAccessChange(connected: Boolean) {}

    override fun onPrepareSync() {
    }
}

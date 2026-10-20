package ai.tambo.tmodeler.notifs

private class IosStore : TncStore {
    private val map = mutableMapOf<String, String>()
    override fun get(key: String): String? = map[key]
    override fun put(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
}

private class IosPlatform : TncPlatform {
    override val store: TncStore = IosStore()
    override val name = "ios"
    override val appId = "ios"

    override fun log(tag: String, message: String) = println("[$tag] $message")
    override fun isForeground() = true
    override fun openSocket(url: String, pingIntervalMs: Long, headers: Map<String, String>, listener: TncSocketListener): TncSocket? = null
    override fun post(url: String, json: String, headers: Map<String, String>, onDone: (Int) -> Unit) = onDone(-1)
    override fun pushAvailable() = false
    override fun fetchPushToken(onResult: (token: String?, error: String?) -> Unit) = onResult(null, "APNs non implémenté")
    override fun showNotification(notice: TncNotice, config: TncFcmConfig) {}
}

actual fun tncPlatform(): TncPlatform = IosPlatform()

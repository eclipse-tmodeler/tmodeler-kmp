package ai.tambo.tmodeler.notifs

interface TncSocket {
    fun close(code: Int = 1000, reason: String = "")
}

interface TncSocketListener {
    fun onOpen()
    fun onMessage(text: String)
    fun onClosed(code: Int, reason: String)
    fun onFailure(error: String)
}

/** Petit stockage clé/valeur persistant (jeton push, dernier utilisateur). */
interface TncStore {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

/**
 * Tout ce qui dépend de la plateforme : WebSocket, HTTP, stockage, premier plan, jeton push, notification système.
 * Une implémentation par cible (Android, JVM, web, iOS) via [tncPlatform] ; remplaçable pour les tests.
 */
interface TncPlatform {
    val store: TncStore

    /** Identifiant de plateforme envoyé au serveur : android, ios, web, jvm. */
    val name: String

    /** Identifiant de l'application (package Android, origine web...). */
    val appId: String

    fun log(tag: String, message: String)

    fun isForeground(): Boolean

    /** Retourne null si la plateforme ne sait pas ouvrir de WebSocket. */
    fun openSocket(
        url: String,
        pingIntervalMs: Long,
        headers: Map<String, String>,
        listener: TncSocketListener,
    ): TncSocket?

    /** POST JSON ; `onDone` reçoit le code HTTP, ou -1 en cas d'échec réseau. */
    fun post(url: String, json: String, headers: Map<String, String>, onDone: (Int) -> Unit)

    fun pushAvailable(): Boolean

    /** Demande le jeton push (FCM...) ; exactement l'un des deux paramètres est non nul. */
    fun fetchPushToken(onResult: (token: String?, error: String?) -> Unit)

    /** Variante avec la configuration FCM (utile au web) ; par défaut, ignore la configuration. */
    fun fetchPushToken(config: TncFcmConfig, onResult: (token: String?, error: String?) -> Unit) =
        fetchPushToken(onResult)

    fun showNotification(notice: TncNotice, config: TncFcmConfig)
}

expect fun tncPlatform(): TncPlatform

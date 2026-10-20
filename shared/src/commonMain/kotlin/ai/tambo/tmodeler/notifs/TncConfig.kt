package ai.tambo.tmodeler.notifs


/** Canaux de notification instantanée. */
enum class TncChannel { WEBSOCKET, FCM }

enum class TncState { IDLE, CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED, ERROR }

/** Comportement d'un push reçu pendant que l'app est au premier plan (le WebSocket fait déjà le travail). */
enum class TncForeground { IGNORE, DELIVER, SHOW }

/** Observateur global de Tnc : état des canaux, signaux reçus, changement d'utilisateur. */
interface TncObserver {
    fun onConnectionChanged(channel: TncChannel, state: TncState, detail: String?) {}
    fun onNotif(notif: TncSignal<*>, source: TncChannel) {}
    fun onUserChanged(user: Any?, userId: Long?) {}
    fun onPushToken(token: String) {}
}

/** Notification système à afficher quand l'app est en arrière-plan. */
class TncNotice(
    val title: String,
    val body: String,
    val id: Int,
    val data: Map<String, String>,
)

class TncWebSocketConfig {
    var enabled = false

    /** Chemin relatif à [TncConfig.host] ; `{userId}` est remplacé par l'id serveur de l'utilisateur. */
    var path = "ws/notifications/{userId}/"

    /** URL complète (ws/wss/http/https) : prioritaire sur host + path. */
    var url: String? = null

    var reconnectDelayMs = 3_000L
    var maxReconnectDelayMs = 30_000L

    /** Facteur multiplicatif entre deux tentatives (1.0 = délai constant). */
    var backoff = 1.0
    var pingIntervalMs = 30_000L
    var headers: Map<String, String> = emptyMap()

    /** Clé de la trame JSON qui contient le signal ; à défaut, la trame entière est le signal. */
    var payloadKey = "payload"

    /** Ignore les signaux sans id serveur. */
    var requireServed = true
}

/** Web Push via Firebase Cloud Messaging : valeurs de l'application Web du projet Firebase. */
class TncWebPushConfig {
    var apiKey = ""
    var projectId = ""
    var messagingSenderId = ""
    var appId = ""

    /** Clé publique VAPID (Cloud Messaging > Certificats Web Push). */
    var vapidKey = ""

    /** Script du service worker (copié à la racine du site, voir tnc-sw.js dans les ressources de la lib). */
    var serviceWorkerPath = "/tnc-sw.js"
    var sdkVersion = "10.12.2"

    val isComplete: Boolean
        get() = apiKey.isNotBlank() && projectId.isNotBlank() && messagingSenderId.isNotBlank() &&
            appId.isNotBlank() && vapidKey.isNotBlank()
}

class TncFcmConfig {
    var enabled = false

    /** Web uniquement. */
    val web = TncWebPushConfig()

    fun web(block: TncWebPushConfig.() -> Unit) = web.block()

    /** Appelé quand l'utilisateur ouvre une notification système (données du push). */
    var onOpen: ((Map<String, String>) -> Unit)? = null
    var registerPath = "users/devices/register/"
    var unregisterPath = "users/devices/unregister/"
    var headers: Map<String, String> = emptyMap()

    /** Champs ajoutés au corps de l'enregistrement du jeton. */
    var extra: Map<String, String> = emptyMap()

    var foreground = TncForeground.IGNORE
    var showNotification = true
    var titleKey = "title"
    var bodyKey = "body"
    var defaultTitle = ""
    var channelId = "tnc_default"
    var channelName = ""

    /** Ressource d'icône (Android) ; à défaut, l'icône de l'app. */
    var smallIcon: Int? = null
    var maxRegisterRetries = 5
    var retryDelayMs = 5_000L

    /** Filtre : retourner false pour ignorer un push. */
    var accept: ((Map<String, String>) -> Boolean)? = null

    /** Notification personnalisée ; retourner null pour ne rien afficher. */
    var notice: ((Map<String, String>) -> TncNotice?)? = null

    /** Conversion personnalisée data FCM -> TncSignal. */
    var toNotif: ((Map<String, String>) -> TncSignal<*>)? = null
}

class TncConfig {
    /** Hôte du serveur, ex. `https://api.example.com`. */
    var host = ""

    /** Reconnecte automatiquement le dernier utilisateur mémorisé dès que TModeler démarre. */
    var autoResume = false
    var dedupe = true
    var logTag = "Tnc"

    /** Extraction de l'id serveur depuis l'objet utilisateur dynamique ; défaut : nombre, TModel (id_), map {id}. */
    var userId: ((Any) -> Long?)? = null

    val ws = TncWebSocketConfig()
    val fcm = TncFcmConfig()
    internal val observers = mutableListOf<TncObserver>()

    fun websocket(block: TncWebSocketConfig.() -> Unit = {}) {
        ws.enabled = true
        ws.block()
    }

    fun fcm(block: TncFcmConfig.() -> Unit = {}) {
        fcm.enabled = true
        fcm.block()
    }

    fun observer(observer: TncObserver) {
        observers.add(observer)
    }

    internal fun baseHost() = host.trim().trimEnd('/')

    fun wsUrl(userId: Long): String? {
        val raw = ws.url ?: run {
            val h = baseHost()
            if (h.isEmpty()) return null
            "$h/${ws.path.trimStart('/')}"
        }
        val url = raw.replace("{userId}", userId.toString())
        return when {
            url.startsWith("https://") -> "wss://" + url.removePrefix("https://")
            url.startsWith("http://") -> "ws://" + url.removePrefix("http://")
            url.startsWith("ws://") || url.startsWith("wss://") -> url
            else -> "wss://$url"
        }
    }

    fun httpUrl(path: String): String? {
        val h = baseHost()
        return if (h.isEmpty()) null else "$h/${path.trimStart('/')}"
    }
}

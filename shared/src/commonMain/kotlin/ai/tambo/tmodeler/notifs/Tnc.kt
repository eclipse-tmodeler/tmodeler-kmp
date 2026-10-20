package ai.tambo.tmodeler.notifs

import ai.tambo.tmodeler.extension.synchronization.core.TncDeepBase
import ai.tambo.tmodeler.extension.synchronization.core.TncHost
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.scheduler.TGate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Tnc : notifications instantanées de TModeler (face publique).
 *
 * ```
 * TModeler.start().synchronization(TBridge.start()).notifications {
 *     host = "https://api.exemple.com"
 *     websocket { reconnectDelayMs = 3_000 }
 *     fcm { foreground = TncForeground.IGNORE }
 * }
 * Tnc.connect(user)                                   // n'importe quand : "voici le nouvel utilisateur"
 * Tnc.listen(Message.tms) { tms, notif -> ... }       // dans la feature de son choix
 * Tnc.disconnect()
 * ```
 * Tnc porte ce que les programmes implémenteurs utilisent : configuration, utilisateur, écoute, observateurs, état.
 * La politique de connexion (WebSocket, reconnexion, protocole des trames, jeton push, dédoublonnage) est la partie
 * profonde [TncDeepBase], fournie par le moteur de synchronisation (`TSyncEngine.tncDeep`, TBridge) ou par
 * [engine]. Les deux canaux (WebSocket, FCM) alimentent le même flux de [TncSignal] : mêmes écouteurs.
 */
class Tnc private constructor() {

    companion object {
        private var _instance: Tnc? = null
        fun getInstance(): Tnc {
            if (_instance == null) _instance = Tnc()
            return _instance!!
        }

        fun config(block: TncConfig.() -> Unit): Tnc = getInstance().config(block)
        fun connect(user: Any): Tnc = getInstance().connect(user)
        fun disconnect(): Tnc = getInstance().disconnect()
        fun listen(vararg tms: TmsGb<*>, notify: (TmsGb<*>, TncSignal<*>) -> Unit): TncSubscription =
            getInstance().listen(tms.toList(), notify)

        fun listenAll(notify: (TncSignal<*>) -> Unit): TncSubscription = getInstance().listenAll(notify)
        fun observe(observer: TncObserver): TncSubscription = getInstance().observe(observer)
        val user: Any? get() = getInstance().user
        val userId: Long? get() = getInstance().userId
    }

    private class Listener(val tms: List<TmsGb<*>>?, val onModel: ((TmsGb<*>, TncSignal<*>) -> Unit)?, val onAny: ((TncSignal<*>) -> Unit)?)

    private var config = TncConfig()
    private var platformOverride: TncPlatform? = null
    private val platform: TncPlatform get() = platformOverride ?: tncPlatform().also { platformOverride = it }

    private var deepOverride: TncDeepBase? = null
    private var attachedDeep: TncDeepBase? = null

    private var listeners: List<Listener> = emptyList()
    private var observers: List<TncObserver> = emptyList()

    var user: Any? = null
        private set
    var userId: Long? = null
        private set
    var pushToken: String? = null
        private set
    var websocketState = TncState.IDLE
        private set
    var pushState = TncState.IDLE
        private set

    val isConnected: Boolean get() = websocketState == TncState.CONNECTED

    private val _notifs = MutableSharedFlow<TncSignal<*>>(replay = 0, extraBufferCapacity = 128)

    /** Tous les signaux reçus (WebSocket + FCM), après dédoublonnage. */
    val notifs: SharedFlow<TncSignal<*>> = _notifs.asSharedFlow()

    // -------------------------------------------------------------- moteur profond

    private val host = object : TncHost {
        override val config: TncConfig get() = this@Tnc.config
        override val platform: TncPlatform get() = this@Tnc.platform
        override val userId: Long? get() = this@Tnc.userId ?: this@Tnc.platform.store.get(KEY_USER)?.toLongOrNull()

        override fun setState(channel: TncChannel, state: TncState, detail: String?) =
            this@Tnc.setState(channel, state, detail)

        override fun deliver(notif: TncSignal<*>, source: TncChannel) = this@Tnc.deliver(notif, source)

        override fun pushTokenChanged(token: String) {
            pushToken = token
            observers.forEach { it.onPushToken(token) }
        }

        override fun log(message: String) = this@Tnc.log(message)
    }

    /** Moteur profond courant : celui imposé par [engine], sinon celui du moteur de synchronisation. */
    private val deep: TncDeepBase
        get() {
            val d = deepOverride ?: TModeler.getInstance().syncEngine().tncDeep
            if (attachedDeep !== d) {
                attachedDeep = d
                d.attach(host)
            }
            return d
        }

    /** Impose un moteur profond (autre politique que celle de TBridge, tests). */
    fun engine(deep: TncDeepBase): Tnc {
        deepOverride = deep
        resumeIfReady()
        return this
    }

    // -------------------------------------------------------------- configuration

    fun config(block: TncConfig.() -> Unit): Tnc {
        val c = TncConfig()
        c.block()
        config = c
        observers = observers + c.observers
        log("configuré host=${c.host} websocket=${c.ws.enabled} fcm=${c.fcm.enabled}")
        return this
    }

    /** Remplace la plateforme (tests). */
    fun platform(platform: TncPlatform): Tnc {
        platformOverride = platform
        return this
    }

    internal fun onModelerReady() = resumeIfReady()

    /** Reprend automatiquement le dernier utilisateur dès que TModeler et un moteur profond sont prêts. */
    internal fun resumeIfReady() {
        if (config.autoResume && userId == null && deep.available) resume()
    }

    /** Reprend avec le dernier utilisateur mémorisé, sans objet utilisateur. */
    fun resume(): Tnc {
        val saved = platform.store.get(KEY_USER)?.toLongOrNull() ?: return this
        if (userId == null) start(null, saved)
        return this
    }

    // -------------------------------------------------------------- utilisateur

    /** Annonce l'utilisateur connecté (objet dynamique : Long, TModel, Map...). Idempotent. */
    fun connect(user: Any): Tnc {
        val id = resolveUserId(user)
        if (id == null) {
            log("connect ignoré : id serveur introuvable pour $user")
            return this
        }
        start(user, id)
        return this
    }

    private fun start(user: Any?, id: Long) {
        val engine = deep
        if (!engine.available) {
            log("aucun moteur de notification instantanée (TBridge requis) : connect ignoré")
            return
        }
        if (userId == id && engine.isAlive) {
            this.user = user ?: this.user
            return
        }
        if (userId != null && userId != id) engine.stop(unregisterPush = true)
        this.user = user
        userId = id
        platform.store.put(KEY_USER, id.toString())
        observers.forEach { it.onUserChanged(user, id) }
        log("utilisateur connecté id=$id")
        engine.start(id)
    }

    fun disconnect(): Tnc {
        if (deep.available) deep.stop(unregisterPush = true)
        user = null
        userId = null
        platform.store.put(KEY_USER, null)
        observers.forEach { it.onUserChanged(null, null) }
        log("utilisateur déconnecté")
        return this
    }

    private fun resolveUserId(user: Any): Long? {
        config.userId?.let { return it(user) }
        return when (user) {
            is Number -> user.toLong()
            is String -> user.toLongOrNull()
            is TBaseModel -> user.id_.takeIf { it > 0 }
            is Map<*, *> -> (user["id"] ?: user["id_"])?.let { (it as? Number)?.toLong() ?: it.toString().toLongOrNull() }
            else -> null
        }
    }

    // -------------------------------------------------------------- écoute

    fun listen(vararg tms: TmsGb<*>, notify: (TmsGb<*>, TncSignal<*>) -> Unit): TncSubscription =
        listen(tms.toList(), notify)

    /** Écoute les signaux de ces modèles ; plusieurs écouteurs par modèle possibles. */
    fun listen(tmsList: List<TmsGb<*>>, notify: (TmsGb<*>, TncSignal<*>) -> Unit): TncSubscription {
        val l = Listener(tmsList, notify, null)
        listeners = listeners + l
        return TncSubscription { listeners = listeners - l }
    }

    /** Écoute tous les signaux, quel que soit le modèle. */
    fun listenAll(notify: (TncSignal<*>) -> Unit): TncSubscription {
        val l = Listener(null, null, notify)
        listeners = listeners + l
        return TncSubscription { listeners = listeners - l }
    }

    fun observe(observer: TncObserver): TncSubscription {
        observers = observers + observer
        return TncSubscription { observers = observers - observer }
    }

    // -------------------------------------------------------------- canaux (délégués au moteur profond)

    /** Relance immédiatement la connexion WebSocket. */
    fun reconnect() {
        if (userId == null) return
        deep.reconnect()
    }

    /** Nouveau jeton d'appareil, appelé par le service push de la plateforme (démarrage ou rotation). */
    fun onPushToken(token: String) = deep.onPushToken(token)

    /** Message push reçu (données seules), appelé par le service push de la plateforme. */
    fun onPushMessage(data: Map<String, String>) = deep.onPushMessage(data)

    // -------------------------------------------------------------- distribution

    private fun deliver(notif: TncSignal<*>, source: TncChannel) {
        observers.forEach { runCatching { it.onNotif(notif, source) } }
        _notifs.tryEmit(notif)
        TGate.scope.launch {
            try { dispatch(notif) } catch (e: Throwable) { log("dispatch en erreur : ${e.message}") }
        }
    }

    private fun dispatch(notif: TncSignal<*>) {
        var matched = false
        for (l in listeners) {
            try {
                l.onAny?.let { it(notif); matched = true }
                l.tms?.forEach { tms ->
                    if (notif.of(tms)) {
                        matched = true
                        l.onModel?.invoke(tms, notif)
                    }
                }
            } catch (e: Throwable) {
                log("écouteur en erreur : ${e.message}")
            }
        }
        if (!matched) log("signal ${notif.id_} ignoré (aucun écouteur)")
    }

    // -------------------------------------------------------------- utilitaires

    private fun setState(channel: TncChannel, state: TncState, detail: String?) {
        if (channel == TncChannel.WEBSOCKET) websocketState = state else pushState = state
        log("$channel -> $state${detail?.let { " ($it)" } ?: ""}")
        observers.forEach { runCatching { it.onConnectionChanged(channel, state, detail) } }
    }

    private fun log(message: String) = platform.log(config.logTag, message)
}

private const val KEY_USER = "user_id"

class TncSubscription internal constructor(private val onCancel: () -> Unit) {
    fun cancel() = onCancel()
}

package ai.tambo.tmodeler.extension.synchronization.core

import ai.tambo.tmodeler.notifs.TncChannel
import ai.tambo.tmodeler.notifs.TncConfig
import ai.tambo.tmodeler.notifs.TncPlatform
import ai.tambo.tmodeler.notifs.TncState
import ai.tambo.tmodeler.notifs.TncSignal

/**
 * Ce que Tnc (face publique, TModeler) met à la disposition de son moteur profond ([TncDeepBase]).
 * Le moteur ne connaît ni les écouteurs ni les observateurs : il remet les signaux à [deliver] et publie ses états.
 */
interface TncHost {
    val config: TncConfig
    val platform: TncPlatform

    /** Id serveur de l'utilisateur connecté, à défaut le dernier mémorisé ; null si aucun. */
    val userId: Long?

    fun setState(channel: TncChannel, state: TncState, detail: String? = null)

    /** Distribue un signal aux observateurs et aux écouteurs (`Tnc.listen`). */
    fun deliver(notif: TncSignal<*>, source: TncChannel)

    /** Annonce un nouveau jeton push (démarrage ou rotation). */
    fun pushTokenChanged(token: String)

    fun log(message: String)
}

/**
 * Partie profonde de Tnc : politique de connexion (WebSocket, reconnexion, ping), protocole des trames,
 * enregistrement du jeton push auprès du serveur, dédoublonnage, règles premier plan/arrière-plan.
 * Les programmes implémenteurs n'y touchent jamais : ils passent par `Tnc`. Elle appartient à la synchronisation
 * (TBridge) et se branche comme [TFeatureDeepBase], via `TSyncEngine.tncDeep`. Sans moteur, Tnc est inactif.
 */
open class TncDeepBase {

    /** Vrai si ce moteur sait réellement gérer les notifications instantanées. */
    open val available: Boolean get() = false

    /** Appelé une fois, avant tout autre appel, avec l'hôte qui fournit configuration, plateforme et distribution. */
    open fun attach(host: TncHost) {}

    /** Un utilisateur est connecté : ouvrir les canaux configurés (WebSocket, push). */
    open fun start(userId: Long) {}

    /** Ferme les canaux ; [unregisterPush] retire aussi le jeton push du serveur. */
    open fun stop(unregisterPush: Boolean) {}

    /** Relance immédiatement le WebSocket. */
    open fun reconnect() {}

    /** Vrai si les canaux sont déjà ouverts ou en cours de (re)connexion : rend `connect` idempotent. */
    open val isAlive: Boolean get() = false

    /** Nouveau jeton d'appareil, fourni par le service push de la plateforme. */
    open fun onPushToken(token: String) {}

    /** Message push reçu (données seules), fourni par le service push de la plateforme. */
    open fun onPushMessage(data: Map<String, String>) {}
}

package ai.tambo.tmodeler.notifs

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Accès isolé à Firebase : la lib reste utilisable dans une app qui n'embarque pas firebase-messaging. */
internal object TncFirebase {
    fun available(): Boolean = try {
        Class.forName("com.google.firebase.messaging.FirebaseMessaging")
        true
    } catch (_: Throwable) {
        false
    }

    fun token(onResult: (String?, String?) -> Unit) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(task.result, null)
                else onResult(null, task.exception?.message ?: "jeton FCM indisponible")
            }
        } catch (t: Throwable) {
            onResult(null, "Firebase indisponible : ${t.message}")
        }
    }
}

/** Service FCM de la lib, déclaré dans son manifest : relaie jeton et messages à [Tnc]. */
class TncMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        TncAndroid.init(applicationContext)
        Tnc.getInstance().onPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        TncAndroid.init(applicationContext)
        Tnc.getInstance().onPushMessage(message.data)
    }
}

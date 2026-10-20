package ai.tambo.tmodeler.notifs

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** Contexte application et suivi du premier plan, capturés avant `Application.onCreate` par [TncInitProvider]. */
object TncAndroid {
    @Volatile
    var app: Context? = null
        private set

    @Volatile
    private var started = 0

    val foreground: Boolean get() = started > 0

    @Synchronized
    fun init(context: Context) {
        if (app != null) return
        val ctx = context.applicationContext ?: context
        app = ctx
        (ctx as? Application)?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { started++ }
            override fun onActivityStopped(activity: Activity) { if (started > 0) started-- }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}

/** Déclaré dans le manifest de la lib : l'app n'a rien à configurer. */
class TncInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { TncAndroid.init(it) }
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}

private class AndroidStore : TncStore {
    private fun prefs() = TncAndroid.app?.getSharedPreferences("tmodeler_tnc", Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs()?.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs()?.edit()?.apply { if (value == null) remove(key) else putString(key, value) }?.apply()
    }
}

private class AndroidPlatform : TncPlatform {
    override val store: TncStore = AndroidStore()
    override val name = "android"
    override val appId: String get() = TncAndroid.app?.packageName ?: ""

    private val client by lazy { OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build() }

    override fun log(tag: String, message: String) {
        Log.d(tag.take(23), message)
    }

    override fun isForeground() = TncAndroid.foreground

    override fun openSocket(
        url: String,
        pingIntervalMs: Long,
        headers: Map<String, String>,
        listener: TncSocketListener,
    ): TncSocket? {
        val http = client.newBuilder().pingInterval(pingIntervalMs, TimeUnit.MILLISECONDS).build()
        val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> addHeader(k, v) } }.build()
        val ws = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = listener.onOpen()
            override fun onMessage(webSocket: WebSocket, text: String) = listener.onMessage(text)
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
                listener.onClosed(code, reason)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = listener.onClosed(code, reason)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                listener.onFailure(t.message ?: t.javaClass.simpleName)
        })
        return object : TncSocket {
            override fun close(code: Int, reason: String) {
                ws.close(code, reason)
            }
        }
    }

    override fun post(url: String, json: String, headers: Map<String, String>, onDone: (Int) -> Unit) {
        Thread {
            var code = -1
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 15_000
                c.readTimeout = 15_000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
                c.outputStream.use { it.write(json.toByteArray()) }
                code = c.responseCode
                c.disconnect()
            } catch (_: Throwable) {}
            onDone(code)
        }.start()
    }

    override fun pushAvailable() = TncFirebase.available()

    override fun fetchPushToken(onResult: (token: String?, error: String?) -> Unit) =
        TncFirebase.token(onResult)

    override fun showNotification(notice: TncNotice, config: TncFcmConfig) {
        val ctx = TncAndroid.app ?: return
        try {
            if (Build.VERSION.SDK_INT >= 33 &&
                ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
            ) {
                log("Tnc", "notification non affichée : permission POST_NOTIFICATIONS refusée")
                return
            }
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        config.channelId,
                        config.channelName.ifBlank { ctx.applicationInfo.loadLabel(ctx.packageManager).toString() },
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                )
            }
            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                notice.data.forEach { (k, v) -> putExtra(k, v) }
            }
            val pending = launch?.let {
                PendingIntent.getActivity(ctx, notice.id, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, config.channelId)
            else @Suppress("DEPRECATION") Notification.Builder(ctx).setPriority(Notification.PRIORITY_HIGH)
            val icon = config.smallIcon ?: ctx.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info
            builder.setSmallIcon(icon)
                .setContentTitle(notice.title)
                .setContentText(notice.body)
                .setAutoCancel(true)
            pending?.let { builder.setContentIntent(it) }
            nm.notify(notice.id, builder.build())
        } catch (e: Throwable) {
            log("Tnc", "notification en erreur : ${e.message}")
        }
    }
}

actual fun tncPlatform(): TncPlatform = AndroidPlatform()

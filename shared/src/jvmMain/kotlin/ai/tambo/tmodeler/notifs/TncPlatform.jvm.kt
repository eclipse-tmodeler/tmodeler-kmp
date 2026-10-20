package ai.tambo.tmodeler.notifs

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private class JvmStore : TncStore {
    private val map = java.util.concurrent.ConcurrentHashMap<String, String>()
    override fun get(key: String): String? = map[key]
    override fun put(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
}

private class JvmPlatform : TncPlatform {
    override val store: TncStore = JvmStore()
    override val name = "jvm"
    override val appId = "jvm"

    override fun log(tag: String, message: String) = println("[$tag] $message")

    override fun isForeground() = true

    override fun openSocket(
        url: String,
        pingIntervalMs: Long,
        headers: Map<String, String>,
        listener: TncSocketListener,
    ): TncSocket? {
        val pinger = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "tnc-ping").apply { isDaemon = true } }
        val buffer = StringBuilder()
        val builder = HttpClient.newHttpClient().newWebSocketBuilder()
        headers.forEach { (k, v) -> builder.header(k, v) }
        val future: CompletableFuture<WebSocket> = builder.buildAsync(URI.create(url), object : WebSocket.Listener {
            override fun onOpen(webSocket: WebSocket) {
                webSocket.request(1)
                pinger.scheduleAtFixedRate(
                    { runCatching { webSocket.sendPing(ByteBuffer.allocate(0)) } },
                    pingIntervalMs, pingIntervalMs, TimeUnit.MILLISECONDS,
                )
                listener.onOpen()
            }

            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                buffer.append(data)
                if (last) {
                    val text = buffer.toString()
                    buffer.setLength(0)
                    listener.onMessage(text)
                }
                webSocket.request(1)
                return null
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                pinger.shutdownNow()
                listener.onClosed(statusCode, reason)
                return null
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                pinger.shutdownNow()
                listener.onFailure(error.message ?: error.javaClass.simpleName)
            }
        })
        future.exceptionally { error ->
            pinger.shutdownNow()
            listener.onFailure(error.cause?.message ?: error.message ?: "connexion impossible")
            null
        }
        return object : TncSocket {
            override fun close(code: Int, reason: String) {
                pinger.shutdownNow()
                future.thenAccept { runCatching { it.sendClose(code, reason) } }
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

    override fun pushAvailable() = false

    override fun fetchPushToken(onResult: (token: String?, error: String?) -> Unit) =
        onResult(null, "push non supporté sur JVM")

    override fun showNotification(notice: TncNotice, config: TncFcmConfig) {
        println("[Tnc] ${notice.title} : ${notice.body}")
    }
}

actual fun tncPlatform(): TncPlatform = JvmPlatform()

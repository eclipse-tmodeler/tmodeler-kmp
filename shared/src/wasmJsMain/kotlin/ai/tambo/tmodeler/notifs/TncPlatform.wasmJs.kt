package ai.tambo.tmodeler.notifs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

private class WebStore : TncStore {
    override fun get(key: String): String? = jsStoreGet("tnc:$key")?.toString()
    override fun put(key: String, value: String?) = jsStorePut("tnc:$key", value ?: "", value == null)
}

private class WebSocketHandle(private val ws: JsAny) : TncSocket {
    override fun close(code: Int, reason: String) = jsSocketClose(ws)
}

/**
 * Web : WebSocket du navigateur, HTTP via fetch, stockage localStorage, premier plan = onglet visible.
 * Push : Firebase Cloud Messaging pour le web (service worker `tnc-sw.js`), configuré par `fcm { web { ... } }`.
 */
private class WebPlatform : TncPlatform {
    override val store: TncStore = WebStore()
    override val name = "web"
    override val appId: String get() = jsOrigin().toString()

    private var fcm: TncFcmConfig? = null

    override fun log(tag: String, message: String) = println("[$tag] $message")

    override fun isForeground() = jsVisible()

    override fun openSocket(
        url: String,
        pingIntervalMs: Long,
        headers: Map<String, String>,
        listener: TncSocketListener,
    ): TncSocket? {
        val ws = jsSocketOpen(
            url,
            { listener.onOpen() },
            { text -> listener.onMessage(text.toString()) },
            { info ->
                val raw = info.toString()
                val code = raw.substringBefore('|').toIntOrNull() ?: 1006
                listener.onClosed(code, raw.substringAfter('|', ""))
            },
            { error -> listener.onFailure(error.toString()) },
        ) ?: return null
        return WebSocketHandle(ws)
    }

    override fun post(url: String, json: String, headers: Map<String, String>, onDone: (Int) -> Unit) {
        val headersJson = JsonObject(headers.mapValues { JsonPrimitive(it.value) }).toString()
        jsPost(url, json, headersJson) { code -> onDone(code.toString().toIntOrNull() ?: -1) }
    }

    override fun pushAvailable() = jsPushSupported()

    override fun fetchPushToken(onResult: (token: String?, error: String?) -> Unit) =
        onResult(null, "configuration Web Push absente")

    override fun fetchPushToken(config: TncFcmConfig, onResult: (token: String?, error: String?) -> Unit) {
        fcm = config
        val web = config.web
        if (!web.isComplete) {
            onResult(null, "fcm.web incomplet (apiKey, projectId, messagingSenderId, appId, vapidKey)")
            return
        }
        jsPushToken(
            web.apiKey, web.projectId, web.messagingSenderId, web.appId, web.vapidKey,
            web.serviceWorkerPath, web.sdkVersion,
            { token, error ->
                val t = token.toString()
                if (t.isEmpty()) onResult(null, error.toString().ifEmpty { "jeton indisponible" }) else onResult(t, null)
            },
            { data -> Tnc.getInstance().onPushMessage(parseData(data.toString())) },
            { data -> fcm?.onOpen?.invoke(parseData(data.toString())) },
        )
    }

    override fun showNotification(notice: TncNotice, config: TncFcmConfig) {
        fcm = config
        val dataJson = JsonObject(notice.data.mapValues { JsonPrimitive(it.value) }).toString()
        jsShowNotification(notice.title, notice.body, dataJson) { data -> fcm?.onOpen?.invoke(parseData(data.toString())) }
    }

    private fun parseData(json: String): Map<String, String> = try {
        Json.parseToJsonElement(json).jsonObject.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
    } catch (_: Exception) {
        emptyMap()
    }
}

private val platform by lazy { WebPlatform() }

actual fun tncPlatform(): TncPlatform = platform

private fun jsOrigin(): JsString = js("location.origin")

private fun jsVisible(): Boolean = js("document.visibilityState === 'visible'")

private fun jsStoreGet(key: String): JsString? = js("localStorage.getItem(key)")

private fun jsStorePut(key: String, value: String, remove: Boolean): Unit =
    js("{ if (remove) localStorage.removeItem(key); else localStorage.setItem(key, value); }")

private fun jsSocketOpen(
    url: String,
    onOpen: () -> Unit,
    onMessage: (JsString) -> Unit,
    onClose: (JsString) -> Unit,
    onError: (JsString) -> Unit,
): JsAny? = js(
    """
(function () {
  if (typeof WebSocket === 'undefined') return null;
  var w = new WebSocket(url);
  w.onopen = function () { onOpen(); };
  w.onmessage = function (e) { onMessage(String(e.data)); };
  w.onclose = function (e) { onClose(String(e.code) + '|' + String(e.reason || '')); };
  w.onerror = function () { onError('erreur WebSocket'); };
  return w;
})()
"""
)

private fun jsSocketClose(ws: JsAny): Unit = js("{ ws.onopen = null; ws.onmessage = null; ws.onclose = null; ws.onerror = null; try { ws.close(1000, 'stop'); } catch (e) {} }")

private fun jsPost(url: String, body: String, headersJson: String, onDone: (JsString) -> Unit): Unit = js(
    """
{
  var h = Object.assign({ 'Content-Type': 'application/json' }, JSON.parse(headersJson));
  fetch(url, { method: 'POST', headers: h, body: body })
    .then(function (r) { onDone(String(r.status)); })
    .catch(function () { onDone('-1'); });
}
"""
)

private fun jsPushSupported(): Boolean =
    js("('serviceWorker' in navigator) && ('Notification' in window) && Notification.permission !== 'denied'")

private fun jsPushToken(
    apiKey: String,
    projectId: String,
    senderId: String,
    appId: String,
    vapidKey: String,
    swPath: String,
    sdkVersion: String,
    onResult: (JsString, JsString) -> Unit,
    onMessage: (JsString) -> Unit,
    onOpen: (JsString) -> Unit,
): Unit = js(
    """
{
  function fail(m) { onResult('', String(m)); }
  function load(src) {
    return new Promise(function (res, rej) {
      if (document.querySelector('script[data-tnc="' + src + '"]')) return res();
      var s = document.createElement('script');
      s.src = src; s.async = true; s.setAttribute('data-tnc', src);
      s.onload = function () { res(); };
      s.onerror = function () { rej(new Error('chargement impossible : ' + src)); };
      document.head.appendChild(s);
    });
  }
  var base = 'https://www.gstatic.com/firebasejs/' + sdkVersion + '/';
  Notification.requestPermission().then(function (p) {
    if (p !== 'granted') throw new Error('permission notifications : ' + p);
    return load(base + 'firebase-app-compat.js');
  }).then(function () {
    return load(base + 'firebase-messaging-compat.js');
  }).then(function () {
    var cfg = { apiKey: apiKey, projectId: projectId, messagingSenderId: senderId, appId: appId };
    if (!firebase.apps.length) firebase.initializeApp(cfg);
    var q = '?apiKey=' + encodeURIComponent(apiKey) + '&projectId=' + encodeURIComponent(projectId) +
      '&senderId=' + encodeURIComponent(senderId) + '&appId=' + encodeURIComponent(appId) +
      '&sdk=' + encodeURIComponent(sdkVersion);
    return navigator.serviceWorker.register(swPath + q).then(function (reg) {
      return navigator.serviceWorker.ready.then(function () {
        var messaging = firebase.messaging();
        messaging.onMessage(function (p) { onMessage(JSON.stringify((p && p.data) || {})); });
        navigator.serviceWorker.addEventListener('message', function (e) {
          if (e.data && e.data.tncOpen) onOpen(JSON.stringify(e.data.tncOpen));
        });
        return messaging.getToken({ vapidKey: vapidKey, serviceWorkerRegistration: reg });
      });
    });
  }).then(function (t) {
    if (!t) throw new Error('jeton vide');
    onResult(String(t), '');
  }).catch(function (e) { fail(e && e.message ? e.message : e); });
}
"""
)

private fun jsShowNotification(title: String, body: String, dataJson: String, onOpen: (JsString) -> Unit): Unit = js(
    """
{
  if ('Notification' in window && Notification.permission === 'granted') {
    var n = new Notification(title, { body: body, data: JSON.parse(dataJson) });
    n.onclick = function () { window.focus(); n.close(); onOpen(dataJson); };
  }
}
"""
)

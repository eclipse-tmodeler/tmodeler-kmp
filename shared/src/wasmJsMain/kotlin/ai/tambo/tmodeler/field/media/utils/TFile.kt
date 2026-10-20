package ai.tambo.tmodeler.field.media.utils

/**
 * TFile  implementation wasmJs
 *
 * Virtual filesystem (in-memory map), with localStorage persistence.
 * Key = "tmodeler__file__<path>", Value = hex-encoded bytes.
 *
 * Media methods (getImageWidth, etc.) return 0 (progressive parity).
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class TFile {

    private var _path: String = ""

    actual constructor() { _path = "" }
    actual constructor(path: String) { _path = norm(path) }
    actual constructor(dir: TFile, name: String) { _path = norm("${dir._path}/$name") }

    // ---- Base ----

    actual fun path(): String = _path
    actual fun url(): String? = if (exists()) null else _path.takeIf { it.isNotEmpty() }

    actual fun exists(): Boolean =
        vfs.containsKey(_path) || vfs.keys.any { it.startsWith("$_path/") }

    actual fun isDirectory(): Boolean =
        vfs.keys.any { it.startsWith("$_path/") }

    actual fun length(): Long = (vfs[_path]?.size ?: 0).toLong()

    actual fun delete(): Boolean {
        val removed = vfs.remove(_path) != null
        if (removed) tfileJsRm("tmodeler__file__$_path")
        return removed
    }

    actual fun renameTo(dest: TFile): Boolean {
        val data = vfs.remove(_path) ?: return false
        tfileJsRm("tmodeler__file__$_path")
        storeData(dest._path, data)
        _path = dest._path
        return true
    }

    actual fun renameTo(name: String): Boolean {
        val parentPart = _path.substringBeforeLast("/", "")
        val newPath = if (parentPart.isEmpty()) name else "$parentPart/$name"
        val data = vfs.remove(_path) ?: return false
        tfileJsRm("tmodeler__file__$_path")
        storeData(newPath, data)
        _path = newPath
        return true
    }

    actual fun listFiles(): List<TFile>? {
        if (!isDirectory()) return null
        val prefix = "$_path/"
        return vfs.keys
            .filter { it.startsWith(prefix) && !it.removePrefix(prefix).contains("/") }
            .map { TFile(it) }
    }

    actual fun parent(): TFile = TFile(_path.substringBeforeLast("/", ""))
    actual fun file(): Any = _path

    // ---- Metadata ----

    actual fun name(): String = _path.substringAfterLast("/")
    actual fun extension(): String = name().substringAfterLast(".", "").lowercase()
    actual fun mimeType(): String = when (extension()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        "mp4"         -> "video/mp4"
        "mp3"         -> "audio/mpeg"
        "m4a"         -> "audio/mp4"
        "wav"         -> "audio/wav"
        "ogg", "opus" -> "audio/ogg"
        "webm"        -> "audio/webm"
        "pdf"         -> "application/pdf"
        "txt"         -> "text/plain"
        "json"        -> "application/json"
        else          -> "application/octet-stream"
    }
    actual fun lastModified(): Long = timestamps[_path] ?: 0L
    actual fun humanSize(): String {
        var s = length().toDouble(); var i = 0
        val u = arrayOf("B","KB","MB","GB")
        while (s >= 1024 && i < u.size - 1) { s /= 1024; i++ }
        return "${s.toInt()} ${u[i]}"
    }
    actual fun typeCategory(): FileCategory = when (extension()) {
        "jpg","jpeg","png","gif" -> FileCategory.IMAGE
        "mp4","avi","mov"        -> FileCategory.VIDEO
        "mp3","wav","aac","m4a","ogg","opus","webm" -> FileCategory.AUDIO
        "pdf","doc","docx","txt" -> FileCategory.DOCUMENT
        else                      -> FileCategory.OTHER
    }

    // ---- Text I/O ----

    actual fun readText(): String = vfs[_path]?.decodeToString() ?: ""
    actual fun writeText(text: String, append: Boolean) {
        val bytes = if (append) (vfs[_path] ?: ByteArray(0)) + text.encodeToByteArray()
                    else text.encodeToByteArray()
        storeData(_path, bytes)
    }
    actual fun readLines(limit: Int): List<String> = readText().split("\n").take(limit)
    actual fun readChars(limit: Int): String = readText().take(limit)

    // ---- Binary I/O ----

    actual fun readBytes(): ByteArray = vfs[_path] ?: ByteArray(0)
    actual fun writeBytes(bytes: ByteArray, append: Boolean) {
        val data = if (append) (vfs[_path] ?: ByteArray(0)) + bytes else bytes
        storeData(_path, data)
    }
    actual fun readBytesChunk(size: Int): ByteArray =
        (vfs[_path] ?: ByteArray(0)).take(size).toByteArray()
    actual fun appendBytesChunk(bytes: ByteArray) {
        writeBytes(bytes, append = true)
    }
    actual fun appendTextChunk(text: String) = appendBytesChunk(text.encodeToByteArray())

    // ---- Directory ops ----

    actual fun mkdir(): Boolean { mkDirMark(_path); return true }
    actual fun mkdirs(): Boolean { mkDirMark(_path); return true }
    actual fun copyTo(dest: TFile, overwrite: Boolean): Boolean {
        if (!exists()) return false
        if (dest.exists() && !overwrite) return false
        val data = vfs[_path] ?: return false
        dest.writeBytes(data, false)
        return true
    }
    actual fun moveTo(dest: TFile, overwrite: Boolean): Boolean {
        if (!copyTo(dest, overwrite)) return false
        delete()
        return true
    }

    // ---- Utilities ----

    actual fun clearDir(): Boolean {
        if (!isDirectory()) return false
        val prefix = "$_path/"
        vfs.keys.filter { it.startsWith(prefix) }.forEach {
            tfileJsRm("tmodeler__file__$it")
            vfs.remove(it)
        }
        return true
    }
    actual fun deleteRecursively(): Boolean { if (isDirectory()) clearDir(); return delete() }
    actual fun sameAs(other: TFile): Boolean =
        _path == other._path && exists() && other.exists() && length() == other.length()

    // ---- Media stubs (progressive parity) ----

    actual fun getImageWidth(): Int = 0
    actual fun getImageHeight(): Int = 0
    actual fun getVideoDuration(): Long = 0L
    actual fun getVideoBitrate(): Int = 0
    actual fun getAudioDuration(): Long = 0L
    actual fun getAudioBitrate(): Int = 0
    actual fun getPdfPageCount(): Int = 0

    // ---- Companion ----

    actual companion object {
        actual fun fromPath(path: String): TFile = TFile(path)
        actual fun tempFile(prefix: String, suffix: String): TFile =
            TFile("tmp/${prefix}_${tfileJsNow()}${suffix}")
        actual fun separator(): String = "/"

        // internal VFS state
        internal val vfs: MutableMap<String, ByteArray> = mutableMapOf()
        internal val timestamps: MutableMap<String, Long> = mutableMapOf()

        internal fun norm(path: String) = path.replace("\\", "/").trimEnd('/')

        internal fun storeData(path: String, data: ByteArray) {
            vfs[path] = data
            timestamps[path] = tfileJsNow()
            // localStorage (quota ~5 Mo, hex = x2) : seuls les petits fichiers sont persistés, les gros restent en mémoire.
            try {
                if (data.size <= PERSIST_MAX_BYTES) {
                    val hex = data.joinToString("") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }
                    tfileJsSet("tmodeler__file__$path", hex)
                } else {
                    tfileJsRm("tmodeler__file__$path")
                }
            } catch (_: Throwable) {}
        }

        private const val PERSIST_MAX_BYTES = 256 * 1024

        internal fun mkDirMark(path: String) {
            if (!vfs.containsKey("$path/")) vfs["$path/"] = ByteArray(0)
        }
    }
}

// ---- Top-level JS interop (required by Kotlin/Wasm) ----
// Note: js() must be the sole expression in a top-level function body in Kotlin/Wasm.

private fun tfileJsNowRaw(): Double = js("Date.now()")
private fun tfileJsNow(): Long = tfileJsNowRaw().toLong()

private fun tfileJsSet(key: String, value: String): Unit = js("localStorage.setItem(key, value)")

private fun tfileJsRm(key: String): Unit = js("localStorage.removeItem(key)")

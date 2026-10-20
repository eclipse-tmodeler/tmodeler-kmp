package ai.tambo.tmodeler.field.media

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.field.media.utils.TFile

open class FileField : TField<TFile, FileField>(TFtype.TEXT) {

    private var dep = TextField()
    private var dep_ by dep

    // `get()` ne lève pas quand la valeur est absente (une exception coûte cher en wasm)
    override fun getValue(): Any? = dep.get()

    override fun setValue(v: Any?) {
        if (v != null && !isValid(v)) {
            throw IllegalArgumentException("Invalid file : $v")
        }

        when (v) {
            is String -> {
                dep_ = v
                _value = TFile(resolveLocalPath(v))
            }
            is TFile -> {
                dep_ = v.path().removePrefix(localMediaDir).removePrefix("/")
                _value = v
            }
        }
    }

    var localMediaDir: String = ""
        private set

    var serverMediaDir: String = ""
        private set

    fun localDir(p: String): FileField {
        localMediaDir = p
        return this
    }

    fun serverDir(p: String): FileField {
        serverMediaDir = p
        return this
    }

    // Résout le chemin complet local en combinant localMediaDir + chemin relatif
    protected fun resolveLocalPath(relativePath: String): String {
        return if (localMediaDir.isEmpty()) relativePath else "$localMediaDir/$relativePath"
    }

    // Media Manager
    private var minSizeBytes: Long? = null
    private var maxSizeBytes: Long? = null
    protected var allowedExtensions: List<String>? = null
    protected var allowedMimeTypes: List<String>? = null
    private var maxDurationSeconds: Long? = null       // Ex: pour video/audio
    private var maxWidth: Int? = null                   // Ex: pour images
    private var maxHeight: Int? = null

    fun size(min: Long? = null, max: Long? = null): FileField {
        minSizeBytes = min
        maxSizeBytes = max
        return this
    }
    fun extension(vararg exts: String): FileField {
        allowedExtensions = exts.toList()
        return this
    }
    fun mimeType(vararg mimes: String): FileField {
        allowedMimeTypes = mimes.toList()
        return this
    }
    fun maxDuration(seconds: Long): FileField {
        maxDurationSeconds = seconds
        return this
    }
    fun maxWidth(px: Int): FileField {
        maxWidth = px
        return this
    }
    fun maxHeight(px: Int): FileField {
        maxHeight = px
        return this
    }

    /**
     * Vérifie que la valeur donnée est valide.
     * Par défaut :
     * - Si c'est un TFile, on vérifie que le fichier existe et n'est pas un dossier.
     * - Si c'est un String, on vérifie que c'est un chemin "valide" (non vide ici, tu peux rajouter regex).
     */
    open fun isValid(v: Any): Boolean {
        return when (v) {
            is TFile -> {
                if (!v.exists() || v.isDirectory()) return false

                minSizeBytes?.let { if (v.length() < it) return false }
                maxSizeBytes?.let { if (v.length() > it) return false }

                allowedExtensions?.let { exts ->
                    val ext = v.extension().lowercase()
                    if (exts.none { it.lowercase() == ext }) return false
                }

                allowedMimeTypes?.let { mimes ->
                    val mime = v.mimeType()
                    if (mimes.none { it.equals(mime, ignoreCase = true) }) return false
                }

                true
            }

            is String -> {
                v.isNotBlank()
            }

            else -> false
        }
    }

    fun clearValue() {
        dep.set(null)
    }
}

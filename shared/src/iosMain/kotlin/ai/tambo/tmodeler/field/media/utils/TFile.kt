package ai.tambo.tmodeler.field.media.utils

actual class TFile {

    // --- Base ---
    actual fun path(): String = ""
    actual fun url() : String? = null
    actual fun exists(): Boolean = false
    actual fun isDirectory(): Boolean = false
    actual fun length(): Long = 0
    actual fun delete(): Boolean = false
    actual fun renameTo(dest: TFile): Boolean = false
    actual fun listFiles(): List<TFile>? = null

    // --- Métadonnées ---
    actual fun name(): String = ""
    actual fun extension(): String = ""
    actual fun mimeType(): String = ""
    actual fun lastModified(): Long = 0
    actual fun humanSize(): String = ""
    actual fun typeCategory(): FileCategory = FileCategory.OTHER

    // --- Lecture / écriture texte ---
    actual fun readText(): String = ""
    actual fun writeText(text: String, append: Boolean) {}
    actual fun readLines(limit: Int): List<String> = emptyList()
    actual fun readChars(limit: Int): String = ""

    // --- Lecture / écriture binaire ---
    actual fun readBytes(): ByteArray = ByteArray(0)
    actual fun writeBytes(bytes: ByteArray, append: Boolean) {}
    actual fun readBytesChunk(size: Int): ByteArray = ByteArray(0)
    actual fun appendBytesChunk(bytes: ByteArray) {}
    actual fun appendTextChunk(text: String) {}

    // --- Gestion dossier ---
    actual fun mkdir(): Boolean = false
    actual fun mkdirs(): Boolean = false
    actual fun copyTo(dest: TFile, overwrite: Boolean): Boolean = false
    actual fun moveTo(dest: TFile, overwrite: Boolean): Boolean = false

    // --- Utilitaires ---
    actual fun clearDir(): Boolean = false
    actual fun deleteRecursively(): Boolean = false
    actual fun sameAs(other: TFile): Boolean = false

    // --- Constructors ---
    actual constructor(path: String) {  }
    actual constructor() {  }

    // --- Companion ---
    actual companion object {
        actual fun fromPath(path: String): TFile = TFile()
        actual fun tempFile(prefix: String, suffix: String): TFile = TFile()
        actual fun separator(): String {
            return ""
        }
    }

    // specific
    actual fun getImageWidth() : Int = 0
    actual fun getImageHeight() : Int = 0
    actual fun getVideoDuration() : Long = 0
    actual fun getVideoBitrate() : Int = 0
    actual fun getAudioDuration() : Long = 0
    actual fun getAudioBitrate() : Int = 0
    actual fun getPdfPageCount() : Int = 0
    actual fun parent(): TFile {
        return TFile()
    }

    actual fun renameTo(name: String): Boolean {
        return false
    }

    actual fun file(): Any {
        return 0
    }

    actual constructor(dir: TFile, name: String) {
        
    }
}

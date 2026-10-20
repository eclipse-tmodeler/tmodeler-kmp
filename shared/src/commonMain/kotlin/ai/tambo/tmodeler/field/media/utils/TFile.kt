package ai.tambo.tmodeler.field.media.utils

expect class TFile {

    // Constructeurs
    constructor()
    constructor(path: String)
    constructor(dir: TFile, name: String)

    // Base
    fun path(): String
    fun url(): String?
    fun exists(): Boolean
    fun isDirectory(): Boolean
    fun length(): Long
    fun delete(): Boolean
    fun parent(): TFile
    fun renameTo(name: String): Boolean
    fun renameTo(dest: TFile): Boolean
    fun listFiles(): List<TFile>?
    fun file(): Any

    // Métadonnées
    fun name(): String
    fun extension(): String
    fun mimeType(): String
    fun lastModified(): Long
    fun humanSize(): String
    fun typeCategory(): FileCategory // IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER

    // Lecture / écriture texte
    fun readText(): String
    fun writeText(text: String, append: Boolean = false)
    fun readLines(limit: Int): List<String>
    fun readChars(limit: Int): String

    // Lecture / écriture binaire
    fun readBytes(): ByteArray
    fun writeBytes(bytes: ByteArray, append: Boolean = false)
    fun readBytesChunk(size: Int): ByteArray
    fun appendBytesChunk(bytes: ByteArray)
    fun appendTextChunk(text: String)

    // Gestion dossier
    fun mkdir(): Boolean
    fun mkdirs(): Boolean
    fun copyTo(dest: TFile, overwrite: Boolean = false): Boolean
    fun moveTo(dest: TFile, overwrite: Boolean = false): Boolean

    // Utilitaires
    fun clearDir(): Boolean
    fun deleteRecursively(): Boolean
    fun sameAs(other: TFile): Boolean

    companion object {
        fun fromPath(path: String): TFile
        fun tempFile(prefix: String = "tmp", suffix: String = ""): TFile

        fun separator() : String
    }

    // specific
    fun getImageWidth() : Int
    fun getImageHeight() : Int
    fun getVideoDuration() : Long
    fun getVideoBitrate() : Int
    fun getAudioDuration() : Long
    fun getAudioBitrate() : Int
    fun getPdfPageCount() : Int

}

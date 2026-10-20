package ai.tambo.tmodeler.field.media.utils

import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class TFile {

    private var file: File
    private var path = ""

    actual constructor() {
        file = File("")
    }

    actual constructor(path: String) {
        this.path = path
        file = File(path)
    }

    actual constructor(dir: TFile, name: String) {
        file = File(dir.path(), name)
    }

    // --- Base ---
    actual fun path() = if(exists()) file.absolutePath else path
    actual fun url() : String? = if(exists()) null else path
    actual fun exists() = file.exists()
    actual fun isDirectory() = file.isDirectory
    actual fun length() = file.length()
    actual fun delete() = file.delete()
    actual fun renameTo(dest: TFile) = file.renameTo(File(dest.path()))

    //actual fun listFiles() List<TFile>? = file.listFiles()?.map { TFile(it.absolutePath) }
    actual fun listFiles(): List<TFile>? = file.listFiles()?.map { TFile(it.absolutePath) }

    actual fun parent(): TFile {
        return file.parentFile?.let { TFile(it.absolutePath) }
            ?: TFile("")
    }
    actual fun renameTo(name: String): Boolean {
        val parent = file.parentFile ?: return false
        val newFile = File(parent, name)
        return file.renameTo(newFile).also {
            if (it) file = newFile
        }
    }

    // --- Métadonnées ---
    actual fun name() = file.name
    actual fun extension() = file.extension.lowercase()
    actual fun mimeType(): String = when (extension()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }
    actual fun lastModified() = file.lastModified()
    actual fun humanSize(): String {
        val size = length()
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var i = 0
        var s = size.toDouble()
        while (s >= 1024 && i < units.size - 1) {
            s /= 1024
            i++
        }
        return "%.1f %s".format(s, units[i])
    }
    actual fun typeCategory(): FileCategory = when (extension()) {
        "jpg", "jpeg", "png", "gif" -> FileCategory.IMAGE
        "mp4", "avi", "mov" -> FileCategory.VIDEO
        "mp3", "wav", "aac" -> FileCategory.AUDIO
        "pdf", "doc", "docx", "txt" -> FileCategory.DOCUMENT
        else -> FileCategory.OTHER
    }

    // --- Lecture / écriture texte ---
    actual fun readText(): String = file.readText()
    @Throws(java.io.IOException::class)
    actual fun writeText(text: String, append: Boolean) {
        FileWriter(file, append).use { writer ->
            writer.write(text)
        }
    }
    @Throws(java.io.IOException::class)
    actual fun readLines(limit: Int): List<String> {
        val result = mutableListOf<String>()
        BufferedReader(FileReader(file)).use { br ->
            var count = 0
            var line: String?
            while (br.readLine().also { line = it } != null && count < limit) {
                result.add(line!!)
                count++
            }
        }
        return result
    }
    @Throws(java.io.IOException::class)
    actual fun readChars(limit: Int): String {
        val buffer = CharArray(limit)
        FileReader(file).use { reader ->
            val readCount = reader.read(buffer, 0, limit)
            return String(buffer, 0, readCount)
        }
    }

    // --- Lecture / écriture binaire ---
    actual fun readBytes(): ByteArray = file.readBytes()
    actual fun writeBytes(bytes: ByteArray, append: Boolean) {
        if (append) file.appendBytes(bytes) else file.writeBytes(bytes)
    }
    @Throws(java.io.IOException::class)
    actual fun readBytesChunk(size: Int): ByteArray {
        val buffer = ByteArray(size)
        FileInputStream(file).use { fis ->
            val bytesRead = fis.read(buffer)
            return buffer.copyOf(bytesRead.coerceAtLeast(0))
        }
    }
    @Throws(java.io.IOException::class)
    actual fun appendBytesChunk(bytes: ByteArray) {
        FileOutputStream(file, true).use { fos ->
            fos.write(bytes)
        }
    }
    @Throws(java.io.IOException::class)
    actual fun appendTextChunk(text: String) {
        FileWriter(file, true).use { writer ->
            writer.write(text)
        }
    }

    // --- Gestion dossier ---
    actual fun mkdir() = file.mkdir()
    actual fun mkdirs() = file.mkdirs()
    actual fun copyTo(dest: TFile, overwrite: Boolean): Boolean {
        return try {
            Files.copy(file.toPath(), File(dest.path()).toPath(),
                if (overwrite) StandardCopyOption.REPLACE_EXISTING else StandardCopyOption.COPY_ATTRIBUTES
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    actual fun moveTo(dest: TFile, overwrite: Boolean): Boolean {
        return try {
            Files.move(file.toPath(), File(dest.path()).toPath(),
                if (overwrite) StandardCopyOption.REPLACE_EXISTING else StandardCopyOption.ATOMIC_MOVE
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Utilitaires ---
    actual fun clearDir(): Boolean {
        if (!isDirectory()) return false
        return file.listFiles()?.all { it.deleteRecursively() } ?: false
    }
    actual fun deleteRecursively(): Boolean = file.deleteRecursively()
    actual fun sameAs(other: TFile): Boolean =
        exists() && other.exists() && length() == other.length() && path() == other.path()

    // --- Companion ---
    actual companion object {
        actual fun fromPath(path: String): TFile = TFile(path)
        actual fun tempFile(prefix: String, suffix: String): TFile {
            val tmp = File.createTempFile(prefix, suffix)
            return TFile(tmp.absolutePath)
        }

        actual fun separator(): String = "/"
    }

    // --- spécifiques ---
    actual fun getImageWidth(): Int {
        if (!exists() || isDirectory()) return 0
        return try {
            val img: BufferedImage = ImageIO.read(file)
            img.width
        } catch (e: Exception) {
            0
        }
    }

    actual fun getImageHeight(): Int {
        if (!exists() || isDirectory()) return 0
        return try {
            val img: BufferedImage = ImageIO.read(file)
            img.height
        } catch (e: Exception) {
            0
        }
    }

    actual fun getVideoDuration(): Long {
        // Complexe sur JVM pur, souvent nécessite libs externes (Xuggler, JCodec, etc.)
        // Ici on retourne 0, à compléter selon libs utilisées
        return 0L
    }

    actual fun getVideoBitrate(): Int {
        // Idem, nécessite parsing metadata vidéo
        return 0
    }

    actual fun getAudioDuration(): Long {
        if (!exists() || isDirectory()) return 0L
        return try {
            val audioInputStream = AudioSystem.getAudioInputStream(file)
            val format = audioInputStream.format
            val frames = audioInputStream.frameLength
            ((frames / format.frameRate).toLong())
        } catch (e: UnsupportedAudioFileException) {
            0L
        } catch (e: IOException) {
            0L
        }
    }

    actual fun getAudioBitrate(): Int {
        // Approximation possible avec format info, ou 0 si non disponible
        return 0
    }

    actual fun getPdfPageCount(): Int {
        /*if (!exists() || isDirectory()) return 0
        return try {
            PDDocument.load(file).use { doc ->
                doc.numberOfPages
            }
        } catch (e: Exception) {
            0
        }*/
        return 0
    }

    actual fun file(): Any = file
}

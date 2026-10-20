package ai.tambo.tmodeler.field.media

import ai.tambo.tmodeler.field.media.utils.TFile

class AudioField : FileField() {

    init {
        allowedExtensions = listOf("mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "webm", "opus")
        allowedMimeTypes = listOf(
            "audio/mpeg",
            "audio/wav",
            "audio/aac",
            "audio/flac",
            "audio/ogg",
            "audio/mp4",
            "audio/webm",
            "audio/x-ms-wma",
            "application/octet-stream"
        )
    }

    var maxDurationSeconds: Long? = null
    var maxBitrateKbps: Int? = null

    override fun isValid(v: Any): Boolean {
        if (!super.isValid(v)) return false

        if(v is TFile){
            val duration = v.getAudioDuration()
            val bitrate = v.getAudioBitrate()

            maxDurationSeconds?.let { if (duration > it) return false }
            maxBitrateKbps?.let { if (bitrate > it) return false }
        }

        return true
    }
}


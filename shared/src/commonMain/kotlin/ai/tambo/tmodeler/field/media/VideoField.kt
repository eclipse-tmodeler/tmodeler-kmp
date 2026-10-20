package ai.tambo.tmodeler.field.media

import ai.tambo.tmodeler.field.media.utils.TFile

class VideoField : FileField() {

    init {
        allowedExtensions = listOf("mp4", "mov", "avi", "mkv", "flv", "wmv", "webm")
        allowedMimeTypes = listOf(
            "video/mp4",
            "video/x-matroska",
            "video/avi",
            "video/x-flv",
            "video/x-ms-wmv",
            "video/webm"
        )
    }

    var maxDurationSeconds: Long? = null
    var maxBitrateKbps: Int? = null

    override fun isValid(v: Any): Boolean {
        if (!super.isValid(v)) return false

        if(v is TFile){
            val duration = v.getVideoDuration()
            val bitrate = v.getVideoBitrate()

            maxDurationSeconds?.let { if (duration > it) return false }
            maxBitrateKbps?.let { if (bitrate > it) return false }
        }

        return true
    }
}


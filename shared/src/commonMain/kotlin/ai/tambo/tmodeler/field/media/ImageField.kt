package ai.tambo.tmodeler.field.media

import ai.tambo.tmodeler.field.media.utils.TFile

class ImageField : FileField() {

    init {
        allowedExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "tiff")
        allowedMimeTypes = listOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp",
            "image/heic",
            "image/tiff"
        )
    }

    var maxWidth: Int? = null
    var maxHeight: Int? = null
    var minWidth: Int? = null
    var minHeight: Int? = null

    override fun isValid(v: Any): Boolean {
        if (!super.isValid(v)) return false

        if(v is TFile){
            val width = v.getImageWidth()
            val height = v.getImageHeight()

            minWidth?.let { if (width < it) return false }
            minHeight?.let { if (height < it) return false }
            maxWidth?.let { if (width > it) return false }
            maxHeight?.let { if (height > it) return false }
        }

        return true
    }
}


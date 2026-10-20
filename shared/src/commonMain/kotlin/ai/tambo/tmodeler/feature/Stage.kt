package ai.tambo.tmodeler.feature

enum class Stage {
    FS_RECEIVED,
    FS_STARTED,

    TSM_SCHEDULED,
    TSM_STARTED,

    META_PREPARING,
    META_LOADING,

    REQUESTING,
    RESPONSE_READY,

    STREAMING,
    PARSING,

    COMPLETED,
    FAILED
}

fun Stage.range(): IntRange = when (this) {
    Stage.FS_RECEIVED -> 0..5
    Stage.FS_STARTED -> 5..10

    Stage.TSM_SCHEDULED -> 10..15
    Stage.TSM_STARTED -> 15..20

    Stage.META_PREPARING -> 20..30
    Stage.META_LOADING -> 30..40

    Stage.REQUESTING -> 40..55
    Stage.RESPONSE_READY -> 55..65

    Stage.STREAMING -> 65..80
    Stage.PARSING -> 80..99   // 👈 important

    Stage.COMPLETED -> 100..100
    Stage.FAILED -> 100..100
}

fun computeProgress(stage: Stage, subProgress: Int?): Int {
    val range = stage.range()

    return if (subProgress == null) {
        range.first
    } else {
        val size = range.last - range.first
        range.first + (size * subProgress / 100)
    }
}
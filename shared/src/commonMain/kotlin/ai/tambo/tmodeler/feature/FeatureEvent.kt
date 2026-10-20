package ai.tambo.tmodeler.feature

data class FeatureEvent(
    val stage: Stage,
    val progress: Int,          // 0 → 100 (global)
    val subProgress: Int? = null, // 0 → 100 (dans le stage)
    val message: String? = null,
    val error: Throwable? = null
)
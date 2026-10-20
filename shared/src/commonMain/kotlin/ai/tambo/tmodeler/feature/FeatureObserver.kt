package ai.tambo.tmodeler.feature

fun interface FeatureObserver {
    fun onEvent(event: FeatureEvent)
}
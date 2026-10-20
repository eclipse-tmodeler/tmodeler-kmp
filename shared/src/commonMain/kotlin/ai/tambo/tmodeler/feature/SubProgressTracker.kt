package ai.tambo.tmodeler.feature

class SubProgressTracker(
    val total: Int
) {

    private var processed: Int = 0
    private var lastProgress: Int = 0

    fun update(inputSize: Int): Int {

        processed += inputSize

        val progress = (processed * 100) / total.coerceAtLeast(1)

        lastProgress = maxOf(lastProgress, progress)

        return lastProgress.coerceIn(0, 100)
    }

    fun reset() {
        processed = 0
        lastProgress = 0
    }
}
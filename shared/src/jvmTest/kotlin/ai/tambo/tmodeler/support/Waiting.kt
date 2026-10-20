package ai.tambo.tmodeler.support

import kotlin.test.fail

/** Attend (sans bloquer indéfiniment) qu'une condition devienne vraie ; échoue au bout de [timeoutMs]. */
fun awaitUntil(timeoutMs: Long = 10_000, message: String = "Condition non atteinte", condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        if (System.currentTimeMillis() > deadline) fail("$message (timeout ${timeoutMs}ms)")
        Thread.sleep(10)
    }
}

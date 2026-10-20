package ai.tambo.tmodeler.flow

import ai.tambo.tmodeler.field.operation.*
import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.scheduler.TFlow
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.awaitUntil
import ai.tambo.tmodeler.support.book
import ai.tambo.tmodeler.support.inData
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `TFlow` : charger une liste PAR LOTS et exposer son état (StateFlow) à l'interface.
 *
 * Règle d'or : toute liste d'écran se charge avec un TFlow, jamais avec un `all()` complet.
 *
 *   Mode.MANUAL → un lot par appel de `load` (défilement infini : on rappelle `load` en bas de liste)
 *   Mode.AUTO   → enchaîne les lots jusqu'à épuisement, puis `isComplete` passe à true
 *
 * Jeu de données : 5 livres « L1 » … « L5 », lots de 2.
 */
class TFlowTest : TModelerTest() {

    @BeforeTest
    fun seed() {
        inData { (1..5).forEach { book("L$it").save() } }
    }

    /** Charge un lot en mode MANUAL et attend sa fin (callback + fin interne du chargement). */
    private fun loadPage(flow: TFlow<Book>): List<Book> {
        var page: List<Book> = emptyList()
        val done = CountDownLatch(1)
        flow.load(onLoaded = { page = it; done.countDown() })
        assertTrue(done.await(10, TimeUnit.SECONDS), "Le lot n'a pas été chargé à temps")
        // onLoaded peut arriver avant la fin interne : on attend isLoading == false avant le load suivant.
        awaitUntil(message = "isLoading ne retombe pas") { !flow.isLoading.value }
        return page
    }

    @Test
    fun `MANUAL charge un lot par appel`() {
        val flow = TFlow(Book.tms, TFlow.Mode.MANUAL, 2)

        assertEquals(listOf("L1", "L2"), loadPage(flow).map { it.title_ })
        assertEquals(listOf("L3", "L4"), loadPage(flow).map { it.title_ })
        assertEquals(listOf("L5"), loadPage(flow).map { it.title_ })
    }

    @Test
    fun `MANUAL accumule les lots dans all`() {
        val flow = TFlow(Book.tms, TFlow.Mode.MANUAL, 2)

        loadPage(flow)
        assertEquals(2, flow.all.value.size)
        loadPage(flow)
        assertEquals(4, flow.all.value.size)
        loadPage(flow)
        assertEquals(5, flow.all.value.size)
    }

    @Test
    fun `MANUAL un lot au dela de la fin est vide`() {
        val flow = TFlow(Book.tms, TFlow.Mode.MANUAL, 5)

        assertEquals(5, loadPage(flow).size)
        assertTrue(loadPage(flow).isEmpty(), "Plus rien à charger → lot vide (hasMore = taille du lot >= taille de page)")
        assertEquals(5, flow.all.value.size)
    }

    @Test
    fun `AUTO charge tout puis passe a complete`() {
        val flow = TFlow(Book.tms, TFlow.Mode.AUTO, 2)
        assertFalse(flow.isComplete.value)

        flow.load()
        awaitUntil(message = "Le flow AUTO ne se termine pas") { flow.isComplete.value }

        assertEquals(5, flow.all.value.size)
        assertEquals(listOf("L1", "L2", "L3", "L4", "L5"), flow.all.value.map { it.title_ })
    }

    @Test
    fun `reset vide la liste et permet de recommencer`() {
        val flow = TFlow(Book.tms, TFlow.Mode.MANUAL, 2)
        loadPage(flow)
        assertEquals(2, flow.all.value.size)

        flow.reset()
        assertEquals(0, flow.all.value.size)

        assertEquals(listOf("L1", "L2"), loadPage(flow).map { it.title_ })
    }

    @Test
    fun `un filtre peut etre passe a load`() {
        val flow = TFlow(Book.tms, TFlow.Mode.MANUAL, 10)
        var page: List<Book> = emptyList()
        val done = CountDownLatch(1)

        flow.load(
            filter = { it.title like "L%" },
            order = { -it._id },
            onLoaded = { page = it; done.countDown() }
        )
        assertTrue(done.await(10, TimeUnit.SECONDS))

        assertEquals(listOf("L5", "L4", "L3", "L2", "L1"), page.map { it.title_ })
    }
}

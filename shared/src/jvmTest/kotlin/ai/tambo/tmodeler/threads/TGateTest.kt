package ai.tambo.tmodeler.threads

import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.scheduler.TGate
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.author
import ai.tambo.tmodeler.support.inData
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * `TGate` : exécuter du code sur le bon thread. Toute opération sur les données passe par `TGate.data`.
 *
 *   TGate.data { ... }        asynchrone, sur le thread de données (cas normal en production)
 *   TGate.dataAwait { ... }   suspend : attend la fin
 *   TGate.dataResult { ... }  suspend : attend et RETOURNE une valeur
 *
 * Ne touchez jamais à la base depuis le thread principal (UI).
 */
class TGateTest : TModelerTest() {

    @Test
    fun `data s'execute en asynchrone sur un autre thread`() {
        val caller = Thread.currentThread()
        var runner: Thread? = null
        val done = CountDownLatch(1)

        TGate.data {
            runner = Thread.currentThread()
            done.countDown()
        }

        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertNotEquals(caller, runner)
    }

    @Test
    fun `dataResult renvoie une valeur`() {
        val size = runBlocking {
            TGate.dataResult {
                Author.tms.create(author("Alice"), author("Bob"))
                Author.tms.size()
            }
        }
        assertEquals(2, size)
    }

    @Test
    fun `dataAwait attend la fin du bloc`() {
        runBlocking {
            TGate.dataAwait { author("Alice").save() }
        }
        // Si dataAwait n'avait pas attendu, la table pourrait encore être vide.
        assertEquals(1, inData { Author.tms.size() })
    }

    @Test
    fun `les blocs data sont executes les uns apres les autres`() {
        // Les écritures sont sérialisées par un verrou : 10 insertions concurrentes ne se perdent pas.
        val done = CountDownLatch(10)
        (1..10).forEach { i ->
            TGate.data {
                author("Auteur$i").save()
                done.countDown()
            }
        }

        assertTrue(done.await(20, TimeUnit.SECONDS))
        assertEquals(10, inData { Author.tms.size() })
    }
}

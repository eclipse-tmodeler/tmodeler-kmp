package ai.tambo.tmodeler.queries

import ai.tambo.tmodeler.field.operation.*
import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.samples.Status
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.author
import ai.tambo.tmodeler.support.book
import ai.tambo.tmodeler.support.inData
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Filtrer sur une relation ou une énumération.
 *
 *     Book.tms.with(b).lazy().filter(b.author eq alice).build()
 *
 * Pas de JOIN nécessaire : on compare directement la relation à un objet (ou une liste d'objets avec `into`).
 */
class RelationQueryTest : TModelerTest() {

    private val alice = author("Alice")
    private val bob = author("Bob")

    private fun seedBooks() {
        inData {
            alice.save()
            bob.save()
            book("A1", status = Status.PUBLISHED, author = alice).save()
            book("A2", status = Status.DRAFT, author = alice).save()
            book("B1", status = Status.PUBLISHED, author = bob).save()
        }
    }

    private fun titles(condition: (Book) -> Tx): List<String> = inData {
        val b = Book.tms.lambda
        Book.tms.with(b).lazy().filter(condition(b)).build().map { it.title_ }.sorted()
    }

    @Test
    fun `livres d'un auteur`() {
        seedBooks()
        assertEquals(listOf("A1", "A2"), titles { it.author eq alice })
        assertEquals(listOf("B1"), titles { it.author eq bob })
    }

    @Test
    fun `livres de plusieurs auteurs avec into`() {
        seedBooks()
        assertEquals(listOf("A1", "A2", "B1"), titles { it.author into listOf(alice, bob) })
    }

    @Test
    fun `filtre sur une enumeration`() {
        seedBooks()
        assertEquals(listOf("A1", "B1"), titles { it.status eq Status.PUBLISHED })
    }

    @Test
    fun `combiner relation et enumeration`() {
        seedBooks()
        assertEquals(listOf("A1"), titles { (it.author eq alice) and (it.status eq Status.PUBLISHED) })
    }
}

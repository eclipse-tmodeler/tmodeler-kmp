package ai.tambo.tmodeler.relations

import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.author
import ai.tambo.tmodeler.support.book
import ai.tambo.tmodeler.support.inData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Relation « un-à-un » : `var author = ModelField(Author::class)`.
 *
 * En base, la relation est stockée sous forme d'identifiant ; TModeler la « résout » en objet à la lecture
 * (résolution profonde) : `book.author_.name_` fonctionne directement.
 */
class ModelFieldTest : TModelerTest() {

    @Test
    fun `affecter une relation puis la relire a travers l'objet`() {
        val alice = author("Alice")
        inData { alice.save() }               // on enregistre l'auteur d'abord

        val b = book("TModeler", author = alice)
        inData { b.save() }

        val back = inData { Book.tms.get(b._id_)!! }
        assertEquals("Alice", back.author_.name_)
    }

    @Test
    fun `available indique si la relation est resolue`() {
        val b = book("Sans auteur")
        assertFalse(b.author.available(), "Aucun auteur affecté")

        val alice = author("Alice")
        inData { alice.save() }
        b.author_ = alice
        assertTrue(b.author.available(), "Auteur affecté")
    }

    @Test
    fun `une relation non affectee ne plante pas la lecture`() {
        val b = book("Orphelin")
        inData { b.save() }

        val back = inData { Book.tms.get(b._id_)!! }
        // Règle d'or : vérifier available() avant de déréférencer, plutôt qu'un try/catch.
        if (back.author.available()) {
            back.author_.name_
        }
        assertEquals("Orphelin", back.title_)
    }

    @Test
    fun `plusieurs objets partagent le meme auteur`() {
        val alice = author("Alice")
        inData { alice.save() }
        val b1 = book("Livre 1", author = alice)
        val b2 = book("Livre 2", author = alice)
        inData { b1.save(); b2.save() }

        val names = inData { Book.tms.all().map { it.author_.name_ } }
        assertEquals(listOf("Alice", "Alice"), names)
    }

    @Test
    fun `changer la relation`() {
        val alice = author("Alice")
        val bob = author("Bob")
        inData { alice.save(); bob.save() }
        val b = book("Livre", author = alice)
        inData { b.save() }

        b.author_ = bob
        inData { b.update() }

        assertEquals("Bob", inData { Book.tms.get(b._id_)!!.author_.name_ })
    }
}

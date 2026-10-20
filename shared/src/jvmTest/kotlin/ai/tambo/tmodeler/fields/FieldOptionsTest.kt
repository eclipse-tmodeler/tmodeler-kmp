package ai.tambo.tmodeler.fields

import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.samples.Status
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.inData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Les options de champ, chaînables à la déclaration (voir `samples/Models.kt`) :
 *
 *     var email  = TextField().unique()          // valeur unique dans la table
 *     var age    = IntField().default(0)         // valeur par défaut
 *     var status = EnumField(Status.values()).default(Status.DRAFT)
 *     var name   = TextField().notNull()         // obligatoire
 */
class FieldOptionsTest : TModelerTest() {

    @Test
    fun `une valeur par defaut est appliquee a l'enregistrement`() {
        // On n'affecte ni age, ni active, ni status : les défauts sont persistés.
        val saved = inData {
            Author().save {
                name_ = "Alice"
                email_ = "alice@example.com"
            }
        }
        val back = inData { Author.tms.get(saved._id_)!! }

        assertEquals(0L, back.age_)
        assertTrue(back.active_)
    }

    @Test
    fun `le defaut d'une enumeration est applique`() {
        val b = Book().also { it.title_ = "Sans statut" }
        inData { b.save() }

        assertEquals(Status.DRAFT, inData { Book.tms.get(b._id_)!!.status_ })
    }

    @Test
    fun `une valeur explicite remplace le defaut`() {
        val a = Author().also {
            it.name_ = "Alice"
            it.email_ = "alice@example.com"
            it.age_ = 18L
            it.active_ = false
        }
        inData { a.save() }

        val back = inData { Author.tms.get(a._id_)!! }
        assertEquals(18L, back.age_)
        assertEquals(false, back.active_)
    }
}

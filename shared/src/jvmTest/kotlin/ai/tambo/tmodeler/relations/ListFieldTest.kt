package ai.tambo.tmodeler.relations

import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.book
import ai.tambo.tmodeler.support.inData
import ai.tambo.tmodeler.support.tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Relation « un-à-plusieurs » : `var tags = ListField(Tag::class)`.
 *
 * ⚠️ Règle : enregistrer les éléments (ici les `Tag`) AVANT de les ajouter à la liste. La liste ne stocke que
 * leurs identifiants.
 */
class ListFieldTest : TModelerTest() {

    @Test
    fun `une liste neuve est vide`() {
        val b = book("Livre")
        assertTrue(b.tags.isEmpty)
        assertEquals(0, b.tags.size)
    }

    @Test
    fun `ajouter des elements puis les relire`() {
        val kotlin = tag("kotlin")
        val sql = tag("sql")
        inData { kotlin.save(); sql.save() }

        val b = book("TModeler")
        b.tags.add(kotlin)
        b.tags.add(sql)
        inData { b.save() }

        val back = inData { Book.tms.get(b._id_)!! }
        assertEquals(2, back.tags.size)
        assertEquals(listOf("kotlin", "sql"), back.tags_.map { it.label_ })
    }

    @Test
    fun `l'ordre des elements est conserve`() {
        val a = tag("a"); val b = tag("b"); val c = tag("c")
        inData { a.save(); b.save(); c.save() }

        val bk = book("Ordre")
        bk.tags.addAll(listOf(c, a, b))
        inData { bk.save() }

        val back = inData { Book.tms.get(bk._id_)!! }
        assertEquals(listOf("c", "a", "b"), back.tags_.map { it.label_ })
    }

    @Test
    fun `first et last`() {
        val a = tag("premier"); val b = tag("dernier")
        inData { a.save(); b.save() }

        val bk = book("Extremites")
        bk.tags.add(a)
        bk.tags.add(b)

        assertEquals("premier", bk.tags.first().label_)
        assertEquals("dernier", bk.tags.last().label_)
    }

    @Test
    fun `remove et clear`() {
        val a = tag("a"); val b = tag("b")
        inData { a.save(); b.save() }

        val bk = book("Retraits")
        bk.tags.add(a)
        bk.tags.add(b)
        assertEquals(2, bk.tags.size)

        bk.tags.remove(a)
        assertEquals(1, bk.tags.size)
        assertEquals("b", bk.tags.first().label_)

        bk.tags.clear()
        assertTrue(bk.tags.isEmpty)
    }

    @Test
    fun `une liste vide persiste comme vide`() {
        val b = book("Sans tags")
        inData { b.save() }

        assertEquals(0, inData { Book.tms.get(b._id_)!!.tags.size })
    }
}

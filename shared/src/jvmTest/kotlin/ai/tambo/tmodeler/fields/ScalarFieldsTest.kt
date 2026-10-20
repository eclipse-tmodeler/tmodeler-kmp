package ai.tambo.tmodeler.fields

import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.samples.Note
import ai.tambo.tmodeler.samples.Status
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.author
import ai.tambo.tmodeler.support.book
import ai.tambo.tmodeler.support.inData
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Les champs « scalaires » : texte, entier, décimal, booléen, énumération, date/heure, JSON.
 *
 * Chaque test enregistre un objet puis le RELIT depuis la base (`tms.get(_id_)`) : on vérifie donc que la
 * valeur a bien fait l'aller-retour dans SQLite, pas seulement qu'elle est en mémoire.
 */
class ScalarFieldsTest : TModelerTest() {

    @Test
    fun `TextField`() {
        val a = author("Alice")
        a.name_ = "Zoé d'Arc"       // les apostrophes et accents passent sans précaution particulière
        inData { a.save() }

        assertEquals("Zoé d'Arc", inData { Author.tms.get(a._id_)!!.name_ })
    }

    @Test
    fun `IntField est un Long`() {
        val a = author("Alice", age = 42)
        inData { a.save() }

        assertEquals(42L, inData { Author.tms.get(a._id_)!!.age_ })
    }

    @Test
    fun `FloatField est un Double`() {
        val a = author("Alice", rating = 4.25)
        inData { a.save() }

        assertEquals(4.25, inData { Author.tms.get(a._id_)!!.rating_ })
    }

    @Test
    fun `BoolField`() {
        val a = author("Alice", active = false)
        inData { a.save() }

        assertFalse(inData { Author.tms.get(a._id_)!!.active_ })
    }

    @Test
    fun `EnumField conserve la valeur de l'enumeration`() {
        val b = book("TModeler", status = Status.PUBLISHED)
        inData { b.save() }

        assertEquals(Status.PUBLISHED, inData { Book.tms.get(b._id_)!!.status_ })
    }

    @Test
    fun `TimeField conserve la date et l'heure`() {
        val n = Note()
        n.at_ = LocalDateTime(2026, 10, 19, 14, 30)
        inData { n.save() }

        val back = inData { Note.tms.get(n._id_)!!.at_ }
        assertEquals(2026, back.year)
        assertEquals(10, back.monthNumber)
        assertEquals(19, back.dayOfMonth)
        assertEquals(14, back.hour)
        assertEquals(30, back.minute)
    }

    @Test
    fun `JsonField conserve un objet JSON`() {
        val n = Note()
        n.payload_ = buildJsonObject {
            put("lang", "fr")
            put("level", 3)
        }
        inData { n.save() }

        val json = inData { Note.tms.get(n._id_)!!.payload_ }.jsonObject
        assertEquals("fr", json["lang"]!!.jsonPrimitive.content)
        assertEquals(3, json["level"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `les champs sont independants d'un objet a l'autre`() {
        val a = author("Alice", age = 20)
        val b = author("Bob", age = 50)
        inData { a.save(); b.save() }

        assertTrue(a.age_ < b.age_)
        assertEquals(20L, inData { Author.tms.get(a._id_)!!.age_ })
        assertEquals(50L, inData { Author.tms.get(b._id_)!!.age_ })
    }
}

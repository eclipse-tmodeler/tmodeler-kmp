package ai.tambo.tmodeler.gettingstarted

import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.scheduler.TmsScope
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.author
import ai.tambo.tmodeler.support.inData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ÉTAPE 3 — Créer, lire, modifier, supprimer (CRUD).
 *
 * `save()`, `delete()`, `Tms.all()`... ont besoin du « contexte de données » (`TmsScope`), qui garantit qu'on est
 * sur le bon thread. Il y a deux façons de le fournir :
 *
 *   1. appeler ces fonctions dans un bloc `TGate.data { ... }` (ici `inData`, qui attend le résultat) ;
 *   2. déclarer sa propre fonction avec `context(TmsScope)` : elle ne peut alors être appelée que depuis un
 *      bloc `TGate.data { ... }` ou depuis une autre fonction `context(TmsScope)`.
 *
 * Importez les extensions : `ai.tambo.tmodeler.ms.ext.*`.
 */
class Step3_CrudTest : TModelerTest() {

    // Façon n°2 : ma fonction exige le contexte de données, son appelant le fournit.
    context(TmsScope)
    private fun register(name: String): Author = author(name).also { it.save() }

    // ---------- Create ----------

    @Test
    fun `save enregistre un objet`() {
        inData { author("Alice").save() }
        assertEquals(1, inData { Author.tms.size() })
    }

    @Test
    fun `une fonction context TmsScope s'appelle depuis un bloc de donnees`() {
        val alice = inData { register("Alice") }
        assertTrue(alice._id_ > 0)
        assertEquals(1, inData { Author.tms.size() })
    }

    @Test
    fun `save avec un bloc renseigne les champs puis enregistre`() {
        val saved = inData {
            Author().save {
                name_ = "Alice"
                email_ = "alice@example.com"
            }
        }
        assertTrue(saved._id_ > 0)
        assertEquals("Alice", saved.name_)
    }

    @Test
    fun `create enregistre plusieurs objets d'un coup`() {
        inData { Author.tms.create(author("Alice"), author("Bob"), author("Carol")) }
        assertEquals(3, inData { Author.tms.size() })
    }

    @Test
    fun `save sur un objet deja enregistre le met a jour au lieu de le dupliquer`() {
        val a = author("Alice")
        inData { a.save() }
        a.name_ = "Alice 2"
        inData { a.save() }

        assertEquals(1, inData { Author.tms.size() })
        assertEquals("Alice 2", inData { Author.tms.get(a._id_)!!.name_ })
    }

    // ---------- Read ----------

    @Test
    fun `all renvoie tous les objets`() {
        inData { Author.tms.create(author("Alice"), author("Bob")) }
        val names = inData { Author.tms.all().map { it.name_ } }
        assertEquals(setOf("Alice", "Bob"), names.toSet())
    }

    @Test
    fun `first et last renvoient les extremites`() {
        inData { Author.tms.create(author("Alice"), author("Bob"), author("Carol")) }
        assertEquals("Alice", inData { Author.tms.first().name_ })
        assertEquals("Carol", inData { Author.tms.last().name_ })
    }

    @Test
    fun `get lit par cle locale`() {
        val a = author("Alice")
        inData { a.save() }

        val found = inData { Author.tms.get(a._id_) }
        assertNotNull(found)
        assertEquals("Alice", found.name_)
    }

    @Test
    fun `get d'une cle inconnue renvoie null`() {
        assertNull(inData { Author.tms.get(987_654L) })
    }

    @Test
    fun `size et isEmpty refletent le contenu`() {
        assertTrue(inData { Author.tms.isEmpty })
        inData { Author.tms.create(author("Alice"), author("Bob")) }
        assertEquals(2, inData { Author.tms.size() })
        assertTrue(inData { Author.tms.isNotEmpty })
    }

    // ---------- Update ----------

    @Test
    fun `update persiste la modification`() {
        val a = author("Alice", age = 30)
        inData { a.save() }

        a.age_ = 31L
        inData { a.update() }

        assertEquals(31L, inData { Author.tms.get(a._id_)!!.age_ })
    }

    // ---------- Delete ----------

    @Test
    fun `delete supprime un objet`() {
        val a = author("Alice")
        val b = author("Bob")
        inData { a.save(); b.save() }

        inData { a.delete() }

        assertEquals(1, inData { Author.tms.size() })
        assertNull(inData { Author.tms.get(a._id_) })
    }

    @Test
    fun `clear vide toute la table`() {
        inData { Author.tms.create(author("Alice"), author("Bob")) }
        inData { Author.tms.clear() }
        assertTrue(inData { Author.tms.isEmpty })
    }
}

package ai.tambo.tmodeler.gettingstarted

import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.samples.Status
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.author
import ai.tambo.tmodeler.support.inData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ÉTAPE 2 — Comprendre un modèle.
 *
 * Un modèle = une classe `TModel` dont chaque colonne est un champ. Pour chaque champ `xxx`, on déclare un accès
 * à la valeur `xxx_` (annoté @TMignore). Voir `samples/Models.kt`.
 *
 *   author.name   → le CHAMP   (pour les options et les requêtes : `t.name eq "Alice"`)
 *   author.name_  → la VALEUR  (pour lire/écrire : `author.name_ = "Alice"`)
 */
class Step2_ModelsTest : TModelerTest() {

    @Test
    fun `lire et ecrire la valeur d'un champ`() {
        val a = Author()
        a.name_ = "Alice"
        a.age_ = 30L

        assertEquals("Alice", a.name_)
        assertEquals(30L, a.age_)
    }

    @Test
    fun `un champ peut avoir une valeur par defaut`() {
        // age = IntField().default(0)  /  active = BoolField().default(true)
        val a = Author()
        assertEquals(0L, a.age_)
        assertTrue(a.active_)

        // EnumField(...).default(DRAFT)
        assertEquals(Status.DRAFT, Book().status_)
    }

    @Test
    fun `un objet neuf n'est pas encore en base`() {
        val a = author("Alice")
        assertFalse(a.isCreated(), "Pas encore enregistré : aucune clé locale")

        inData { a.save() }

        assertTrue(a.isCreated(), "Après save(), l'objet a une clé locale")
    }

    @Test
    fun `chaque modele recoit automatiquement les colonnes _id et id`() {
        // _id = clé LOCALE (auto-incrémentée) ; id = identifiant SERVEUR (vide tant qu'il n'y a pas de synchro).
        val a = author("Alice")
        inData { a.save() }

        assertTrue(a._id_ > 0, "_id local attribué à l'enregistrement")
        assertFalse(a.isServed(), "Sans serveur, l'objet n'a jamais d'id serveur")
    }

    @Test
    fun `deux objets enregistres ont des cles locales distinctes`() {
        val a = author("Alice")
        val b = author("Bob")
        inData { a.save(); b.save() }

        assertTrue(a._id_ != b._id_)
    }
}

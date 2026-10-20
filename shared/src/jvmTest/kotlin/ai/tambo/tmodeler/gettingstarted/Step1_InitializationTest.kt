package ai.tambo.tmodeler.gettingstarted

import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.inData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * ÉTAPE 1 — Démarrer TModeler.
 *
 * Dans une vraie app, on appelle une seule fois, au lancement du processus :
 *
 *     TModeler.start().connect(
 *         Tdb.Builder()
 *             .type(Tdb.Type.SQLITE)
 *             .dbDir("db/").dbName("MonApp")
 *             .accept(Author().clazz().getPackage())    // un package de modèles
 *             .start()!!
 *     )
 *     Author.tms.commit()                              // crée la table
 *
 * Ici, c'est la classe `TestApp` (dossier support) qui fait ce travail : lisez-la comme un exemple complet.
 * Ce test vérifie que le démarrage a produit ce qu'on attend.
 */
class Step1_InitializationTest : TModelerTest() {

    @Test
    fun `TModeler est un singleton`() {
        // start() renvoie toujours la même instance : on peut l'appeler de n'importe où.
        assertSame(TModeler.start(), TModeler.start())
    }

    @Test
    fun `les tables des modeles existent apres commit`() {
        assertTrue(Author.tms.exists, "La table Author doit exister")
        assertTrue(Book.tms.exists, "La table Book doit exister")
    }

    @Test
    fun `chaque gestionnaire connait le nom de sa table`() {
        assertTrue(Author.tms.name().isNotBlank())
        assertTrue(Book.tms.name().isNotBlank())
    }

    @Test
    fun `une table fraichement creee est vide`() {
        // Les opérations sur les données se font toujours dans un bloc TGate.data ;
        // `inData` (dossier support) est sa version « j'attends le résultat » pour les tests.
        val empty = inData { Author.tms.isEmpty }
        assertTrue(empty)
        assertEquals(0, inData { Author.tms.size() })
    }

    @Test
    fun `les moteurs par defaut sont presents sans aucune extension`() {
        val tm = TModeler.start()
        assertNotNull(tm.syncEngine())
        assertNotNull(tm.securityEngine())
        assertNotNull(tm.aiEngine())
    }
}

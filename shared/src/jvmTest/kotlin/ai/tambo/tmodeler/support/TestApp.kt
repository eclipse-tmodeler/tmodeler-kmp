package ai.tambo.tmodeler.support

import ai.tambo.tmodeler.db.Tdb
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.samples.Book
import ai.tambo.tmodeler.samples.Note
import ai.tambo.tmodeler.samples.Tag
import java.io.File
import java.nio.file.Files
import kotlin.test.BeforeTest

/**
 * Démarrage de TModeler pour les tests : une base SQLite temporaire, aucun serveur, aucune extension.
 *
 * TModeler est initialisé UNE SEULE FOIS par processus (c'est un singleton) ; [reset] vide ensuite les tables
 * avant chaque test pour qu'ils soient indépendants.
 */
object TestApp {
    private var started = false

    @Synchronized
    fun ready() {
        if (started) return

        val dir = Files.createTempDirectory("tmodeler-tests").toFile().also { it.deleteOnExit() }
        val dbPath = File(dir, "tests").path          // le driver JVM ajoute l'extension .db

        TModeler.start().connect(
            Tdb.Builder()
                .type(Tdb.Type.SQLITE)
                .dbDir(dir.path)
                .dbName(dbPath)
                .server(TServerConfigBase())            // app 100 % locale : configuration de base
                .accept(Author().clazz().getPackage())  // package qui contient les modèles
                .start()!!
        )

        inData {
            // commit() crée les tables si elles n'existent pas
            Author.tms.commit()
            Tag.tms.commit()
            Book.tms.commit()
            Note.tms.commit()
        }
        started = true
    }

    private var datasetOwner: Class<*>? = null

    /** Pour les classes de tests en lecture seule : le jeu de données n'est préparé qu'une fois par classe. */
    @Synchronized
    fun seedOnce(owner: Class<*>, seed: () -> Unit) {
        if (datasetOwner == owner) return
        reset()
        seed()
        datasetOwner = owner
    }

    fun reset() {
        datasetOwner = null
        inData {
            Book.tms.clear()
            Tag.tms.clear()
            Author.tms.clear()
            Note.tms.clear()
        }
    }
}

/** Classe de base : démarre TModeler une fois et repart d'une base vide avant chaque test. */
abstract class TModelerTest {
    /** `true` pour une classe qui ne fait que lire un jeu de données préparé via [TestApp.seedOnce]. */
    protected open val sharedDataset: Boolean = false

    @BeforeTest
    fun prepareDatabase() {
        TestApp.ready()
        if (!sharedDataset) TestApp.reset()
    }
}

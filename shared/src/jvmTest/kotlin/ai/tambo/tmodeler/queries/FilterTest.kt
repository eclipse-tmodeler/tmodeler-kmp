package ai.tambo.tmodeler.queries

import ai.tambo.tmodeler.field.operation.*
import ai.tambo.tmodeler.ms.ext.*
import ai.tambo.tmodeler.samples.Author
import ai.tambo.tmodeler.support.TModelerTest
import ai.tambo.tmodeler.support.TestApp
import ai.tambo.tmodeler.support.inData
import ai.tambo.tmodeler.support.seedAuthors
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Interroger les données.
 *
 * Anatomie d'une requête :
 *
 *     val t = Author.tms.lambda                    // « t » représente la table dans l'expression
 *     Author.tms
 *         .with(t)                                  // sur quelle table
 *         .lazy()                                   // mode requête
 *         .filter((t.age gt 25L) and (t.active eq true))
 *         .build()                                  // exécute → liste
 *
 * Jeu de données : voir `seedAuthors()` dans support/Fixtures.kt (Alice 30, Bob 25, Carol 35, Dave 40 inactif,
 * Eve 22, Frank 30).
 */
class FilterTest : TModelerTest() {
    override val sharedDataset = true

    @BeforeTest
    fun seed() {
        TestApp.seedOnce(javaClass) { seedAuthors() }
    }

    /** Exécute un filtre et renvoie les noms trouvés (triés pour des assertions stables). */
    private fun names(condition: (Author) -> Tx): List<String> = inData {
        val t = Author.tms.lambda
        Author.tms.with(t).lazy().filter(condition(t)).build().map { it.name_ }.sorted()
    }

    @Test
    fun `eq egal`() = assertEquals(listOf("Bob"), names { it.name eq "Bob" })

    @Test
    fun `ne different`() =
        assertEquals(listOf("Alice", "Bob", "Carol", "Dave", "Eve"), names { it.name ne "Frank" })

    @Test
    fun `gt plus grand que`() = assertEquals(listOf("Carol", "Dave"), names { it.age gt 30L })

    @Test
    fun `lt plus petit que`() = assertEquals(listOf("Bob", "Eve"), names { it.age lt 30L })

    @Test
    fun `gte plus grand ou egal`() =
        assertEquals(listOf("Alice", "Carol", "Dave", "Frank"), names { it.age gte 30L })

    @Test
    fun `lte plus petit ou egal`() =
        assertEquals(listOf("Alice", "Bob", "Eve", "Frank"), names { it.age lte 30L })

    @Test
    fun `comparaison sur un decimal`() = assertEquals(listOf("Alice", "Carol"), names { it.rating gte 4.5 })

    @Test
    fun `like motif`() = assertEquals(listOf("Alice"), names { it.name like "Al%" })

    @Test
    fun `like avec joker au debut`() = assertEquals(listOf("Alice", "Dave", "Eve"), names { it.name like "%e" })

    @Test
    fun `into appartient a une liste`() =
        assertEquals(listOf("Bob", "Eve"), names { it.age into listOf(22L, 25L) })

    @Test
    fun `outOf n'appartient pas a la liste`() =
        assertEquals(listOf("Alice", "Carol", "Dave", "Frank"), names { it.age outOf listOf(22L, 25L) })

    @Test
    fun `and combine deux conditions`() =
        assertEquals(listOf("Alice", "Frank"), names { (it.age eq 30L) and (it.rating lt 5.0) })

    @Test
    fun `or accepte l'une ou l'autre`() =
        assertEquals(listOf("Dave", "Eve"), names { (it.age lt 23L) or (it.age gt 39L) })

    @Test
    fun `filtre sur un booleen`() = assertEquals(listOf("Dave"), names { it.active eq false })

    @Test
    fun `un filtre sans resultat renvoie une liste vide`() = assertEquals(emptyList(), names { it.name eq "Nobody" })

    @Test
    fun `filter sans argument renvoie tout`() {
        val count = inData {
            val t = Author.tms.lambda
            Author.tms.with(t).lazy().filter().build().size
        }
        assertEquals(6, count)
    }
}

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
 * Trier et paginer.
 *
 *     .order(+t.age)     // croissant        .order(-t.age)   // décroissant
 *     .sub(25, 0)        // 25 résultats à partir de la position 0 (pagination)
 *
 * L'ordre d'insertion du jeu de données est : Alice, Bob, Carol, Dave, Eve, Frank (clés locales croissantes).
 */
class OrderAndPaginationTest : TModelerTest() {
    override val sharedDataset = true

    @BeforeTest
    fun seed() {
        TestApp.seedOnce(javaClass) { seedAuthors() }
    }

    private fun names(sort: (Author) -> Tx, size: Int? = null, offset: Int = 0): List<String> = inData {
        val t = Author.tms.lambda
        val ordered = Author.tms.with(t).lazy().filter().order(sort(t))
        val page = if (size != null) ordered.sub(size, offset) else ordered
        page.build().map { it.name_ }
    }

    @Test
    fun `tri croissant sur la cle locale`() =
        assertEquals(listOf("Alice", "Bob", "Carol", "Dave", "Eve", "Frank"), names({ +it._id }))

    @Test
    fun `tri decroissant sur la cle locale`() =
        assertEquals(listOf("Frank", "Eve", "Dave", "Carol", "Bob", "Alice"), names({ -it._id }))

    @Test
    fun `tri decroissant sur un champ`() {
        val oldestFirst = names({ -it.age })
        assertEquals("Dave", oldestFirst.first())   // 40 ans
        assertEquals("Eve", oldestFirst.last())     // 22 ans
    }

    @Test
    fun `sub limite le nombre de resultats`() =
        assertEquals(listOf("Alice", "Bob"), names({ +it._id }, size = 2))

    @Test
    fun `sub avec un decalage pagine les resultats`() {
        assertEquals(listOf("Alice", "Bob"), names({ +it._id }, size = 2, offset = 0))
        assertEquals(listOf("Carol", "Dave"), names({ +it._id }, size = 2, offset = 2))
        assertEquals(listOf("Eve", "Frank"), names({ +it._id }, size = 2, offset = 4))
    }

    @Test
    fun `une page au dela de la fin est vide`() =
        assertEquals(emptyList(), names({ +it._id }, size = 2, offset = 6))

    @Test
    fun `une derniere page incomplete`() =
        assertEquals(listOf("Frank"), names({ +it._id }, size = 5, offset = 5))

    @Test
    fun `filtrer puis trier puis paginer`() {
        val result = inData {
            val t = Author.tms.lambda
            Author.tms.with(t).lazy()
                .filter(t.active eq true)       // 5 auteurs actifs
                .order(-t.age)                  // du plus âgé au plus jeune : Carol 35, Alice 30, Frank 30, Bob 25, Eve 22
                .sub(2, 0)
                .build()
                .map { it.name_ }
        }
        assertEquals("Carol", result.first())
        assertEquals(2, result.size)
    }
}

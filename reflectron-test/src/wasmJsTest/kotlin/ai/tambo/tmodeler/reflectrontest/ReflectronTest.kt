package ai.tambo.tmodeler.reflectrontest

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.db.Tdb
import ai.tambo.tmodeler.db.TsqliteDriver
import ai.tambo.tmodeler.model.TModelRegistry
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.ms.Tms
import ai.tambo.tmodeler.ms.ext.igetByKeys
import ai.tambo.tmodeler.reflectrontest.generated.registerAllTModels
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReflectronTest {
    @BeforeTest
    fun register() {
        registerAllTModels()
    }

    @Test
    fun generatedMetadataIncludesInheritedAndCoreFieldsButNotDelegates() {
        val ping = TestPing()
        val clazz = ping.clazz()
        assertEquals(
            setOf("_id", "id", "meta", "_state", "state", "linkers", "content", "count", "inherited"),
            clazz.fieldsKeys().toSet()
        )
        assertEquals(clazz.fieldsKeys().size, clazz.fieldsKeys().distinct().size)
        assertEquals("ai_tambo_tmodeler_reflectrontest_TestPing", clazz.modelName())
        assertSame(ping.content, clazz.getField("content"))
        assertSame(ping.inherited, clazz.getField("inherited"))
        assertSame(ping._id, clazz.getField("_id"))
        assertSame(clazz, ping.clazz())
        val other = TestPing()
        assertSame(other.content, ping.clazz(other).getField("content"))
        assertNotSame(ping.content, other.clazz().getField("content"))
    }

    @Test
    fun parsingThreeRowsNeverAliasesInstancesOrFields() {
        val prototype = TestPing()
        val rows = TobGb.parse(
            prototype,
            """[{"content":"one","count":1},{"content":"two","count":2},{"content":"three","count":3}]"""
        )
        assertEquals(listOf("one", "two", "three"), rows.map { it.content_ })
        assertEquals(listOf(1L, 2L, 3L), rows.map { it.count_ })
        for (i in rows.indices) {
            assertNotSame(prototype, rows[i])
            for (j in 0 until i) {
                assertNotSame(rows[i], rows[j])
                assertNotSame(rows[i].content, rows[j].content)
            }
        }
        rows.first().content_ = "changed"
        assertEquals("two", rows[1].content_)
        val clone = rows[1].clone()
        assertNotSame(rows[1], clone)
        assertEquals("two", clone.content_)
    }

    @Test
    fun generatedFactoriesSupportRelationsAndSelfReferences() {
        val envelope = TModelRegistry.instanceFor(TestEnvelope::class)
        assertTrue(envelope.ping.createInstance() is TestPing)
        assertNotSame(envelope.ping.createInstance(), envelope.ping.createInstance())
        assertTrue(envelope.pings.createInstance() is TestPing)
        assertTrue(TestNode().next.createInstance() is TestNode)
    }

    @Test
    fun classKeysAndPackagesDistinguishIdenticalSimpleNames() {
        val first = TModelRegistry.instanceFor(TestPing::class)
        val other = TModelRegistry.instanceFor(ai.tambo.tmodeler.reflectrontest.other.TestPing::class)
        assertFalse(first.clazz().modelName() == other.clazz().modelName())
        assertSame(other.other, other.clazz().getField("other"))
        val nested = TModelRegistry.instanceFor(Container.NestedPing::class)
        assertSame(nested.`when`, nested.clazz().getField("when"))
    }

    @Test
    fun tmsCrudPersistsAcrossDriverReopening() {
        val driver = TsqliteDriver()
        val db = Tdb(driver).apply { dbName = "reflectron_smoke" }
        TModeler.getInstance().connect(db)
        val tms = Tms(TestPing::class)
        assertTrue(tms.ready, "Generated metadata must prepare the model table")
        assertTrue(tms.clear())
        val rows = (1..3).map { number ->
            TestPing().apply {
                content_ = "ping-$number"
                count_ = number.toLong()
                assertEquals(1, isave(), "Create ping-$number")
            }
        }
        val keys = rows.map { it._id_ }
        assertEquals(3, keys.distinct().size)
        assertEquals(listOf("ping-1", "ping-2", "ping-3"), tms.igetByKeys(keys).map { it.content_ })
        rows[1].content_ = "updated"
        assertEquals(1, rows[1].isave())
        assertEquals("updated", tms.igetByKeys(listOf(keys[1])).single().content_)
        driver.close()
        val reopened = TsqliteDriver()
        TModeler.getInstance().connect(Tdb(reopened).apply { dbName = "reflectron_smoke" })
        val reloaded = Tms(TestPing::class)
        assertTrue(reloaded.ready)
        assertEquals(listOf("ping-1", "updated", "ping-3"), reloaded.igetByKeys(keys).map { it.content_ })
        val loaded = reloaded.igetByKeys(listOf(keys.first())).single()
        loaded.idelete()
        assertTrue(reloaded.igetByKeys(listOf(keys.first())).isEmpty())
        assertEquals(2, reloaded.igetByKeys(keys).size)
        assertTrue(reloaded.clear())
    }
}

package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.field.common.TextField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TModelRegistryTest {
    class Registered : TBaseModel() {
        val content = TextField()
    }

    class Missing : TBaseModel()

    @Test
    fun factoriesAndFieldsAreBoundToEachInstance() {
        TModelRegistry.register(
            Registered::class,
            factory = { Registered() },
            fieldsOf = { listOf(it.content.apply { name = "content" }) },
            packageName = "test.models"
        )
        val first = TModelRegistry.instanceFor(Registered::class)
        val second = TModelRegistry.instanceFor(Registered::class)
        assertNotSame(first, second)
        val parent = Tclass(TBaseModel::class).fields(first._id.apply { name = "_id" })
        val clazz = TModelRegistry.clazzFor(Registered::class, first, parent)
        assertSame(parent, clazz.sup())
        assertSame(first.content, clazz.getField("content"))
        assertSame(first._id, clazz.getField("_id"))
        assertEquals("test_models_Registered", clazz.modelName())
        assertSame(second.content, TModelRegistry.clazzFor(Registered::class, second, null).getField("content"))
    }

    @Test
    fun missingRegistrationFailsExplicitly() {
        val error = assertFailsWith<IllegalStateException> {
            TModelRegistry.instanceFor(Missing::class)
        }
        assertTrue(error.message.orEmpty().contains("registerAllTModels()"))
        assertFailsWith<IllegalStateException> {
            TModelRegistry.clazzFor(Missing::class, Missing(), null)
        }
    }
}

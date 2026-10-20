package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.field.advanced.ListFieldGb
import ai.tambo.tmodeler.field.advanced.ModelFieldGb
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ContextReceiverDelegationTest {
    class ModelProbe : ModelFieldGb<TBaseModel>(TBaseModel::class) {
        var calls = 0

        context(TmsScope)
        override fun resolveDeepLink() {
            calls++
        }
    }

    class ListProbe : ListFieldGb<TBaseModel>(TBaseModel::class) {
        var calls = 0

        context(TmsScope)
        override fun resolveDeepLink() {
            calls++
        }
    }

    class SaveProbe : TModelGb<SaveProbe>() {
        var saves = 0

        override fun isave(onServed: Any?): Int {
            saves++
            return 1
        }
    }

    @Test
    fun explicitDelegationPreservesVirtualDispatchAndNoOpFields() {
        val modelField = ModelProbe()
        val listField = ListProbe()
        val text = TextField().apply { set("unchanged") }
        val model = TBaseModel().apply {
            setClazz(Tclass(TBaseModel::class).fields(modelField, listField, text))
        }
        with(TmsScope()) {
            model.resolveDeepLink()
            text.resolveDeepLink()
        }
        assertEquals(1, modelField.calls)
        assertEquals(1, listField.calls)
        assertEquals("unchanged", text.get())
    }

    @Test
    fun saveThroughDeclaringTypeKeepsReceiverAndSaveDispatch() {
        val model = SaveProbe()
        val declaringType: TModel0<*> = model
        var blocks = 0
        with(TmsScope()) {
            val saved = declaringType.save {
                assertSame(model, this)
                blocks++
            }
            assertSame(model, saved)
        }
        assertEquals(1, blocks)
        assertEquals(1, model.saves)
    }
}

package ai.tambo.tmodeler.reflectrontest.other

import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.model.TModel

class TestPing : TModel<TestPing>() {
    val other = TextField()
}

package ai.tambo.tmodeler.reflectrontest

import ai.tambo.tmodeler.field.advanced.ListField
import ai.tambo.tmodeler.field.advanced.ModelField
import ai.tambo.tmodeler.field.common.IntField
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.model.TMignore
import ai.tambo.tmodeler.model.TModel
import ai.tambo.tmodeler.ms.Tms

abstract class PingBase<T : PingBase<T>> : TModel<T>() {
    var inherited = TextField()
    @TMignore var inherited_ by inherited
}

class TestPing : PingBase<TestPing>() {
    var content = TextField()
    var count = IntField()
    @TMignore var ignored = TextField()
    @TMignore var content_ by content
    @TMignore var count_ by count

    companion object {
        val tms by lazy { Tms(TestPing::class) }
    }
}

class TestEnvelope : TModel<TestEnvelope>() {
    var ping = ModelField(TestPing::class)
    var pings = ListField(TestPing::class)
    @TMignore var ping_ by ping
    @TMignore var pings_ by pings
}

class TestNode : TModel<TestNode>() {
    var next = ModelField(TestNode::class)
    @TMignore var next_ by next
}

class Container {
    class NestedPing : TModel<NestedPing>() {
        val `when` = TextField()
    }
}

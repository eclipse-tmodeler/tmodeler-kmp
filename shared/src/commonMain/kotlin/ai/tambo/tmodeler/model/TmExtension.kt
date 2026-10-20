package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.field.operation.Top.Equal
import ai.tambo.tmodeler.field.operation.Top.In
import ai.tambo.tmodeler.field.operation.Top.NotEqual
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.operation.listToString


infix fun TBaseModel.family(other: TBaseModel?): Boolean {
    if (other == null) return false
    if (this === other) return true
    return this.clazz().modelName() == other.clazz().modelName()
}




infix fun <T : TModel0<T>> TModel0<T>.outOf(values: List<T>) = !into(values)
infix fun <T : TModel0<T>> TModel0<T>.into(values: List<T>): Tx {
    if(values.isEmpty()) return Tx()
    val v = values.first()

    return if(v.isServed()){
        Tx(this.id, In, listToString(values.filter { it.isServed() }.map { it.id_ }))
    }else{
        Tx(this._id, In, listToString(values.filter { it.isSaved() }.map { it._id_ }))
    }
}

infix fun <T : TModel0<T>> TModel0<T>.eq(v: T): Tx {
    return if(v.isServed()){
        Tx(this.id, Equal, v.id_)
    }else{
        Tx(this._id, Equal, v._id_)
    }
}

infix fun <T : TModel0<T>> TModel0<T>.ne(v: T): Tx {
    return if(v.isServed()){
        Tx(this.id, NotEqual, v.id_)
    }else{
        Tx(this._id, NotEqual, v._id_)
    }
}
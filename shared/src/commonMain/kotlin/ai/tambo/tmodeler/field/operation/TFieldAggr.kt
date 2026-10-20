@file:Suppress("UNCHECKED_CAST")

package ai.tambo.tmodeler.field.operation

import ai.tambo.tmodeler.field.base.TField

// Extension pour TField<T, D>
fun <T, D : TField<T, D>> TField<T, D>.max(): D {
    this.aggr = Tx(this, Top.Max)
    return this as D
}

fun <T, D : TField<T, D>> TField<T, D>.min(): D {
    this.aggr = Tx(this, Top.Min)
    return this as D
}

fun <T, D : TField<T, D>> TField<T, D>.avg(): D {
    this.aggr = Tx(this, Top.Avg)
    return this as D
}

fun <T, D : TField<T, D>> TField<T, D>.sum(): D {
    this.aggr = Tx(this, Top.Sum)
    return this as D
}

fun <T, D : TField<T, D>> TField<T, D>.count(): D {
    this.aggr = Tx(this, Top.Count)
    return this as D
}

fun <T, D : TField<T, D>> TField<T, D>.countDistinct(): D {
    this.aggr = Tx(this, Top.CountDistinct)
    return this as D
}
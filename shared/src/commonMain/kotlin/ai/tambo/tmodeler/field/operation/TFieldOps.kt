package ai.tambo.tmodeler.field.operation

import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.operation.Top.*

// Opérations avec des valeurs
infix fun <T, D : TField<T, D>> TField<T, D>.gt(v: T): Tx = Tx(this, GreaterThan, v)
infix fun <T, D : TField<T, D>> TField<T, D>.lt(v: T): Tx = Tx(this, LessThan, v)
infix fun <T, D : TField<T, D>> TField<T, D>.eq(v: T?) = ieq(v)
infix fun <T, D : TField<T, D>> TField<T, D>.ne(v: T?) = ine(v)
infix fun <T, D : TField<T, D>> TField<T, D>.gte(v: T): Tx = Tx(this, GreaterThanEqual, v)
infix fun <T, D : TField<T, D>> TField<T, D>.lte(v: T): Tx = Tx(this, LessThanEqual, v)
infix fun <T, D : TField<T, D>> TField<T, D>.like(pattern: T): Tx = Tx(this, Like, pattern)
infix fun <T, D : TField<T, D>> TField<T, D>.into(values: List<T>) = iinto(values)
infix fun <T, D : TField<T, D>> TField<T, D>.outOf(values: List<T>) = !into(values)

// Opérations avec un autre champ (field-to-field)
infix fun <T, D : TField<T, D>> TField<T, D>.gt(other: BaseField): Tx = Tx(this, GreaterThan, other)
infix fun <T, D : TField<T, D>> TField<T, D>.lt(other: BaseField): Tx = Tx(this, LessThan, other)
infix fun <T, D : TField<T, D>> TField<T, D>.eq(other: BaseField): Tx = Tx(this, Equal, other)
infix fun <T, D : TField<T, D>> TField<T, D>.ne(other: BaseField): Tx = Tx(this, NotEqual, other)
infix fun <T, D : TField<T, D>> TField<T, D>.gte(other: BaseField): Tx = Tx(this, GreaterThanEqual, other)
infix fun <T, D : TField<T, D>> TField<T, D>.lte(other: BaseField): Tx = Tx(this, LessThanEqual, other)

// Asc / Desc
operator fun <T, D : TField<T, D>> TField<T, D>.unaryPlus(): Tx = Tx(this, Asc)
operator fun <T, D : TField<T, D>> TField<T, D>.unaryMinus(): Tx = Tx(this, Desc)

// isNull / notNull
fun <T, D : TField<T, D>> TField<T, D>.isNull(): Tx = Tx(this, IsNull)
//fun <T, D : TField<T, D>> TField<T, D>.notNull(): Tx = !this.isNull()

fun <T> listToString(
    vec: List<T>,
    elementFormatter: (T) -> String = { it.toString() }
): String {
    return vec.joinToString(
        prefix = "(",
        postfix = ")",
        separator = ", "
    ) { elementFormatter(it) }
}
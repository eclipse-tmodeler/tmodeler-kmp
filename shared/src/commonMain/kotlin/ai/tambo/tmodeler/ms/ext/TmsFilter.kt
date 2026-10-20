package ai.tambo.tmodeler.ms.ext

import ai.tambo.tmodeler.core.FieldList
import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.field.operation.Top
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.operation.notify
import ai.tambo.tmodeler.field.operation.splitOrderExpr
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.filter.JoinClause
import ai.tambo.tmodeler.ms.filter.JoinType
import ai.tambo.tmodeler.ms.filter.OrderByClause
import ai.tambo.tmodeler.ms.filter.TFilterBuilder
import ai.tambo.tmodeler.ms.filter.WhereCondition
import ai.tambo.tmodeler.ms.result.Tlist
import ai.tambo.tmodeler.ms.utils.Ttuple


fun <T : TBaseModel> TmsGb<T>.filter(): Tlist<T> {
    if(_builder==null){
        _builder = TFilterBuilder()
        _builder!!.from(name())
    }

    return ibuild()
}
fun <T : TBaseModel> TmsGb<T>.filter(x: Tx): Tlist<T> {
    return when {
        _tuplet != null -> filter(_tuplet!!, x)
        _instance != null -> filter(_instance!!, x)
        else -> filter()
    }
}
fun <T : TBaseModel> TmsGb<T>.filter(tuplet: Ttuple, tx: Tx): Tlist<T> {
    apply(tuplet, tx)

    if (tx.isJoining()) {
        _builder!!.joins(listOf(
            JoinClause(joinType.str(), tx.foreignModel?:"---"/*tx.targetModel*/, tx.toString())
        ))
    } else if (tx.isAggr()) {
        _builder!!.having(tx.toString())
    } else {
        _builder!!.where(listOf(
            WhereCondition("", "", tx.toString())
        ))
    }

    return filter()
}
fun <T : TBaseModel> TmsGb<T>.filter(item: T, tx: Tx): Tlist<T> {
    fillTx(tx, name(), item.clazz().getFields())

    if (tx.isAggr()) {
        _builder!!.having(tx.toString())
    } else {
        _builder!!.where(listOf(
            WhereCondition("", "", tx.toString())
        ))
    }

    return filter()
}


fun <T : TBaseModel> TmsGb<T>.mergeLeft(x: Tx): Tlist<T> {
    join(JoinType.LEFT)
    return iMerge(x)
}
fun <T : TBaseModel> TmsGb<T>.mergeRight(x: Tx): Tlist<T> {
    join(JoinType.RIGHT)
    return iMerge(x)
}
fun <T : TBaseModel> TmsGb<T>.mergeFull(x: Tx): Tlist<T> {
    join(JoinType.FULL)
    return iMerge(x)
}
fun <T : TBaseModel> TmsGb<T>.mergeCross(x: Tx): Tlist<T> {
    join(JoinType.CROSS)
    return iMerge(x)
}
fun <T : TBaseModel> TmsGb<T>.mergeSelf(x: Tx): Tlist<T> {
    join(JoinType.LEFT)
    return iMerge(x)
}
fun <T : TBaseModel> TmsGb<T>.merge(x: Tx): Tlist<T> {
    join(JoinType.INNER)
    return iMerge(x)
}
private fun <T : TBaseModel> TmsGb<T>.iMerge(x: Tx): Tlist<T> {
    x.joining()
    return when {
        _tuplet != null -> filter(_tuplet!!, x)
        _instance != null -> filter(_instance!!, x)
        else -> filter()
    }
}


fun <T : TBaseModel> TmsGb<T>.order(tx: Tx): Tlist<T> {
    return if (_tuplet != null) {
        order(_tuplet!!, tx)
    } else {
        order(_instance!!, tx)
    }
}
fun <T : TBaseModel> TmsGb<T>.order(item: T, tx: Tx): Tlist<T> {
    fillTx(tx, name(), item.clazz().getFields())

    _builder!!.orderBy(OrderByClause(listOf(tx.toString())))

    return ibuild()
}
fun <T : TBaseModel> TmsGb<T>.order(tuplet: Ttuple, tx: Tx): Tlist<T> {
    val v = tx.field!!._functions.size
    apply(tuplet, tx)
    val v0 = tx.field!!._functions.size

    if (tx.hasIntegratedFunc()) {
        val v2 = tx.field!!._functions.size
        val (orderExpr, direction) = tx.splitOrderExpr()
        val v3 = tx.field!!._functions.size

        val alias = "__distance"

        _builder!!.addSelect("$orderExpr AS $alias")
        _builder!!.orderBy(OrderByClause(listOf("$alias $direction")))
    } else {
        _builder!!.orderBy(OrderByClause(listOf(tx.toString())))
    }

    return ibuild()
}



fun <T : TBaseModel> TmsGb<T>.group(vararg args: BaseField): Tlist<T> {
    val item = _instance!!

    args.forEach { arg ->
        if (arg !is BaseField) { // j'envois bien un derived de BaseField, mais un breakpoint me montre que ici arg est un Object qui contient un JsonField
            throw IllegalArgumentException("All args must derive from BaseField")
        }
        val tx = Tx(arg, Top.GroupBy)
        if (_tuplet != null) {
            groupByTuple(_tuplet!!, tx)
        } else {
            groupByItem(item, tx)
        }
    }
    return ibuild()
}
fun <T : TBaseModel> TmsGb<T>.groupByItem(item: T, x: Tx) {
    fillTx(x, name(), item.clazz().getFields())
    _builder!!.addGroupBy(x.toString())
}
fun <T : TBaseModel> TmsGb<T>.groupByTuple(tuplet: Ttuple, tx: Tx) {
    apply(tuplet, tx)
    _builder!!.addGroupBy(tx.toString())
}



fun <T : TBaseModel> TmsGb<T>.fillTx(x: Tx, modelName: String, modelFields: FieldList) {
    var bf: BaseField? = null
    for (attr in modelFields) {
        bf = attr as? BaseField ?: continue

        x.notify(bf, modelName, "$modelName.${attr.name}")
    }
}
fun <T : TBaseModel> TmsGb<T>.apply(tuple: Ttuple, tx: Tx) {
    tuple.values.forEach { element ->
        if (element != null) {
            val elt = element
            val clazz = (elt as? TBaseModel)?.clazz() ?: return@forEach
            val name = clazz.modelName()
            val fields = clazz.getFields()

            fillTx(tx, name, fields)
        }
    }
}


fun <T : TBaseModel> TmsGb<T>.sub(size:Int, offset:Int=0): Tlist<T> {
    _builder!!.limit(size)
    _builder!!.offset(offset)
    return ibuild()
}
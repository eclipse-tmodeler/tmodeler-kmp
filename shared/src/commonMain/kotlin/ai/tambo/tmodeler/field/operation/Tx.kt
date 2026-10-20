package ai.tambo.tmodeler.field.operation

import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.field.utils.geometry.TGeo
import kotlin.js.JsName

class Tx(
    var field: BaseField? = null,
    var op: Top = Top.NONE,
    var value: Any? = null
) {
    // Constructeur secondaire : permet Tx(expr, op)
    constructor(expr: Tx, op: Top) : this(expr.field, op, expr)
    enum class Type { WHERE, JOIN, AGGR }

    var geoValue: TGeo? = null
    var values: List<Any> = emptyList()
    var fieldKey: String = ""
    var v1: Tx? = null
    var v2: Tx? = null
    var targetModel: String = ""
    var foreignModel: String? = null
    var type: Type = Type.WHERE

    @JsName("jsTargetModel")
    fun targetModel(): String = targetModel
    fun foreignModel(v: String) : Tx{
        this.foreignModel = v
        return this
    }

    private var _joining = false
    override fun toString(): String = toScript()
    fun joining(v: Boolean=true) {
        _joining = true
    }
    fun isJoining(): Boolean = _joining
    /*fun isJoining(): Boolean {
        return if (value is String) {
            (value as String).contains('.')
        } else {
            false
        }
    }*/

    fun isAggr(): Boolean {
        return (field != null && field!!.hasAggr()) ||
                (v1 != null && v1!!.isAggr()) ||
                (v2 != null && v2!!.isAggr())
    }
    fun hasIntegratedFunc(): Boolean {
        return field?.hasFunction() ?: false
    }
    fun isGeoOp(): Boolean {
        return geoValue != null && (
                opToString().startsWith("ST_") || op == Top.MbrIntersects
                )
    }
    fun empty(): Boolean {
        return op == Top.NONE
    }

    companion object {
        //
    }

    // operators
    // operator &&
    operator fun plus(other: Tx): Tx {
        return Tx().apply {
            op = Top.And
            v1 = this@Tx
            v2 = other
        }
    }
    // operator ||
    infix fun or(other: Tx): Tx {
        return Tx().apply {
            op = Top.Or
            v1 = this@Tx
            v2 = other
        }
    }
    // operator !
    operator fun not(): Tx {
        return Tx().apply {
            op = Top.Not
            v1 = this@Tx
        }
    }
    // Alternative : overloads Kotlin `and`, `or`
    infix fun and(other: Tx): Tx = this + other
    infix fun orx(other: Tx): Tx = this.or(other)
}
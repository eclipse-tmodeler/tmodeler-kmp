package ai.tambo.tmodeler.field.base

import ai.tambo.tmodeler.core.Tattr
import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.field.operation.Top
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.scheduler.TmsScope

open class BaseField : Tattr("", {null}, {}) {
    context(TmsScope)
    override fun resolveDeepLink() = super.resolveDeepLink()

    fun isCoreField() : Boolean {
        return name in listOf("_state", "state", "linkers", "meta")
    }

    override fun getValue(): Any? {
        return _value
    }

    override fun setValue(v: Any?) {
        _value = v
    }

    protected var _nullValue: Any? = null
    protected var _value: Any? = null
    protected var _type: TFtype = TFtype.TEXT
    protected var _maxLen: Int = -1

    protected var _targetModel_: Tclass? = null
    protected var _targetModel: Tclass = Tclass()
    protected var _targetField: String = ""
    protected var _constraints = TFconstraints()
    protected var _onDelete: TF = TF.NONE

    var isGeo: Boolean = false
    var aggr: Tx = Tx(this, Top.NONE)


    open fun refValue(): Any? {
        return _value
    }

    open fun exists(): Boolean {
        return _value != null
    }

    open fun empty(): Boolean {
        return when (_value) {
            null -> true
            is String -> (_value as String).isEmpty()
            is Collection<*> -> (_value as Collection<*>).isEmpty()
            else -> false
        }
    }

    open fun getType(): TFtype {
        return _type
    }

    open fun getMaxLen(): Int {
        return _maxLen
    }

    open fun getConstraints(): TFconstraints {
        return _constraints
    }

    open fun getOnDelete(): TF {
        return _onDelete
    }

    open fun getTargetModel(): Tclass {
        return _targetModel_
            ?: _targetModel
    }

    open fun getTargetField(): String {
        return _targetField
    }

    fun field(k: String): Pair<String, Any?> = k to this@BaseField.getValue()
    /*
    operator fun shr(k: String): Pair<String, Any?> = field(k)
    */

    fun initTmpAggr() {
        aggr.op = Top.NONE
    }
    fun hasAggr(): Boolean {
        return !aggr.empty()
    }
    fun isNull(): Tx = Tx(this, Top.IsNull)
    fun targetField(): String = _targetField

    var _function: ((String, String) -> String)? = null
    val _functions = mutableListOf<(String, String) -> String>()
    private var _currentIndex = 0

    fun clearFunc() {
        resetFuncIndex()
        _functions.clear()
    }

    fun addFunc(func: (String, String) -> String) {
        resetFuncIndex()
        _functions.add(func)
    }

    fun applyFunc(fieldKey: String, wkt: String): String {
        if(_functions.isEmpty()) return ""

        val result = _functions.getOrNull(_currentIndex)?.invoke(fieldKey, wkt) ?: ""

        if(_currentIndex + 1 < _functions.size){
            _currentIndex + 1
        }else{
            if(_functions.size > 1) {
                _functions.subList(1, _functions.size).clear()
            }
        }

        return result
    }

    fun hasFunction(): Boolean = _functions.isNotEmpty() || _function!=null
    fun resetFuncIndex(v: Int = 0) {
        if(v >= _functions.size || _currentIndex == v) return
        _currentIndex = v
    }

    companion object {
        const val NULL_INT: Long = Long.MIN_VALUE
        const val NULL_FLOAT: Double = -Double.MAX_VALUE
        const val NULL_STRING: String = ""
    }

    protected var isPrepared = true
    protected open fun prepare() {}

    open infix fun sameInstance(other: Any?): Boolean {
        return this === other
    }

}
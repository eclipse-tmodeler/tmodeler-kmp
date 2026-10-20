package ai.tambo.tmodeler.field.base

import ai.tambo.tmodeler.field.operation.Top.Equal
import ai.tambo.tmodeler.field.operation.Top.In
import ai.tambo.tmodeler.field.operation.Top.NotEqual
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.operation.listToString
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
open class TField<T, Derived : TField<T, Derived>>(
    val tfType: TFtype
) : BaseField() {
    context(TmsScope)
    override fun resolveDeepLink() = super.resolveDeepLink()

    init {
        _type = tfType

        /*
        val defaultValue: T = when (tfType) {
            TFtype.INTEGER -> BaseField.NULL_INT as T
            TFtype.REAL    -> BaseField.NULL_FLOAT as T
            TFtype.TEXT    -> BaseField.NULL_STRING as T
            else           -> null as T
        }
        defaults(defaultValue)
        */
    }

    fun exists(block: Derived.(T) -> Unit) : T? {
        if (this.exists()) {
            val v = get()!!
            (this as Derived).block(v)
            return v
        }
        return null
    }

    open fun get(): T? {
        return _value as T
    }

    open fun set(value: T?) {
        if(value is Int || value is Long || value is Double || value is Float || value is String)
            _value = value
        else
            setValue(value)
    }

    open operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()!!
    open operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        set(newValue)
    }

    var defaultValue: T? = null

    @Suppress("UNCHECKED_CAST")
    open fun len(max: Int): Derived {
        this._maxLen = max
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun defaults(v: T): Derived {
        this.defaultValue = v

        //ca be dangerous
        set(v)

        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun default(v: T): Derived = defaults(v)

    @Suppress("UNCHECKED_CAST")
    open fun onDelete(v: TF): Derived {
        this._onDelete = v
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun constraint(v: TF): Derived {
        this._constraints.addIfNotExist(v)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun constraints(vararg args: TF): Derived {
        for (v in args) this._constraints.addIfNotExist(v)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun primaryKey(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.PRIMARY_KEY)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun autoIncrement(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.AUTOINCREMENT)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun nullable(v: Boolean = true): Derived = notNull(!v)

    @Suppress("UNCHECKED_CAST")
    open fun notNull(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.NOT_NULL)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun unique(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.UNIQUE)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun check(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.CHECK)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun foreignKey(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.FOREIGN_KEY)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun defaultValue(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.DEFAULT)
        return this as Derived
    }

    @Suppress("UNCHECKED_CAST")
    open fun indexed(v: Boolean = true): Derived {
        if (v) this._constraints.addIfNotExist(TF.INDEX)
        return this as Derived
    }


    // Ops to ov
    /*internal open infix fun ieq(v: T): Tx { return Tx(this, Equal, v) }
    internal open infix fun ine(v: T): Tx { return Tx(this, NotEqual, v) }*/
    internal open infix fun ieq(v: T?): Tx { return if(v!=null) Tx(this, Equal, v) else isNull() }
    internal open infix fun ine(v: T?): Tx { return if(v!=null) Tx(this, NotEqual, v) else !isNull() }
    internal open infix fun iinto(values: List<T>): Tx = Tx(this, In, listToString(values))


    override infix fun sameInstance(other: Any?): Boolean {
        return this === other
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TField<*, *>) return false

        return name == other.name &&
                this::class == other::class
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + this::class.hashCode()
        return result
    }

}

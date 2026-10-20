package ai.tambo.tmodeler.core

import ai.tambo.tmodeler.field.base.BaseField
import kotlin.reflect.KClass

typealias AttrList = List<Tattr>
typealias FieldList = List<Tattr>

open class Tclass {

    protected var _fields: AttrList = emptyList()
    protected var _package: String = ""
    protected var _db: String = ""

    protected var _fieldsMap: MutableMap<String, Tattr?> = mutableMapOf()

    protected var _type: KClass<*>? = null
    protected var _superClass: Tclass? = null

    constructor()
    constructor(inst: TobGb, sup: Tclass? = null) {
        _type = inst::class
        _superClass = sup

        _package = try {
            (classQualifiedName(inst::class) ?: "")
                .substringBeforeLast(".", "")
        } catch (_: Exception) {
            ""
        }
    }

    constructor(type: KClass<*>, sup: Tclass? = null) {
        _type = type
        _superClass = sup

        _package = try {
            classQualifiedName(type)?.substringBeforeLast('.', "") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        fun with(instance: TobGb): Tclass {
            return Tclass(instance)
        }
    }

    fun autoFill(instance: TobGb): Tclass {
        return this
    }

    fun type(): KClass<*>? {
        return _type
    }

    fun sup(): Tclass? {
        return _superClass
    }

    fun sup(v: Tclass?): Tclass {
        _superClass = v
        return this
    }

    fun packageName(v: String): Tclass {
        _package = v
        return this
    }

    fun targetModel(): Tclass {
        return this
    }

    fun db(v: String): Tclass {
        _db = v
        return this
    }

    fun prepare() {
        for (field in _fields) {
            _fieldsMap[field.name] = field
        }
    }

    fun fields(vararg args: Tattr): Tclass {
        _fields = args.toList()
        prepare()
        return this
    }

    fun fields(list: AttrList): Tclass {
        _fields = list
        prepare()
        return this
    }

    fun getAttrSafe(name : String) = getAttrs().first { it.name == name }
    fun getAttr(name : String) = getAttrs().firstOrNull { it.name == name }

    fun getAttrs(includeSup: Boolean = true): AttrList {
        return if (includeSup && _superClass != null) {
            _superClass!!.getFields(true) + _fields
        } else {
            _fields
        }
    }

    fun getFieldSafe(name : String) = getFields().first { it.name == name }
    fun getField(name : String) = getFields().firstOrNull { it.name == name }

    fun getFields(includeSup: Boolean = true): List<BaseField> = getAttrs(includeSup).filterIsInstance<BaseField>()

    fun fieldsKeys(): List<String> {
        val attrs = getAttrs()
        return attrs
            .filterIsInstance<BaseField>()
            .map { it.name }
    }

    fun geoFieldsKeys(): List<String> {
        val attrs = getAttrs()
        return attrs
            .filterIsInstance<BaseField>()
            .filter { it.isGeo }
            .map { it.name }
    }

    fun isFunctField(v: String): Boolean {
        // À implémenter
        return false
    }

    fun hasFunction(fkey: String): Boolean {
        return try {
            val bf = _fieldsMap[fkey] as BaseField
            bf.hasFunction()
        }catch (_: Exception){ false }
    }

    fun hasGeo(): Boolean {
        return try {
            geoFieldsKeys().isNotEmpty()
        }catch (_: Exception){ false }
    }

    fun applyFunction(fkey: String, value: String): String {
        return try {
            val bf = _fieldsMap[fkey] as BaseField
            bf.resetFuncIndex()
            bf.applyFunc(fkey, value)
        }catch (_: Exception){ "" }
    }

    fun descendsFrom(parent: Tclass): Int {
        // À implémenter
        return -1
    }

    fun name(): String {
        return name(_type?.simpleName ?: "")
    }

    fun name(rawName: String): String {
        // Simple transformation : à adapter
        return rawName.replace(" ", "")
    }

    fun getPackage(): String = _package

    fun getDb(): String = _db

    fun modelName(): String {
        val pkg = getPackage().replace(".", "_")
        val cls = name()
        return "${pkg}_$cls"
    }

    fun equals(cls: Tclass?): Boolean {
        return this === cls
    }

    fun toMap(vec: AttrList): Map<String, Any?> {
        // À implémenter
        return emptyMap()
    }

    override fun equals(other: Any?): Boolean {
        return (other is Tclass) && this === other
    }

    override fun hashCode(): Int {
        return _type?.hashCode() ?: super.hashCode()
    }
}

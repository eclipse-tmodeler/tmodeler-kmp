package ai.tambo.tmodeler.ms

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.db.TtableField
import ai.tambo.tmodeler.feature.TFeature
import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.field.utils.ThreadLocalBoolean
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.ms.ext.TmsDeep
import ai.tambo.tmodeler.ms.ext.all
import ai.tambo.tmodeler.ms.ext.empty
import ai.tambo.tmodeler.ms.ext.isEmpty
import ai.tambo.tmodeler.ms.ext.isNotEmpty
import ai.tambo.tmodeler.ms.ext.lazy
import ai.tambo.tmodeler.ms.ext.prepare
import ai.tambo.tmodeler.ms.ext.selectClause
import ai.tambo.tmodeler.ms.ext.size
import ai.tambo.tmodeler.ms.ext.tableFields
import ai.tambo.tmodeler.ms.filter.JoinType
import ai.tambo.tmodeler.ms.filter.TFilterBuilder
import ai.tambo.tmodeler.ms.observer.TModelCallback
import ai.tambo.tmodeler.ms.result.Tlist
import ai.tambo.tmodeler.ms.result.data
import ai.tambo.tmodeler.ms.utils.Ttuple
import ai.tambo.tmodeler.scheduler.TmsScope
import ai.tambo.tmodeler.scheduler.TFlow
import kotlinx.datetime.Clock
import kotlin.reflect.KClass
import kotlin.js.JsName

open class TmsGb<T : TBaseModel>(
    private val cls: KClass<T>
) {

    context(TmsScope)
    var loadTimer:String?
        get() { return TmsDeep.synchronizer.cache.getLoadTimer(this) }
        set(value) { if(value!=null) TmsDeep.synchronizer.cache.setLoadTimer(this, value) }

    var contentLink = false
    var contentMedia = false
    var contentModel = false
    var contentList = false
    var contentDict = false


    internal var flow: TFlow<*>? = null
    internal var feature: TFeature<*>? = null

    internal var _builder: TFilterBuilder? = null
    internal var _tuplet: Ttuple? = null
    internal var _isReady = false
    internal var _modelClass: Tclass? = null
    internal var _instance: T? = null

    internal var _modelCallback: TModelCallback? = null

    internal var joinType: JoinType = JoinType.INNER

    val ready : Boolean
        get() { return _isReady && _modelClass!=null && _prototype!=null }
    val exists : Boolean
        get() { return exists() }
    val empty : Tlist<T>
        get() { return empty() }
    context(TmsScope)
    val size : Int
        get() { return size() }
    val name : String
        get() { return name() }
    val serverName : String?
        get() { return serverConfig?.getModelName() }
    val random : Long
        get() { return Clock.System.now().toEpochMilliseconds() }
    context(TmsScope)
    val isEmpty : Boolean
        get() { return isEmpty() }
    context(TmsScope)
    val isNotEmpty : Boolean
        get() { return isNotEmpty() }

    @JsName("jsExists")
    fun exists(): Boolean = TmsDeep.exists(_modelClass!!)
    fun modelClass(): Tclass = _modelClass!!
    @JsName("jsName")
    fun name(): String {

        return _modelClass!!.modelName()
    }
    val db by lazy {
        TModeler.getInstance().geTdb(_modelClass!!)
    }
    val baseServerConfig by lazy {
        db.serverConfig
    }
    val serverConfig by lazy {
        _prototype!!.onSync()
    }
    val prototype by lazy {
        _prototype
    }
    fun fieldsKeys(): List<String> = emptyList()
    fun allFieldsKeys(): List<List<String>> = emptyList()
    fun tableFields(): List<TtableField> = tableFields(modelClass())

    var _tableAllFields: MutableList<List<TtableField>> = mutableListOf()
    fun tableAllFields(): List<List<TtableField>> {
        //if(_tableAllFields.isNotEmpty()) return _tableAllFields
        if(_tableAllFields.isNotEmpty()) _tableAllFields.clear()

        val values = _tuplet!!.values

        for (v in values){
            v as TBaseModel
            val kc : KClass<TBaseModel> = v::class as KClass<TBaseModel>
            // Les champs d'une table ne dépendent que de la classe : un TmsGb complet (setup + schéma) par ligne était très coûteux.
            val fields = tableFieldsByClass.getOrPut(kc) {
                val tms = TmsGb(kc).onInit { cls -> v }
                tms.name()
                tms.tableFields()
            }
            _tableAllFields.add(fields)
        }

        return _tableAllFields
    }
    fun geoFieldsKeys(): List<String> = emptyList()
    fun dbId(): String = ""
    fun clear(): Boolean {
        if (exists()) {
            val result = TmsDeep.clear(_modelClass!!, emptyList())
            if (result) {
                _isReady = false
                setupClassInfo()

                return true
            }
        }
        return false
    }
    context(TmsScope)
    fun migrate(): Boolean{
        val backup = all().data()
        if(clear()){
            val list: List<T> = TobGb.parse(_prototype!!, backup)
            for(e in list) {
                e.create()
            }
            return true
        }
        return false
    }

    fun clearBuilder(){
        if (_builder != null) {
            _builder = null
        }
    }

    fun commit() {}

    //
    //----------------------------------------------------------------------
    /**
     * @return Tlist : this is on testing
     * */
    context(TmsScope)
    fun build(
        query: TmsGb<T>.(T) -> Tlist<T>
    ): Tlist<T> {
        val l = lambda
        val model = (this as TmsGb<T>).with(l).lazy()
        return model.query(l).build()
    }
    //---------------
    /**
     * @return Tlist : this is on testing
     * */
    /*context(TmsScope)
    inline fun build(
        query: ((TmsGb<T>, T) -> Tlist<T>)
    ): Tlist<T> {
        val l = lambda
        val model = with(l).lazy()
        return query(model, l).build()
    }*/
    /**
     * @return Tlist : this is on testing
     * */
    context(TmsScope)
    inline fun <reified U : TBaseModel> build(
        m2: TmsGb<U>,
        query: ((TmsGb<T>, T, U) -> Tlist<T>)
    ): Tlist<T> {
        val l1 = lambda
        val l2 = m2.lambda
        val model = with(l1, l2).lazy()
        return query(model, l1, l2).build()
    }
    /**
     * @return Tlist : this is on testing
     * */
    context(TmsScope)
    inline fun <reified U : TBaseModel, reified V : TBaseModel> build(
        m2: TmsGb<U>,
        m3: TmsGb<V>,
        query: ((TmsGb<T>, T, U, V) -> Tlist<T>)
    ): Tlist<T> {
        val l1 = lambda
        val l2 = m2.lambda
        val l3 = m3.lambda
        val model = with(l1, l2, l3).lazy()
        return query(model, l1, l2, l3).build()
    }
    //----------------------------------------------------------------------
    //
    fun with(vararg lambdas: Any): TmsGb<T> {
        if (_builder != null) {
            _builder = null
        }
        _builder = TFilterBuilder()

        selectClause()

        _builder?.from(name())

        // _tuplet représente un tuple de références : on simule par une liste d'objets ici
        _tuplet = Ttuple(lambdas.toList())

        return this
    }
    fun with(lambda: T): TmsGb<T> {
        if (_builder != null) {
            _builder = null
        }
        _builder = TFilterBuilder()

        selectClause()

        _builder?.from(name())

        _instance = lambda
        _tuplet = Ttuple(listOf(_instance))

        return this
    }


    fun join(type: JoinType): TmsGb<T> { joinType = type; return this }

    init {
        setupClassInfo()
    }
    companion object {
        private val alreadyIn = ThreadLocalBoolean()
        private val tableFieldsByClass = mutableMapOf<KClass<*>, List<TtableField>>()
    }

    var _prototype : T? = null
    private var creator: ((KClass<T>) -> T)? = null

    fun onInit(block: (KClass<T>) -> T): TmsGb<T> {
        this.creator = block
        setupClassInfo()
        return this
    }

    fun createInstance(): T? {
        return creator?.invoke(cls)
    }

    fun prototype(p:  T) : TmsGb<T>{
        _prototype = p
        return this
    }

    @JsName("jsPrototype")
    fun prototype() : T{
        return _prototype!!
    }

    val lambdaLocal by lazy { lambda(false) }
    val lambdaServer by lazy { lambda(true) }
    val lambda by lazy { lambda(true) }
    fun lambda(server: Boolean=false) : T{
        return if(server){
            instance().apply {
                id_ = Long.MAX_VALUE
            }
        }else{
            instance()
        }
    }

    fun instance(): T {
        return prototype().instance() as T
    }

    fun clone(): TmsGb<T> {
        return TmsGb(cls).onInit { cls -> instance() }
    }

    fun setupClassInfo() {

        if (!alreadyIn.get()) {
            // Pas de `!!` : sans créateur (constructeur nu), on évite une exception coûteuse en wasm.
            val proto = if (creator == null) null else try { createInstance() } catch (_: Exception) { null }
            if (proto != null) prototype(proto)

            try {
                alreadyIn.set(true)

                prepare()
            } catch (_: Exception) {
                // silence
            } finally {
                alreadyIn.set(false)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TmsGb<*>) return false
        return this.cls == other.cls
    }

    override fun hashCode(): Int {
        return cls.hashCode()
    }

    override fun toString(): String {
        val name = _modelClass?.name()?:cls.simpleName
        return if(name!=null) "$name.tms"
        else super.toString()
    }


    fun deepLinkResolve(enabled: Boolean): TmsGb<T> {
        _builder?.deepLinkResolve(enabled)
        return this
    }



    inline fun <reified U : TBaseModel> merge(field: BaseField?=null): Tlist<T> {
        //join(JoinType.INNER)
        //return iMerge(Tx())
        return empty
    }
}

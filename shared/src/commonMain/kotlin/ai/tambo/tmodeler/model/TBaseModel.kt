package ai.tambo.tmodeler.model

import ai.tambo.tmodeler.core.Tattr
import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase
import ai.tambo.tmodeler.field.advanced.JsonField
import ai.tambo.tmodeler.field.advanced.ListFieldGb
import ai.tambo.tmodeler.field.advanced.ModelFieldGb
import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.common.IntField
import ai.tambo.tmodeler.field.media.FileField
import ai.tambo.tmodeler.field.media.utils.TFile
import ai.tambo.tmodeler.field.operation.Top.Equal
import ai.tambo.tmodeler.field.operation.Tx
import kotlin.js.JsName
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.result.Tlist
import ai.tambo.tmodeler.ms.server.ProcessType
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlin.collections.set

open class TBaseModel : TobGb(){

    var _id = IntField().primaryKey(true).autoIncrement(true).notNull(true).unique(true)
    var id = IntField().unique(true)
    var meta = JsonField()

    @TMignore var _id_ by _id
    @TMignore var id_ by id
    @TMignore var meta_ by meta

    override fun serial(): TmSerial {
        if (_serializer == null) _serializer = TmSerial()
        return _serializer as TmSerial
    }

    override fun clazz(): Tclass {
        if (_clazz == null) _clazz = Tclass(TBaseModel::class, super.clazz()).fields(
            _id.apply { name = "_id" },
            id.apply { name = "id" },
            meta.apply { name = "meta" }
        )
        return _clazz!!
    }


    @Suppress("UNCHECKED_CAST")
    override fun clazz(inst : TobGb): Tclass {
        inst as TBaseModel

        if (_clazz == null) _clazz = Tclass(TBaseModel::class, super.clazz()).fields(
            inst._id.apply { name = "_id" },
            inst.id.apply { name = "id" },
            inst.meta.apply { name = "meta" }
        )
        return _clazz!!
    }



    open fun lid() : Long? = _id.get()
    open fun getModelName() : String = clazz().modelName()



    var _null = false

    /*var key: Long
        get() = if (isServed()) _id_ else id_
        set(value) {
            if (isServed()) {
                _id_ = value
            } else {
                id_ = value
            }
        }*/

    val ids: IntField
        get() {
            return if (isServed()) id else _id
        }

    var key by _id
    val sKey: Long // signed key
        get() = if (isServed()) id_ else -_id_
    val keyName: String
        get() = if (isServed()) "id" else "_id"

    @JsName("jsKey")
    open fun key(): IntField = _id
    @JsName("jsKeyName")
    open fun keyName(): String = "_id"

    context(TmsScope)
    open fun save(onServed: Any? = null) = isave(onServed)
    context(TmsScope)
    open fun save(fromServer: Boolean, onServed: Any? = null) = isave(fromServer, onServed)
    open fun isave(onServed: Any? = null): Int {
        return if (isCreated()) update() else create()
    }
    open fun isave(fromServer: Boolean, onServed: Any? = null): Int = 0
    open fun create(): Int {
        // Implémentation fictive
        return 1
    }
    open fun update(): Int {
        // Implémentation fictive
        return 1
    }
    context(TmsScope)
    open fun delete() = idelete()
    context(TmsScope)
    open fun delete(fromServer: Boolean) = idelete(fromServer)
    open fun idelete(): Int {
        // Implémentation fictive
        return 1
    }
    open fun idelete(fromServer: Boolean): Int = 0
    open fun reset() {
        key().set(null) //BaseField.NULL_INT
    }
    open fun stop() {}


    internal open fun deleteX(fromServer: Boolean, manualServe: Boolean = false): ProcessType = ProcessType.NONE
    open fun saveX(fromServer: Boolean, manualServe: Boolean = false): ProcessType = ProcessType.NONE


    open fun isCreated(): Boolean = !key().empty()
    open fun saveOnLinking(): Boolean = true

    open fun isNull(): Boolean = _null
    open fun setNull(v: Boolean) {
        _null = v
    }

    open fun empty(): Boolean = key().empty()
    open fun exists(): Boolean = !empty() && !onActionDelete() && !onStopped()

    fun equals(k: Int): Boolean {
        return false
    }



    open fun init(): Any? {
        return this
    }










    open fun onUpdate(): Boolean = false
    open fun onDelete(): Boolean = false
    open fun onActionDelete(): Boolean = false
    open fun onStopped(): Boolean = false



    fun isSaved(): Boolean = _id.exists()
    fun isServed(): Boolean = id.exists()

    fun onCreate(): Boolean = id.empty() && _id.exists()

    fun dispatchSave(): Boolean {
        return true
    }

    open fun autoServe(): Boolean {
        return false
    }

    open fun onSync(): TServerConfigBase? {
        return null
    }

    fun autoLoad(): Boolean {
        return true
    }

    fun autoNowDates() {}

    fun nowDates() {}

    open fun initStates() {}
    fun getMedias(): Map<String, TFile>? {
        val medias: MutableMap<String, TFile> = HashMap()
        val attrs = clazz().getAttrs()
        for (attr in attrs) {
            try {

                if (attr is FileField) {
                    val file: TFile? = attr.get()
                    if (file != null && file.exists())
                        medias["${_id_}.${attr.name}"] = file
                }
            } catch (_:Exception) {}
        }
        return medias.ifEmpty { null }
    }

    /**
     * ???
     * */
    fun notifyDicts(it: TBaseModel, contentDict: Boolean) {
        if (!contentDict || getModelName()!=it.getModelName()) return

        val attrs = clazz().getAttrs()

        for (attr in attrs) {
            if (attr is ListFieldGb<*>) {
                val lf = it.field(attr.name)
                if(attr.isEmpty && lf!=null)
                    attr.setValue(lf.getValue())
            }
        }
    }

    /*
    fun contentMedia(): Boolean {
        val attrs = clazz().getAttrs()

        for (attr in attrs) {

            if (attr is FileField) return true
        }

        return false
    }
    fun contentLink(): Boolean {
        val attrs = clazz().getAttrs()

        for (attr in attrs) {

            if (attr is ModelFieldGb<*> || attr is ListFieldGb<*>) return true
        }

        return false
    }
    */
    override fun deepLinkResolve(enabled: Boolean) {
        super.deepLinkResolve(enabled)

        val attrs = clazz().getAttrs()

        for (attr in attrs) {

            if (attr is ModelFieldGb<*> || attr is ListFieldGb<*>)
                attr.deepLinkResolve(enabled)
        }
    }
    context(TmsScope)
    override fun resolveDeepLink() {
        super.resolveDeepLink()

        val attrs = clazz().getAttrs()

        for (attr in attrs) {

            if (attr is ModelFieldGb<*> || attr is ListFieldGb<*>)
                attr.resolveDeepLink()
        }
    }
    /*
    fun contentModelField(): Boolean {
        val attrs = clazz().getAttrs()

        for (attr in attrs) {

            if (attr is ModelFieldGb<*>) return true
        }

        return false
    }
    */
    fun getModelFields(): List<ModelFieldGb<*>> {

        val fields = mutableListOf<ModelFieldGb<*>>()
        val attrs = clazz().getAttrs()

        for (attr in attrs) {
            if (attr is ModelFieldGb<*>) {
                fields.add(attr)
            }
        }

        return fields
    }
    fun getListFields(): List<ListFieldGb<*>> {

        val fields = mutableListOf<ListFieldGb<*>>()
        val attrs = clazz().getAttrs()

        for (attr in attrs) {
            if (attr is ListFieldGb<*>) {
                fields.add(attr)
            }
        }

        return fields
    }
    /*
    fun contentDict(): Boolean {
        val attrs = clazz().getAttrs()

        for (attr in attrs) {

            if (attr is ListFieldGb<*> || attr is JsonField) return true
        }

        return false
    }
    */
    fun notifyMedias(host: String, mediaHost: String) {
        val attrs = clazz().getAttrs()
        for (attr in attrs) {
            if (attr is FileField) {
                val file: TFile? = attr.get()
                if (file != null)
                    attr.setValue("$host${file.path()}")
            }
        }
    }

    fun getMediaFields(contentMedia: Boolean): List<String> {
        if (!contentMedia) return emptyList()

        val fields = mutableListOf<String>()
        val attrs = clazz().getAttrs()

        for (attr in attrs) {
            if (attr is FileField) {
                fields.add(attr.name)
            }
        }
        return fields
    }

    fun clearMedias(contentMedia: Boolean) {
        if (!contentMedia) return

        val attrs = clazz().getAttrs()

        for (attr in attrs) {
            if (attr is FileField) {
                attr.clearValue()
            }
        }

    }
    fun clearLocalLinks(contentLink: Boolean) {
        if (!contentLink) return

        val attrs = clazz().getAttrs()

        for (attr in attrs) {
            if (attr is ModelFieldGb<*>) {
                attr.clearLocalLink()
            }else if (attr is ListFieldGb<*>) {
                attr.clearLocalLinks()
            }
        }

    }

    fun attr(name: String): Tattr? {
        val attrs = clazz().getAttrs()
        return attrs.find { it.name == name }
    }
    fun field(name: String): TField<*, *>? {
        return clazz()
            .getAttrs()
            .filterIsInstance<TField<*, *>>()
            .find { it.name == name }
    }
    fun modelFields(): List<ModelFieldGb<*>> {
        return clazz()
            .getAttrs()
            .filterIsInstance<ModelFieldGb<*>>()
    }
    fun listFields(): List<ListFieldGb<*>> {
        return clazz()
            .getAttrs()
            .filterIsInstance<ListFieldGb<*>>()
    }

    override fun equals(other: Any?): Boolean {
        if(other is TBaseModel) return equals(other)

        return false
    }

    fun equals(other: TBaseModel?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (this.clazz().modelName() != other.clazz().modelName()) return false

        return _id.exists() && other._id.exists() && _id_ == other._id_ ||
                id.exists() && other.id.exists() && id_ == other.id_
    }

    override fun hashCode(): Int {
        return when {
            _id.exists() -> 31 * (clazz().modelName().hashCode() + 1) + _id_.hashCode()
            id.exists()  -> 31 * (clazz().modelName().hashCode() + 2) + id_.hashCode()
            else -> clazz().modelName().hashCode()
        }
    }

    operator fun <T: TBaseModel> plus(v: ModelFieldGb<T>): Tx {
        v.prepare()
        val s = if(isServed()) "" else "-"
        return Tx(v, Equal, "$s${v.tms()?.name()}.${keyName}").foreignModel("")
    }

    fun <K: TBaseModel> findItemsOf(model: TmsGb<K>, depth:Int=3) : Tlist<K> {
        val all = mutableListOf<K>()
        val mf = getModelFields()
        val lf = getListFields()
        mf.forEach {
            if(it.linkedTo(model))
                all.add(it.get() as K)
        }
        lf.forEach {
            if(it.linkedTo(model))
                all.addAll(it.get() as List<K>)
        }

        return if(all.isEmpty()) model.empty
        else Tlist.of(all.toList())
    }

    fun <K: TBaseModel> findItemsDeep(
        model: TmsGb<K>,
        depth: Int = 3
    ): Tlist<K> {

        val visited = mutableSetOf<TBaseModel>()
        val all = collectAll(model, visited, depth)
            .filter { it.instanceOf(model) }

        return if (all.isEmpty()) model.empty
        else Tlist.of(all) as Tlist<K>
    }

    private fun instanceOf(model: TmsGb<*>): Boolean {
        return clazz().modelName() == model.name()
    }

    fun collectAll(
        model: TmsGb<*>,
        visited: MutableSet<TBaseModel>,
        depth: Int = 3
    ): List<TBaseModel> {
        if (depth < 0) return emptyList()

        val result = mutableListOf<TBaseModel>()

        val mf = getModelFields()
        val lf = getListFields()

        // 🔹 champs simples
        mf.forEach {
            val item = it.get()
            if (item != null && visited.add(item)) {
                if (it.linkedTo(model)) result.add(item)
                result.addAll(item.collectAll(model, visited, depth - 1))
            }
        }

        // 🔹 listes
        lf.forEach {
            val items = it.get() as? List<TBaseModel> ?: return@forEach
            items.forEach { item ->
                if (visited.add(item)) {
                    if (it.linkedTo(model)) result.add(item)
                    result.addAll(item.collectAll(model, visited, depth - 1))
                }
            }
        }

        return result
    }

    //-----------------------------------------------------------------------

    fun <K: TBaseModel> findOnUpdateItemsDeep(
        model: TmsGb<K>,
        depth: Int = 3
    ): Tlist<K> {

        val visited = mutableSetOf<TBaseModel>()
        val all = collectOnUpdateAll(model, visited, depth)
            .filter { it.instanceOf(model) }

        return if (all.isEmpty()) model.empty
        else Tlist.of(all) as Tlist<K>
    }

    /*fun instanceOf(model: TmsGb<*>): Boolean {
        return clazz().modelName() == model.name()
    }*/

    fun TBaseModel.collectOnUpdateAll(
        model: TmsGb<*>,
        visited: MutableSet<TBaseModel>,
        depth: Int = 3
    ): List<TBaseModel> {
        if (depth < 0) return emptyList()

        val result = mutableListOf<TBaseModel>()

        val mf = getModelFields()
        val lf = getListFields()

        // 🔹 champs simples
        mf.forEach {
            val item = it.get()
            if (item != null && visited.add(item)) {
                if (it.linkedTo(model) && (item.onCreate() || item.onUpdate())/*it.isLocalLink()*/) result.add(item)
                result.addAll(item.collectOnUpdateAll(model, visited, depth - 1))
            }
        }

        // 🔹 listes
        lf.forEach {
            val items = it.get() as? List<TBaseModel> ?: return@forEach
            items.forEach { item ->
                if (visited.add(item)) {
                    if (it.linkedTo(model) && (item.onCreate() || item.onUpdate())/*!item.isServed()*/) result.add(item) // en realité, 1 seul item etant linked, tous les autres le sont aussi
                    result.addAll(item.collectOnUpdateAll(model, visited, depth - 1))
                }
            }
        }

        return result
    }


    fun clearLocalLinks(excludes:List<TmsGb<*>>, contentLink: Boolean) {
        if (!contentLink) return

        val attrs = clazz().getAttrs()

        for (attr in attrs) {
            if (attr is ModelFieldGb<*> && !attr.linkedToAny(excludes)) {
                attr.clearLocalLink()
            }else if (attr is ListFieldGb<*> && !attr.linkedToAny(excludes)) {
                attr.clearLocalLinks()
            }
        }

    }

    //-----------------------------------------------------------------------

}

fun Tattr.linkedToAny(models:List<TmsGb<*>>): Boolean {

    return when (this) {

        is ModelFieldGb<*> -> {
            val model = this.get() as? TModelGb<*> ?: return false
            (model.tms() as TmsGb<*>) in models
        }

        is ListFieldGb<*> -> {
            val list = this.get() ?: return false

            list.any {
                val model = it as? TModelGb<*> ?: return@any false
                (model.tms() as TmsGb<*>) in models
            }
        }

        else -> false
    }
}
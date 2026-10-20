package ai.tambo.tmodeler.ms.ext

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.db.KeysType
import ai.tambo.tmodeler.feature.TFeature
import ai.tambo.tmodeler.field.operation.into
import ai.tambo.tmodeler.field.operation.ne
import ai.tambo.tmodeler.field.operation.unaryMinus
import ai.tambo.tmodeler.field.operation.unaryPlus
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TModelGb
import ai.tambo.tmodeler.model.Tlinker
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.data.TdataCreator
import ai.tambo.tmodeler.ms.data.TdataDeletor
import ai.tambo.tmodeler.ms.data.TdataLoader
import ai.tambo.tmodeler.ms.data.TdataUpdator
import ai.tambo.tmodeler.ms.filter.SelectClause
import ai.tambo.tmodeler.ms.filter.TFilterBuilder
import ai.tambo.tmodeler.ms.observer.ResultItemsCallback
import ai.tambo.tmodeler.ms.server.ProcessType
import ai.tambo.tmodeler.ms.result.Tlist
import ai.tambo.tmodeler.ms.result.TqResult
import ai.tambo.tmodeler.ms.server.serveDelete
import ai.tambo.tmodeler.ms.server.serveSave
import ai.tambo.tmodeler.scheduler.TmsScope
import ai.tambo.tmodeler.scheduler.TFlow

// --- Fonctions avec vararg, qui appellent les versions List ---
/**
 * @return available Server Id
 * */
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.avid(): Long {
    val t = _prototype!!.instance() as T
    val res = with(t).lazy().filter(t.id ne null).order(-t.id).build()

    return if(res.isNotEmpty()) {
        res.first().id_ +1
    }else{
        1
    }
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.simulate(vararg items: T) = simulate(items.toList())
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.save(fromServer: Boolean=false, vararg items: T) = save(fromServer, items.toList())
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.save(vararg items: T) = save(instances = items.toList())
fun <T : TBaseModel> TmsGb<T>.create(vararg items: T) = create(items.toList())
fun <T : TBaseModel> TmsGb<T>.update(vararg items: T) = update(items.toList())
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.delete(fromServer: Boolean=false, vararg items: T) = delete(fromServer, items.toList())
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.delete(vararg items: T) = delete(instances = items.toList())
fun <T : TBaseModel> TmsGb<T>.idelete(fromServer: Boolean=false, vararg items: T) = idelete(fromServer, items.toList())
fun <T : TBaseModel> TmsGb<T>.idelete(vararg items: T) = idelete(instances = items.toList())
fun <T : TBaseModel> TmsGb<T>.serve(vararg items: T) = serve(items.toList())



context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.simulate(instances: List<T>): Int {
    if(!instances.isEmpty()){
        val index : Long = (all().size + 1) as Long

        val count = instances.size
        for (i in 0 until count) {
            instances[i].key().set(index)
            index+1
        }

        return count
    }
    return 0
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.save(fromServer: Boolean=false, instances: List<T>, onServed: ResultItemsCallback<T>? = null) = isave(fromServer, instances, onServed)
fun <T : TBaseModel> TmsGb<T>.isave(fromServer: Boolean=false, instances: List<T>, onServed: ResultItemsCallback<T>? = null): Int {
    val serveList = mutableListOf<T>()
    val noneList  = mutableListOf<T>()

    val result = saveX(instances)

    instances.forEach {
        when (it.saveX(fromServer, onServed!=null)) {
            ProcessType.SERVE -> serveList.add(it)
            else -> noneList.add(it)
        }
    }

    if(serveList.isNotEmpty()) serveSave(serveList, result = onServed)

    return result
}
fun <T : TBaseModel> TmsGb<T>.create(instances: List<T>): Int {
    return TdataCreator.create(this, _builder, instances)
}
fun <T : TBaseModel> TmsGb<T>.emitCreate(instances: List<T>): Int {
    return TdataCreator.emitCreate( instances)
}
fun <T : TBaseModel> TmsGb<T>.update(instances: List<T>): Int {
    return TdataUpdator.update(this, _builder, instances)
}
fun <T : TBaseModel> TmsGb<T>.emitUpdate(instances: List<T>): Int {
    return TdataUpdator.emitUpdate( instances)
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.delete(fromServer: Boolean=false, instances: List<T>) = idelete(fromServer, instances)
fun <T : TBaseModel> TmsGb<T>.idelete(fromServer: Boolean=false, instances: List<T>): Int {
    val deleteList = mutableListOf<T>()
    val serveList = mutableListOf<T>()
    val noneList  = mutableListOf<T>()

    instances.forEach {
        when (it.deleteX(fromServer)) {
            ProcessType.DIRECT -> deleteList.add(it)
            ProcessType.SERVE  -> serveList.add(it)
            ProcessType.NONE   -> noneList.add(it)
        }
    }

    if(deleteList.isNotEmpty()) deleteX(deleteList)
    if(serveList.isNotEmpty()) serveDelete(serveList)

    return 0
}
fun <T : TBaseModel> TmsGb<T>.serve(instances: List<T>): Int {
    return TdataDeletor.del(this, _builder, instances)
}


fun <T : TBaseModel> TmsGb<T>.emit(instances: List<T>): Int {
    val createList = mutableListOf<T>()
    val updateList  = mutableListOf<T>()

    instances.forEach {
        when {
            it.isCreated() -> updateList.add(it)
            else -> {
                it.init()
                createList.add(it)
            }
        }
    }

    var result = 0

    if(createList.isNotEmpty()) result = emitCreate(createList)
    if(updateList.isNotEmpty()) result = emitUpdate(updateList)

    return result
}

fun <T : TBaseModel> TmsGb<T>.saveX(instances: List<T>): Int {
    val createList = mutableListOf<T>()
    val updateList  = mutableListOf<T>()

    instances.forEach {
        when {
            it.isCreated() -> updateList.add(it)
            else -> {
                it.init()
                createList.add(it)
            }
        }
    }

    var result = 0

    if(createList.isNotEmpty()) result = create(createList)
    if(updateList.isNotEmpty()) result = update(updateList)

    // extablish linking
    (createList + updateList).forEach { instance ->
        val mfields = instance.modelFields()
        mfields.forEach { field ->
            val t = field.get()
            if(t is TModelGb<*>){
                if(!t.contentLink(instance, field.name)){
                    t.isave {
                        linkers + Tlinker().isave {
                            model_ = instance.getModelName()
                            field_ = field.name
                            fk_ = instance._id_
                        }
                    }
                }
            }
        }

        val lfields = instance.listFields()
            .filter { it.cls != Tlinker::class }
        lfields.forEach { field ->
            field.get()!!.forEach { t ->
                if(t is TModelGb<*>){
                    if(!t.contentLink(instance, field.name)){
                        t.isave {
                            linkers + Tlinker().isave {
                                model_ = instance.getModelName()
                                field_ = field.name
                                fk_ = instance._id_
                            }
                        }
                    }
                }
            }
        }
    }

    return result
}
internal fun <T : TBaseModel> TmsGb<T>.deleteX(instances: List<T>): Int {
    return TdataDeletor.del(this, _builder, instances)
}


fun selectUnique(clazz: Tclass, builder: TFilterBuilder) {
    val modelName = clazz.modelName()
    val fields = clazz.fieldsKeys()
    val geoFields = clazz.geoFieldsKeys()

    val normalizeFields = mutableListOf<String>()

    for (field in fields) {
        val fullName = "$modelName.$field"
        if (geoFields.contains(field)) {
            normalizeFields.add("AsText($fullName) AS $field")
        } else {
            normalizeFields.add(fullName)
        }
    }

    val clause = SelectClause(
        columns = normalizeFields,
        distinct = false
    )

    builder.addSelect(clause)
}
fun <T : TBaseModel> TmsGb<T>.selectClause() {
    val types = _tuplet?.getAll<TBaseModel>() ?: listOf(_prototype)

    if (types.size == 1) {
        val clazz: Tclass? = types[0]?.clazz()
        if (clazz != null && clazz.hasGeo()) {
            selectUnique(clazz, this._builder!!)
        }
    } else {
        val anyGeo = types.any { it!!.clazz().hasGeo() }

        if (anyGeo) {
            types.forEach { model ->
                selectUnique(model!!.clazz(), this._builder!!)
            }
        }
    }
}

fun <T : TBaseModel> TmsGb<T>.query(sql: String): TqResult = TqResult()

fun <T : TBaseModel> TmsGb<T>.collectAsFlow(
    flow: TFlow<T> = TFlow(this, TFlow.Mode.MANUAL, 25),
    items: Tlist<T>?=null
): Tlist<T> {
    this.flow = flow
    return items?:empty()
}
fun <T : TBaseModel> TmsGb<T>.asFlow(
    flow: TFlow<T> = TFlow(this, TFlow.Mode.MANUAL, 25)
) = collectAsFlow(flow)

fun <T : TBaseModel> TmsGb<T>.executeAsFeature(
    feature: TFeature<T>,
    items: Tlist<T>?=null
): Tlist<T> {
    this.feature = feature
    return items?:empty()
}
fun <T : TBaseModel> TmsGb<T>.asFeature(feature: TFeature<T>) = executeAsFeature(feature)

fun <T : TBaseModel> TmsGb<T>.lazy(v: Boolean = true): TmsGb<T> {
    _builder?.lazyBuild = v
    return this
}
fun <T : TBaseModel> TmsGb<T>.disableDeepLink(v: Boolean = true) = deepLinkResolve(!v)
/*
fun <T : TBaseModel> TmsGb<T>.ibuild(): Tlist<T> {
    if(!_builder!!.lazyBuild){

        return if(_builder!=null) TdataLoader.get(this, _builder!!)
        else Tlist<T>(this)
    }

    return Tlist<T>(this)
}
*/
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.build(): Tlist<T> = coreBuild()
fun <T : TBaseModel> TmsGb<T>.ibuild(): Tlist<T> {
    if(!_builder!!.lazyBuild){
        return coreBuild()
    }
    return Tlist<T>(this)
}
fun <T : TBaseModel> TmsGb<T>.coreBuild(): Tlist<T> {
    val v = _builder?.buildSQL()
    return if(_builder!=null) TdataLoader.get(this, _builder!!)
    else Tlist<T>(this)
}

fun <T : TBaseModel> TmsGb<T>.build4server(): String? {
    if(_builder==null)
        return null

    //_builder!!.backupSelect()
    if(!baseServerConfig.supportsSpatial)
        _builder!!.clearSelect()
    var cmd = _builder!!.buildSQL()
    //_builder!!.restoreSelect()

    _tuplet?.toList()?.forEach {
        if(!(it is TModelGb<*>)) return@forEach

        val serverTable = it.tms().serverConfig?.getModelName() ?: return@forEach
        val localTable = it.tms().name()

        // Remplacement sécurisé (avec bordures SQL)
        cmd = replaceTableNameSafe(cmd, localTable, serverTable)

        // 2️⃣ remplacer les relations
        it.getModelFields()
            .forEach { field ->
                cmd = replaceRelationFieldSafe(
                    cmd,
                    serverTable,
                    field.name
                )
            }
    }

    return cmd
}


context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.first(): T {
    val i = _prototype?.instance()
    return with(i as T).lazy().filter().order(+i._id).sub(1).build().first()
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.second(): T {
    val i = _prototype?.instance()
    return with(i as T).lazy().filter().order(+i._id).sub(1, 1).build().first()
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.last(): T {
    val i = _prototype?.instance()
    return with(i as T).lazy().filter().order(-i._id).sub(1).build().first()
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.size(): Int  = TmsDeep.size(_modelClass!!)
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.all(): Tlist<T> {
    val i = _prototype?.instance()
    return with(i as T).lazy().filter().build()
}
fun <T : TBaseModel> TmsGb<T>.list(list: List<T>) = Tlist.fromList(this, list)
fun <T : TBaseModel> TmsGb<T>.list(vararg args: T) = list( args.toList())
fun <T : TBaseModel> TmsGb<T>.empty() = Tlist.fromList(this, emptyList())

context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.isEmpty() = !isNotEmpty()
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.isNotEmpty() = TmsDeep.isNotEmpty(_modelClass!!)
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.get(keys: KeysType) = iget(keys)
fun <T : TBaseModel> TmsGb<T>.iget(keys: KeysType) = igetByKeys(keys)
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.getByIds(ids: KeysType) = igetByIds(ids)
fun <T : TBaseModel> TmsGb<T>.igetByIds(ids: KeysType): Tlist<T> {
    val i = _prototype?.instance()
    return with(i as T).lazy().filter(i.id into ids).coreBuild()
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.getByKeys(keys: KeysType) = igetByKeys(keys)
fun <T : TBaseModel> TmsGb<T>.igetByKeys(keys: KeysType): Tlist<T> {
    val i = _prototype?.instance()
    return with(i as T).lazy().filter(i._id into keys).coreBuild()
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.get(k: Long) = iget(k)
fun <T : TBaseModel> TmsGb<T>.iget(k: Long): T? { return igetByKey(k) }
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.getById(id: Long) = igetById(id)
fun <T : TBaseModel> TmsGb<T>.igetById(id: Long): T? {
    val i = _prototype?.instance()
    val res = with(i as T).lazy().filter(i.id ieq id).coreBuild()
    if(res.isNotEmpty()) return res.first()
    return null
}
context(TmsScope)
fun <T : TBaseModel> TmsGb<T>.getByKey(_id: Long) = igetByKey(_id)
fun <T : TBaseModel> TmsGb<T>.igetByKey(_id: Long): T? {
    val i = _prototype?.instance()
    val res = with(i as T).lazy().filter(i._id ieq _id).coreBuild()
    if(res.isNotEmpty()) return res.first()
    return null
}

fun replaceTableNameSafe(sql: String, local: String, server: String): String {
    // Regex : mot exact suivi optionnellement d'un point
    val regex = Regex("(?i)(?<![A-Za-z0-9_])$local(?![A-Za-z0-9_])")
    return sql.replace(regex) { matchResult ->
        val nextChar = matchResult.range.last + 1
        // si le caractère suivant est '.' → on garde le point
        if (nextChar < sql.length && sql[nextChar] == '.') {
            server
        } else {
            server
        }
    }
}

fun replaceRelationFieldSafe(
    sql: String,
    table: String,
    field: String
): String {
    // ex: ChatApp_messagingmember.messaging
    val regex = Regex(
        "(?i)(?<![A-Za-z0-9_])$table\\.$field(?![A-Za-z0-9_])"
    )
    return sql.replace(regex, "$table.${field}_id")
}
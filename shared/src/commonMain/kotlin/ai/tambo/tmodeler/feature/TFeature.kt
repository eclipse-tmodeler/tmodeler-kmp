package ai.tambo.tmodeler.feature

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.extension.synchronization.core.TServerConfigBase
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.feature.FeatureLock.controlled
import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TModel0
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.filter
import ai.tambo.tmodeler.ms.ext.lazy
import ai.tambo.tmodeler.ms.ext.list
import ai.tambo.tmodeler.ms.result.Tlist
import ai.tambo.tmodeler.ms.server.buildAsFlow
import ai.tambo.tmodeler.ms.server.pullAsFlow
import kotlin.js.JsName
import ai.tambo.tmodeler.ms.server.serveAsFlow
import ai.tambo.tmodeler.scheduler.TFlow
import ai.tambo.tmodeler.scheduler.TGate
import ai.tambo.tmodeler.ms.ext.TmsDeep
import kotlin.collections.forEach

abstract class TFeature<T : TBaseModel> : TobGb() {

    val models by lazy { modelsMain + modelsLinked }
    val modelsMain by lazy { modelsMain() }
    val modelsLinked by lazy { modelsLinked() }

    //--------------------------------------------------------------------------------

    open var resolveDepth = 3
    protected open var metaDataLabel = "t_meta"

    //--------------------------------------------------------------------------------

    var items = emptyList<T>()

    val tms by lazy {
        models.first() as TmsGb<T>
    }

    protected var saveRaw = true
    protected var flashMode = true

    //--------------------------------------------------------------------------------
    @JsName("jsModelsMain")
    protected open fun modelsMain() : List<TmsGb<*>> { return emptyList() }
    @JsName("jsModelsLinked")
    protected open fun modelsLinked() : List<TmsGb<*>> { return emptyList() }
    //--------------------------------------------------------------------------------
    private var observer: FeatureObserver? = null

    open fun observer(block: ((FeatureEvent) -> Unit)?): TFeature<T> {
        if(block!=null)
            this.observer = FeatureObserver(block)
        else
            this.observer = null
        return this
    }

    fun emit(
        stage: Stage,
        subProgress: Int? = null,
        message: String? = null,
        error: Throwable? = null
    ) {
        if(observer==null) return
        TGate.main {
            observer?.onEvent(
                FeatureEvent(
                    stage = stage,
                    progress = computeProgress(stage, subProgress),
                    subProgress = subProgress,
                    message = message,
                    error = error
                )
            )
        }
    }
    //--------------------------------------------------------------------------------
    var subProgressTracker:SubProgressTracker?=null
    fun parsing(data:String){
        subProgressTracker = SubProgressTracker(data.length)
        emit(Stage.PARSING, subProgress = 0)
    }
    private var updateProcessing = false
    fun updater(input: TBaseModel) = TGate.back{
        if(updateProcessing) return@back
        updateProcessing = true

        val progress = subProgressTracker?.update(
            input.data().length
        ) ?: 0
        emit(
            stage = Stage.PARSING,
            subProgress = progress
        )

        updateProcessing = false
    }
    fun update(inputs: List<TBaseModel>) {
        if(observer==null) return
        val data = TobGb.data(inputs)
        TGate.back {
            if (updateProcessing) return@back
            updateProcessing = true

            val progress = subProgressTracker?.update(
                data.length
            ) ?: 0
            emit(
                stage = Stage.PARSING,
                subProgress = progress
            )

            updateProcessing = false
        }
    }
    //--------------------------------------------------------------------------------

    open fun push(
        items: List<T> = emptyList(),
        result: (TFlow<T>?) -> Unit
    ) {
        emit(Stage.FS_RECEIVED)
        TGate.back {
            controlled { done ->
                doPush(items, emptyList()) { flow ->
                    done()
                    TGate.main { result(flow) }
                }
            }
        }
    }

    open fun pull(
        query: Tlist<T>? = null,
        result: (TFlow<T>?) -> Unit
    ) {
        emit(Stage.FS_RECEIVED)
        TGate.back {
            controlled { done ->
                doPull(query) { flow ->
                    done()
                    TGate.main { result(flow) }
                }
            }
        }
    }

    open fun build(
        query: Tlist<T>? = null,
        result: (TFlow<T>?) -> Unit
    ) {
        emit(Stage.FS_RECEIVED)
        TGate.back {
            controlled { done ->
                doBuild(query) { flow ->
                    done()
                    TGate.main { result(flow) }
                }
            }
        }
    }

    //--------------------------------------------------------------------------------


    var forcePush = false

    val mainModel: TmsGb<*>
        get() = models.first()
    val mediasItemsList = mutableListOf<List<TBaseModel>>()
    val flows: MutableMap<TmsGb<*>, TFlow<*>> = mutableMapOf()

    fun <K : TBaseModel> flow(tms:TmsGb<K>) : TFlow<K> {
        return flows[tms] as? TFlow<K> ?: TFlow(tms, TFlow.Mode.MANUAL)
    }

    open fun doPush(
        items: List<T>,
        notify: List<TBaseModel> = emptyList(),
        result: (TFlow<T>?) -> Unit
    ) {
        emit(Stage.FS_STARTED)
        // ma reflexion, peut etre juste pour la v1 : utiliser le meme canal tsm mis en place pour les models
        // et faire en sorte que les features soient capables de faire du publish sur ce canal
        // en exploitant request config par exemple
        // on peut mettre ca sur pied, hyper vite, en quelques heures

        //val ids = notify?.map { it.id } ?: emptyList()

        if(!this.tms.ready){
            result(null)
            return
        }

        val tms = this.tms.clone()
        tms.commit()

        this.items = items
        //this.notify = notify

        if(tms.ready){
            if(items.isNotEmpty() && !flashMode){
                tms.list(items)
                    .asFlow()
                    .asFeature(this)
                    .serveAsFlow {
                        result(it)
                    }
            }else{
                inject(items as List<TModel0<*>>)

                forcePush = true

                val l = tms.lambda
                tms.with(l)
                    .lazy()
                    .filter()
                    .asFlow()
                    .asFeature(this)
                    .buildAsFlow {
                        forcePush = false
                        result(it)
                    }
            }
        }else{
            result(null)
        }
    }

    open fun doPull(
        query: Tlist<T>? = null,
        result: (TFlow<T>?) -> Unit
    ) {
        emit(Stage.FS_STARTED)
        // on peut gerer une variante build pour l'instant : mais normalement, c'est toujours pull dans ce genre de
        // request. on a aucune raison de recharger des données qu'on a deja : c'est juste que le system avec time
        // last update ne gere pas encore bien ca

        // ma reflexion, peut etre juste pour la v1 : utiliser le meme canal tsm mis en place pour les models
        // et faire en sorte que les features soient capables de faire du pulling sur ce canal
        // en exploitant request config par exemple
        // on peut mettre ca sur pied, hyper vite, en quelques heures

        // query nous permet d'appeler asFlow si on veut
        // on doit sinder la fonction tms.pull comme est fait asFlow
        // qu'on puisse impliquer le pull sans faire de build

        if(!this.tms.ready){
            result(null)
            return
        }

        val tms = this.tms.clone()
        tms.commit()

        if(query!=null){
            query.asFlow()
                .asFeature(this)
                .pullAsFlow {
                    result(it)
                }
        }else if(tms.ready){
            val l = tms.lambda
            tms.with(l)
                .lazy()
                .filter()
                .asFlow()
                .asFeature(this)
                .pullAsFlow {
                    result(it)
                }
        }else{
            result(null)
        }

    }

    open fun doBuild(
        query: Tlist<T>? = null,
        result: (TFlow<T>?) -> Unit
    ) {
        emit(Stage.FS_STARTED)
        // on peut gerer une variante build pour l'instant : mais normalement, c'est toujours pull dans ce genre de
        // request. on a aucune raison de recharger des données qu'on a deja : c'est juste que le system avec time
        // last update ne gere pas encore bien ca

        // ma reflexion, peut etre juste pour la v1 : utiliser le meme canal tsm mis en place pour les models
        // et faire en sorte que les features soient capables de faire du pulling sur ce canal
        // en exploitant request config par exemple
        // on peut mettre ca sur pied, hyper vite, en quelques heures

        // query nous permet d'appeler asFlow si on veut
        // on doit sinder la fonction tms.pull comme est fait asFlow
        // qu'on puisse impliquer le pull sans faire de build

        if(!this.tms.ready){
            result(null)
            return
        }

        val tms = this.tms.clone()
        tms.commit()

        if(query!=null){
            query.asFlow()
                .asFeature(this)
                .buildAsFlow {
                    result(it)
                }
        }else if(tms.ready){
            val l = tms.lambda
            tms.with(l)
                .lazy()
                .filter()
                .asFlow()
                .asFeature(this)
                .buildAsFlow {
                    result(it)
                }
        }else{
            result(null)
        }

    }

    open fun onSync(): TServerConfigBase? = TModeler.getInstance().syncEngine().newServerConfig()

    //----------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------
    open fun <K : TBaseModel> getItems(model: TmsGb<K>): Tlist<out TBaseModel> {

        val items = modelItemsRegistry[model]?.toList()?:return model.empty

        return model.list(items as List<K>)
    }
    protected open fun inject(items: Collection<TModel0<*>>) {
        if(items.isEmpty()) return

        val model = items.first().tms()

        val list = modelItemsRegistry.getOrPut(model) {
            mutableListOf()
        }

        list.addAll(items)
    }
    private val modelItemsRegistry =
        mutableMapOf<TmsGb<*>, MutableList<TBaseModel>>()
    open fun clearItems() {
        modelItemsRegistry.clear()
    }
    //----------------------------------------------------------------------------------------
    open fun getParams(params: MutableMap<String, String>) {
        paramsRegistry.forEach { (label, value) ->
            when (value) {
                is TModel0<*> -> {
                    params[label] = value.data()
                }
                else -> {
                    params[label] = value.toString()
                }
            }
        }

        queryRegistry.forEach { (serverModelName, holder) ->
            val cmd = holder.query.build4server()
            if(cmd!=null) params["t_cmd_$serverModelName"] = cmd
        }
    }
    protected inline fun <reified K : TBaseModel> inject(
        model: TmsGb<K>,
        queryBuilder: TmsGb<K>.(K) -> Tlist<K>
    ) {
        val lambda = model.lambda

        val mp = model.with(lambda).lazy()
        val query = mp.queryBuilder(lambda)

        if(saveRaw){
            val serverModelName =
                model.serverConfig?.getModelName()
                    ?: return

            val value = query.build4server() ?: return

            inject(
                label = "t_cmd_$serverModelName",
                value = value,
            )
        }else{
            val holder = QueryHolder(
                model,
                lambda,
                query
            )

            injectQuery(holder)
        }
    }
    private val queryRegistry =
        mutableMapOf<String, QueryHolder<out TBaseModel>>()
    protected open fun injectQuery(
        holder: QueryHolder<out TBaseModel>
    ) {
        val serverModelName =
            holder.model.serverConfig?.getModelName()
                ?: return

        queryRegistry.getOrPut(serverModelName) {
            holder
        }
    }
    private val paramsRegistry = linkedMapOf<String, Any>()
    protected open fun inject(label:String, value: Any){
        paramsRegistry[label] = value
    }
    protected open fun inject(value: TModel0<*>){
        // simulation systématique pour l'instant
        inject(metaDataLabel, value)
        /*
        clazz().getAttr()
        items
        //paramsRegistry[label] = value
        */
    }
    open fun getLoadTimer(): String? {
        return null
    }
    protected open fun inject(field: BaseField){
        /*val fields = clazz().getAttrs()
        fields.forEach {
            if(it === item){
                inject(it.name, item)
                return
            }
        }*/
    }

    //-----------------------

    open fun data(firstMainInstances: Tlist<T>) =
        TmsDeep.synchronizer.featureDeep.data(this, firstMainInstances)
    fun handleNotification(notif: TModel0<*>, result:(Boolean)->Unit) =
        TmsDeep.synchronizer.featureDeep.handleNotification(this, notif, result)

    /*

    protected open val JsonField.containsKeys : Boolean
        get() {
            try {
                return asList<Long>().isNotEmpty()
            }catch (_: Exception){}
            return false
        }

    open fun data(firstMainInstances: Tlist<T>): String {

        val result = linkedMapOf<String, MutableList<TBaseModel>>()

        mediasItemsList.clear()

        modelsMain.forEachIndexed { i, mainModel ->

            val instances =
                if(i==0) firstMainInstances + getItems(mainModel)
                else getItems(mainModel)

            processMainModel(
                mainModel = mainModel,
                instances = instances,
                result = result
            )
        }

        val data = serialize(result)

        clearItems()

        return data
    }

    fun handleNotification(notif: TModel0<*>, result:(Boolean)->Unit) {
        TGate.back {
            val items = notif.clazz().getField("items") as? JsonField

            if(items==null){
                result(true)
                return@back
            }

            if(items.isNotEmpty() && !items.containsKeys){
                val data = items.getValue() as? String
                val config = onSync()
                if(data==null || config==null){
                    result(false)
                    return@back
                }

                config.with(mainModel)
                val feature = this

                feature.models.forEach {model->
                    val mFlow = TFlow(model, TFlow.Mode.MANUAL)
                    feature.flows[model] = mFlow
                    performModel(feature, data, config, model, mFlow)
                }
                result(false)
            }else{
                result(true)
            }
        }
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    open fun serialize(data: Map<String, List<TBaseModel>>): String {
        return buildString {
            append("{")

            data.entries.forEachIndexed { i, (key, items) ->
                append("\"$key\":")
                append("[")

                items.forEachIndexed { j, item ->
                    append(item.data())
                    if (j < items.lastIndex) append(",")
                }

                append("]")
                if (i < data.size - 1) append(",")
            }

            append("}")
        }
    }

    private fun processMainModel(
        mainModel: TmsGb<*>,
        instances: Tlist<out TBaseModel>,
        result: MutableMap<String, MutableList<TBaseModel>>
    ) {

        (listOf(mainModel) + modelsLinked).forEach { model ->

            val serverTable = model.serverConfig?.getModelName()
                ?: return@forEach

            val collected = mutableSetOf<TBaseModel>()

            if(model==mainModel){
                collected.addAll(instances)
            }else{
                instances.forEach { instance ->
                    val list = instance.findOnUpdateItemsDeep(model = model, depth = resolveDepth)
                    list.forEach {
                        it.clearLocalLinks(models, model.contentLink)
                        it.clearMedias(model.contentMedia)
                    }
                    collected.addAll(list)
                }
            }

            if (collected.isNotEmpty()) {
                val existing = result.getOrPut(serverTable) { mutableListOf() }
                existing.addAll(collected.filter { it !in existing })
                if(model.contentMedia)
                    mediasItemsList.add(collected.toList())
            }
        }
    }

    protected open suspend fun <K : TBaseModel> performModel(
        feature: TFeature<*>,
        data:String,
        config: TServerConfigBase,
        tms: TmsGb<K>,
        flow: TFlow<*>,
        onError: ((String) -> Unit)?=null,
        onDone: ((KeysType) -> Unit)?=null,
    ) {
        val inputs = mutableListOf<K>()
        val ids = mutableListOf<Long>()

        // filter init
        flow.filter{it._id eq -1}

        //val config = tms.serverConfig?:return

        val serverTable = tms.serverConfig?.getModelName()
            ?: return

        try {
            TobGb.parseSequenceForKey(
                { tms._prototype!!.instance() as K },
                data,
                serverTable
            ).forEach { input ->
                inputs.add(input)
                if(inputs.size >= flow.bufferSize){
                    performBigData2(config, tms, inputs, onError)
                    ids.addAll(inputs.map { it._id_ })
                    feature.update(inputs)
                    inputs.clear()
                }
            }

            if(inputs.isNotEmpty()){
                performBigData2(config, tms, inputs, onError)
                ids.addAll(inputs.map { it._id_ })
                feature.update(inputs)
                inputs.clear()
            }

            if(ids.isEmpty()) {
                onError?.invoke(DAE)
                return
            }

        } catch (_: Exception) {}

        if (ids.isNotEmpty()) {
            flow.availableCount(ids.size)
            flow.filter{it._id into ids}

            onDone?.invoke(ids)
        }
    }

    protected open suspend fun <T : TBaseModel> performBigData2(
        config: TServerConfigBase,
        tms: TmsGb<T>,
        inputs: List<T>,
        onError: ((String) -> Unit)?=null
    ) = TGate.backAwait {

        val existingList = TGate.dataResult(tms.empty) {
            val i = tms._prototype!!
            tms.with(i)
                .lazy()
                .disableDeepLink()
                .filter(i.id into inputs.map { it.id_ })
                .build()
        }

        val existingById = existingList
            .filterIsInstance<TModel0<*>>()
            .associateBy { it.id_ }

        inputs
            .filterIsInstance<TModelGb<*>>()
            .forEach {input->
                input._id.setValue(null)
                input.init()

                existingById[input.id_]?.let { existing ->
                    input._id_ = existing._id_
                }

                if(tms.contentMedia)
                    input.notifyMedias(config.host(), config.getMediaHost())
            }

        TGate.dataAwait(
            onError = {
                onError?.invoke(DAE)
            }
        ) {
            tms.save(true, inputs)
        }
    }

    //----------------------------------------------------------------------------------------

    */

}
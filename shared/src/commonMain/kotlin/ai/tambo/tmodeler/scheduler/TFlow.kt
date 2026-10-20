package ai.tambo.tmodeler.scheduler

import ai.tambo.tmodeler.field.operation.Top
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.operation.unaryPlus
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.filter
import ai.tambo.tmodeler.ms.ext.lazy
import ai.tambo.tmodeler.ms.result.Tlist
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

class TFlow<T : TBaseModel>(
    private val tms: TmsGb<T>,
    private val mode: Mode = Mode.AUTO,
    private val buffer: Int = 10,
) {

    enum class Mode { MANUAL, AUTO }

    private val _all = MutableStateFlow<List<T>>(emptyList())
    val all: StateFlow<List<T>> = _all.asStateFlow()

    val isLoading = MutableStateFlow(false)
    val isComplete = MutableStateFlow(false)

    private var offset = 0
    private var loadJob: Job? = null

    private var _order = Top.Asc
    private var _filter: ((T) -> Tx)? = null
    private var _availableCount = 0

    fun filter(v: ((T) -> Tx)? = null) {
        _filter = v
    }

    fun availableCount(v: Int) {
        _availableCount = v
    }
    fun availableCount() = _availableCount

    val isUnavailable : Boolean
        get() =  !isAvailable
    val isAvailable : Boolean
        get() =  availableCount() > 0
    val bufferSize : Int
        get() = if(buffer>0) buffer else 1

    fun fetch(
        onLoaded: ((List<T>) -> Unit) = {},
        query: ((TmsGb<T>, T) -> Tlist<T>)
    ) = load(query=query, filter = null, onLoaded = onLoaded)
    fun load(
        query: ((TmsGb<T>, T) -> Tlist<T>)? = null,
        filter: ((T) -> Tx)? = null,
        order: ((T) -> Tx)? = null,
        onLoaded: ((List<T>) -> Unit)? = null
    ) {
        if (isLoading.value || isComplete.value) return

        TGate.back {
            when (mode) {
                Mode.MANUAL -> loadOnce(filter, order, query, onLoaded)
                Mode.AUTO -> autoLoad(filter, order, query, onLoaded)
            }
        }
    }

    private suspend fun loadOnce(
        filter: ((T) -> Tx)?,
        order: ((T) -> Tx)?,
        query: ((TmsGb<T>, T) -> Tlist<T>)?,
        onLoad: ((List<T>) -> Unit)? = null
    ): List<T> {
        isLoading.value = true
        return try {
            val res = loadBatch(filter, order, query)
            /*if (res.isNotEmpty()) */onLoad?.invoke(res)
            res
        } finally {
            isLoading.value = false
        }
    }

    private suspend fun autoLoad(
        filter: ((T) -> Tx)?,
        order: ((T) -> Tx)?,
        query: ((TmsGb<T>, T) -> Tlist<T>)?,
        onLoad: ((List<T>) -> Unit)? = null
    ): List<T> = coroutineScope {

        isLoading.value = true
        val results = mutableListOf<T>()
        loadJob?.cancel()

        loadJob = launch {
            while (isActive && !isComplete.value) {

                val batch = loadBatch(filter, order, query)

                if (batch.isEmpty()) {
                    isComplete.value = true
                    break
                }

                results += batch
                onLoad?.invoke(batch)
            }
            isLoading.value = false
        }

        results
    }

    private suspend fun loadBatch(
        filter: ((T) -> Tx)?,
        order: ((T) -> Tx)?,
        query: ((TmsGb<T>, T) -> Tlist<T>)?,
    ): List<T> {
        return TGate.dataResult(tms.empty) {

            if (tms.isEmpty || offset >= tms.size)
                return@dataResult emptyList()

            //val t = tms._prototype?.instance() as? T ?: return@dataResult emptyList()
            val t = tms.lambda

            val xOrder = order?.invoke(t)?:(+t._id)
            _order = xOrder.op

            val xFilter = filter?:_filter

            val res = when {
                query != null ->
                    query(
                            tms.with(t)
                                .lazy()
                                .deepLinkResolve(isDeepLinkResolve),
                            t
                        ).sub(buffer, offset)
                        .build()

                xFilter != null ->
                    tms.with(t)
                        .lazy()
                        .deepLinkResolve(isDeepLinkResolve)
                        .filter(xFilter(t))
                        .order(xOrder)
                        .sub(buffer, offset)
                        .build()

                else ->
                    tms.with(t)
                        .lazy()
                        .deepLinkResolve(isDeepLinkResolve)
                        .filter()
                        .order(xOrder)
                        .sub(buffer, offset)
                        .build()
            }

            offset += buffer

            if (res.isNotEmpty()) {
                _all.update { old ->
                    (old + res).distinctBy { it._id_ }
                }
            }

            res
        }
    }

    fun restart() {
        offset = 0
        isComplete.value = false
    }
    fun cursorToStart() {
        offset = 0
        isComplete.value = false
    }
    fun cursorToEnd() {
        offset = _all.value.size
        isComplete.value = false
    }

    fun reset() {
        offset = 0
        isComplete.value = false
        _all.value = emptyList()
    }

    private var pullObservable : ()->Unit = {}
    fun onPull(block:()->Unit) : TFlow<T> {
        pullObservable = block

        return this
    }
    fun pull() {
        pullObservable?.invoke()
    }

    fun add(items: List<T>) {
        if (items.isEmpty()) return

        _all.update { old ->
            when(_order){
                Top.Desc ->{
                    (items + old)
                        .distinctBy { it._id_ }
                }
                else ->{
                    (old + items)
                        .distinctBy { it._id_ }
                }
            }
        }
    }

    fun notify(items: List<T>) {
        if (items.isEmpty()) return

        val map = items.associateBy { it._id_ }

        //TGate.exe {
        TGate.back {
            _all.update { old ->
                old.filter {existing ->
                    map[existing._id_]==null
                }
            }

            delay(200)

            _all.update { old ->

                when(_order){
                    Top.Desc ->{
                        (items + old)
                            .distinctBy { it._id_ }
                    }
                    else ->{
                        (old + items)
                            .distinctBy { it._id_ }
                    }
                }
            }

        }
    }

    fun order(order: Top?=null) = TGate.back {
        val order_ = order?:_order

        _all.update { list ->
            when (order_) {
                Top.Asc  -> list.sortedBy { it._id_ }
                else -> list.sortedByDescending { it._id_ }
            }
        }
    }


    open fun disableDeepLink(v: Boolean=true) = deepLinkResolve(!v)
    open fun deepLinkResolve(enabled: Boolean = true) : TFlow<T> {
        isDeepLinkResolve = enabled
        return this
    }
    var isDeepLinkResolve = true

}
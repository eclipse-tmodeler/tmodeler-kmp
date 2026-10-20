package ai.tambo.tmodeler.notifs

import ai.tambo.tmodeler.core.Tattr
import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.core.Tserial
import ai.tambo.tmodeler.field.advanced.JsonField
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TMignore
import ai.tambo.tmodeler.model.TModel0
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.ext.list
import ai.tambo.tmodeler.ms.result.Tlist
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Signal instantané : "tel modèle a changé". Base commune que voient les programmes implémenteurs
 * (`Tnc.listen`) ; le format réel (champs propres, protocole serveur) est celui de la sous-classe fournie
 * par le moteur profond (`TncDeepBase`, synchronisation).
 *
 * Deux régimes de payload cohabitent :
 *  - signal seul : `items` = liste d'ids, l'app va rechercher les données ([isSignalOnly]) ;
 *  - données embarquées : `items` = données déjà sérialisées, rien à rechercher ([carriesData]).
 * `TFeature.handleNotification(signal) { pull -> ... }` tranche entre les deux.
 */
open class TncSignal<T : TncSignal<T>> : TModel0<T>() {

    /** Nom du modèle visé, tel que connu de TModeler. */
    var _model = TextField()
    var items = JsonField().resetAsArray()
    var title = TextField()
    var message = TextField()
    var extraData = JsonField()

    @TMignore var _model_ by _model
    @TMignore var items_ by items
    @TMignore var title_ by title
    @TMignore var message_ by message
    @TMignore var extraData_ by extraData

    /** Vrai si le signal concerne ce modèle ; les sous-classes peuvent affiner (nom serveur...). */
    open fun of(tms: TmsGb<*>): Boolean = _model.get() == tms.name()

    val containsKeys: Boolean
        get() {
            try {
                return items.asList<Long>().isNotEmpty()
            } catch (_: Exception) {}
            return false
        }

    /** Signal seul : il ne porte que des ids, les données sont à rechercher en aval. */
    val isSignalOnly: Boolean get() = containsKeys

    /** Données embarquées : les données du modèle sont directement dans le signal. */
    val carriesData: Boolean get() = items.isNotEmpty() && !containsKeys

    /** Instances de [model] désignées par le signal (ids seuls) ; liste vide si le signal porte des données. */
    fun <K : TBaseModel> items(model: TmsGb<K>): Tlist<K> {
        if (items.isNotEmpty() && containsKeys) {
            val all = mutableListOf<K>()
            items.asList<Long>().forEach {
                all.add(model.instance().apply { id_ = it })
            }
            return model.list(all.toList())
        }
        return model.empty
    }

    /** Remplit le signal depuis un JSON ; les clés inconnues ou mal typées sont ignorées. */
    fun parse2(json: String) {
        try {
            val jsonObj = Json.parseToJsonElement(json).jsonObject
            fromJson2(serial(), jsonObj, clazz().getFields())
        } catch (_: Exception) {}
    }

    fun fromJson2(serializer: Tserial, jsonObj: JsonObject, attrs: List<Tattr>) {
        for (attr in attrs) {
            try {
                if (jsonObj.containsKey(attr.name)) serializer.deserialize(jsonObj, attr.name, attr)
            } catch (_: Exception) {}
        }
    }

    override fun clazz(): Tclass {
        if (_clazz == null) _clazz = Tclass(TncSignal::class, super.clazz()).fields(
            _model.apply { name = "_model" },
            items.apply { name = "items" },
            title.apply { name = "title" },
            message.apply { name = "message" },
            extraData.apply { name = "extraData" },
        )
        return _clazz!!
    }

    override fun clazz(inst: TobGb): Tclass {
        inst as TncSignal<*>

        if (_clazz == null) _clazz = Tclass(TncSignal::class, super.clazz(inst)).fields(
            inst._model.apply { name = "_model" },
            inst.items.apply { name = "items" },
            inst.title.apply { name = "title" },
            inst.message.apply { name = "message" },
            inst.extraData.apply { name = "extraData" },
        )
        return _clazz!!
    }
}

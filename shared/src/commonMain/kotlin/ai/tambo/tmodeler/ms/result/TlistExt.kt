package ai.tambo.tmodeler.ms.result

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.model.TBaseModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray


fun <T : TBaseModel> Tlist<T>.data(
    includes: List<String>? = null,
    excludes: List<String>? = null
): String {
    return try {
        if (tms._tuplet!!.values.size == 1) {
            // Cas d'un seul type → on extrait la liste et serialize via Tob.data(...)
            val list = this.map { it } // car Tlist<T> hérite de AbstractMutableList<T>
            TobGb.data(
                list = list,
                includes = includes,
                excludes = excludes
            )
        } else {
            // Cas tuple multiple → on exporte chaque ligne comme un tableau JSON
            val jsonArray = buildJsonArray {
                for (tuple in rows) {
                    add(buildJsonArray {
                        for (value in tuple.values) {
                            val tob = value as? TobGb
                                ?: throw IllegalArgumentException("All values in Ttuple must inherit from Tob")
                            val json = Json.parseToJsonElement(tob.data(
                                includes = includes,
                                excludes = excludes
                            ))
                            add(json)
                        }
                    })
                }
            }
            jsonArray.toString() ///Json.encodeToString(jsonArray)
        }
    } catch (e: Exception) {
        "[]"
    }
}

fun <T : TBaseModel> Tlist<T>.clone(
    predicate: (T) -> Boolean
): Tlist<T> {
    val list = mutableListOf<T>()
    forEach {
        val c = it.clone() as T
        if (predicate(c))
            list.add(c)
    }
    return Tlist.fromList(tms, list)
}


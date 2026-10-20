package ai.tambo.tmodeler.ms.ext

import ai.tambo.tmodeler.core.Tclass
import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.db.KeysType
import ai.tambo.tmodeler.db.TModelObserver
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.filter.TFilterBuilder
import ai.tambo.tmodeler.ms.observer.ResultCallback
import ai.tambo.tmodeler.extension.synchronization.core.RequestConfigBase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long


object TmsDeep : TobGb() {

    fun exists(modelClass: Tclass): Boolean = TModeler.getInstance().geTdb(modelClass).tableExists(modelClass.modelName())
    fun clear(modelClass: Tclass, keys: KeysType): Boolean = TModeler.getInstance().geTdb(modelClass).dropTable(modelClass.modelName(), keys)
    fun isNotEmpty(modelClass: Tclass): Boolean = TModeler.getInstance().geTdb(modelClass).tableNotEmpty(modelClass.modelName())
    fun size(modelClass: Tclass): Int = TModeler.getInstance().geTdb(modelClass).tableSize(modelClass.modelName())
    fun lastInsertRowId(modelClass: Tclass): Long = TModeler.getInstance().geTdb(modelClass).lastInsertRowId()
    fun notifyChange(
        modelClass: Tclass,
        keys: KeysType,
        onCreate: Boolean,
        onUpdate: Boolean,
        onDelete: Boolean
    ) = TModeler.getInstance().geTdb(modelClass).notifyChange(modelClass.modelName(), keys, onCreate, onUpdate, onDelete)
    fun notifyServer(
        modelClass: Tclass,
        ids: KeysType,
        res: ResultCallback?,
        config: RequestConfigBase?=null,
        onServeCreate: Boolean,
        onServeUpdate: Boolean,
        onServeDelete: Boolean,
        onServeRefresh: Boolean
    ) = TModeler.getInstance().geTdb(modelClass).notifyServer(modelClass.modelName(), ids, res, config, onServeCreate, onServeUpdate, onServeDelete, onServeRefresh)
    fun addObserver(modelClass: Tclass, observer: TModelObserver) = TModeler.getInstance().geTdb(modelClass).addObserver(modelClass.modelName(), observer)
    fun removeObserver(modelClass: Tclass, observer: TModelObserver) = TModeler.getInstance().geTdb(modelClass).removeObserver(modelClass.modelName(), observer)
    fun fieldsKeys(modelClass: Tclass): List<String> = modelClass.fieldsKeys()
    fun geoFieldsKeys(modelClass: Tclass): List<String> = modelClass.geoFieldsKeys()



    fun registerTms(tmsList: List<out TmsGb<*>>) = TModeler.getInstance().registerTms(tmsList)
    fun findTms(name: String) = TModeler.getInstance().findTms(name)


    fun buildJsonArrayApply(
        columns: List<String>,
        jsonArray: JsonArray,
        modelClass: Tclass
    ): String {
        val newArray = buildJsonArray {
            jsonArray.forEach { element ->
                val obj = element.jsonObject
                val newObj = buildJsonObject {
                    columns.forEach { col ->
                        val value = obj[col]
                        if (value != null) {
                            if (value is JsonPrimitive && value.isString) {
                                if (modelClass.hasFunction(col)) {
                                    put(col, JsonPrimitive(modelClass.applyFunction(col, value.content)))
                                } else {
                                    put(col, value)
                                }
                            } else {
                                put(col, value)
                            }
                        }
                    }
                }
                add(newObj)
            }
        }
        return newArray.toString()
    }

    fun insertDataSafe(modelClass: Tclass, json: String): Boolean {

        return try {
            val jsonArray = Json.parseToJsonElement(json).jsonArray
            if (jsonArray.isEmpty()) return false

            val tableName = modelClass.modelName()

            /*
            val firstRow = jsonArray.first().jsonObject
            val columns = firstRow.keys.toList()
            */
            val columns = jsonArray
                .flatMap { it.jsonObject.keys }
                .distinct()

            val jsonArrayData = buildJsonArrayApply(columns, jsonArray, modelClass)

            TModeler.getInstance().geTdb(modelClass).executeInserts(tableName, jsonArrayData)
        } catch (e: Exception) {
            println("insertData error: ${e.message}")
            false
        }
    }
    fun updateDataSafe(modelClass: Tclass, jsonData: String, key: String): Boolean {

        return try {
            val jsonArray = Json.parseToJsonElement(jsonData).jsonArray
            if (jsonArray.isEmpty()) return false

            val tableName = modelClass.modelName()

            /*
            val firstRow = jsonArray.first().jsonObject
            val columns = firstRow.keys.toList()
            */
            val columns = jsonArray
                .flatMap { it.jsonObject.keys }
                .distinct()

            val jsonArrayData = buildJsonArrayApply(columns, jsonArray, modelClass)

            TModeler.getInstance().geTdb(modelClass).executeUpdates(tableName, jsonArrayData, key)
        } catch (e: Exception) {
            println("updateData exception: ${e.message}")
            false
        }
    }

    fun read(modelClass: Tclass, builder: TFilterBuilder): String = TModeler.getInstance().geTdb(modelClass).executeQuery(builder.buildSQL(), builder)
    fun insertData(modelClass: Tclass, json: String): Boolean {

        return try {
            val jsonArray = Json.parseToJsonElement(json).jsonArray
            if (jsonArray.isEmpty()) return false

            val tableName = modelClass?.modelName() ?: return false

            val firstRow = jsonArray.first().jsonObject
            val columns = firstRow.keys.toList()

            val sql = buildString {
                append("INSERT INTO $tableName (")
                append(columns.joinToString(", "))
                append(") VALUES ")

                jsonArray.forEachIndexed { i, element ->
                    val obj = element.jsonObject
                    append("(")
                    columns.forEachIndexed { j, col ->
                        val value = obj[col] ?: return@forEachIndexed
                        append(
                            if (value is JsonPrimitive && value.isString) {
                                if (modelClass.hasFunction(col)) {
                                    modelClass.applyFunction(col, value.content)
                                } else {
                                    "'${value.content}'"
                                }
                            } else {
                                value.toString()
                            }
                        )
                        if (j < columns.size - 1) append(", ")
                    }
                    append(")")
                    if (i < jsonArray.size - 1) append(", ")
                }
                append(";")
            }

            TModeler.getInstance().geTdb(modelClass).executeSQL(sql)
        } catch (e: Exception) {
            println("insertData error: ${e.message}")
            false
        }
    }
    fun updateData(modelClass: Tclass, jsonData: String, key: String): Boolean {

        return try {
            val jsonArray = Json.parseToJsonElement(jsonData).jsonArray
            if (jsonArray.isEmpty()) return false

            val tableName = modelClass?.modelName() ?: return false

            for (element in jsonArray) {
                val obj = element.jsonObject

                val id = obj[key]
                if (id == null) {
                    println("updateData error: missing $key field.")
                    continue
                }
                /*val id = obj[key] ?: run {
                    println("updateData error: missing $key field.")
                    continue
                }*/

                val updates = mutableListOf<String>()
                for ((col, value) in obj) {
                    if (col == key) continue
                    val str = if (value is JsonPrimitive && value.isString) {
                        if (modelClass.hasFunction(col)) {
                            "${col} = ${modelClass.applyFunction(col, value.content)}"
                        } else {
                            "${col} = '${value.content}'"
                        }
                    } else {
                        "${col} = ${value.toString()}"
                    }
                    updates.add(str)
                }

                val sql = buildString {
                    append("UPDATE $tableName SET ")
                    append(join(updates, ", "))
                    append(" WHERE $key = ")
                    append(
                        if (id is JsonPrimitive && id.isString) "'${id.content}'"
                        else id.toString()
                    )
                    append(";")
                }

                if (!TModeler.getInstance().geTdb(modelClass).executeSQL(sql)) {
                    println("updateData error on: $sql")
                    return false
                }
            }

            true
        } catch (e: Exception) {
            println("updateData exception: ${e.message}")
            false
        }
    }
    fun deleteData(modelClass: Tclass, jsonData: String, key: String): Boolean {

        return try {
            val jsonArray = Json.parseToJsonElement(jsonData).jsonArray
            if (jsonArray.isEmpty()) return false

            val ids = mutableListOf<String>()
            for (element in jsonArray) {
                val obj = element.jsonObject

                val id = obj[key]
                if (id == null) {
                    println("deleteData error: missing $key field.")
                    continue
                }
                /*val id = obj[key] ?: run {
                    println("deleteData error: missing $key field.")
                    continue
                }*/

                if (id is JsonPrimitive && !id.isString) {
                    ids.add(id.long.toString())
                } else {
                    println("deleteData warning: id is not an integer, skipping.")
                }
            }

            if (ids.isEmpty()) return false

            val tableName = modelClass?.modelName() ?: return false
            val sql = "DELETE FROM $tableName WHERE $key IN (${join(ids, ", ")});"

            if (!TModeler.getInstance().geTdb(modelClass).executeSQL(sql)) {
                println("deleteData error on: $sql")
                return false
            }

            true
        } catch (e: Exception) {
            println("deleteData exception: ${e.message}")
            false
        }
    }
    fun join(vec: List<String>, delim: String): String {
        return vec.joinToString(delim)
    }

    fun sync(tms : TmsGb<*>) = TModeler.getInstance().syncEngine().observe(tms)
    val synchronizer = TModeler.getInstance().syncEngine()

}

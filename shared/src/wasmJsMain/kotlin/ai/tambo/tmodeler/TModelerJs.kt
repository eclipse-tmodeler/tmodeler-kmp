@file:OptIn(ExperimentalJsExport::class)

package ai.tambo.tmodeler

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.db.Tdb
import ai.tambo.tmodeler.model.Tstate
import ai.tambo.tmodeler.modeler.TModeler
import ai.tambo.tmodeler.ms.ext.all
import ai.tambo.tmodeler.ms.ext.create
import ai.tambo.tmodeler.ms.ext.update
import ai.tambo.tmodeler.ms.ext.delete
import ai.tambo.tmodeler.ms.result.data
import ai.tambo.tmodeler.scheduler.TmsScope
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * TModeler Kotlin/Wasm public export API for JavaScript and React.
 *
 * In Kotlin/Wasm, top-level functions with primitive, String, Unit, and function
 * parameters/return values are directly exported to the ES module.
 */

private fun getDb(): Tdb? {
    val m = TModeler.getInstance()
    return try {
        m.geTdb("tmodeler") ?: m.geTdb(Tstate().clazz())
    } catch (_: Throwable) {
        null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Database Lifecycle & Universal Access
// ─────────────────────────────────────────────────────────────────────────────

@JsExport
@JsName("tmodelerInitDefault")
fun tmodelerInitDefault(): Boolean {
    return tmodelerInit("tmodeler")
}

@JsExport
@JsName("tmodelerInit")
fun tmodelerInit(dbName: String): Boolean {
    val db = Tdb.Builder()
        .type(Tdb.Type.SQLITE)
        .dbName(dbName)
        .start()
    return if (db != null) {
        TModeler.getInstance().init(db)
        true
    } else {
        false
    }
}

@JsExport
@JsName("tmodelerExecuteSQL")
fun tmodelerExecuteSQL(sql: String): Boolean {
    return try {
        getDb()?.executeSQL(sql) ?: false
    } catch (_: Exception) {
        false
    }
}

@JsExport
@JsName("tmodelerExecuteQuery")
fun tmodelerExecuteQuery(query: String): String {
    return try {
        getDb()?.executeQuery(query) ?: "[]"
    } catch (_: Exception) {
        "[]"
    }
}

@JsExport
@JsName("tmodelerExecuteInserts")
fun tmodelerExecuteInserts(table: String, jsonArray: String): Boolean {
    return try {
        getDb()?.executeInserts(table, jsonArray) ?: false
    } catch (_: Exception) {
        false
    }
}

@JsExport
@JsName("tmodelerExecuteUpdates")
fun tmodelerExecuteUpdates(table: String, jsonArray: String, key: String): Boolean {
    return try {
        getDb()?.executeUpdates(table, jsonArray, key) ?: false
    } catch (_: Exception) {
        false
    }
}

@JsExport
@JsName("tmodelerExecuteUpdatesDefaultKey")
fun tmodelerExecuteUpdatesDefaultKey(table: String, jsonArray: String): Boolean {
    return tmodelerExecuteUpdates(table, jsonArray, "_id")
}

// ─────────────────────────────────────────────────────────────────────────────
// Tstate Model Operations (Application State Store)
// ─────────────────────────────────────────────────────────────────────────────

@JsExport
@JsName("tstatePrototype")
fun tstatePrototype(): String {
    return Tstate().data()
}

@JsExport
@JsName("tstateAll")
fun tstateAll(): String {
    return try {
        val tms = Tstate.tms
        val scope = TmsScope()
        val list = with(scope) {
            tms.all()
        }
        list.data()
    } catch (_: Exception) {
        "[]"
    }
}

@JsExport
@JsName("tstateCreate")
fun tstateCreate(json: String): Double {
    return try {
        val items = TobGb.parse(Tstate(), json)
        if (items.isEmpty()) return -1.0
        val tms = Tstate.tms
        val count = tms.create(items)
        if (count > 0) {
            val first = items.first()
            val id = if (first._id_ > 0) first._id_ else first.key
            id.toDouble()
        } else {
            -1.0
        }
    } catch (_: Exception) {
        -1.0
    }
}

@JsExport
@JsName("tstateUpdate")
fun tstateUpdate(json: String): Int {
    return try {
        val items = TobGb.parse(Tstate(), json)
        if (items.isEmpty()) return 0
        val tms = Tstate.tms
        tms.update(items)
    } catch (_: Exception) {
        0
    }
}

@JsExport
@JsName("tstateDelete")
fun tstateDelete(id: Double): Int {
    return try {
        val targetId = id.toLong()
        val sql = "DELETE FROM _tstate WHERE _id = $targetId;"
        if (tmodelerExecuteSQL(sql)) 1 else 0
    } catch (_: Exception) {
        0
    }
}

@JsExport
@JsName("tstateGetById")
fun tstateGetById(id: Double): String {
    return try {
        val tms = Tstate.tms
        val scope = TmsScope()
        val allItems = with(scope) { tms.all() }
        val targetId = id.toLong()
        val item = allItems.firstOrNull { it._id_ == targetId || it.key == targetId || it.id_ == targetId } ?: return ""
        item.data()
    } catch (_: Exception) {
        ""
    }
}

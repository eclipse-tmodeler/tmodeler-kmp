package ai.tambo.tmodeler.ms.filter

import ai.tambo.tmodeler.core.TobGb
import ai.tambo.tmodeler.db.TtableField

@Suppress("UNCHECKED_CAST")
class TFilterBuilder : TobGb(){

    var tableFields: List<List<TtableField>> = listOf()
    var fieldsKeys: List<List<String>> = listOf()
    var lazyBuild = false

    private val _query = mutableMapOf<String, Any>()

    fun select(): TFilterBuilder = this

    fun distinct(): TFilterBuilder = this



    private var savedSelect: MutableList<String>? = null
    private var savedDistinct: Boolean? = null

    fun backupSelect(): TFilterBuilder {
        savedSelect = (_query["select"] as? MutableList<String>)?.toMutableList()
        savedDistinct = _query["distinct"] as? Boolean
        return this
    }

    fun restoreSelect(): TFilterBuilder {
        if (savedSelect != null) {
            _query["select"] = savedSelect!!
        } else {
            _query.remove("select")
        }

        if (savedDistinct != null) {
            _query["distinct"] = savedDistinct!!
        } else {
            _query.remove("distinct")
        }

        return this
    }



    fun clearSelect(): TFilterBuilder {
        _query.remove("select")
        _query.remove("distinct")
        return this
    }

    fun select(clause: SelectClause): TFilterBuilder {
        _query["select"] = clause.columns.toMutableList()
        _query["distinct"] = clause.distinct
        return this
    }

    fun addSelect(column: String): TFilterBuilder {
        val select = _query.getOrPut("select") { mutableListOf<String>() } as MutableList<String>
        select.add(column)
        return this
    }

    fun addSelect(clause: SelectClause): TFilterBuilder {
        val select = _query.getOrPut("select") { mutableListOf<String>() } as MutableList<String>
        select.addAll(clause.columns)
        if (_query["distinct"] != true) {
            _query["distinct"] = clause.distinct
        }
        return this
    }

    fun from(table: String): TFilterBuilder {
        _query["from"] = table
        return this
    }

    fun where(conditions: List<WhereCondition>): TFilterBuilder {
        val whereList = _query.getOrPut("where") { mutableListOf<Map<String, String>>() } as MutableList<Map<String, String>>
        for (cond in conditions) {
            whereList.add(mapOf("column" to cond.column, "op" to cond.op, "value" to cond.value))
        }
        return this
    }

    fun joins(joinList: List<JoinClause>): TFilterBuilder {
        val joins = _query.getOrPut("joins") { mutableListOf<Map<String, String>>() } as MutableList<Map<String, String>>
        for (j in joinList) {
            joins.add(mapOf("type" to j.type, "table" to j.table, "on" to j.on))
        }
        return this
    }

    fun groupBy(columns: List<String>): TFilterBuilder {
        _query["groupBy"] = columns
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun addGroupBy(column: String): TFilterBuilder {
        val groupBy = _query.getOrPut("groupBy") { mutableListOf<String>() } as MutableList<String>
        groupBy.add(column)
        return this
    }

    fun having(condition: String): TFilterBuilder {
        _query["having"] = condition
        return this
    }

    fun orderBy(clause: OrderByClause): TFilterBuilder {
        _query["orderBy"] = clause.columns
        return this
    }

    fun limit(max: Int): TFilterBuilder {
        _query["limit"] = max
        return this
    }

    fun offset(skip: Int): TFilterBuilder {
        if (skip > 0) {
            _query["offset"] = skip
        }
        return this
    }

    fun buildSQL(): String {
        val sql = StringBuilder()
        sql.append(parseSelect())

        if (_query.containsKey("from")) {
            sql.append(" FROM ${_query["from"]}")
        }

        sql.append(" ").append(parseJoins())
        sql.append(" ").append(parseWhere())
        sql.append(" ").append(parseGroupBy())
        sql.append(" ").append(parseHaving())
        sql.append(" ").append(parseOrderBy())
        sql.append(" ").append(parseLimit())

        return sql.toString().trim()
    }

    private fun join(vec: List<String>, delimiter: String): String =
        vec.joinToString(delimiter)

    @Suppress("UNCHECKED_CAST")
    private fun parseSelect(): String {
        val select = _query["select"] as? List<String> ?: return "SELECT *"
        val distinct = _query["distinct"] == true
        val clause = if (select.size == 1 && select[0] == "*") "*" else join(select, ", ")
        return if (distinct) "SELECT DISTINCT $clause" else "SELECT $clause"
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJoins(): String {
        val joins = _query["joins"] as? List<Map<String, String>> ?: return ""
        return joins.joinToString(" ") {
            "${it["type"]} JOIN ${it["table"]} ON ${it["on"]}"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseWhere(): String {
        val whereList = _query["where"] as? List<Map<String, String>> ?: return ""
        val conditions = whereList.map {
            val op = it["op"].orEmpty()
            val valStr = if (op.isEmpty()) it["value"].orEmpty() else {
                val raw = it["value"].orEmpty()
                val quoted = if (raw.any { c -> !c.isDigit() }) "'$raw'" else raw
                "${it["column"]} $op $quoted"
            }
            valStr
        }
        return if (conditions.isEmpty()) "" else "WHERE ${join(conditions, " AND ")}"
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseGroupBy(): String {
        val groupBy = _query["groupBy"] as? List<String> ?: return ""
        return if (groupBy.isNotEmpty()) "GROUP BY ${join(groupBy, ", ")}" else ""
    }

    private fun parseHaving(): String {
        val having = _query["having"] as? String ?: return ""
        return if (having.isNotBlank()) "HAVING $having" else ""
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseOrderBy(): String {
        val orderBy = _query["orderBy"] as? List<String> ?: return ""
        return if (orderBy.isNotEmpty()) "ORDER BY ${join(orderBy, ", ")}" else ""
    }

    private fun parseLimit(): String {
        val limit = _query["limit"] as? Int ?: return ""
        val offset = _query["offset"] as? Int ?: 0

        return if (offset > 0) {
            "LIMIT $limit OFFSET $offset"
        } else {
            "LIMIT $limit"
        }
    }
}

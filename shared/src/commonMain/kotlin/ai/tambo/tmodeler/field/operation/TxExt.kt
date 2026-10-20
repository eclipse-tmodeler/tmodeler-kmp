package ai.tambo.tmodeler.field.operation

import ai.tambo.tmodeler.field.advanced.GeoField
import ai.tambo.tmodeler.field.advanced.ModelFieldGb
import ai.tambo.tmodeler.field.base.BaseField
import ai.tambo.tmodeler.field.utils.geometry.TGeo

fun Tx.opToString(): String {
    return when (op) {
        // Top.NONE -> ""
        Top.GreaterThan -> ">"
        Top.LessThan -> "<"
        Top.Equal -> "="
        Top.NotEqual -> "!="
        Top.In -> "IN"
        Top.Like -> "LIKE"
        Top.GreaterThanEqual -> ">="
        Top.LessThanEqual -> "<="
        Top.Between -> "BETWEEN"
        Top.IsNull -> "IS NULL"
        Top.And -> " AND "
        Top.Or -> " OR "
        Top.Not -> " NOT "

        Top.Asc -> "ASC"
        Top.Desc -> "DESC"

        Top.GroupBy -> " GROUP BY "

        Top.Max -> "MAX"
        Top.Min -> "MIN"
        Top.Avg -> "AVG"
        Top.Sum -> "SUM"
        Top.Count -> "COUNT"
        Top.CountDistinct -> "COUNT(DISTINCT"

        // Opérateurs spatiaux - test (booléens)
        Top.ST_Equals -> "ST_Equals"
        Top.ST_Contains -> "ST_Contains"
        Top.ST_Within -> "ST_Within"
        Top.ST_Intersects -> "ST_Intersects"
        Top.MbrIntersects -> "MbrIntersects"
        Top.ST_Touches -> "ST_Touches"
        Top.ST_Overlaps -> "ST_Overlaps"
        Top.ST_Crosses -> "ST_Crosses"
        Top.ST_Disjoint -> "ST_Disjoint"

        // Opérateurs spatiaux - calculs / mesures
        Top.ST_Distance -> "ST_Distance"
        Top.ST_Length -> "ST_Length"
        Top.ST_Area -> "ST_Area"
        Top.ST_Perimeter -> "ST_Perimeter"
        Top.ST_NumPoints -> "ST_NumPoints"
        Top.ST_Dimension -> "ST_Dimension"
        Top.ST_GeometryType -> "ST_GeometryType"
        Top.ST_IsClosed -> "ST_IsClosed"
        Top.ST_X -> "ST_X"
        Top.ST_Y -> "ST_Y"
        Top.ST_Azimuth -> "ST_Azimuth"

        // Transformations (si utilisées comme expressions)
        Top.ST_Buffer -> "ST_Buffer"
        Top.ST_Intersection -> "ST_Intersection"

        else -> ""
    }
}

fun Tx.toScript(): String {
    val expr = StringBuilder()

    if (op == Top.And || op == Top.Or || op == Top.Not) {
        if (op == Top.Not) {
            expr.append(opToString())
                .append("(")
                .append(v1?.toString())
                .append(")")
        } else {
            expr.append("(")
                .append(v1?.toString())
                .append(opToString())
                .append(v2?.toString())
                .append(")")
        }
    } else if (isGeoOp()) {
        when (op) {
            Top.ST_Equals,
            Top.ST_Intersects,
            Top.MbrIntersects,
            Top.ST_Within,
            Top.ST_Contains,
            Top.ST_Touches,
            Top.ST_Crosses,
            Top.ST_Overlaps,
            Top.ST_Disjoint -> {
                expr.append(" ")
                    .append(opToString())
                    .append("(")
                    .append(fieldKey)
                    .append(", ")
                    .append(field?.applyFunc(if (geoValue?.hasFunction() == true) "+" else "", geoValue?.toString() ?: ""))
                    .append(") ")
            }

            else -> {}
        }
    } else {
        if (field != null && field!!.hasAggr()) {
            expr.append(field!!.aggr.opToString())
                .append("(")
                .append(fieldKey)
                .append(")")

            if (field != null && field!!.hasAggr()) {
                field!!.initTmpAggr()
            }
        } else {
            expr.append(
                if (field?.hasFunction() == true && op != Top.IsNull)
                    field!!.applyFunc(fieldKey, "")
                else
                    fieldKey
            )
        }

        if (op != Top.GroupBy) {
            expr.append(" ").append(opToString()).append(" ")
        }

        if (op != Top.IsNull &&
            op != Top.Asc &&
            op != Top.Desc &&
            op != Top.GroupBy
        ) {
            when (val arg = value) {
                is String -> {
                    if (op != Top.In && !isJoining()) {
                        expr.append("\"").append(arg).append("\"")
                    } else {
                        expr.append(arg)
                    }
                }
                else -> {
                    expr.append(arg)
                }
            }
        }
    }

    return expr.toString()
}

fun Tx.prepare(valGeo: TGeo? = null) {
    var function: ((String, String) -> String)?=null

    val strOp = opToString()
    val geo = field as? GeoField

    if (geo != null) {
        when (op) {
            Top.ST_Distance,
            Top.ST_Azimuth -> {
                function = { fieldKey: String, wkt: String ->
                    "$strOp($fieldKey, ${geo._baseFunc}('${valGeo?.toString()}', ${geo._srid}))"
                }
            }

            Top.ST_Length,
            Top.ST_Area,
            Top.ST_Perimeter,
            Top.ST_NumPoints,
            Top.ST_Dimension,
            Top.ST_GeometryType,
            Top.ST_IsClosed,
            Top.ST_X,
            Top.ST_Y -> {
                function = { fieldKey: String, _: String ->
                    "$strOp($fieldKey)"
                }
            }

            Top.NONE -> {
                function = { key: String, wkt: String ->
                    if (key == "+") {
                        wkt
                    } else {
                        "${geo._baseFunc}('$wkt', ${geo._srid})"
                    }
                }
            }

            else -> {}
        }
    }

    function?.let { f ->
        field?.addFunc(f)
    }
}

fun Tx.Companion.prepare(f: BaseField?, baseFunc: String, srid: Int) {
    f?.clearFunc()

    f?.addFunc { key: String, wkt: String ->
        if (key == "+") {
            wkt
        } else {
            "$baseFunc('$wkt', $srid)"
        }
    }
}

fun Tx.splitOrderExpr(): Pair<String, String> {
    val v0 = field!!._functions.size
    val strOrder = toString()
    val v1 = field!!._functions.size

    var trimmed = strOrder
    var dir = "ASC"

    // Nettoyage des espaces en fin de chaîne
    while (trimmed.isNotEmpty() && trimmed.last().isWhitespace()) {
        trimmed = trimmed.dropLast(1)
    }

    if (trimmed.length >= 4 &&
        (trimmed.substring(trimmed.length - 4) == " ASC" || trimmed.substring(trimmed.length - 4) == " asc")) {
        dir = "ASC"
        trimmed = trimmed.substring(0, trimmed.length - 4)
    } else if (trimmed.length >= 5 &&
        (trimmed.substring(trimmed.length - 5) == " DESC" || trimmed.substring(trimmed.length - 5) == " desc")) {
        dir = "DESC"
        trimmed = trimmed.substring(0, trimmed.length - 5)
    }

    return Pair(trimmed, dir)
}

fun Tx.notify(bf: BaseField?, targetModel: String, key: String) {
    bf?.resetFuncIndex(1)

    //this.targetModel = targetModel // ATTENTION

    if (v1 != null) {
        v1?.notify(bf, targetModel, key)
    }
    if (v2 != null) {
        v2?.notify(bf, targetModel, key)
    }
    if (bf!=null && bf sameInstance field) {
        this.targetModel = targetModel
        if(field!=null && field is ModelFieldGb<*>) {
            if(this.foreignModel==null)
                this.foreignModel = field!!.getTargetModel().modelName()
            else
                this.foreignModel = targetModel
        }
        fieldKey = key
    }
    /*if (bf == field) {
        fieldKey = key
    }*/
}

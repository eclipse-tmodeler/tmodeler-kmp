package ai.tambo.tmodeler.field.base

class TFconstraints {
    private val all = mutableListOf<TF>()

    fun addIfNotExist(v: TF) {
        if (v !in all) all.add(v)
    }

    fun removeIfExist(v: TF) {
        all.remove(v)
    }

    fun isPrimaryKey() = TF.PRIMARY_KEY in all
    fun isAutoIncrement() = TF.AUTOINCREMENT in all
    fun isNotNull() = TF.NOT_NULL in all
    fun isUnique() = TF.UNIQUE in all
    fun isCheck() = TF.CHECK in all
    fun isForeignKey() = TF.FOREIGN_KEY in all
    fun isDefault() = TF.DEFAULT in all
    fun isIndexed() = TF.INDEX in all

    override fun toString(): String {
        return all.joinToString(", ")
    }

    fun str(): String {
        var result = ""
        for (i in all.indices) {
            result += tf_str(all[i])
            if (i < all.size - 1) {
                result += " "
            }
        }
        return result
    }
}

/*
fun tf_str(type: TFtype): String {
    return type.name
}

fun tf_str(value: TF): String {
    return value.name
}
*/

fun tf_str(type: TFtype): String = when (type) {
    TFtype.INTEGER -> "INTEGER"
    TFtype.VARCHAR -> "VARCHAR"
    TFtype.TEXT    -> "TEXT"
    TFtype.REAL    -> "REAL"
    TFtype.GEO     -> "GEOMETRY"
    TFtype.BLOB    -> "BLOB"
}

fun tf_str(value: TF): String = when (value) {
    TF.PRIMARY_KEY       -> "PRIMARY KEY"
    TF.AUTOINCREMENT     -> "AUTOINCREMENT"
    TF.NOT_NULL          -> "NOT NULL"
    TF.UNIQUE            -> "UNIQUE"
    TF.CHECK             -> "CHECK"
    TF.FOREIGN_KEY       -> "FOREIGN KEY"
    TF.DEFAULT           -> "DEFAULT"
    TF.INDEX             -> "INDEX"
    TF.CASCADE           -> "CASCADE"
    TF.SET_NULL          -> "SET NULL"
    TF.NO_ACTION         -> "NO ACTION"
    TF.RESTRICT          -> "RESTRICT"
    TF.SET_DEFAULT       -> "SET DEFAULT"
    TF.CURRENT_TIMESTAMP -> "CURRENT_TIMESTAMP"
    TF.NONE -> ""
    TF.DATE -> ""
    TF.DATE_TIME -> ""
}
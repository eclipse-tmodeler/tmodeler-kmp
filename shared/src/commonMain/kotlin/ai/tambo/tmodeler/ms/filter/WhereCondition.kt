package ai.tambo.tmodeler.ms.filter

data class WhereCondition(
    val column: String,
    val op: String,
    val value: String
)
package ai.tambo.tmodeler.ms.filter

data class JoinClause(
    val type: String,
    val table: String,
    val on: String
)
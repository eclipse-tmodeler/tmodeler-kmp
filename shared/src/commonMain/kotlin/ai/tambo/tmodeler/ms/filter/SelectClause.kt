package ai.tambo.tmodeler.ms.filter

data class SelectClause(
    val columns: List<String>,
    val distinct: Boolean = false
)
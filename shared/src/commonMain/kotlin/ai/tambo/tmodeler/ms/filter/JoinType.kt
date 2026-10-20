package ai.tambo.tmodeler.ms.filter

enum class JoinType {
    INNER, LEFT, RIGHT, FULL, CROSS, SELF;

    fun str(): String = when (this) {
        JoinType.INNER -> "INNER"
        JoinType.LEFT -> "LEFT"
        JoinType.RIGHT -> "RIGHT"
        JoinType.FULL -> "FULL"
        JoinType.CROSS -> "CROSS"
        JoinType.SELF -> "SELF"
    }
}
package ai.tambo.tmodeler.field.utils.geometry

open class Polygon() : Line() {

    constructor(vararg pts: Spot) : this() {
        add(*pts)
        close()
    }

    init {
        type = Type.Polygon
    }

    fun close() {
        if (size() >= 3) {
            val first = get(0)
            val last = get(size() - 1)
            if (first.x != last.x || first.y != last.y) {
                add(first)
            }
        }
    }

    fun isClosed(): Boolean {
        if (size() < 3) return false
        val first = spots.first()
        val last = spots.last()
        return first.x == last.x && first.y == last.y
    }

    fun validate() {
        if (!isClosed()) throw IllegalStateException("TPolygon must be closed")
    }

    override fun clone(): Polygon {
        val e = this
        return Polygon().apply {
            spots = e.spots
        }
    }

    override fun buffered(p: Double): Polygon {
        return super.buffered(p) as Polygon
    }

    override fun buffer(p: Double): Polygon {
        return super.buffer(p) as Polygon
    }
}
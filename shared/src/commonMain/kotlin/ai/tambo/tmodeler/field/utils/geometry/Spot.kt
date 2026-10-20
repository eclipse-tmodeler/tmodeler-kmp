package ai.tambo.tmodeler.field.utils.geometry

import kotlin.math.*

open class Spot() : TGeo(Type.Spot) {
    var x: Double = 0.0
    var y: Double = 0.0

    constructor(_x: Double, _y: Double) : this() {
        x = _x
        y = _y
        spots.add(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Spot) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return x.hashCode() * 31 + y.hashCode()
    }

    fun projection(a: Spot, b: Spot): Spot {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len2 = dx * dx + dy * dy

        if (len2 == 0.0) return a

        var t = ((x - a.x) * dx + (y - a.y) * dy) / len2
        t = max(0.0, min(1.0, t))

        return Spot(a.x + t * dx, a.y + t * dy)
    }

    fun distance(other: Spot): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    override fun clone(): Spot {
        val e = this
        return Spot().apply {
            spots = e.spots
        }
    }

    override fun buffered(p: Double): Spot {
        return super.buffered(p) as Spot
    }

    override fun buffer(p: Double): Spot {
        return super.buffer(p) as Spot
    }
}
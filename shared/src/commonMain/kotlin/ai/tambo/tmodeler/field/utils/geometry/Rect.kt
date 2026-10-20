package ai.tambo.tmodeler.field.utils.geometry

import kotlin.math.*

class Rect : Polygon {

    constructor() : super()

    constructor(a: Spot, b: Spot, c: Spot, d: Spot) : this() {
        val points = listOf(a, b, c, d, a)
        for (i in 0 until 4) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (p1.x != p2.x && p1.y != p2.y) {
                throw IllegalStateException("Two consecutive points are not axis-aligned")
            }
        }
        add(a, b, c, d, a)
        type = Type.Polygon
    }

    fun xmin(): Double = (0 until 4).minOf { get(it).x }
    fun xmax(): Double = (0 until 4).maxOf { get(it).x }
    fun ymin(): Double = (0 until 4).minOf { get(it).y }
    fun ymax(): Double = (0 until 4).maxOf { get(it).y }

    fun gravity(): Spot {
        val cx = (xmin() + xmax()) / 2.0
        val cy = (ymin() + ymax()) / 2.0
        return Spot(cx, cy)
    }

    fun cross(l: TGeo): Boolean {
        val r = l.bound()
        val overlapX = !(r.xmax() < xmin() || r.xmin() > xmax())
        val overlapY = !(r.ymax() < ymin() || r.ymin() > ymax())
        return overlapX && overlapY
    }

    fun contains(l: TGeo): Boolean {
        val r = l.bound()
        return r.xmin() >= xmin() && r.xmax() <= xmax() && r.ymin() >= ymin() && r.ymax() <= ymax()
    }

    override fun bound(): Rect = this

    fun area(): Double {
        val a = get(0)
        val c = get(2)
        val width = abs(c.x - a.x)
        val height = abs(c.y - a.y)
        return width * height
    }

    override fun clone(): Rect {
        val e = this
        return Rect().apply {
            spots = e.spots
        }
    }

    override fun buffered(p: Double): Rect {
        return super.buffered(p) as Rect
    }

    override fun buffer(p: Double): Rect {
        return super.buffer(p) as Rect
    }
}
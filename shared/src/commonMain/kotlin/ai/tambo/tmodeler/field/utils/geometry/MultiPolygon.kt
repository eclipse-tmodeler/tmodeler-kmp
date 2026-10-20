package ai.tambo.tmodeler.field.utils.geometry

import kotlin.math.*

class MultiPolygon() : TGeo(Type.MultiPolygon) {

    constructor(vararg polys: Polygon) : this() {
        add(*polys)
    }

    fun add(vararg ps: Polygon) {
        polygons.addAll(ps)
    }

    fun get(i: Int): Polygon = polygons[i]

    fun size(): Int = polygons.size

    fun clear() {
        polygons.clear()
    }

    fun all(): List<Polygon> = polygons

    override fun bound(): Rect {
        if (polygons.isEmpty()) return Rect()
        var r = polygons[0].bound()
        for (i in 1 until polygons.size) {
            val b = polygons[i].bound()
            val xmin = min(r.xmin(), b.xmin())
            val ymin = min(r.ymin(), b.ymin())
            val xmax = max(r.xmax(), b.xmax())
            val ymax = max(r.ymax(), b.ymax())
            r = Rect(Spot(xmin, ymin), Spot(xmin, ymax), Spot(xmax, ymax), Spot(xmax, ymin))
        }
        return r
    }

    override fun clone(): MultiPolygon {
        val e = this
        return MultiPolygon().apply {
            polygons = e.polygons
        }
    }

    override fun buffered(p: Double): MultiPolygon {
        return super.buffered(p) as MultiPolygon
    }

    override fun buffer(p: Double): MultiPolygon {
        return super.buffer(p) as MultiPolygon
    }
}

package ai.tambo.tmodeler.field.utils.geometry

import kotlin.math.*

open class Line() : TGeo(Type.Line) {

    constructor(vararg pts: Spot) : this() {
        spots.addAll(pts)
    }

    constructor(pts: List<Spot>) : this() {
        spots.addAll(pts)
    }

    fun add(vararg pts: Spot) {
        spots.addAll(pts)
    }

    fun add(index: Int, s: Spot) {
        spots.add(index, s)
    }

    fun set(index: Int, s: Spot) {
        spots[index] = s
    }

    fun get(index: Int): Spot = spots[index]

    fun first(): Spot = spots.first()

    fun last(): Spot = spots.last()

    fun remove(index: Int) {
        spots.removeAt(index)
    }

    fun size(): Int = spots.size

    fun isEmpty(): Boolean = spots.isEmpty()

    fun clear() {
        spots.clear()
    }

    fun all(): List<Spot> = spots

    override fun bound(): Rect {
        if (size() == 0) {
            throw IllegalStateException("Cannot compute bound of empty location")
        }

        var minX = get(0).x
        var maxX = minX
        var minY = get(0).y
        var maxY = minY

        for (i in 1 until size()) {
            val p = get(i)
            val x = p.x
            val y = p.y

            minX = kotlin.math.min(minX, x)
            maxX = kotlin.math.max(maxX, x)
            minY = kotlin.math.min(minY, y)
            maxY = kotlin.math.max(maxY, y)
        }

        val a = Spot(minX, minY)
        val b = Spot(maxX, minY)
        val c = Spot(maxX, maxY)
        val d = Spot(minX, maxY)

        return Rect(a, b, c, d)
    }

    fun length(): Double {
        if (spots.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until spots.size) {
            val a = spots[i - 1]
            val b = spots[i]
            total += sqrt((b.x - a.x).pow(2) + (b.y - a.y).pow(2))
        }
        return total
    }

    fun projection(p: Spot): Spot {
        var minDist = Double.MAX_VALUE
        var closest = Spot()
        for (i in 1 until size()) {
            val a = get(i - 1)
            val b = get(i)
            val proj = p.projection(a, b)
            val dist = proj.distance(p)
            if (dist < minDist) {
                minDist = dist
                closest = proj
            }
        }
        return closest
    }

    fun distance(spot: Spot): Double {
        val p = projection(spot)
        return spot.distance(p)
    }

    fun sub(s1: Spot, s2: Spot): Line {
        if (spots.isEmpty()) return Line()

        // 1. Trouver projection de s1 et s2 sur la ligne, ainsi que l'indice de segment correspondant
        fun projectPoint(p: Spot): Pair<Spot, Int> {
            var minDist = Double.MAX_VALUE
            var closest = Spot(0.0, 0.0)
            var closestSegment = 0

            for (i in 0 until spots.size - 1) {
                val a = spots[i]
                val b = spots[i + 1]
                val proj = p.projection(a, b)
                val dist = proj.distance(p)
                if (dist < minDist) {
                    minDist = dist
                    closest = proj
                    closestSegment = i
                }
            }
            return Pair(closest, closestSegment)
        }

        var (proj1, seg1) = projectPoint(s1)
        var (proj2, seg2) = projectPoint(s2)

        // 2. S'assurer que la portion est dans le bon sens
        var reversed = false
        if (seg2 < seg1 || (seg1 == seg2 && proj2.distance(spots[seg1]) < proj1.distance(spots[seg1]))) {
            val tmpProj = proj1
            proj1 = proj2
            proj2 = tmpProj

            val tmpSeg = seg1
            seg1 = seg2
            seg2 = tmpSeg

            reversed = true
        }

        // 3. Construire la sous-ligne
        val subPoints = mutableListOf<Spot>()
        subPoints.add(proj1)

        // Ajouter les points intermédiaires
        for (i in seg1 + 1..seg2) {
            subPoints.add(spots[i])
        }

        if (subPoints.size > 1) {
            // Remplacer le dernier point par la projection exacte s2
            subPoints[subPoints.lastIndex] = proj2
        } else {
            subPoints.add(proj2)
        }

        // 4. Si ordre inversé, on inverse la ligne
        if (reversed) {
            subPoints.reverse()
        }

        return Line(subPoints)
    }

    override fun clone(): Line {
        val e = this
        return Line().apply {
            spots = e.spots
        }
    }

    override fun buffered(p: Double): Line {
        return super.buffered(p) as Line
    }

    override fun buffer(p: Double): Line {
        return super.buffer(p) as Line
    }
}
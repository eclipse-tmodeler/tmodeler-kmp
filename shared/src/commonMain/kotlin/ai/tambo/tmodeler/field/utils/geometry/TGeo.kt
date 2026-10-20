package ai.tambo.tmodeler.field.utils.geometry

open class TGeo() {
    /*enum class Type {
        Spot,
        Line,
        Polygon,
        MultiPolygon;
    }*/
    enum class Type(val code: String) {
        Spot("Point"),
        Line("LineString"),
        Polygon("Polygon"),
        MultiPolygon("MultiPolygon");
    }

    var filterFunction: String = "ST_GeomFromText"
    var srid: Int = 4326
    var _buffer: Double = 0.0

    var style: DrawingStyle = DrawingStyle()
    var label: String = ""


    var type: TGeo.Type = TGeo.Type.Spot
    var bufferDistance: Int = 5
    var distanceThreshold: Int = 10


    var spots: MutableList<Spot> = mutableListOf()
    var polygons: MutableList<Polygon> = mutableListOf()


    constructor(t: Type) : this() {
        type = t
    }

    open fun bound(): Rect = Rect()

    fun add(s: Spot) {
        spots.add(s)
    }

    // --- Type checks ---
    fun isSpot(): Boolean {
        return type == Type.Spot
    }

    fun isLine(): Boolean {
        return type == Type.Line
    }

    fun isPolygon(): Boolean {
        return type == Type.Polygon
    }

    fun isRect(): Boolean {
        if (spots.size != 5 || spots[0] != spots[4]) {
            return false
        }

        val points = listOf(spots[0], spots[1], spots[2], spots[3], spots[4])

        for (i in 0 until 4) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val isAligned = (p1.x == p2.x) || (p1.y == p2.y)
            if (!isAligned) {
                return false
            }
        }

        return true
    }

    fun isMultiPolygon(): Boolean {
        return type == Type.MultiPolygon
    }

    fun asSpot(): Spot {
        if (!isSpot() || spots.isEmpty()) {
            throw IllegalStateException("TGeo: not a Spot")
        }
        return spots[0]
    }

    fun asLine(): Line {
        if (!isLine()) {
            throw IllegalStateException("TGeo: not a Line")
        }

        val line = Line()
        for (s in spots) {
            line.add(s)
        }
        return line
    }

    fun asPolygon(): Polygon {
        if (!isPolygon()) {
            throw IllegalStateException("TGeo: not a Polygon")
        }

        val poly = Polygon()
        for (s in spots) {
            poly.add(s)
        }
        return poly
    }

    fun asRect(): Rect {
        if (!isRect()) {
            throw IllegalStateException("TGeo: not a Rect")
        }
        return Rect(spots[0], spots[1], spots[2], spots[3])
    }

    fun asMultiPolygon(): MultiPolygon {
        if (!isMultiPolygon()) {
            throw IllegalStateException("TGeo: not a MultiPolygon")
        }

        val multiPoly = MultiPolygon()
        for (p in polygons) {
            multiPoly.add(p)
        }
        return multiPoly
    }

    // --- String conversion ---
    fun toStringRepresentation(): String = ""

    companion object {

    }

    fun hasFunction(): Boolean {
        return _buffer > 0
    }

    override fun toString(): String {
        return _toString()
    }

    open fun buffered(p: Double): TGeo {
        return this.clone().apply { buffer(p) }
    }

    open fun buffer(p: Double): TGeo {
        this._buffer = p
        return this
    }

    open fun clone(): TGeo {
        val e = this
        return TGeo().apply {
            type = e.type
            spots = e.spots
        }
    }
}
package ai.tambo.tmodeler.field.utils.geometry

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun TGeo._toString(): String {
    val v = this
    val str = when (v.type) {
        TGeo.Type.Spot -> {
            val spot = v as Spot
            "POINT(${spot.spots[0].x} ${spot.spots[0].y})"
        }
        TGeo.Type.Line -> {
            val line = v as Line
            "LINESTRING(" + line.spots.joinToString(", ") { "${it.x} ${it.y}" } + ")"
        }
        TGeo.Type.Polygon -> {
            val poly = v as Polygon
            poly.validate()
            "POLYGON((" + poly.spots.joinToString(", ") { "${it.x} ${it.y}" } + "))"
        }
        TGeo.Type.MultiPolygon -> {
            val parts = v.polygons.map { poly ->
                poly.validate()
                "((" + poly.spots.joinToString(", ") { "${it.x} ${it.y}" } + "))"
            }
            "MULTIPOLYGON(${parts.joinToString(", ")})"
        }
        else -> throw IllegalStateException("Unsupported geometry type")
    }

    return if (this._buffer != 0.0) {
        "ST_Buffer(${this.filterFunction}('$str', ${this.srid}), ${this._buffer})"
    } else {
        str
    }
}

fun TGeo.Companion.fromString(str: String): TGeo {
    fun String.trimmed(): String = this.trim()

    fun parseSpot(token: String): Spot {
        val parts = token.trimmed().split(" ")
        val x = parts[0].toDouble()
        val y = parts[1].toDouble()
        return Spot(x, y)
    }

    /*return when {
        str.startsWith("POINT(") -> {
            val coords = str.substringAfter("(").substringBefore(")")
            val spot = parseSpot(coords)
            TGeo(TGeo.Type.Spot).apply { spots.add(spot) }
        }
        str.startsWith("LINESTRING(") -> {
            val coords = str.substringAfter("(").substringBefore(")")
            val line = Line()
            coords.split(",").forEach { token ->
                line.add(parseSpot(token))
            }
            TGeo(TGeo.Type.Line).apply { spots = line.spots }
        }
        str.startsWith("POLYGON((") -> {
            val coords = str.substringAfter("((").substringBefore("))")
            val poly = Polygon()
            coords.split(",").forEach { token ->
                poly.add(parseSpot(token))
            }
            TGeo(TGeo.Type.Polygon).apply { spots = poly.spots }
        }
        str.startsWith("MULTIPOLYGON(") -> {
            val all = str.substringAfter("(").substringBeforeLast(")")
            val polys = mutableListOf<Polygon>()
            var content = all
            while ("((" in content) {
                val coords = content.substringAfter("((").substringBefore("))")
                content = content.substringAfter("))", "")
                val poly = Polygon()
                coords.split(",").forEach { token ->
                    poly.add(parseSpot(token))
                }
                polys.add(poly)
            }
            TGeo(TGeo.Type.MultiPolygon).apply { polygons = polys }
        }
        else -> throw IllegalArgumentException("Unsupported or malformed geometry string: $str")
    }*/

    return when {
        str.startsWith("POINT(") -> {
            val coords = str.substringAfter("(").substringBefore(")")
            val spot = parseSpot(coords)
            Spot().apply { spots.add(spot) }
        }
        str.startsWith("LINESTRING(") -> {
            val coords = str.substringAfter("(").substringBefore(")")
            val line = Line()
            coords.split(",").forEach { token ->
                line.add(parseSpot(token))
            }
            Line().apply { spots = line.spots }
        }
        str.startsWith("POLYGON((") -> {
            val coords = str.substringAfter("((").substringBefore("))")
            val poly = Polygon()
            coords.split(",").forEach { token ->
                poly.add(parseSpot(token))
            }
            Polygon().apply { spots = poly.spots }
        }
        str.startsWith("MULTIPOLYGON(") -> {
            val all = str.substringAfter("(").substringBeforeLast(")")
            val polys = mutableListOf<Polygon>()
            var content = all
            while ("((" in content) {
                val coords = content.substringAfter("((").substringBefore("))")
                content = content.substringAfter("))", "")
                val poly = Polygon()
                coords.split(",").forEach { token ->
                    poly.add(parseSpot(token))
                }
                polys.add(poly)
            }
            MultiPolygon().apply { polygons = polys }
        }
        else -> throw IllegalArgumentException("Unsupported or malformed geometry string: $str")
    }
}

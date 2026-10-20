package ai.tambo.tmodeler.field.utils.geometry

import ai.tambo.tmodeler.field.media.utils.TFile
import kotlinx.serialization.json.*

enum class CoordOrder { LAT_LON, LON_LAT }

class GeoBuilder {

    /**
     * Construit un TGeo à partir d'un JSON (GeoJSON-like)
     * @param json Le texte JSON complet
     * @param typePath Chemin vers le type ("geometry.type")
     * @param coordsPath Chemin vers les coordonnées ("geometry.coordinates")
     */
    fun build(
        data: TFile,
        typePath: String = "geometry.type",
        coordsPath: String = "geometry.coordinates",
        autoClose: Boolean = true,
        systemCoordOrder: CoordOrder = CoordOrder.LON_LAT,
        dataCoordOrder: CoordOrder = CoordOrder.LON_LAT
    ): TGeo {
        return build(
            data = data.readText(),
            typePath = typePath,
            coordsPath = coordsPath,
            autoClose = autoClose,
            systemCoordOrder = systemCoordOrder,
            dataCoordOrder = dataCoordOrder
        )
    }

    /**
     * Construit un TGeo à partir d'un JSON (GeoJSON-like)
     * @param data Le texte JSON complet
     * @param typePath Chemin vers le type ("geometry.type")
     * @param coordsPath Chemin vers les coordonnées ("geometry.coordinates")
     */
    fun build(
        data: String,
        typePath: String = "geometry.type",
        coordsPath: String = "geometry.coordinates",
        autoClose: Boolean = true,
        systemCoordOrder: CoordOrder = CoordOrder.LON_LAT,
        dataCoordOrder: CoordOrder = CoordOrder.LON_LAT,
        geoType: TGeo.Type? = null
    ): TGeo {

        val root = try {
            Json.parseToJsonElement(data)
        } catch (_: Exception) {
            return TGeo.fromString(data)
        }

        var (type, coords) = if (coordsPath.isNotEmpty()) {
            val t = geoType?.code?:getByPath(root, typePath)?.jsonPrimitive?.content
                ?: error("Geometry type not found at path: $typePath")
            val c = getByPath(root, coordsPath)
                ?: error("Coordinates not found at path: $coordsPath")
            t to c
        } else {
            val t = geoType?.code?:TGeo.Type.Polygon.code
            val c = root
            t to c
        }

        return when (type.uppercase()) {
            "POINT" -> Spot().apply {
                spots.add(parseSpot(coords, systemCoordOrder, dataCoordOrder))
            }
            "LINESTRING" -> Line().apply {
                spots = parseLine(coords, systemCoordOrder, dataCoordOrder).spots
            }
            "POLYGON" -> Polygon().apply {
                spots = parsePolygon(coords, systemCoordOrder, dataCoordOrder).spots
                if (autoClose) close()
            }
            "MULTIPOLYGON" -> MultiPolygon().apply {
                polygons = parseMultiPolygon(coords, autoClose, systemCoordOrder, dataCoordOrder)
            }
            else -> error("Unsupported geometry type: $type")
        }
    }

    private fun parseSpot(
        json: JsonElement,
        systemCoordOrder: CoordOrder,
        dataCoordOrder: CoordOrder
    ): Spot {
        val arr = json.jsonArray
        val (lonIndex, latIndex) = when (dataCoordOrder) {
            CoordOrder.LON_LAT -> 0 to 1
            CoordOrder.LAT_LON -> 1 to 0
        }

        val lon = arr[lonIndex].jsonPrimitive.double
        val lat = arr[latIndex].jsonPrimitive.double

        return when (systemCoordOrder) {
            CoordOrder.LON_LAT -> Spot(lon, lat)
            CoordOrder.LAT_LON -> Spot(lat, lon)
        }
    }

    private fun parseLine(json: JsonElement, systemCoordOrder: CoordOrder, dataCoordOrder: CoordOrder): Line {
        val arr = json.jsonArray
        val line = Line()
        arr.forEach { line.add(parseSpot(it, systemCoordOrder, dataCoordOrder)) }
        return line
    }

    private fun parsePolygon(json: JsonElement, systemCoordOrder: CoordOrder, dataCoordOrder: CoordOrder): Polygon {
        val outer = json.jsonArray.first() // généralement le premier anneau
        val poly = Polygon()
        outer.jsonArray.forEach { poly.add(parseSpot(it, systemCoordOrder, dataCoordOrder)) }
        return poly
    }

    private fun parseMultiPolygon(json: JsonElement, autoClose: Boolean, systemCoordOrder: CoordOrder, dataCoordOrder: CoordOrder): MutableList<Polygon> {
        val list = mutableListOf<Polygon>()
        json.jsonArray.forEach { list.add(
            parsePolygon(it, systemCoordOrder, dataCoordOrder).apply {
                if (autoClose) close()
            }
        ) }
        return list
    }

    /**
     * Navigation générique dans le JSON (ex: "geometry.coordinates")
     */
    private fun getByPath(root: JsonElement, path: String): JsonElement? {
        var current = root
        for (key in path.split(".")) {
            current = when (current) {
                is JsonObject -> current[key] ?: return null
                else -> return null
            }
        }
        return current
    }
}


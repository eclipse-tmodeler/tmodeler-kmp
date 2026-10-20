package ai.tambo.tmodeler.field.operation

import ai.tambo.tmodeler.field.advanced.GeoField
import ai.tambo.tmodeler.field.utils.geometry.TGeo


//fonctions spatiales simples
fun GeoField.equals(v: TGeo): Tx {
    val x = Tx(this, Top.ST_Equals)
    x.geoValue = v
    return x
}

fun GeoField.contains(content: TGeo): Tx {
    val x = Tx(this, Top.ST_Contains)
    x.geoValue = content
    return x
}

fun GeoField.intersects(v: TGeo): Tx {
    val x = Tx(this, Top.ST_Intersects)
    x.geoValue = v
    return x
}

fun GeoField.index(v: TGeo): Tx {
    return intersectsMbr(v)
}

fun GeoField.intersectsMbr(v: TGeo): Tx {
    val x = Tx(this, Top.MbrIntersects)
    x.geoValue = v
    return x
}

fun GeoField.intersectsOpt(v: TGeo): Tx {
    return index(v) and intersects(v)
}

fun GeoField.inside(container: TGeo): Tx {
    val x = Tx(this, Top.ST_Within)
    x.geoValue = container
    return x
}

fun GeoField.dWithin(v: TGeo): Tx {
    val x = Tx(this, Top.ST_DWithin)
    x.geoValue = v
    return x
}

fun GeoField.touches(v: TGeo): Tx {
    val x = Tx(this, Top.ST_Touches)
    x.geoValue = v
    return x
}

fun GeoField.crosses(v: TGeo): Tx {
    val x = Tx(this, Top.ST_Crosses)
    x.geoValue = v
    return x
}

fun GeoField.overlaps(v: TGeo): Tx {
    val x = Tx(this, Top.ST_Overlaps)
    x.geoValue = v
    return x
}

fun GeoField.disjoint(v: TGeo): Tx {
    val x = Tx(this, Top.ST_Disjoint)
    x.geoValue = v
    return x
}


// advanced
fun GeoField.distance(v: TGeo): GeoField {
    val x = Tx(this, Top.ST_Distance)
    x.prepare(v)
    val v = _functions.size
    return this
}

fun GeoField.length(): GeoField {
    val x = Tx(this, Top.ST_Length)
    x.prepare()
    return this
}

fun GeoField.area(): GeoField {
    val x = Tx(this, Top.ST_Area)
    x.prepare()
    return this
}

fun GeoField.perimeter(): GeoField {
    val x = Tx(this, Top.ST_Perimeter)
    x.prepare()
    return this
}

fun GeoField.azimuth(v: TGeo): GeoField {
    val x = Tx(this, Top.ST_Azimuth)
    x.prepare(v)
    return this
}

fun GeoField.xCoord(): GeoField {
    val x = Tx(this, Top.ST_X)
    x.prepare()
    return this
}

fun GeoField.yCoord(): GeoField {
    val x = Tx(this, Top.ST_Y)
    x.prepare()
    return this
}

fun GeoField.pointsCount(): GeoField {
    val x = Tx(this, Top.ST_NumPoints)
    x.prepare()
    return this
}

fun GeoField.dimension(): GeoField {
    val x = Tx(this, Top.ST_Dimension)
    x.prepare()
    return this
}

fun GeoField.geometryType(): GeoField {
    val x = Tx(this, Top.ST_GeometryType)
    x.prepare()
    return this
}

fun GeoField.isClosed(): GeoField {
    val x = Tx(this, Top.ST_IsClosed)
    x.prepare()
    return this
}



typealias TargetField = GeoField

// ----------- Greater Than ------------
infix fun TargetField.gt(v: Int): Tx = Tx(this, Top.GreaterThan, v)
infix fun TargetField.gt(v: Float): Tx = Tx(this, Top.GreaterThan, v)
infix fun TargetField.gt(v: String): Tx = Tx(this, Top.GreaterThan, v)

// ----------- Less Than ------------
infix fun TargetField.lt(v: Int): Tx = Tx(this, Top.LessThan, v)
infix fun TargetField.lt(v: Float): Tx = Tx(this, Top.LessThan, v)
infix fun TargetField.lt(v: String): Tx = Tx(this, Top.LessThan, v)

// ----------- Equal ------------
infix fun TargetField.eq(v: Int?): Tx = if (v != null) Tx(this, Top.Equal, v) else isNull()
infix fun TargetField.eq(v: Float?): Tx = if (v != null) Tx(this, Top.Equal, v) else isNull()
infix fun TargetField.eq(v: String?): Tx = if (v != null) Tx(this, Top.Equal, v) else isNull()

// ----------- Not Equal ------------
infix fun TargetField.ne(v: Int?): Tx = if (v != null) Tx(this, Top.NotEqual, v) else !isNull()
infix fun TargetField.ne(v: Float?): Tx = if (v != null) Tx(this, Top.NotEqual, v) else !isNull()
infix fun TargetField.ne(v: String?): Tx = if (v != null) Tx(this, Top.NotEqual, v) else !isNull()

// ----------- Greater Than Equal ------------
infix fun TargetField.gte(v: Int): Tx = Tx(this, Top.GreaterThanEqual, v)
infix fun TargetField.gte(v: Float): Tx = Tx(this, Top.GreaterThanEqual, v)
infix fun TargetField.gte(v: String): Tx = Tx(this, Top.GreaterThanEqual, v)

// ----------- Less Than Equal ------------
infix fun TargetField.lte(v: Int): Tx = Tx(this, Top.LessThanEqual, v)
infix fun TargetField.lte(v: Float): Tx = Tx(this, Top.LessThanEqual, v)
infix fun TargetField.lte(v: String): Tx = Tx(this, Top.LessThanEqual, v)

// ----------- Like (only for String) ------------
infix fun TargetField.like(pattern: String): Tx = Tx(this, Top.Like, pattern)

// ----------- In List ------------
//infix fun TargetField.inList(values: List<Int>): Tx = Tx(this, Top.In, values)
//infix fun TargetField.inList(values: List<Float>): Tx = Tx(this, Top.In, values)
//infix fun TargetField.inList(values: List<String>): Tx = Tx(this, Top.In, values)

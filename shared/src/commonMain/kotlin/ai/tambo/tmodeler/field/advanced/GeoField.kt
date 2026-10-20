package ai.tambo.tmodeler.field.advanced

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.common.TextField
import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.field.operation.prepare
import ai.tambo.tmodeler.field.utils.geometry.TGeo
import ai.tambo.tmodeler.field.utils.geometry.fromString

class GeoField : TField<TGeo, GeoField>(TFtype.TEXT) {

    private val depField = TextField()
    private var dep by depField

    override fun getValue(): Any? = depField.get()

    override fun setValue(v: Any?) {
        if(v is String) {
            dep = v
            _value = TGeo.fromString(v)
        }
        else if(v is TGeo) {
            dep = v.toString()
            _value = v
        }
    }



    enum class Type {
        POINT, LINESTRING, POLYGON,
        MULTIPOINT, MULTILINESTRING,
        MULTIPOLYGON, GEOMETRYCOLLECTION, GEOMETRY
    }

    var _geoType: Type = Type.GEOMETRY
    var _srid: Int = 4326
    var _dim: Int = 2
    var _measure: Boolean = false
    var _spatialIndex: Boolean = true
    var _baseFunc: String = "ST_GeomFromText"

    init {
        //defaults(BaseField.NULL_STRING)
        isGeo = true
        prepare()
    }

    override fun prepare() {
        _baseFunc = when (_geoType) {
            Type.GEOMETRY -> "ST_GeomFromText"
            Type.POINT -> "ST_PointFromText"
            Type.LINESTRING -> "ST_LineFromText"
            else -> "ST_GeomFromText"
        }

        Tx.prepare(this, _baseFunc, _srid)
    }

    fun spatialIndex(v: Boolean = true): GeoField {
        _spatialIndex = v
        prepare()
        return this
    }

    fun srid(v: Int): GeoField {
        _srid = v
        prepare()
        return this
    }

    fun type(v: Type): GeoField {
        _geoType = v
        prepare()
        return this
    }

    fun type(v: String): GeoField {
        return type(typeFromStr(v))
    }

    fun dim(v: Int): GeoField {
        if (v >= 2) {
            _dim = v
        }
        return this
    }

    fun measure(v: Boolean): GeoField {
        _measure = v
        return this
    }

    companion object {
        fun typeToStr(type: Type): String = when (type) {
            Type.POINT -> "POINT"
            Type.LINESTRING -> "LINESTRING"
            Type.POLYGON -> "POLYGON"
            Type.MULTIPOINT -> "MULTIPOINT"
            Type.MULTILINESTRING -> "MULTILINESTRING"
            Type.MULTIPOLYGON -> "MULTIPOLYGON"
            Type.GEOMETRYCOLLECTION -> "GEOMETRYCOLLECTION"
            Type.GEOMETRY -> "GEOMETRY"
        }

        fun typeFromStr(str: String): Type = when (str.uppercase()) {
            "POINT" -> Type.POINT
            "LINESTRING" -> Type.LINESTRING
            "POLYGON" -> Type.POLYGON
            "MULTIPOINT" -> Type.MULTIPOINT
            "MULTILINESTRING" -> Type.MULTILINESTRING
            "MULTIPOLYGON" -> Type.MULTIPOLYGON
            "GEOMETRYCOLLECTION" -> Type.GEOMETRYCOLLECTION
            "GEOMETRY" -> Type.GEOMETRY
            else -> Type.GEOMETRY
        }
    }

}
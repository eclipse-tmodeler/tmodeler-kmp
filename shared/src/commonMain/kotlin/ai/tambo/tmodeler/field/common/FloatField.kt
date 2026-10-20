package ai.tambo.tmodeler.field.common

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype

open class FloatField : TField<Double, FloatField>(TFtype.REAL) {}
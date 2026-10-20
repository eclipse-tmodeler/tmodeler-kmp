package ai.tambo.tmodeler.field.common

import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype

open class IntField : TField<Long, IntField>(TFtype.INTEGER) {}
package ai.tambo.tmodeler.ms.result

import ai.tambo.tmodeler.field.operation.Tx
import ai.tambo.tmodeler.model.TBaseModel


fun <T : TBaseModel> Tlist<T>.filter(x: Tx): Tlist<T> = this
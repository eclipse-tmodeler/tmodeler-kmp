package ai.tambo.tmodeler.extension.synchronization.core

import ai.tambo.tmodeler.feature.TFeature
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.model.TModel0
import ai.tambo.tmodeler.ms.result.Tlist

open class TFeatureDeepBase {

    open fun <T : TBaseModel> data(feature: TFeature<T>, firstMainInstances: Tlist<T>): String {
        return ""
    }

    open fun <T : TBaseModel> handleNotification(feature: TFeature<T>, notif: TModel0<*>, result:(Boolean)->Unit) {

    }

}
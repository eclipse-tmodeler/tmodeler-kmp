package ai.tambo.tmodeler.ms.ext


import ai.tambo.tmodeler.db.TModelObserver
import ai.tambo.tmodeler.model.TBaseModel
import ai.tambo.tmodeler.ms.TmsGb
import ai.tambo.tmodeler.ms.observer.ModelCallback
import ai.tambo.tmodeler.ms.observer.TModelCallback

fun <T : TBaseModel> TmsGb<T>.addObserver(observer: TModelObserver) {
    TmsDeep.addObserver(modelClass(), observer)
}

fun <T : TBaseModel> TmsGb<T>.removeObserver(observer: TModelObserver) {
    TmsDeep.removeObserver(modelClass(), observer)
}

fun <T : TBaseModel> TmsGb<T>.onModelChange(c: ModelCallback) {
    _modelCallback?.addOnChangeCallback(c)
}
fun <T : TBaseModel> TmsGb<T>.onChange(c: ModelCallback) {
    _modelCallback?.addOnChangeCallback(c)
}
fun <T : TBaseModel> TmsGb<T>.onSave(c: ModelCallback) {
    _modelCallback?.addOnSaveCallback(c)
}
fun <T : TBaseModel> TmsGb<T>.onCreate(c: ModelCallback) {
    _modelCallback?.addOnCreateCallback(c)
}
fun <T : TBaseModel> TmsGb<T>.onUpdate(c: ModelCallback) {
    _modelCallback?.addOnUpdateCallback(c)
}
fun <T : TBaseModel> TmsGb<T>.onDelete(c: ModelCallback) {
    _modelCallback?.addOnDeleteCallback(c)
}



fun <T : TBaseModel> TmsGb<T>.initCallbacks() {
    _modelCallback = TModelCallback()
    addObserver(_modelCallback!!)
}
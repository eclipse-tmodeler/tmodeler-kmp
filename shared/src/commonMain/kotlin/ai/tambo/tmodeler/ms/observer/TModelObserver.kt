package ai.tambo.tmodeler.ms.observer

import ai.tambo.tmodeler.db.KeysType

open class TModelObserver2 {
    open fun onModelChange(keys: KeysType) {}
    open fun onSave(keys: KeysType) {}
    open fun onCreate(keys: KeysType) {}
    open fun onUpdate(keys: KeysType) {}
    open fun onDelete(keys: KeysType) {}
}
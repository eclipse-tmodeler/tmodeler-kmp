package ai.tambo.tmodeler.modeler

import ai.tambo.tmodeler.db.Tdb

class Mutex {
    fun read(function: () -> Tdb?): Tdb? { return function.invoke() }
    fun write(function: () -> Unit) {
        function.invoke()
    }
}
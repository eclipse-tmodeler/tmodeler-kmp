package ai.tambo.tmodeler.application

abstract class TApp {
    val name: String
        get() = this::class.simpleName ?: "UnknownApp"
}
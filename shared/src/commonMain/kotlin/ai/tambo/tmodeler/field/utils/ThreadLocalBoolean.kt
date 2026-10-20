package ai.tambo.tmodeler.field.utils

/*expect*/ class ThreadLocalBoolean() {
    var value = false
    fun get(): Boolean = value
    fun set(value: Boolean) {this.value = value}
}
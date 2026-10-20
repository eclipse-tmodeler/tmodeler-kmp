package ai.tambo.tmodeler.field.utils.geometry

data class DrawingStyle(
    var fillColor: Int = 0xFFFFFF,     // Blanc
    var borderColor: Int = 0x000000,   // Noir
    var borderWidth: Int = 1,
    var zIndex: Int = 0,
    var visible: Boolean = true,
    var selected: Boolean = false,
    var oneWay: Boolean = false        // add new
)

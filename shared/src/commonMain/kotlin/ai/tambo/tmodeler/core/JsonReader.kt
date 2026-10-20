package ai.tambo.tmodeler.core



class JsonReader(private val json: String) {

    private var index = 0

    init {
        while (index < json.length && json[index] != '[') index++
        index++
    }

    fun hasNext(): Boolean {
        skipSpaces()
        return index < json.length && json[index] != ']'
    }

    fun nextObject(): String {

        skipSpaces()

        val start = index
        var depth = 0

        while (index < json.length) {

            val c = json[index]

            if (c == '{') depth++
            if (c == '}') {
                depth--
                if (depth == 0) {
                    index++
                    break
                }
            }

            index++
        }

        val obj = json.substring(start, index)

        skipComma()

        return obj
    }

    private fun skipSpaces() {
        while (index < json.length && json[index].isWhitespace()) index++
    }

    private fun skipComma() {
        skipSpaces()
        if (index < json.length && json[index] == ',') index++
    }
}
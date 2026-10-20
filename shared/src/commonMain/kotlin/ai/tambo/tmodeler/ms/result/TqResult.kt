package ai.tambo.tmodeler.ms.result

class TqResult {
    enum class TStatus {
        DONE,
        NOT_FOUND
    }

    var status: TStatus = TStatus.DONE
    var data: String = ""
}
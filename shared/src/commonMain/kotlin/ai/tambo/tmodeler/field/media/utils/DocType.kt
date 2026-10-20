package ai.tambo.tmodeler.field.media.utils

enum class DocType(val ext: String, val mime: String) {
    PDF("pdf", "application/pdf"),
    DOC("doc", "application/msword"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLS("xls", "application/vnd.ms-excel"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPT("ppt", "application/vnd.ms-powerpoint"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    TXT("txt", "text/plain"),
    CSV("csv", "text/csv"),
    RTF("rtf", "application/rtf")
}

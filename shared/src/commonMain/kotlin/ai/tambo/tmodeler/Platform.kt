package ai.tambo.tmodeler

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
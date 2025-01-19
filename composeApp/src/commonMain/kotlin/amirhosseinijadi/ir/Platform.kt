package amirhosseinijadi.ir

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
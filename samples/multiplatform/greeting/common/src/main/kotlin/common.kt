package org.greeting

expect class Platform() {
    val platform: String
}

class Greeting {
    fun greeting(): String = "Hello, ${Platform().platform}"
}

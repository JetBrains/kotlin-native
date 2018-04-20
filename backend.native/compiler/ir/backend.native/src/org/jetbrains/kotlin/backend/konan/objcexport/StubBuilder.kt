package org.jetbrains.kotlin.backend.konan.objcexport

internal class StubBuilder {
    private val children = mutableListOf<Stub<*>>()

    operator fun String.unaryPlus() {
        children.add(this)
    }

    fun build() = children
}

internal inline fun buildMembers(block: StubBuilder.() -> Unit): List<Stub<*>> = StubBuilder().let {
    it.block()
    it.build()
}

internal inline fun MutableCollection<Stub<*>>.addBuiltBy(block: StubBuilder.() -> Unit) {
    this.addAll(buildMembers(block))
}

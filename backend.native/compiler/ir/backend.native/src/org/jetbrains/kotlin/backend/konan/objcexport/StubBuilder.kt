package org.jetbrains.kotlin.backend.konan.objcexport

internal class StubBuilder {
    private val children = mutableListOf<Stub<*>>()

    operator fun String.unaryPlus() {
        children.add(this)
    }

    operator fun Stub<*>.unaryPlus() {
        children.add(this)
    }

    operator fun plusAssign(set: Set<Stub<*>>) {
        children += set
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

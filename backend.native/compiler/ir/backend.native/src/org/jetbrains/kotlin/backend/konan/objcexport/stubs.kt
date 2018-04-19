package org.jetbrains.kotlin.backend.konan.objcexport

internal data class Stub(val lines: List<String>)

internal class StubBuilder {
    private val lines = mutableListOf<String>()

    operator fun String.unaryPlus() {
        lines.add(this)
    }

    operator fun Stub.unaryPlus() {
        this@StubBuilder.lines.addAll(this.lines)
    }

    fun build() = Stub(lines)
}

internal inline fun buildStub(block: StubBuilder.() -> Unit) = StubBuilder().let {
    it.block()
    it.build()
}

internal inline fun MutableCollection<Stub>.addBuiltBy(block: StubBuilder.() -> Unit) {
    this.add(buildStub(block))
}
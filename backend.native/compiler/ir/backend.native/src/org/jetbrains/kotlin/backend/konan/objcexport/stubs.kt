package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

open class Stub<out D : DeclarationDescriptor>(val name: String, val descriptor: D)

abstract class ClassStubBase<out D : DeclarationDescriptor>(name: String,
                                                            descriptor: D,
                                                            val superProtocols: List<String>,
                                                            val members: List<Stub<*>>) : Stub<D>(name, descriptor)

class ObjcProtocol(name: String,
                   descriptor: ClassDescriptor,
                   superProtocols: List<String>,
                   members: List<Stub<*>>) : ClassStubBase<ClassDescriptor>(name, descriptor, superProtocols, members)

internal inline fun buildStub(block: StubBuilder.() -> Unit) = StubBuilder().let {
    it.block()
    it.build()
}

internal inline fun MutableCollection<Stub>.addBuiltBy(block: StubBuilder.() -> Unit) {
    this.add(buildStub(block))
}
package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

open class Stub<out D : DeclarationDescriptor>(val name: String, val descriptor: D?)

abstract class ClassStubBase<out D : DeclarationDescriptor>(name: String,
                                                            descriptor: D,
                                                            val superProtocols: List<String>,
                                                            val members: List<Stub<*>>) : Stub<D>(name, descriptor)

class ObjcProtocol(name: String,
                   descriptor: ClassDescriptor,
                   superProtocols: List<String>,
                   members: List<Stub<*>>) : ClassStubBase<ClassDescriptor>(name, descriptor, superProtocols, members)

class ObjcInterface(name: String,
                    descriptor: ClassDescriptor,
                    val superClass: String,
                    superProtocols: List<String>,
                    members: List<Stub<*>>) : ClassStubBase<ClassDescriptor>(name, descriptor, superProtocols, members)

class ObjcMethod(descriptor: DeclarationDescriptor?,
                 val isInstanceMethod: Boolean,
                 val returnType: ObjCType,
                 val selectors: List<String>,
                 val parameters: List<ObjcParameter>,
                 val swiftName: String,
                 val isConstructor: Boolean,
                 val isDesignatedConstructor: Boolean) : Stub<DeclarationDescriptor>(buildMethodName(selectors, parameters), descriptor)

class ObjcParameter(name: String,
                    descriptor: ParameterDescriptor?,
                    val type: ObjCType) : Stub<ParameterDescriptor>(name, descriptor)

class ObjcProperty(name: String,
                   descriptor: PropertyDescriptor?,
                   val type: ObjCType,
                   val attributes: List<String>) : Stub<PropertyDescriptor>(name, descriptor)

private fun buildMethodName(selectors: List<String>, parameters: List<ObjcParameter>): String =
        if (selectors.size == 1 && parameters.size == 0) {
            selectors[0]
        } else {
            assert(selectors.size == parameters.size)
            selectors.joinToString(separator = ":", postfix = ":")
        }

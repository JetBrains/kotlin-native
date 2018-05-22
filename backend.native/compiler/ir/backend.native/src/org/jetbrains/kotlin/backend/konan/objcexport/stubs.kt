package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

open class Stub<out D : DeclarationDescriptor>(val name: String, val descriptor: D?)

abstract class ObjcClass<out D : DeclarationDescriptor>(name: String,
                                                        descriptor: D?,
                                                        val superProtocols: List<String>,
                                                        val members: List<Stub<*>>) : Stub<D>(name, descriptor)

class ObjcProtocol(name: String,
                   descriptor: ClassDescriptor,
                   superProtocols: List<String>,
                   members: List<Stub<*>>) : ObjcClass<ClassDescriptor>(name, descriptor, superProtocols, members)

class ObjcInterface(name: String,
                    val generics: List<String> = emptyList(),
                    descriptor: ClassDescriptor? = null,
                    val superClass: String? = null,
                    superProtocols: List<String> = emptyList(),
                    val categoryName: String? = null,
                    members: List<Stub<*>> = emptyList(),
                    val attributes: List<String> = emptyList()) : ObjcClass<ClassDescriptor>(name, descriptor, superProtocols, members)

class ObjcMethod(descriptor: DeclarationDescriptor?,
                 val isInstanceMethod: Boolean,
                 val returnType: ObjCType,
                 val selectors: List<String>,
                 val parameters: List<ObjcParameter>,
                 val attributes: List<String>) : Stub<DeclarationDescriptor>(buildMethodName(selectors, parameters), descriptor) {

    //parameters and returnType are not included in equals as they are not included into signature
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ObjcMethod

        if (isInstanceMethod != other.isInstanceMethod) return false
        if (selectors != other.selectors) return false
        if (attributes != other.attributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isInstanceMethod.hashCode()
        result = 31 * result + selectors.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }
}

class ObjcParameter(name: String,
                    descriptor: ParameterDescriptor?,
                    val type: ObjCType) : Stub<ParameterDescriptor>(name, descriptor)

class ObjcProperty(name: String,
                   descriptor: PropertyDescriptor?,
                   val type: ObjCType,
                   val attributes: List<String>,
                   val setterName: String? = null,
                   val getterName: String? = null) : Stub<PropertyDescriptor>(name, descriptor) {

    //type is not included in equals as it's not included into signature
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ObjcProperty

        if (name != other.name) return false
        if (attributes != other.attributes) return false
        if (setterName != other.setterName) return false
        if (getterName != other.getterName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + (setterName?.hashCode() ?: 0)
        result = 31 * result + (getterName?.hashCode() ?: 0)
        return result
    }
}

private fun buildMethodName(selectors: List<String>, parameters: List<ObjcParameter>): String =
        if (selectors.size == 1 && parameters.size == 0) {
            selectors[0]
        } else {
            assert(selectors.size == parameters.size)
            selectors.joinToString(separator = "")
        }

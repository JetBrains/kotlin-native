package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor

object StubRenderer {
    fun render(stub: Stub<*>): List<String> = collect {
        stub.run {
            when (this) {
                is ObjcProtocol -> {
                    +renderProtocolHeader()
                    +"@required"
                    renderMembers(this)
                    +"@end;"
                }
                is ObjcInterface -> {
                    attributes.forEach {
                        +renderAttribute(it)
                    }
                    +renderInterfaceHeader()
                    renderMembers(this)
                    +"@end;"
                }
                is ObjcMethod -> {
                    +renderMethod(this)
                }
                is ObjcProperty -> {
                    +renderProperty(this)
                }
                else -> throw IllegalArgumentException("unsupported stub: " + stub::class)
            }
        }
    }

    private fun renderProperty(property: ObjcProperty): String = buildString {
        fun StringBuilder.appendType() {
            append(' ')
            append(property.type.render())
        }

        fun ObjcProperty.getAllAttributes(): List<String> {
            if (getterName == null && setterName == null) return attributes

            val allAttributes = attributes.toMutableList()
            getterName?.let { allAttributes += "getter=$it" }
            setterName?.let { allAttributes += "setter=$it" }
            return allAttributes
        }

        fun StringBuilder.appendAttributes() {
            val attributes = property.getAllAttributes()
            if (attributes.isNotEmpty()) {
                append(' ')
                attributes.joinTo(this, prefix = "(", postfix = ")")
            }
        }

        append("@property")
        appendAttributes()
        appendType()
        append(property.name)
        append(';')
    }

    private fun renderMethod(method: ObjcMethod): String = buildString {
        fun appendStaticness() {
            if (method.isInstanceMethod) {
                append('-')
            } else {
                append('+')
            }
        }

        fun appendReturnType() {
            append(" (")
            append(method.returnType.render())
            append(')')
        }

        fun appendParameters() {
            append(' ')
            assert(method.selectors.size == method.parameters.size ||
                   method.selectors.size == 1 && method.parameters.size == 0)

            if (method.selectors.size == 1 && method.parameters.size == 0) {
                append(method.selectors[0])
            } else {
                for (i in 0 until method.selectors.size) {
                    if (i > 0) append(' ')

                    val parameter = method.parameters[i]
                    val selector = method.selectors[i]
                    append(selector)
                    append(": (")
                    append(parameter.type.render())
                    append(") ")
                    append(parameter.name)
                }
            }
        }

        fun appendAttributes() {
            method.attributes.joinTo(this, separator = " ", transform = ::renderAttribute)
        }

        appendStaticness()
        appendReturnType()
        appendParameters()
        appendAttributes()
        append(';')
    }

    private fun ObjcProtocol.renderProtocolHeader() = buildString {
        append("@protocol ")
        append(name)
        appendSuperProtocols(this@renderProtocolHeader)
    }

    private fun StringBuilder.appendSuperProtocols(clazz: ClassStubBase<ClassDescriptor>) {
        val protocols = clazz.superProtocols
        if (protocols.isNotEmpty()) {
            protocols.joinTo(this, separator = ", ", prefix = " <", postfix = ">")
        }
    }

    private fun ObjcInterface.renderInterfaceHeader() = buildString {
        fun appendSuperClass() {
            if (superClass != null) append(superClass)
        }

        fun appendGenerics() {
            val generics = generics
            if (generics.isNotEmpty()) {
                generics.joinTo(this, separator = ", ", prefix = " <", postfix = ">")
            }
        }

        fun appendCategoryName() {
            if (categoryName != null) {
                append(" (")
                append(categoryName)
                append(')')
            }
        }

        append("@interface ")
        append(name)
        appendGenerics()
        appendSuperClass()
        appendCategoryName()
        appendSuperProtocols(this@renderInterfaceHeader)
    }

    private fun Collector.renderMembers(clazz: ClassStubBase<*>) {
        clazz.members.forEach {
            +render(it)
        }
    }

    private fun renderAttribute(attribute: String) = "__attribute__(($attribute))"

    private fun collect(p: Collector.() -> Unit): List<String> {
        val collector = Collector()
        collector.p()
        return collector.build()
    }

    private class Collector {
        private val collection: MutableList<String> = mutableListOf()
        fun build(): List<String> = collection

        operator fun String.unaryPlus() {
            collection += this
        }

        operator fun List<String>.unaryPlus() {
            collection += this
        }
    }
}


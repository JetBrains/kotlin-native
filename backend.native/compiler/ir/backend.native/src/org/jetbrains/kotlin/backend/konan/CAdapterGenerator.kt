/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.descriptors.isUnit
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

enum class ScopeKind {
    TOP,
    CLASS,
    PACKAGE
}

enum class ElementKind {
    FUNCTION,
    PROPERTY
}

private operator fun String.times(count: Int): String {
    val builder = StringBuilder()
    repeat(count, { builder.append(this) })
    return builder.toString()
}


private data class ExportedElementScope(val kind: ScopeKind, val name: String) {
    val elements = mutableListOf<ExportedElement>()
    val valelements = mutableListOf<ExportedElement>()
    val scopes = mutableListOf<ExportedElementScope>()

    override fun toString(): String {
        return "$kind: $name ${elements.joinToString(", ")} ${scopes.joinToString("\n")}"
    }

    fun generateCAdapters() {
        elements.forEach {
            it.generateCAdapter()
        }
        scopes.forEach {
            it.generateCAdapters()
        }
    }

    private var constructorIndex = 0
    fun nextConstructorIndex(): Int = constructorIndex++
}

private data class ExportedElement(val kind: ElementKind,
                                   val scope: ExportedElementScope,
                                   val declaration: IrDeclaration,
                                   val owner: CAdapterGeneratorVisitor) {
    init {
        scope.elements.add(this)
    }

    var bridge: LLVMValueRef? = null

    val name: String
        get() = declaration.descriptor.fqNameSafe.shortName().asString()

    val llvm: LLVMValueRef?
        get() = owner.codegen.llvmFunction(declaration.descriptor as FunctionDescriptor)


    override fun toString(): String {
        return "$kind: $name $llvm}"
    }

    fun generateCAdapter() {
        //println("making C adapter for $name in scope ${scope.kind} ${scope.name}")
    }

    fun functionName(descriptor: FunctionDescriptor): String {
        return when (descriptor) {
            is PropertyGetterDescriptor -> "get_${descriptor.correspondingProperty.name.asString()}"
            is PropertySetterDescriptor -> "set_${descriptor.correspondingProperty.name.asString()}"
            else -> descriptor.name.asString()
        }
    }

    val isFunction = declaration.descriptor is FunctionDescriptor

    fun makeFunctionPointerString(): String {
        val descriptor = declaration.descriptor
        if (descriptor !is FunctionDescriptor) {
            throw Error("only for functions")
        }
        val original = descriptor.original
        val (name, returnType) = when {
            original is ConstructorDescriptor -> Pair(
                    "construct_${scope.nextConstructorIndex()}",
                    owner.translateType(original.constructedClass))
            original.isSuspend -> Pair(functionName(original), owner.translateType(
                    owner.codegen.context.builtIns.nullableAnyType))  // Suspend functions actually return Any?.
            else -> Pair(functionName(original), owner.translateType(original.returnType!!))
        }
        val params = ArrayList(original.allParameters.map { "${owner.translateType(it.type)} ${owner.translateName(it.name.asString())}" })
        if (original is ConstructorDescriptor) {
            // Remove `this` parameter from constructor.
            params.removeAt(0)
        }

        return "$returnType (*$name)(${params.joinToString(", ")})"
    }

    fun addUsedTypes(set: MutableSet<ClassDescriptor>) {
        val descriptor = declaration.descriptor
        when (descriptor) {
            is FunctionDescriptor -> {
                val original = descriptor.original
                original.allParameters.forEach { set += TypeUtils.getClassDescriptor(it.type)!!}
                original.returnType?.let { set += TypeUtils.getClassDescriptor(it)!! }
            }
            is PropertyAccessorDescriptor -> {
                val original = descriptor.original
                set += TypeUtils.getClassDescriptor(original.correspondingProperty.type)!!
            }
        }
    }
}

internal class CAdapterGeneratorVisitor(
        val context: Context, internal val codegen: CodeGenerator) : IrElementVisitorVoid {
    private val scopes = mutableListOf<ExportedElementScope>()
    private val prefix: String = context.config.outputName

    override fun visitElement(element: IrElement) {
        //println(ir2string(element))
        element.acceptChildrenVoid(this)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        val fqName = declaration.packageFragmentDescriptor.fqName
        val name = if (fqName.isRoot) "root" else fqName.shortName().asString()
        val packageScope = ExportedElementScope(ScopeKind.PACKAGE, name)
        scopes.last().scopes += packageScope
        scopes.push(packageScope)
        declaration.acceptChildrenVoid(this)
        scopes.pop()
    }

    override fun visitProperty(declaration: IrProperty) {
        val descriptor = declaration.descriptor
        if (!descriptor.isEffectivelyPublicApi || !descriptor.kind.isReal) return
        ExportedElement(ElementKind.PROPERTY, scopes.last(), declaration, this)
    }

    override fun visitFunction(function: IrFunction) {
        val descriptor = function.descriptor
        if (!descriptor.isEffectivelyPublicApi || !descriptor.kind.isReal) return
        ExportedElement(ElementKind.FUNCTION, scopes.last(), function, this)
    }

    override fun visitClass(declaration: IrClass) {
        val descriptor = declaration.descriptor
        if (!descriptor.isEffectivelyPublicApi)
            return
        // TODO: fix me!
        val shortName = descriptor.fqNameSafe.shortName()
        if (shortName.isSpecial || shortName.asString().contains("<anonymous>"))
            return
        val classScope = ExportedElementScope(ScopeKind.CLASS, shortName.asString())
        scopes.last().scopes += classScope
        scopes.push(classScope)
        declaration.acceptChildrenVoid(this)
        scopes.pop()
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
        scopes.push(ExportedElementScope(ScopeKind.TOP, "kotlin"))
        declaration.acceptChildrenVoid(this)
        val top = scopes.pop()
        assert(scopes.isEmpty() && top.kind == ScopeKind.TOP)

        // Now, let's generate C world adapters for all functions.
        top.generateCAdapters()

        // Then generate data structure, describing referring adapters.
        makeGlobalStruct(top)
    }

    private fun output(string: String, indent: Int = 0) {
        if (indent != 0) print("  " * indent)
        println(string)
    }

    private fun makeElementDefinition(element: ExportedElement, indent: Int) {
        if (element.isFunction)
            output(element.makeFunctionPointerString() + ";", indent)
        // TODO: handle properties.
    }

    private fun makeScopeDefinitions(scope: ExportedElementScope, indent: Int) {
        output("struct {", indent)
        scope.elements.forEach { makeElementDefinition(it, indent + 1) }
        scope.scopes.forEach { makeScopeDefinitions(it, indent + 1) }
        output("} ${scope.name};", indent)
    }

    private fun defineUsedTypesImpl(scope: ExportedElementScope, set: MutableSet<ClassDescriptor>) {
        scope.elements.forEach {
            it.addUsedTypes(set)
        }
        scope.scopes.forEach {
            defineUsedTypesImpl(it, set)
        }
    }

    private fun defineUsedTypes(scope: ExportedElementScope, indent: Int) {
        val set = mutableSetOf<ClassDescriptor>()
        defineUsedTypesImpl(scope, set)
        set.forEach {
            if (!it.isUnit() && !simpleTypeMapping.contains(it.fqNameSafe.asString())) {
                output("typedef struct {", indent)
                output("${prefix}_KNativePtr pinned;", indent + 1)
                output("} ${translateType(it)};", indent)
            }
        }
    }

    private fun makeGlobalStruct(top: ExportedElementScope) {
        output("#ifndef KONAN_${prefix.toUpperCase()}_H")
        output("#define KONAN_${prefix.toUpperCase()}_H")
        // TODO: use namespace for C++ case?
        output("""
        #ifdef __cplusplus
        extern "C" {
        #endif""".trimIndent())
        output("typedef unsigned char   ${prefix}_KBoolean;")
        output("typedef char            ${prefix}_KByte;")
        output("typedef unsigned short  ${prefix}_KChar;")
        output("typedef short           ${prefix}_KShort;")
        output("typedef int             ${prefix}_KInt;")
        output("typedef long long       ${prefix}_KLong;")
        output("typedef float           ${prefix}_KFloat;")
        output("typedef double          ${prefix}_KDouble;")
        output("typedef void*           ${prefix}_KNativePtr;")

        output("")
        defineUsedTypes(top, 0)

        output("")
        output("typedef struct {")
        output("/* Service functions. */", 1)
        output("void (*DisposeStablePointer)(${prefix}_KNativePtr ptr);", 1)
        output("void (*DisposeString)(char* string);", 1)

        output("")
        output("/* User functions. */", 1)
        makeScopeDefinitions(top, 1)
        output("} ${prefix}_ExportedSymbols;")

        output("extern ${prefix}_ExportedSymbols* ${prefix}_symbols();")
        output("""
        #ifdef __cplusplus
        }  /* extern "C" */
        #endif""".trimIndent())

        output("#endif  /* KONAN_${prefix.toUpperCase()}_H */")
    }

    private val simpleNameMapping = mapOf(
            "<this>" to "thiz"
    )

    private val simpleTypeMapping = mapOf(
            "kotlin.String" to "const char*",
            "kotlin.Byte" to "${prefix}_KByte",
            "kotlin.Short" to "${prefix}_KShort",
            "kotlin.Int" to "${prefix}_KInt",
            "kotlin.Long" to "${prefix}_KLong",
            "kotlin.Float" to "${prefix}_KFloat",
            "kotlin.Double" to "${prefix}_KDouble",
            "kotlin.Boolean" to "${prefix}_KBoolean",
            "kotlin.Char" to "${prefix}_KChar"
    )

    fun translateName(name: String): String {
        return when {
            simpleNameMapping.contains(name) -> simpleNameMapping[name]!!
            else -> name
        }
    }

    fun translateType(clazz: ClassDescriptor): String {
        val fqName = clazz.fqNameSafe.asString()
        return when {
            clazz.isUnit() -> "void"
            simpleTypeMapping.contains(fqName) -> simpleTypeMapping[fqName]!!
            else -> "${prefix}_kref_${translateTypeFqName(clazz.fqNameSafe.asString())}"
        }
    }

    fun translateType(type: KotlinType): String = translateType(TypeUtils.getClassDescriptor(type)!!)

    fun translateTypeFqName(name: String): String {
        return name.replace('.', '_')
    }
}

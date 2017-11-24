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
import org.jetbrains.kotlin.konan.file.File

enum class ScopeKind {
    TOP,
    CLASS,
    PACKAGE
}

enum class ElementKind {
    FUNCTION,
    PROPERTY,
    TYPE
}

enum class DefinitionKind {
    C_HEADER,
    C_SOURCE
}

private operator fun String.times(count: Int): String {
    val builder = StringBuilder()
    repeat(count, { builder.append(this) })
    return builder.toString()
}

private data class ExportedElementScope(val kind: ScopeKind, val name: String) {
    val elements = mutableListOf<ExportedElement>()
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
                                   val declaration: DeclarationDescriptor,
                                   val owner: CAdapterGenerator) {
    init {
        scope.elements.add(this)
    }

    var bridge: LLVMValueRef? = null

    val name: String
        get() = declaration.fqNameSafe.shortName().asString()

    val llvm: LLVMValueRef?
        get() = owner.codegen.llvmFunction(declaration as FunctionDescriptor)


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

    val isFunction = declaration is FunctionDescriptor
    val isClass = declaration is ClassDescriptor

    fun makeFunctionPointerString(): String {
        val descriptor = declaration
        if (descriptor !is FunctionDescriptor) {
            throw Error("only for functions")
        }
        val original = descriptor.original
        val (name, returnType) = when {
            original is ConstructorDescriptor -> Pair(
                    "_construct_${scope.nextConstructorIndex()}",
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
        val descriptor = declaration
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

internal class CAdapterGenerator(val context: Context,
                                 internal val codegen: CodeGenerator) : IrElementVisitorVoid {
    private val scopes = mutableListOf<ExportedElementScope>()
    private val prefix: String = context.config.outputName
    private lateinit var outputStreamWriter: java.io.PrintWriter

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
        ExportedElement(ElementKind.PROPERTY, scopes.last(), declaration.descriptor, this)
    }

    override fun visitFunction(function: IrFunction) {
        val descriptor = function.descriptor
        if (!descriptor.isEffectivelyPublicApi || !descriptor.kind.isReal) return
        ExportedElement(ElementKind.FUNCTION, scopes.last(), function.descriptor, this)
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
        // Add type getter.
        ExportedElement(ElementKind.TYPE, scopes.last(), descriptor, this)
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

        // Then generate data structure, describing generated adapters.
        makeGlobalStruct(top)
    }

    private fun output(string: String, indent: Int = 0) {
        if (indent != 0) outputStreamWriter.print("  " * indent)
        outputStreamWriter.println(string)
    }

    private fun makeElementDefinition(element: ExportedElement,
                                      kind: DefinitionKind,
                                      indent: Int) {
        when (kind) {
            DefinitionKind.C_HEADER -> {
                when {
                    element.isFunction ->
                        output(element.makeFunctionPointerString() + ";", indent)
                    element.isClass ->
                        output("${prefix}_KType* (*_type)();", indent)
                    // TODO: handle properties.
                }
            }
            DefinitionKind.C_SOURCE -> {
                when {
                    element.isFunction ->
                        output("/* ${element.name} = */ foo, ", indent)
                    element.isClass ->
                        output("/* Type for ${element.name} = */ get_type, ", indent)
                // TODO: handle properties.
                }
            }
        }
    }

    private fun makeScopeDefinitions(scope: ExportedElementScope,
                                     kind: DefinitionKind,
                                     indent: Int) {
        if (kind == DefinitionKind.C_HEADER) output("struct {", indent)
        if (kind == DefinitionKind.C_SOURCE) output(".${scope.name} = {", indent)
        scope.elements.forEach { makeElementDefinition(it,  kind, indent + 1) }
        scope.scopes.forEach { makeScopeDefinitions(it,  kind,indent + 1) }
        if (kind == DefinitionKind.C_HEADER) output("} ${scope.name};", indent)
        if (kind == DefinitionKind.C_SOURCE) output("},", indent)
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
        outputStreamWriter = java.io.PrintWriter(File(".", "${prefix}_api.h").outputStream())
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
        output("struct ${prefix}_KType;")
        output("typedef struct ${prefix}_KType ${prefix}_KType;")

        output("")
        defineUsedTypes(top, 0)

        output("")
        output("typedef struct {")
        output("/* Service functions. */", 1)
        output("void (*DisposeStablePointer)(${prefix}_KNativePtr ptr);", 1)
        output("void (*DisposeString)(char* string);", 1)
        output("${prefix}_KBoolean (*CheckCast)(${prefix}_KType type, ${prefix}_KNativePtr ref);", 1)

        output("")
        output("/* User functions. */", 1)
        makeScopeDefinitions(top, DefinitionKind.C_HEADER, 1)
        output("} ${prefix}_ExportedSymbols;")

        output("extern ${prefix}_ExportedSymbols* ${prefix}_symbols();")
        output("""
        #ifdef __cplusplus
        }  /* extern "C" */
        #endif""".trimIndent())

        output("#endif  /* KONAN_${prefix.toUpperCase()}_H */")

        outputStreamWriter.close()
        println("Produced dynamic library API in ${prefix}_api.h")

        outputStreamWriter = java.io.PrintWriter(File(".", "${prefix}_api.c").outputStream())
        output("#include \"${prefix}_api.h\"")
        output("void foo() {}")
        output("void get_type() {}")
        output("static ${prefix}_ExportedSymbols __konan_symbols = {")
        output(".DisposeStablePointer = 0,", 1)
        output(".DisposeString = 0,", 1)
        output(".CheckCast = 0,", 1)
        makeScopeDefinitions(top, DefinitionKind.C_SOURCE, 1)
        output("};")
        output("${prefix}_ExportedSymbols* ${prefix}_symbols() { return &__konan_symbols;}")
        outputStreamWriter.close()
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

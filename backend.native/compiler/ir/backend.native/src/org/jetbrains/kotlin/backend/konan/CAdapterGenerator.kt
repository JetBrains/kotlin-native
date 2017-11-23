package org.jetbrains.kotlin.backend.konan

import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.konan.descriptors.isUnit
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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
    VARIABLE
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

operator fun String.times(count: Int): String {
    val builder = StringBuilder()
    repeat(count, { builder.append(this) })
    return builder.toString()
}

private data class ExportedElement(val kind: ElementKind,
                                   val scope: ExportedElementScope,
                                   val function: IrFunction,
                                   val owner: CAdapterGeneratorVisitor) {
    init {
        scope.elements.add(this)
    }

    var bridge: LLVMValueRef? = null


    val name: String
        get() {
            return function.descriptor.functionName
        }

    val llvm: LLVMValueRef?
        get() {
            return owner.
                    codegen.llvmFunction(function.descriptor)
        }


    override fun toString(): String {
        return "$kind: $name $llvm}"
    }

    fun generateCAdapter() {
        println("making C adapter for $name in scope ${scope.kind} ${scope.name}")
    }

    fun functionName(descriptor: FunctionDescriptor): String {
        return descriptor.name.asString()
    }

    fun makeFunctionPointerString(): String {
        val original = function.descriptor.original
        val (name, returnType) = when {
            original is ConstructorDescriptor -> Pair(
                    "construct_${scope.nextConstructorIndex()}",
                    owner.translateType(original.constructedClass))
            original.isSuspend -> Pair(functionName(original), "ref_Any")                // Suspend functions return Any?.
            else -> Pair(functionName(original), owner.translateType(original.returnType!!))
        }
        val paramTypes = ArrayList(original.allParameters.map { owner.translateType(it.type) })


        return "$returnType (*$name)(${paramTypes.joinToString(", ")})"
    }
}

internal class CAdapterGeneratorVisitor(
        val context: Context, internal val codegen: CodeGenerator) : IrElementVisitorVoid {
    private val scopes = mutableListOf<ExportedElementScope>()
    private val prefix: String = context.config.outputName

    override fun visitElement(element: IrElement) {
        println(ir2string(element))
        element.acceptChildrenVoid(this)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        val packageScope = ExportedElementScope(
                ScopeKind.PACKAGE, declaration.packageFragmentDescriptor.fqName.shortName().asString())
        scopes.last().scopes += packageScope
        scopes.push(packageScope)
        declaration.acceptChildrenVoid(this)
        scopes.pop()
    }

    override fun visitFunction(function: IrFunction) {
        val descriptor = function.descriptor
        if (!descriptor.isEffectivelyPublicApi || !descriptor.kind.isReal) return

        // TODO: enable getters.
        if (function.declarationKind == IrDeclarationKind.PROPERTY_ACCESSOR) return

        ExportedElement(ElementKind.FUNCTION, scopes.last(), function, this)
    }

    override fun visitClass(declaration: IrClass) {
        val classScope = ExportedElementScope(
                ScopeKind.CLASS, declaration.descriptor.fqNameSafe.shortName().asString())
        scopes.last().scopes += classScope
        scopes.push(classScope)
        declaration.acceptChildrenVoid(this)
        scopes.pop()
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
        scopes.push(ExportedElementScope(ScopeKind.TOP, "any"))
        declaration.acceptChildrenVoid(this)
        val top = scopes.pop()
        assert(scopes.isEmpty() && top.kind == ScopeKind.TOP)

        println(top)
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
        output(element.makeFunctionPointerString() + ";", indent)
    }

    private fun makeScopeDefinitions(scope: ExportedElementScope, indent: Int) {
        output("struct {", indent)
        scope.elements.forEach { makeElementDefinition(it, indent + 1) }
        scope.scopes.forEach { makeScopeDefinitions(it, indent + 1) }
        output("} ${scope.name};", indent)
    }

    private fun makeGlobalStruct(top: ExportedElementScope) {
        output("#ifndef KONAN_${prefix.toUpperCase()}_H")
        output("#define KONAN_${prefix.toUpperCase()}_H")
        output("typedef uint8_t    ${prefix}_KBoolean;")
        output("typedef int8_t     ${prefix}_KByte;")
        output("typedef uint16_t   ${prefix}_KChar;")
        output("typedef int16_t    ${prefix}_KShort;")
        output("typedef int32_t    ${prefix}_KInt;")
        output("typedef int64_t    ${prefix}_KLong;")
        output("typedef float      ${prefix}_KFloat;")
        output("typedef double     ${prefix}_KDouble;")
        output("typedef void*      ${prefix}_KNativePtr;")
        output("""
        #ifdef __cplusplus
        extern "C" {
        #endif""".trimIndent())
        output("typedef struct {")
        output("/* Service functions. */", 1)
        output("void (*DisposeStablePointer)(${prefix}_KNativePtr ptr);", 1)

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

    fun translateType(clazz: ClassDescriptor): String {
        return when {
            clazz.isUnit() -> "void"
            else -> "${prefix}_${clazz.fqNameSafe.asString()}"
        }
    }

    fun translateType(type: KotlinType): String = translateType(TypeUtils.getClassDescriptor(type)!!)
}

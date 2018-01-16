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

import java.io.PrintWriter
import llvm.*
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.descriptors.isUnit
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.module

private enum class ScopeKind {
    TOP,
    CLASS,
    PACKAGE
}

private enum class ElementKind {
    FUNCTION,
    PROPERTY,
    TYPE
}

private enum class DefinitionKind {
    C_HEADER_STRUCT,
    C_SOURCE_DECLARATION,
    C_SOURCE_STRUCT
}

private enum class Direction {
    KOTLIN_TO_C,
    C_TO_KOTLIN
}

private operator fun String.times(count: Int): String {
    val builder = StringBuilder()
    repeat(count, { builder.append(this) })
    return builder.toString()
}

private val cKeywords = setOf(
        // Actual C keywords.
        "auto", "break", "case",
        "char", "const", "continue",
        "default", "do", "double",
        "else", "enum", "extern",
        "float", "for", "goto",
        "if", "int", "long",
        "register", "return",
        "short", "signed", "sizeof", "static", "struct", "switch",
        "typedef", "union", "unsigned",
        "void", "volatile", "while",
        // C99-specific.
        "_Bool", "_Complex", "_Imaginary", "inline", "restrict",
        // C11-specific.
        "_Alignas", "_Alignof", "_Atomic", "_Generic", "_Noreturn", "_Static_assert", "_Thread_local",
        // Not exactly keywords, but reserved or standard-defined.
        "and", "not", "or", "xor",
        "bool", "complex", "imaginary"
        )

private fun org.jetbrains.kotlin.types.KotlinType.isGeneric() =
        constructor.declarationDescriptor is TypeParameterDescriptor

private fun isExportedFunction(descriptor: FunctionDescriptor): Boolean {
    if (!descriptor.isEffectivelyPublicApi || !descriptor.kind.isReal)
        return false
    descriptor.allParameters.forEach {
        if (it.type.isGeneric()) return false
    }
    val returnType = descriptor.returnType
    if (returnType == null) return true
    return !returnType.isGeneric()
}

private fun isExportedClass(descriptor: ClassDescriptor): Boolean {
    if (!descriptor.isEffectivelyPublicApi) return false
    // No sense to export annotations.
    if (DescriptorUtils.isAnnotationClass(descriptor)) return false
    // Do not export types with type parameters.
    // TODO: is it correct?
    if (!descriptor.declaredTypeParameters.isEmpty()) return false

    return true
}

private class ExportedElementScope(val kind: ScopeKind, val name: String) {
    val elements = mutableListOf<ExportedElement>()
    val scopes = mutableListOf<ExportedElementScope>()
    private val scopeNames = mutableSetOf<String>()
    private val scopeNamesMap = mutableMapOf<DeclarationDescriptor, String>()

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

    fun scopeUniqueName(descriptor: DeclarationDescriptor): String {
        scopeNamesMap[descriptor]?.apply { return this }
        var computedName = when (descriptor) {
            is ConstructorDescriptor -> "${descriptor.constructedClass.fqNameSafe.shortName().asString()}"
            is PropertyGetterDescriptor -> "get_${descriptor.correspondingProperty.name.asString()}"
            is PropertySetterDescriptor -> "set_${descriptor.correspondingProperty.name.asString()}"
            else -> descriptor.fqNameSafe.shortName().asString()
        }
        while (scopeNames.contains(computedName) || cKeywords.contains(computedName)) {
            computedName += "_"
        }
        scopeNames += computedName
        scopeNamesMap[descriptor] = computedName
        return computedName
    }
}

private class ExportedElement(val kind: ElementKind,
                              val scope: ExportedElementScope,
                              val declaration: DeclarationDescriptor,
                              val owner: CAdapterGenerator) : ContextUtils {
    init {
        scope.elements.add(this)
    }

    val name: String
        get() = declaration.fqNameSafe.shortName().asString()

    lateinit var cname: String

    override fun toString(): String {
        return "$kind: $name (aliased to $cname)"
    }

    override val context = owner.context

    fun generateCAdapter() {
        when {
            isFunction -> {
                val function = declaration as FunctionDescriptor
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                val llvmFunction = owner.codegen.llvmFunction(function)
                // If function is virtual, we need to resolve receiver properly.
                val bridge = if (!DescriptorUtils.isTopLevelDeclaration(function) && !function.isExtension &&
                        function.isOverridable) {
                    // We need LLVMGetElementType() as otherwise type is function pointer.
                    generateFunction(owner.codegen, LLVMGetElementType(llvmFunction.type)!!, cname) {
                        val receiver = param(0)
                        val numParams = LLVMCountParams(llvmFunction)
                        val args = (0 .. numParams - 1).map { index -> param(index) }
                        val callee = lookupVirtualImpl(receiver, function)
                        val result = callAtFunctionScopeVerbatim(callee, args)
                        ret(result)
                    }

                } else {
                    LLVMAddAlias(context.llvmModule, llvmFunction.type, llvmFunction, cname)!!
                }
                LLVMSetLinkage(bridge, LLVMLinkage.LLVMExternalLinkage)
            }
            isClass -> {
                // Produce type getter.
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                val getTypeFunction = LLVMAddFunction(context.llvmModule, cname, owner.kGetTypeFuncType)!!
                val builder = LLVMCreateBuilder()!!
                val bb = LLVMAppendBasicBlock(getTypeFunction, "")!!
                LLVMPositionBuilderAtEnd(builder, bb)
                LLVMBuildRet(builder, (declaration as ClassDescriptor).typeInfoPtr.llvm)
                LLVMDisposeBuilder(builder)
            }
        }
    }

    fun uniqueName(descriptor: DeclarationDescriptor) = scope.scopeUniqueName(descriptor)

    val isFunction = declaration is FunctionDescriptor
    val isClass = declaration is ClassDescriptor

    fun makeCFunctionSignature(): List<Pair<String, ClassDescriptor>> {
        if (!isFunction) {
            throw Error("only for functions")
        }
        val descriptor = declaration
        val original = descriptor.original as FunctionDescriptor
        val returned = when {
            original is ConstructorDescriptor -> uniqueName(original) to original.constructedClass
            // Suspend functions actually return Any?.
            original.isSuspend -> uniqueName(original) to
                    TypeUtils.getClassDescriptor(owner.context.builtIns.nullableAnyType)!!
            else -> uniqueName(original) to TypeUtils.getClassDescriptor(original.returnType!!)!!
        }
        val uniqueNames = owner.paramsToUniqueNames(original.explicitParameters)
        val params = ArrayList(original.explicitParameters.mapIndexed { idx, it ->
            uniqueNames[idx] to TypeUtils.getClassDescriptor(it.type)!!
        })
        return listOf(returned) + params
    }

    fun makeBridgeSignature(): List<String> {
        if (!isFunction) {
            throw Error("only for functions")
        }
        val descriptor = declaration
        val original = descriptor.original as FunctionDescriptor
        val returnedType = when {
            original is ConstructorDescriptor -> owner.context.builtIns.unitType
            original.isSuspend -> owner.context.builtIns.nullableAnyType
            else -> original.returnType!!
        }
        val returnedClass = TypeUtils.getClassDescriptor(returnedType)!!
        val params = ArrayList(original.allParameters.map {
            owner.translateTypeBridge(TypeUtils.getClassDescriptor(it.type)!!)
        })
        if (owner.isMappedToReference(returnedClass) || owner.isMappedToString(returnedClass)) {
            params += "KObjHeader**"
        }
        return listOf(owner.translateTypeBridge(returnedClass)) + params
    }


    fun makeFunctionPointerString(): String {
        val signature = makeCFunctionSignature()
        return "${owner.translateType(signature[0].second)} (*${signature[0].first})(${signature.drop(1).map { it -> "${owner.translateType(it.second)} ${it.first}" }.joinToString(", ")});"
    }

    fun makeFunctionDeclaration(): String {
        assert(isFunction)
        val bridge = makeBridgeSignature()

        val builder = StringBuilder()
        builder.append("extern \"C\" ${bridge[0]} $cname")
        builder.append("(${bridge.drop(1).joinToString(", ")});\n")

        // Now the C function body.
        builder.append(translateBody(makeCFunctionSignature()))
        return builder.toString()
    }

    fun makeClassDeclaration(): String {
        assert(isClass)
        return "extern \"C\" ${owner.prefix}_KType* $cname(void);"
    }

    private fun translateArgument(name: String, clazz: ClassDescriptor, direction: Direction,
                                  builder: StringBuilder): String {
        val fqName = clazz.fqNameSafe.asString()
        return when {
            fqName == "kotlin.String" ->
                if (direction == Direction.C_TO_KOTLIN) {
                    builder.append("  KObjHolder ${name}_holder;\n")
                    "CreateStringFromCString($name, ${name}_holder.slot())"
                } else {
                    "CreateCStringFromString($name)"
                }
            owner.isMappedToReference(clazz) ->
                if (direction == Direction.C_TO_KOTLIN) {
                    builder.append("  KObjHolder ${name}_holder2;\n")
                    "DerefStablePointer(${name}.pinned, ${name}_holder2.slot())"
                } else {
                    "((${owner.translateType(clazz)}){ .pinned = CreateStablePointer(${name})})"
                }
            else -> name
        }
    }

    private fun translateBody(cfunction: List<Pair<String, ClassDescriptor>>): String {
        val builder = StringBuilder()
        builder.append("static ${owner.translateType(cfunction[0].second)} ${cname}_impl(${cfunction.drop(1).mapIndexed { index, it -> "${owner.translateType(it.second)} arg${index}" }.joinToString(", ")}) {\n")
        val args = ArrayList(cfunction.drop(1).mapIndexed { index, pair ->
            translateArgument("arg$index", pair.second, Direction.C_TO_KOTLIN, builder)
        })
        val isVoidReturned = owner.isMappedToVoid(cfunction[0].second)
        val isConstructor = declaration is ConstructorDescriptor
        val isObjectReturned = !isConstructor && owner.isMappedToReference(cfunction[0].second)
        val isStringReturned = owner.isMappedToString(cfunction[0].second)
        // TODO: do we really need that in every function?
        builder.append("  Kotlin_initRuntimeIfNeeded();\n")
        if (isObjectReturned || isStringReturned) {
            builder.append("  KObjHolder result_holder;\n")
            args += "result_holder.slot()"
        }
        if (isConstructor) {
            builder.append("  KObjHolder result_holder;\n")
            val clazz = scope.elements[0]
            assert(clazz.kind == ElementKind.TYPE)
            builder.append("  KObjHeader* result = AllocInstance((const KTypeInfo*)${clazz.cname}(), result_holder.slot());\n")
            args.add(0, "result")
        }
        if (!isVoidReturned && !isConstructor) {
            builder.append("  auto result = ")
        }
        builder.append("  $cname(")
        builder.append(args.joinToString(", "))
        builder.append(");\n")

        if (!isVoidReturned) {
            val result = translateArgument(
                    "result", cfunction[0].second, Direction.KOTLIN_TO_C, builder)
            builder.append("  return $result;\n")
        }
        builder.append("}\n")

        return builder.toString()
    }

    private fun addUsedType(type: org.jetbrains.kotlin.types.KotlinType, set: MutableSet<ClassDescriptor>) {
        if (type.constructor.declarationDescriptor is TypeParameterDescriptor) return
        val clazz = TypeUtils.getClassDescriptor(type)
        if (clazz == null) {
            context.reportCompilationWarning("cannot get class for $type")
        } else {
            set += clazz
        }
    }

    fun addUsedTypes(set: MutableSet<ClassDescriptor>) {
        val descriptor = declaration
        when (descriptor) {
            is FunctionDescriptor -> {
                val original = descriptor.original
                original.allParameters.forEach { addUsedType(it.type, set) }
                original.returnType?.let { addUsedType(it, set) }
            }
            is PropertyAccessorDescriptor -> {
                val original = descriptor.original
                addUsedType(original.correspondingProperty.type, set)
            }
        }
    }
}

private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
    val result = mutableSetOf<FqName>()

    fun getSubPackages(fqName: FqName) {
        result.add(fqName)
        module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
    }

    getSubPackages(FqName.ROOT)
    return result
}

private fun ModuleDescriptor.getPackageFragments(): List<PackageFragmentDescriptor> =
        getPackagesFqNames(this).flatMap {
            getPackage(it).fragments.filter { it.module == this }
        }

internal class CAdapterGenerator(
        val context: Context, internal val codegen: CodeGenerator) : DeclarationDescriptorVisitor<Boolean, Void?> {

    private val scopes = mutableListOf<ExportedElementScope>()
    internal val prefix = context.config.moduleId
    private lateinit var outputStreamWriter: PrintWriter
    private val paramNamesRecorded = mutableMapOf<String, Int>()

    internal fun paramsToUniqueNames(params: List<ParameterDescriptor>): List<String> {
        paramNamesRecorded.clear()
        return params.map {
            val name = translateName(it.name.asString()) 
            val count = paramNamesRecorded.getOrDefault(name, 0)
            paramNamesRecorded[name] = count + 1
            if (count == 0) {
                name
            } else {
                "$name${count.toString()}"
            }
        }
    }

    private fun visitChildren(descriptors: Collection<DeclarationDescriptor>) {
        for (descriptor in descriptors) {
            descriptor.accept(this, null)
        }
    }

    private fun visitChildren(descriptor: DeclarationDescriptor) {
        descriptor.accept(this, null)
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, ignored: Void?): Boolean {
        if (!isExportedClass(descriptor)) return true
        // TODO: fix me!
        val shortName = descriptor.fqNameSafe.shortName()
        if (shortName.isSpecial || shortName.asString().contains("<anonymous>"))
            return true
        val classScope = ExportedElementScope(ScopeKind.CLASS, shortName.asString())
        scopes.last().scopes += classScope
        scopes.push(classScope)
        // Add type getter.
        ExportedElement(ElementKind.TYPE, scopes.last(), descriptor, this)
        visitChildren(descriptor.getConstructors())
        visitChildren(DescriptorUtils.getAllDescriptors(descriptor.getDefaultType().memberScope))
        scopes.pop()
        return true
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, ignored: Void?): Boolean {
        descriptor.getter?.let { visitChildren(it) }
        descriptor.setter?.let { visitChildren(it) }
        return true
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitScriptDescriptor(descriptor: ScriptDescriptor, ignored: Void?): Boolean {
        context.reportCompilationWarning("visitScriptDescriptor() is ignored")
        return true
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, ignored: Void?): Boolean {
        if (descriptor.module != context.moduleDescriptor) return true
        val fragments = descriptor.module.getPackage(FqName.ROOT).fragments.filter { it.module == context.moduleDescriptor }
        visitChildren(fragments)
        return true
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, ignored: Void?): Boolean {
        TODO("visitValueParameterDescriptor() shall not be seen")
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, ignored: Void?): Boolean {
        TODO("visitReceiverParameterDescriptor() shall not be seen")
    }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, ignored: Void?): Boolean {
        context.reportCompilationWarning("visitVariableDescriptor() is ignored for now")
        return true
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, ignored: Void?): Boolean {
        context.reportCompilationWarning("visitTypeParameterDescriptor() is ignored for now")
        return true
    }

    private val seenPackageFragments = mutableSetOf<PackageFragmentDescriptor>()
    private var currentPackageFragments: List<PackageFragmentDescriptor> = emptyList()

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, ignored: Void?): Boolean {
        currentPackageFragments = descriptor.getPackageFragments().sortedWith(
                Comparator {
                    o1, o2 -> o1.fqName.toString().compareTo(o2.fqName.toString())
                })
        seenPackageFragments.clear()
        descriptor.getPackage(FqName.ROOT).accept(this, null)
        return true
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, ignored: Void?): Boolean {
        context.reportCompilationWarning("visitTypeAliasDescriptor() is ignored for now")
        return true
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, ignored: Void?): Boolean {
        val fqName = descriptor.fqName
        val name = if (fqName.isRoot) "root" else translateName(fqName.shortName().asString())
        val packageScope = ExportedElementScope(ScopeKind.PACKAGE, name)
        scopes.last().scopes += packageScope
        scopes.push(packageScope)
        visitChildren(DescriptorUtils.getAllDescriptors(descriptor.getMemberScope()))
        for (currentPackageFragment in currentPackageFragments) {
            if (!seenPackageFragments.contains(currentPackageFragment) &&
                    currentPackageFragment.fqName.isChildOf(descriptor.fqName)) {
                seenPackageFragments += currentPackageFragment
                visitChildren(currentPackageFragment)
            }
        }
        scopes.pop()
        return true
    }

    fun generateBindings() {
        scopes.push(ExportedElementScope(ScopeKind.TOP, "kotlin"))
        context.moduleDescriptor.accept(this, null)
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

    private fun makeElementDefinition(element: ExportedElement, kind: DefinitionKind, indent: Int) {
        when (kind) {
            DefinitionKind.C_HEADER_STRUCT -> {
                when {
                    element.isFunction ->
                        output(element.makeFunctionPointerString(), indent)
                    element.isClass ->
                        output("${prefix}_KType* (*_type)(void);", indent)
                // TODO: handle properties.
                }
            }

            DefinitionKind.C_SOURCE_DECLARATION -> {
                when {
                    element.isFunction ->
                        output(element.makeFunctionDeclaration(), 0)
                    element.isClass ->
                        output(element.makeClassDeclaration(), 0)
                // TODO: handle properties.
                }
            }

            DefinitionKind.C_SOURCE_STRUCT -> {
                when {
                    element.isFunction ->
                        output("/* ${element.name} = */ ${element.cname}_impl, ", indent)
                    element.isClass ->
                        output("/* Type for ${element.name} = */  ${element.cname}, ", indent)
                // TODO: handle properties.
                }
            }
        }
    }

    private fun makeScopeDefinitions(scope: ExportedElementScope,
                                     kind: DefinitionKind,
                                     indent: Int) {
        if (kind == DefinitionKind.C_HEADER_STRUCT) output("struct {", indent)
        if (kind == DefinitionKind.C_SOURCE_STRUCT) output(".${scope.name} = {", indent)
        scope.elements.forEach { makeElementDefinition(it, kind, indent + 1) }
        scope.scopes.forEach { makeScopeDefinitions(it, kind, indent + 1) }
        if (kind == DefinitionKind.C_HEADER_STRUCT) output("} ${scope.name};", indent)
        if (kind == DefinitionKind.C_SOURCE_STRUCT) output("},", indent)
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
            if (isMappedToReference(it)) {
                output("typedef struct {", indent)
                output("${prefix}_KNativePtr pinned;", indent + 1)
                output("} ${translateType(it)};", indent)
            }
        }
    }

    private fun makeGlobalStruct(top: ExportedElementScope) {
        val headerFile = context.config.tempFiles.cAdapterHeader
        outputStreamWriter = headerFile.printWriter()

        val exportedSymbol = "${prefix}_symbols"

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
        output("void (*DisposeString)(const char* string);", 1)
        output("${prefix}_KBoolean (*IsInstance)(${prefix}_KNativePtr ref, const ${prefix}_KType* type);", 1)

        output("")
        output("/* User functions. */", 1)
        makeScopeDefinitions(top, DefinitionKind.C_HEADER_STRUCT, 1)
        output("} ${prefix}_ExportedSymbols;")

        output("extern ${prefix}_ExportedSymbols* $exportedSymbol(void);")
        output("""
        #ifdef __cplusplus
        }  /* extern "C" */
        #endif""".trimIndent())

        output("#endif  /* KONAN_${prefix.toUpperCase()}_H */")

        outputStreamWriter.close()
        println("Produced dynamic library API in ${prefix}_api.h")

        outputStreamWriter = context.config.tempFiles
            .cAdapterCpp
            .printWriter()

        // Include header into C++ source.
        headerFile.forEachLine { it -> output(it) }

        output("""
        |struct KObjHeader;
        |typedef struct KObjHeader KObjHeader;
        |struct KTypeInfo;
        |typedef struct KTypeInfo KTypeInfo;
        |
        |#define RUNTIME_NOTHROW __attribute__((nothrow))
        |#define RUNTIME_USED __attribute__((used))
        |
        |extern "C" {
        |void UpdateRef(KObjHeader**, const KObjHeader*) RUNTIME_NOTHROW;
        |KObjHeader* AllocInstance(const KTypeInfo*, KObjHeader**) RUNTIME_NOTHROW;
        |KObjHeader* DerefStablePointer(void*, KObjHeader**) RUNTIME_NOTHROW;
        |void* CreateStablePointer(KObjHeader*) RUNTIME_NOTHROW;
        |void DisposeStablePointer(void*) RUNTIME_NOTHROW;
        |int IsInstance(const KObjHeader*, const KTypeInfo*) RUNTIME_NOTHROW;
        |void Kotlin_initRuntimeIfNeeded();
        |
        |KObjHeader* CreateStringFromCString(const char*, KObjHeader**);
        |char* CreateCStringFromString(const KObjHeader*);
        |void DisposeCString(char* cstring);
        |}  // extern "C"
        |
        |class KObjHolder {
        |public:
        |  KObjHolder() : obj_(nullptr) {}
        |  explicit KObjHolder(const KObjHeader* obj) : obj_(nullptr) {
        |    UpdateRef(&obj_, obj);
        |  }
        |  ~KObjHolder() {
        |    UpdateRef(&obj_, nullptr);
        |  }
        |  KObjHeader* obj() { return obj_; }
        |  KObjHeader** slot() { return &obj_; }
        | private:
        |  KObjHeader* obj_;
        |};
        |static void DisposeStablePointerImpl(${prefix}_KNativePtr ptr) {
        |  DisposeStablePointer(ptr);
        |}
        |static void DisposeStringImpl(const char* ptr) {
        |  DisposeCString((char*)ptr);
        |}
        |static ${prefix}_KBoolean IsInstanceImpl(${prefix}_KNativePtr ref, const ${prefix}_KType* type) {
        |  KObjHolder holder;
        |  return IsInstance(DerefStablePointer(ref, holder.slot()), (const KTypeInfo*)type);
        |}
        """.trimMargin())
        makeScopeDefinitions(top, DefinitionKind.C_SOURCE_DECLARATION, 0)
        output("static ${prefix}_ExportedSymbols __konan_symbols = {")
        output(".DisposeStablePointer = DisposeStablePointerImpl,", 1)
        output(".DisposeString = DisposeStringImpl,", 1)
        output(".IsInstance = IsInstanceImpl,", 1)
        makeScopeDefinitions(top, DefinitionKind.C_SOURCE_STRUCT, 1)
        output("};")
        output("RUNTIME_USED ${prefix}_ExportedSymbols* $exportedSymbol(void) { return &__konan_symbols;}")
        outputStreamWriter.close()

        if (context.config.targetManager.target.family == Family.WINDOWS) {
            outputStreamWriter = context.config.tempFiles
                .cAdapterDef
                .printWriter()
            output("EXPORTS")
            output(exportedSymbol)
            outputStreamWriter.close()
        }
    }

    private val simpleNameMapping = mapOf(
            "<this>" to "thiz",
            "<set-?>" to "set"
    )

    private val primitiveTypeMapping = mapOf(
            "kotlin.Byte" to "${prefix}_KByte",
            "kotlin.Short" to "${prefix}_KShort",
            "kotlin.Int" to "${prefix}_KInt",
            "kotlin.Long" to "${prefix}_KLong",
            "kotlin.Float" to "${prefix}_KFloat",
            "kotlin.Double" to "${prefix}_KDouble",
            "kotlin.Boolean" to "${prefix}_KBoolean",
            "kotlin.Char" to "${prefix}_KChar"
    )

    internal fun isMappedToString(descriptor: ClassDescriptor) =
            descriptor.fqNameSafe.asString() == "kotlin.String"

    internal fun isMappedToReference(descriptor: ClassDescriptor): Boolean {
        val name = descriptor.fqNameSafe.asString()
        return !descriptor.isUnit() && name != "kotlin.String" && !primitiveTypeMapping.contains(name)
    }

    internal fun isMappedToVoid(descriptor: ClassDescriptor): Boolean {
        return descriptor.isUnit()
    }

    fun translateName(name: String): String {
        return when {
            simpleNameMapping.contains(name) -> simpleNameMapping[name]!!
            cKeywords.contains(name) -> "${name}_"
            else -> name
        }
    }

    private fun translateTypeFull(clazz: ClassDescriptor): Pair<String, String> {
        val fqName = clazz.fqNameSafe.asString()
        return when {
            clazz.isUnit() -> "void" to "void"
            fqName == "kotlin.String" -> "const char*" to "KObjHeader*"
            primitiveTypeMapping.contains(fqName) -> primitiveTypeMapping[fqName]!! to primitiveTypeMapping[fqName]!!
            else -> "${prefix}_kref_${translateTypeFqName(clazz.fqNameSafe.asString())}" to "KObjHeader*"
        }
    }

    fun translateType(clazz: ClassDescriptor): String = translateTypeFull(clazz).first

    fun translateTypeBridge(clazz: ClassDescriptor): String = translateTypeFull(clazz).second

    fun translateTypeFqName(name: String): String {
        return name.replace('.', '_')
    }

    private var functionIndex = 0
    fun nextFunctionIndex() = functionIndex++

    internal val kGetTypeFuncType =
            LLVMFunctionType(codegen.kTypeInfoPtr, null, 0, 0)!!
}

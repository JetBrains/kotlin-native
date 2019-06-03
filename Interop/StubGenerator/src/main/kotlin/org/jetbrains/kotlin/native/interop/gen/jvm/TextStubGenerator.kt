/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.gen.*
import org.jetbrains.kotlin.native.interop.indexer.*
import java.lang.IllegalStateException

enum class KotlinPlatform {
    JVM,
    NATIVE
}

class TextStubGenerator(
        nativeIndex: NativeIndex,
        configuration: InteropConfiguration,
        private val libName: String,
        verbose: Boolean = false,
        val platform: KotlinPlatform = KotlinPlatform.JVM,
        val imports: Imports
) : StubGenerator<KotlinTextStub>(nativeIndex, verbose, configuration), TextualContext {

    private val jvmFileClassName = if (pkgName.isEmpty()) {
        libName
    } else {
        pkgName.substringAfterLast('.')
    }

    val generatedObjCCategoriesMembers = mutableMapOf<ObjCClass, GeneratedObjCCategoriesMembers>()

    private val platformWStringTypes = setOf("LPCWSTR")

    val declarationMapper = object : DeclarationMapper {
        override fun getKotlinClassForPointed(structDecl: StructDecl): Classifier {
            val baseName = structDecl.kotlinName
            val pkg = when (platform) {
                KotlinPlatform.JVM -> pkgName
                KotlinPlatform.NATIVE -> if (structDecl.def == null) {
                    cnamesStructsPackageName // to be imported as forward declaration.
                } else {
                    getPackageFor(structDecl)
                }
            }
            return Classifier.topLevel(pkg, baseName)
        }

        override fun isMappedToStrict(enumDef: EnumDef): Boolean = enumDef.isStrictEnum

        override fun getKotlinNameForValue(enumDef: EnumDef): String = enumDef.kotlinName

        override fun getPackageFor(declaration: TypeDeclaration): String {
            return imports.getPackage(declaration.location) ?: pkgName
        }

        override val useUnsignedTypes: Boolean
            get() = when (platform) {
                KotlinPlatform.JVM -> false
                KotlinPlatform.NATIVE -> true
            }
    }

    fun mirror(type: Type): TypeMirror = mirror(declarationMapper, type)

    private val macroConstantsByName = (nativeIndex.macroConstants + nativeIndex.wrappedMacros).associateBy { it.name }

    val kotlinFile = object : KotlinTextFile(pkgName, namesToBeDeclared = computeNamesToBeDeclared()) {
        override val mappingBridgeGenerator: MappingBridgeGenerator
            get() = this@TextStubGenerator.mappingBridgeGenerator
    }

    private fun computeNamesToBeDeclared(): MutableList<String> {
        return mutableListOf<String>().apply {
            nativeIndex.typedefs.forEach {
                getTypeDeclaringNames(Typedef(it), this)
            }

            nativeIndex.objCProtocols.forEach {
                add(it.kotlinClassName(isMeta = false))
                add(it.kotlinClassName(isMeta = true))
            }

            nativeIndex.objCClasses.forEach {
                add(it.kotlinClassName(isMeta = false))
                add(it.kotlinClassName(isMeta = true))
            }

            nativeIndex.structs.forEach {
                getTypeDeclaringNames(RecordType(it), this)
            }

            nativeIndex.enums.forEach {
                if (!it.isAnonymous) {
                    getTypeDeclaringNames(EnumType(it), this)
                }
            }
        }
    }

    /**
     * Finds all names to be declared for the given type declaration,
     * and adds them to [result].
     *
     * TODO: refactor to compute these names directly from declarations.
     */
    private fun getTypeDeclaringNames(type: Type, result: MutableList<String>) {
        if (type.unwrapTypedefs() == VoidType) {
            return
        }

        val mirror = mirror(type)
        val varClassifier = mirror.pointedType.classifier
        if (varClassifier.pkg == pkgName) {
            result.add(varClassifier.topLevelName)
        }
        when (mirror) {
            is TypeMirror.ByValue -> {
                val valueClassifier = mirror.valueType.classifier
                if (valueClassifier.pkg == pkgName && valueClassifier.topLevelName != varClassifier.topLevelName) {
                    result.add(valueClassifier.topLevelName)
                }
            }
            is TypeMirror.ByRef -> {}
        }
    }

    /**
     * The output currently used by the generator.
     * Should append line separator after any usage.
     */
    private var out: (String) -> Unit = {
        throw IllegalStateException()
    }

    private fun <R> withOutput(output: (String) -> Unit, action: () -> R): R {
        val oldOut = out
        out = output
        try {
            return action()
        } finally {
            out = oldOut
        }
    }

    private fun generateLinesBy(action: () -> Unit): List<String> {
        val result = mutableListOf<String>()
        withOutput({ result.add(it) }, action)
        return result
    }

    private fun <R> withOutput(appendable: Appendable, action: () -> R): R {
        return withOutput({ appendable.appendln(it) }, action)
    }

    override fun generateKotlinFragmentBy(block: () -> Unit): KotlinTextStub {
        val lines = generateLinesBy(block)
        return object : KotlinTextStub {
            override fun generate(context: StubGenerationContext) = lines.asSequence()
        }
    }

    private fun <R> indent(action: () -> R): R {
        val oldOut = out
        return withOutput({ oldOut("    $it") }, action)
    }

    private fun <R> block(header: String, body: () -> R): R {
        out("$header {")
        val res = indent {
            body()
        }
        out("}")
        return res
    }

    fun representCFunctionParameterAsValuesRef(type: Type): KotlinType? {
        val pointeeType = when (type) {
            is PointerType -> type.pointeeType
            is ArrayType -> type.elemType
            else -> return null
        }

        val unwrappedPointeeType = pointeeType.unwrapTypedefs()

        if (unwrappedPointeeType is VoidType) {
            // Represent `void*` as `CValuesRef<*>?`:
            return KotlinTypes.cValuesRef.typeWith(StarProjection).makeNullable()
        }

        if (unwrappedPointeeType is FunctionType) {
            // Don't represent function pointer as `CValuesRef<T>?` currently:
            return null
        }

        if (unwrappedPointeeType is ArrayType) {
            return representCFunctionParameterAsValuesRef(pointeeType)
        }


        return KotlinTypes.cValuesRef.typeWith(mirror(pointeeType).pointedType).makeNullable()
    }

    private fun Type.isAliasOf(names: Set<String>): Boolean {
        var type = this
        while (type is Typedef) {
            if (names.contains(type.def.name)) return true
            type = type.def.aliased
        }
        return false
    }

    private fun representCFunctionParameterAsString(function: FunctionDecl, type: Type): Boolean {
        val unwrappedType = type.unwrapTypedefs()
        return unwrappedType is PointerType && unwrappedType.pointeeIsConst &&
                unwrappedType.pointeeType.unwrapTypedefs() == CharType &&
                !noStringConversion.contains(function.name)
    }

    // We take this approach as generic 'const short*' shall not be used as String.
    private fun representCFunctionParameterAsWString(function: FunctionDecl, type: Type) = type.isAliasOf(platformWStringTypes)
            && !noStringConversion.contains(function.name)

    private fun getArrayLength(type: ArrayType): Long {
        val unwrappedElementType = type.elemType.unwrapTypedefs()
        val elementLength = if (unwrappedElementType is ArrayType) {
            getArrayLength(unwrappedElementType)
        } else {
            1L
        }

        val elementCount = when (type) {
            is ConstArrayType -> type.length
            is IncompleteArrayType -> 0L
            else -> TODO(type.toString())
        }

        return elementLength * elementCount
    }

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given struct.
     */
    override fun generateStruct(struct: StructDecl) {
        val def = struct.def
        if (def == null) {
            generateForwardStruct(struct)
            return
        }

        if (platform == KotlinPlatform.JVM) {
            if (def.kind == StructDef.Kind.STRUCT && def.fieldsHaveDefaultAlignment()) {
                out("@CNaturalStruct(${def.members.joinToString { it.name.quoteAsKotlinLiteral() }})")
            }
        } else {
            tryRenderStructOrUnion(def)?.let {
                out("@CStruct".applyToStrings(it))
            }
        }

        val kotlinName = kotlinFile.declare(declarationMapper.getKotlinClassForPointed(struct))

        block("class $kotlinName(rawPtr: NativePtr) : CStructVar(rawPtr)") {
            out("")
            out("companion object : Type(${def.size}, ${def.align})") // FIXME: align
            out("")
            for (field in def.fields) {
                try {
                    assert(field.name.isNotEmpty())
                    assert(field.offset % 8 == 0L)
                    val offset = field.offset / 8
                    val fieldRefType = mirror(field.type)
                    val unwrappedFieldType = field.type.unwrapTypedefs()
                    if (unwrappedFieldType is ArrayType) {
                        val type = (fieldRefType as TypeMirror.ByValue).valueType.render(kotlinFile)

                        if (platform == KotlinPlatform.JVM) {
                            val length = getArrayLength(unwrappedFieldType)

                            // TODO: @CLength should probably be used on types instead of properties.
                            out("@CLength($length)")
                        }

                        out("val ${field.name.asSimpleName()}: $type")
                        out("    get() = arrayMemberAt($offset)")
                    } else {
                        val pointedTypeName = fieldRefType.pointedType.render(kotlinFile)
                        if (fieldRefType is TypeMirror.ByValue) {
                            out("var ${field.name.asSimpleName()}: ${fieldRefType.argType.render(kotlinFile)}")
                            out("    get() = memberAt<$pointedTypeName>($offset).value")
                            out("    set(value) { memberAt<$pointedTypeName>($offset).value = value }")
                        } else {
                            out("val ${field.name.asSimpleName()}: $pointedTypeName")
                            out("    get() = memberAt($offset)")
                        }
                    }
                    out("")
                } catch (e: Throwable) {
                    log("Warning: cannot generate definition for field ${struct.kotlinName}.${field.name}")
                }
            }

            if (platform == KotlinPlatform.NATIVE) {
                for (field in def.bitFields) {
                    val typeMirror = mirror(field.type)
                    val typeInfo = typeMirror.info
                    val kotlinType = typeMirror.argType.render(kotlinFile)
                    val rawType = typeInfo.bridgedType

                    out("var ${field.name.asSimpleName()}: $kotlinType")

                    val signed = field.type.isIntegerTypeSigned()

                    val readBitsExpr =
                            "readBits(this.rawPtr, ${field.offset}, ${field.size}, $signed).${rawType.convertor!!}()"

                    val getExpr = typeInfo.argFromBridged(readBitsExpr, kotlinFile, object : NativeBacked {})
                    out("    get() = $getExpr")

                    val rawValue = typeInfo.argToBridged("value")
                    val setExpr = "writeBits(this.rawPtr, ${field.offset}, ${field.size}, $rawValue.toLong())"
                    out("    set(value) = $setExpr")
                    out("")
                }
            }
        }
    }

    private tailrec fun Type.isIntegerTypeSigned(): Boolean = when (this) {
        is IntegerType -> this.isSigned
        is BoolType -> false
        is EnumType -> this.def.baseType.isIntegerTypeSigned()
        is Typedef -> this.def.aliased.isIntegerTypeSigned()
        else -> error(this)
    }

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given forward (incomplete) struct.
     */
    private fun generateForwardStruct(s: StructDecl) = when (platform) {
        KotlinPlatform.JVM -> out("class ${s.kotlinName.asSimpleName()}(rawPtr: NativePtr) : COpaque(rawPtr)")
        KotlinPlatform.NATIVE -> {}
    }

    private fun EnumConstant.isMoreCanonicalThan(other: EnumConstant): Boolean = with(other.name.toLowerCase()) {
        contains("min") || contains("max") ||
                contains("first") || contains("last") ||
                contains("begin") || contains("end")
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum.
     */
    override fun generateEnum(e: EnumDef) {
        if (!e.isStrictEnum) {
            generateEnumAsConstants(e)
            return
        }

        val baseTypeMirror = mirror(e.baseType)
        val baseKotlinType = baseTypeMirror.argType.render(kotlinFile)

        val canonicalsByValue = e.constants
                .groupingBy { it.value }
                .reduce { _, accumulator, element ->
                    if (element.isMoreCanonicalThan(accumulator)) {
                        element
                    } else {
                        accumulator
                    }
                }

        val (canonicalConstants, aliasConstants) = e.constants.partition { canonicalsByValue[it.value] == it }

        val clazz = (mirror(EnumType(e)) as TypeMirror.ByValue).valueType.classifier

        block("enum class ${kotlinFile.declare(clazz)}(override val value: $baseKotlinType) : CEnum") {
            canonicalConstants.forEach {
                val literal = integerLiteral(e.baseType, it.value)!!
                out("${it.name.asSimpleName()}($literal),")
            }
            out(";")
            out("")
            block("companion object") {
                aliasConstants.forEach {
                    val mainConstant = canonicalsByValue[it.value]!!
                    out("val ${it.name.asSimpleName()} = ${mainConstant.name.asSimpleName()}")
                }
                if (aliasConstants.isNotEmpty()) out("")

                out("fun byValue(value: $baseKotlinType) = " +
                        "${e.kotlinName.asSimpleName()}.values().find { it.value == value }!!")
            }
            out("")
            block("class Var(rawPtr: NativePtr) : CEnumVar(rawPtr)") {
                val basePointedTypeName = baseTypeMirror.pointedType.render(kotlinFile)
                out("companion object : Type($basePointedTypeName.size.toInt())")
                out("var value: ${e.kotlinName.asSimpleName()}")
                out("    get() = byValue(this.reinterpret<$basePointedTypeName>().value)")
                out("    set(value) { this.reinterpret<$basePointedTypeName>().value = value.value }")
            }
        }
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum which shouldn't be represented as Kotlin enum.
     *
     * @see isStrictEnum
     */
    private fun generateEnumAsConstants(e: EnumDef) {
        // TODO: if this enum defines e.g. a type of struct field, then it should be generated inside the struct class
        // to prevent name clashing

        val constants = e.constants.filter {
            // Macro "overrides" the original enum constant.
            it.name !in macroConstantsByName
        }

        val kotlinType: KotlinType

        val baseKotlinType = mirror(e.baseType).argType
        if (e.isAnonymous) {
            if (constants.isNotEmpty()) {
                out("// ${e.spelling}:")
            }

            kotlinType = baseKotlinType
        } else {
            val typeMirror = mirror(EnumType(e))
            if (typeMirror !is TypeMirror.ByValue) {
                error("unexpected enum type mirror: $typeMirror")
            }

            // Generate as typedef:
            val varTypeName = typeMirror.info.constructPointedType(typeMirror.valueType).render(kotlinFile)
            val varTypeClassifier = typeMirror.pointedType.classifier
            val valueTypeClassifier = typeMirror.valueType.classifier
            out("typealias ${kotlinFile.declare(varTypeClassifier)} = $varTypeName")
            out("typealias ${kotlinFile.declare(valueTypeClassifier)} = ${baseKotlinType.render(kotlinFile)}")

            if (constants.isNotEmpty()) {
                out("")
            }

            kotlinType = typeMirror.valueType
        }

        for (constant in constants) {
            val literal = integerLiteral(e.baseType, constant.value) ?: continue
            out(topLevelValWithGetter(constant.name, kotlinType, literal))
        }
    }

    override fun generateTypedef(def: TypedefDef) {
        val mirror = mirror(Typedef(def))
        val baseMirror = mirror(def.aliased)

        val varType = mirror.pointedType
        when (baseMirror) {
            is TypeMirror.ByValue -> {
                val valueType = (mirror as TypeMirror.ByValue).valueType
                val varTypeAliasee = mirror.info.constructPointedType(valueType).render(kotlinFile)
                val valueTypeAliasee = baseMirror.valueType.render(kotlinFile)

                out("typealias ${kotlinFile.declare(varType.classifier)} = $varTypeAliasee")
                out("typealias ${kotlinFile.declare(valueType.classifier)} = $valueTypeAliasee")
            }
            is TypeMirror.ByRef -> {
                val varTypeAliasee = baseMirror.pointedType.render(kotlinFile)
                out("typealias ${kotlinFile.declare(varType.classifier)} = $varTypeAliasee")
            }
        }
    }

    override fun generateStubsForFunctions(functions: List<FunctionDecl>): List<KotlinTextStub> {
        val stubs = functions.mapNotNull {
            try {
                KotlinFunctionTextStub(it)
            } catch (e: Throwable) {
                log("Warning: cannot generate stubs for function ${it.name}")
                null
            }
        }

        return stubs
    }

    private fun FunctionDecl.returnsVoid(): Boolean = this.returnType.unwrapTypedefs() is VoidType

    private inner class KotlinFunctionTextStub(val func: FunctionDecl) : KotlinTextStub, NativeBacked {
        override fun generate(context: StubGenerationContext): Sequence<String> =
                if (isCCall) {
                    sequenceOf("@CCall".applyToStrings(cCallSymbolName!!), "external $header")
                } else if (context.nativeBridges.isSupported(this)) {
                    block(header, bodyLines)
                } else {
                    sequenceOf(
                            annotationForUnableToImport,
                            "$header = throw UnsupportedOperationException()"
                    )
                }

        private val header: String
        private val bodyLines: List<String>
        private val isCCall: Boolean
        private val cCallSymbolName: String?

        init {
            val kotlinParameters = mutableListOf<Pair<String, KotlinType>>()
            val bodyGenerator = KotlinCodeBuilder(scope = kotlinFile)
            val bridgeArguments = mutableListOf<TypedKotlinValue>()

            func.parameters.forEachIndexed { index, parameter ->
                val parameterName = parameter.name.let {
                    if (it == null || it.isEmpty()) {
                        "arg$index"
                    } else {
                        it.asSimpleName()
                    }
                }

                val representAsValuesRef = representCFunctionParameterAsValuesRef(parameter.type)

                val bridgeArgument = when {
                    representCFunctionParameterAsString(func, parameter.type) -> {
                        val annotations = when (platform) {
                            KotlinPlatform.JVM -> ""
                            KotlinPlatform.NATIVE -> "@CCall.CString "
                        }
                        kotlinParameters.add(annotations + parameterName to KotlinTypes.string.makeNullable())
                        bodyGenerator.pushMemScoped()
                        "$parameterName?.cstr?.getPointer(memScope)"
                    }
                    representCFunctionParameterAsWString(func, parameter.type) -> {
                        val annotations = when (platform) {
                            KotlinPlatform.JVM -> ""
                            KotlinPlatform.NATIVE -> "@CCall.WCString "
                        }
                        kotlinParameters.add(annotations + parameterName to KotlinTypes.string.makeNullable())
                        bodyGenerator.pushMemScoped()
                        "$parameterName?.wcstr?.getPointer(memScope)"
                    }
                    representAsValuesRef != null -> {
                        kotlinParameters.add(parameterName to representAsValuesRef)
                        bodyGenerator.pushMemScoped()
                        bodyGenerator.getNativePointer(parameterName)
                    }
                    else -> {
                        val mirror = mirror(parameter.type)
                        kotlinParameters.add(parameterName to mirror.argType)
                        parameterName
                    }
                }
                bridgeArguments.add(TypedKotlinValue(parameter.type, bridgeArgument))
            }

            if (!func.isVararg || platform != KotlinPlatform.NATIVE) {
                val result = mappingBridgeGenerator.kotlinToNative(
                        bodyGenerator,
                        this,
                        func.returnType,
                        bridgeArguments,
                        independent = false
                ) { nativeValues ->
                    "${func.name}(${nativeValues.joinToString()})"
                }
                bodyGenerator.returnResult(result)
                isCCall = false
                cCallSymbolName = null
            } else {
                kotlinParameters.add("vararg variadicArguments" to KotlinTypes.any.makeNullable())
                isCCall = true // TODO: don't generate unused body in this case.
                cCallSymbolName = "knifunptr_" + pkgName.replace('.', '_') + nextUniqueId()

                simpleBridgeGenerator.insertNativeBridge(
                        this,
                        emptyList(),
                        listOf("extern const void* $cCallSymbolName __asm(${cCallSymbolName.quoteAsKotlinLiteral()});",
                                "extern const void* $cCallSymbolName = &${func.name};")
                )
            }

            val returnType = if (func.returnsVoid()) {
                KotlinTypes.unit
            } else {
                mirror(func.returnType).argType
            }.render(kotlinFile)

            val joinedKotlinParameters = kotlinParameters.joinToString { (name, type) ->
                "$name: ${type.render(kotlinFile)}"
            }
            this.header = "fun ${func.name.asSimpleName()}($joinedKotlinParameters): $returnType"

            this.bodyLines = bodyGenerator.build()
        }
    }

    private fun integerLiteral(type: Type, value: Long): String? {
        val integerType = type.unwrapTypedefs() as? IntegerType ?: return null
        return integerLiteral(integerType.size, declarationMapper.isMappedToSigned(integerType), value)
    }

    private fun integerLiteral(size: Int, isSigned: Boolean, value: Long): String? {
        return if (isSigned) {
            if (value == Long.MIN_VALUE) {
                return "${value + 1} - 1" // Workaround for "The value is out of range" compile error.
            }

            val narrowedValue: Number = when (size) {
                1 -> value.toByte()
                2 -> value.toShort()
                4 -> value.toInt()
                8 -> value
                else -> return null
            }

            narrowedValue.toString()
        } else {
            // Note: stub generator is built and run with different ABI versions,
            // so Kotlin unsigned types can't be used here currently.

            val narrowedValue: String = when (size) {
                1 -> (value and 0xFF).toString()
                2 -> (value and 0xFFFF).toString()
                4 -> (value and 0xFFFFFFFF).toString()
                8 -> java.lang.Long.toUnsignedString(value)
                else -> return null
            }

            "${narrowedValue}u"
        }
    }

    private fun floatingLiteral(type: Type, value: Double): String? {
        val unwrappedType = type.unwrapTypedefs()
        if (unwrappedType !is FloatingType) return null
        return when (unwrappedType.size) {
            4 -> {
                val floatValue = value.toFloat()
                val bits = java.lang.Float.floatToRawIntBits(floatValue)
                "bitsToFloat($bits) /* == $floatValue */"
            }
            8 -> {
                val bits = java.lang.Double.doubleToRawLongBits(value)
                "bitsToDouble($bits) /* == $value */"
            }
            else -> null
        }
    }

    private fun topLevelValWithGetter(name: String, type: KotlinType, expressionBody: String): String =
            "val ${name.asSimpleName()}: ${type.render(kotlinFile)} get() = $expressionBody"

    private fun topLevelConstVal(name: String, type: KotlinType, initializer: String): String =
            "const val ${name.asSimpleName()}: ${type.render(kotlinFile)} = $initializer"

    override fun generateConstant(constant: ConstantDef) {
        val kotlinName = constant.name
        val declaration = when (constant) {
            is IntegerConstantDef -> {
                val literal = integerLiteral(constant.type, constant.value) ?: return
                val kotlinType = mirror(constant.type).argType
                when (platform) {
                    KotlinPlatform.NATIVE -> topLevelConstVal(kotlinName, kotlinType, literal)
                    // No reason to make it const val with backing field on Kotlin/JVM yet:
                    KotlinPlatform.JVM -> topLevelValWithGetter(kotlinName, kotlinType, literal)
                }
            }
            is FloatingConstantDef -> {
                val literal = floatingLiteral(constant.type, constant.value) ?: return
                val kotlinType = mirror(constant.type).argType
                topLevelValWithGetter(kotlinName, kotlinType, literal)
            }
            is StringConstantDef -> {
                val literal = constant.value.quoteAsKotlinLiteral()
                val kotlinType = KotlinTypes.string
                topLevelValWithGetter(kotlinName, kotlinType, literal)
            }
            else -> return
        }

        out(declaration)

        // TODO: consider using `const` modifier in all cases.
        // Note: It is not currently possible for floating literals.
    }

    /**
     * Produces to [out] the contents of file with Kotlin bindings.
     */
    private fun generateKotlinFile(nativeBridges: NativeTextBridges, stubs: List<KotlinTextStub>) {
        if (platform == KotlinPlatform.JVM) {
            out("@file:JvmName(${jvmFileClassName.quoteAsKotlinLiteral()})")
        }
        if (platform == KotlinPlatform.NATIVE) {
            out("@file:kotlinx.cinterop.InteropStubs")
        }

        val suppress = mutableListOf("UNUSED_VARIABLE", "UNUSED_EXPRESSION").apply {
            if (configuration.library.language == Language.OBJECTIVE_C) {
                add("CONFLICTING_OVERLOADS")
                add("RETURN_TYPE_MISMATCH_ON_INHERITANCE")
                add("PROPERTY_TYPE_MISMATCH_ON_INHERITANCE") // Multiple-inheriting property with conflicting types
                add("VAR_TYPE_MISMATCH_ON_INHERITANCE") // Multiple-inheriting mutable property with conflicting types
                add("RETURN_TYPE_MISMATCH_ON_OVERRIDE")
                add("WRONG_MODIFIER_CONTAINING_DECLARATION") // For `final val` in interface.
                add("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                add("UNUSED_PARAMETER") // For constructors.
                add("MANY_IMPL_MEMBER_NOT_IMPLEMENTED") // Workaround for multiple-inherited properties.
                add("MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED") // Workaround for multiple-inherited properties.
                add("EXTENSION_SHADOWED_BY_MEMBER") // For Objective-C categories represented as extensions.
                add("REDUNDANT_NULLABLE") // This warning appears due to Obj-C typedef nullability incomplete support.
                add("DEPRECATION") // For uncheckedCast.
                add("DEPRECATION_ERROR") // For initializers.
            }
        }

        out("@file:Suppress(${suppress.joinToString { it.quoteAsKotlinLiteral() }})")
        if (pkgName != "") {
            val packageName = pkgName.split(".").joinToString("."){
                if(it.matches(VALID_PACKAGE_NAME_REGEX)){
                    it
                }else{
                    "`$it`"
                }
            }
            out("package $packageName")
            out("")
        }
        if (platform == KotlinPlatform.NATIVE) {
            out("import kotlin.native.SymbolName")
            out("import kotlinx.cinterop.internal.*")
        }
        out("import kotlinx.cinterop.*")

        kotlinFile.buildImports().forEach {
            out(it)
        }

        out("")

        out("// NOTE THIS FILE IS AUTO-GENERATED")
        out("")

        val context = object : StubGenerationContext {
            val topLevelDeclarationLines = mutableListOf<String>()

            override val nativeBridges: NativeTextBridges get() = nativeBridges
            override fun addTopLevelDeclaration(lines: List<String>) {
                topLevelDeclarationLines.addAll(lines)
            }
        }

        stubs.forEach {
            it.generate(context).forEach(out)
            out("")
        }

        context.topLevelDeclarationLines.forEach(out)
        nativeBridges.kotlinParts.forEach(out)
        if (platform == KotlinPlatform.JVM) {
            out("private val loadLibrary = System.loadLibrary(\"$libName\")")
        }
    }

    val libraryForCStubs = configuration.library.copy(
            includes = mutableListOf<String>().apply {
                add("stdint.h")
                add("string.h")
                if (platform == KotlinPlatform.JVM) {
                    add("jni.h")
                }
                addAll(configuration.library.includes)
            },

            compilerArgs = configuration.library.compilerArgs,

            additionalPreambleLines = configuration.library.additionalPreambleLines +
                    when (configuration.library.language) {
                        Language.C -> emptyList()
                        Language.OBJECTIVE_C -> listOf("void objc_terminate();")
                    }
    )

    /**
     * Produces to [out] the contents of C source file to be compiled into native lib used for Kotlin bindings impl.
     */
    private fun generateCFile(bridges: NativeTextBridges, entryPoint: String?) {
        libraryForCStubs.preambleLines.forEach {
            out(it)
        }
        out("")

        out("// NOTE THIS FILE IS AUTO-GENERATED")
        out("")

        bridges.nativeParts.forEach {
            out(it)
        }

        if (entryPoint != null) {
            out("extern int Konan_main(int argc, char** argv);")
            out("")
            out("__attribute__((__used__))")
            out("int $entryPoint(int argc, char** argv)  {")
            out("  return Konan_main(argc, argv);")
            out("}")
        }
    }

    fun generateFiles(ktFile: Appendable, cFile: Appendable, entryPoint: String?) {
        val stubs: List<KotlinTextStub> = generateStubs()

        val nativeBridges: NativeTextBridges = simpleBridgeGenerator.prepare()

        withOutput(cFile) {
            generateCFile(nativeBridges, entryPoint)
        }

        withOutput(ktFile) {
            generateKotlinFile(nativeBridges, stubs)
        }
    }

    val simpleBridgeGenerator: SimpleBridgeGenerator =
            SimpleBridgeGeneratorImpl(
                    platform,
                    pkgName,
                    jvmFileClassName,
                    libraryForCStubs,
                    topLevelNativeScope = object : NativeScope {
                        override val mappingBridgeGenerator: MappingBridgeGenerator
                            get() = this@TextStubGenerator.mappingBridgeGenerator
                    },
                    topLevelKotlinScope = kotlinFile
            )

    val mappingBridgeGenerator: MappingBridgeGenerator =
            MappingBridgeGeneratorImpl(declarationMapper, simpleBridgeGenerator)

    companion object {
        private val VALID_PACKAGE_NAME_REGEX = "[a-zA-Z0-9_.]+".toRegex()
    }

    override fun generateObjCProtocolStub(protocol: ObjCProtocol): KotlinTextStub =
            ObjCProtocolTextStub(this, protocol)

    override fun generateObjCClassStub(klass: ObjCClass): KotlinTextStub =
            ObjCClassTextStub(this, klass)

    override fun generateObjCCategory(category: ObjCCategory): KotlinTextStub =
            ObjCCategoryTextStub(this, category)

    override fun generateGlobalVariableStub(globalVariable: GlobalDecl): KotlinTextStub =
            GlobalVariableTextStub(globalVariable, this)
}

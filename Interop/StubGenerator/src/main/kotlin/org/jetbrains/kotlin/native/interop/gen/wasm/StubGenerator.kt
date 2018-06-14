package org.jetbrains.kotlin.native.interop.gen.wasm

import org.jetbrains.idl2k.*
import org.jetbrains.idl2k.AttributeKind.*
import org.jetbrains.kotlin.native.interop.gen.argsToCompiler
import org.jetbrains.kotlin.native.interop.tool.JsInteropArguments
import org.jetbrains.kotlin.native.interop.tool.parseCommandLine
import java.io.File

fun kotlinHeader(packageName: String, allPackages: Sequence<String>): String {
    return "/* Generated file, DO NOT EDIT */\n" +
            if (packageName == "") "\n"
            else "package $packageName\n" +
            allPackages.filter { it != packageName }.map { "import $it.*" }.joinToString("\n") + 
            "\nimport kotlinx.wasm.jsinterop.*\n"
}

val IntType =  SimpleType("Int", false)
val ShortType = SimpleType("Short", false)
val ByteType = SimpleType("Byte", false)
val FloatType = SimpleType("Float", false)
val DoubleType = SimpleType("Double", false)
val BooleanType = SimpleType("Boolean", false)
val StringType = SimpleType("String", false)

val NullableIntType =  SimpleType("Int", true)
val NullableShortType = SimpleType("Short", true)
val NullableByteType = SimpleType("Byte", true)
val NullableFloatType = SimpleType("Float", true)
val NullableDoubleType = SimpleType("Double", true)
val NullableBooleanType = SimpleType("Boolean", true)
val NullableStringType = SimpleType("String", true)


val SimpleType.isPrimitive get() = when (this) {
    IntType, ShortType, ByteType, FloatType, DoubleType, BooleanType, StringType, NullableIntType, NullableShortType,
    NullableByteType, NullableFloatType, NullableDoubleType, NullableBooleanType, NullableStringType
        -> true
    else
        -> false
}

fun Type.toKotlinType(argName: String? = null): String = when (this) {
    is UnitType -> "Unit"
    BooleanType -> "Boolean"
    IntType -> "Int"
    FloatType -> "Float"
    DoubleType -> "Double"
    StringType -> "String"
    is AnyType, is DynamicType, is UnionType, is PromiseType -> "JsValue"
    is ArrayType -> "JsArray"
    is FunctionType -> "KtFunction<R${argName!!}>"
    is SimpleType -> type
    else -> TODO("Unexpected type: $this")
} + if (nullable) "?" else ""

fun GenerateAttribute.wasmArgMapping(): String {
    val arg = "${_name}Arg"
    return when (type) {
        is UnitType -> error("An arg can not be UnitType")
        BooleanType -> "if ($arg) 1 else 0"
        ByteType -> "$arg.toInt()"
        ShortType -> "$arg.toInt()"
        IntType -> arg
        FloatType -> "$arg.toBits()"
        DoubleType -> "doubleUpper($arg), doubleLower($arg)"
        StringType -> "stringPointer($arg), stringLengthBytes($arg)"
        is AnyType, is DynamicType, is UnionType, is PromiseType -> "$arg._arena, $arg._index"
        is FunctionType -> if (!type.nullable) "wrapFunction<R$name>($arg), ArenaManager.currentArena"
                            else "$arg._arena, $arg._index"
        is SimpleType -> if (type.isEnum) "stringPointer($arg.value), stringLengthBytes($arg.value)"
                         else "$arg._arena, $arg._index"
        is ArrayType -> "$arg._arena, $arg._index"
        else -> error("Unexpected type: $type")
    }
}

fun Type.wrap(value: String): String {
    val arena = "ArenaManager.currentArena"
    return "      val ${value}Arg = if ($value == null) JsNullValue else " +
        when (this) {
            ByteType, NullableByteType
                -> "$value.boxByte($arena)"
            ShortType, NullableShortType
                -> "$value.boxShort($arena)"
            IntType, NullableIntType
                -> "$value.boxInt($arena)"
            BooleanType, NullableBooleanType
                -> "$value.boxBoolean($arena)"
            StringType, NullableStringType
                -> "$value.boxString($arena)"
            DoubleType, NullableDoubleType
                -> "$value.boxDouble($arena)"
            is FunctionType
                -> "$value.wrapFunction($arena)"
            is SimpleType
                -> if (isEnum || isNullableEnum) "$value.value.boxString($arena)"
                        else value
            else -> value
        }
}


fun Type.wasmReturnArg(): String = "ArenaManager.currentArena"

val GenerateFunction.wasmReturnArg: String get() = returnType.wasmReturnArg()
val GenerateAttribute.wasmReturnArg: String get() = type.wasmReturnArg()

fun GenerateAttribute.wasmArgNames(): List<String> = when (type) {
    is UnitType -> error("An arg can not be UnitType")
    BooleanType -> listOf(_name)
    ByteType -> listOf(_name)
    ShortType -> listOf(_name)
    IntType -> listOf(_name)
    FloatType -> listOf(_name)
    DoubleType -> listOf("${name}Upper", "${name}Lower")
    StringType -> listOf("${name}Ptr", "${name}Len")
    BooleanType -> listOf(_name)
    is AnyType, is DynamicType, is UnionType, is PromiseType -> listOf("${name}Arena", "${name}Index")
    is FunctionType -> listOf("${name}Index", "${name}ResultArena")
    is SimpleType -> if (type.isEnum) listOf("${name}Ptr", "${name}Len")
                     else listOf("${name}Arena", "${name}Index")
    is ArrayType -> listOf("${name}Arena", "${name}Index")
    else -> error("Unexpected type: $type")
}

val SimpleType.toNonNullable get() = SimpleType(this.type, false)
val SimpleType.implType get() = "${this.type}Impl"

fun Type.wasmReturnMapping(value: String): String = when (this) {
    is UnitType -> ""
    BooleanType, NullableBooleanType -> "(($value) != 0)"
    ByteType, NullableByteType -> "$value.toByte()"
    ShortType, NullableShortType -> "$value.toShort()"
    IntType, NullableIntType -> value
    FloatType, NullableFloatType -> "Float.Companion.fromBits($value)"
    DoubleType, NullableDoubleType -> value // TODO: conversion?

    StringType, NullableStringType -> value
    is AnyType, is DynamicType, is UnionType, is PromiseType -> "JsValue(ArenaManager.currentArena, $value)"
    is FunctionType -> "TODO(\"Implement me\")"
    is SimpleType -> if (this.isEnum || this.isNullableEnum) value else  "$implType(ArenaManager.currentArena, $value)"
    is ArrayType -> "JsArray(ArenaManager.currentArena, $value)"
    else -> error("Unexpected type")
}

val wasmFunctionCache = mutableMapOf<Pair<GenerateTraitOrClass, GenerateFunction>, String>()
val wasmGetterCache = mutableMapOf<Pair<GenerateTraitOrClass, GenerateAttribute>, String>()
val wasmSetterCache = mutableMapOf<Pair<GenerateTraitOrClass, GenerateAttribute>, String>()

fun <T> String.uniqueIn(cache: Map<Pair<GenerateTraitOrClass, T>, String>): String {
    var candidate = this
    var addend = 0
    val takenNames = cache.values

    while (candidate in takenNames) {
        ++addend;
        candidate = "${this}_$addend"
    }
    return candidate
}

fun wasmFunctionName(func: GenerateFunction, iface: GenerateTraitOrClass)
    = wasmFunctionCache.getOrPut(iface to func){"knjs__${iface.name}_${func.name}".uniqueIn(wasmFunctionCache)}

fun wasmGetterName(attr: GenerateAttribute, iface: GenerateTraitOrClass)
    = wasmGetterCache.getOrPut(iface to attr){ "knjs_get__${iface.name}_${attr.name}".uniqueIn(wasmGetterCache) }

fun wasmSetterName(attr: GenerateAttribute, iface: GenerateTraitOrClass)
    = wasmSetterCache.getOrPut(iface to attr){ "knjs_set__${iface.name}_${attr.name}".uniqueIn(wasmSetterCache) }

val GenerateFunction.kotlinTypeParameters: String get() {
    val lambdaRetTypes = arguments.filter { it.type is FunctionType }
        .map { "R${it.name}" }. joinToString(", ")
    return if (lambdaRetTypes == "") "" else "<$lambdaRetTypes>"
}

val GenerateAttribute.kotlinTypeParameters: String get() {
    val lambdaRetTypes = if (type is FunctionType) "R$name" else ""
    return if (lambdaRetTypes == "") "" else "<$lambdaRetTypes>"
}

val GenerateTraitOrClass.wasmReceiverArgs get() =
    listOf("this._arena", "this._index")

fun GenerateAttribute.wasmReceiverArgs(parent: GenerateTraitOrClass) =
    if (static) emptyList()
    else parent.wasmReceiverArgs

fun GenerateFunction.wasmReceiverArgs(parent: GenerateTraitOrClass) =
    if (static) emptyList()
    else parent.wasmReceiverArgs

fun Type.generateKotlinCall(name: String, wasmArgList: String) =
    "$name($wasmArgList)" 

val Type.unbox: String get() {
    return when (this) {
        NullableBooleanType, NullableIntType, NullableFloatType
            -> "val wasmRetVal = unboxPrimitive(ArenaManager.currentArena, nullable)\n"
        NullableDoubleType
            -> "unboxDouble(ArenaManager.currentArena, nullable)\nval wasmRetVal = ReturnSlot_getDouble()\n"
        NullableStringType
            -> "   val size = unboxString(ArenaManager.currentArena, nullable)\n" +
               "    val wasmRetVal = AllocateAndFetchString(size)\n"
        is SimpleType
            -> if (this.isNullableEnum)
                   "    val size = unboxString(ArenaManager.currentArena, nullable)\n" +
                   "    val stringValue = AllocateAndFetchString(size)\n" +
                   "    val wasmRetVal = ${this.type}.valueOf(stringValue)\n"
               else
                   "    val wasmRetVal = nullable\n"
        else
            -> "    val wasmRetVal = nullable\n"
    }
}

fun Type.generateKotlinCallWithReturn(name: String, wasmArgList: String): String {
    return if (this.nullable)
        "    val nullable = ${generateKotlinCall(name, wasmArgList)}\n" +
        "    if (isNull(ArenaManager.currentArena, nullable) == 1) return null\n" +
        this.unbox
    else when(this) {
        is UnitType ->   "    ${generateKotlinCall(name, wasmArgList)}\n"
        DoubleType -> "    ${generateKotlinCall(name, wasmArgList)}\n" +
                        "    val wasmRetVal = ReturnSlot_getDouble()\n"
        StringType -> "    val size = ${generateKotlinCall(name, wasmArgList)}\n" +
                        "    val wasmRetVal = AllocateAndFetchString(size)\n"
        is SimpleType -> if (this.isEnum) "    val size = ${generateKotlinCall(name, wasmArgList)}\n" +
                        "    val stringValue = AllocateAndFetchString(size)\n" +
                        "    val wasmRetVal = ${this.type}.valueOf(stringValue)\n"
                        else "    val wasmRetVal = ${generateKotlinCall(name, wasmArgList)}\n"

        else ->         "    val wasmRetVal = ${generateKotlinCall(name, wasmArgList)}\n"
    }
}

fun GenerateFunction.generateKotlinCallWithReturn(parent: GenerateTraitOrClass, wasmArgList: String) =
    returnType.generateKotlinCallWithReturn(
        wasmFunctionName(this, parent), 
        wasmArgList)

fun GenerateAttribute.generateKotlinGetterCallWithReturn(parent: GenerateTraitOrClass, wasmArgList: String) =
    type.generateKotlinCallWithReturn(
        wasmGetterName(this, parent),
        wasmArgList)

val Collection<GenerateAttribute>.noVarargs get() = this.filterNot{it.vararg}

fun GenerateFunction.generateKotlin(parent: GenerateTraitOrClass): String {
    if (parent.needsOverride(this)) return "" // TODO: don't mess with overrides for now
    val argList = arguments.noVarargs
            .map {
                "${it._name}: ${it.type.toKotlinType(it.name)}"
            }.joinToString(", ")

    val wrapNullables = arguments.noVarargs
        .filter { it.type.nullable }
        .map{ it.type.wrap(it._name) }
        .joinToString("\n", postfix="\n")

    val copyNonNullables = arguments.noVarargs
        .filterNot { it.type.nullable}
        .map { "    val ${it._name}Arg = ${it._name}" }
        .joinToString("\n", postfix="\n")

    val wasmArgList = (wasmReceiverArgs(parent) + arguments.noVarargs.map{it.wasmArgMapping()} + wasmReturnArg).joinToString(", ")

    val overriden = if (parent.needsOverride(this)) "override " else ""

    // TODO: there can be multiple Rs.
    return "  ${overriden}fun $kotlinTypeParameters $name(" +
    argList + 
    "): ${returnType.toKotlinType()} {\n" +
        wrapNullables +
        copyNonNullables +
        generateKotlinCallWithReturn(parent, wasmArgList) +
    "    return ${returnType.wasmReturnMapping("wasmRetVal")}\n"+
    "  }\n" +
    this.generateTrailingLambdaOverload +
    "\n"
}

val List<GenerateAttribute>.overloadForTrailingLambda: List<GenerateAttribute>? get() {
    val functionTypes = this.filter { it.type is FunctionType }
    if (functionTypes.size != 1) return null
    if (this.indexOf(functionTypes.single()) == this.size - 1) return null
    val nonFunctionTypes = this.filterNot { it.type is FunctionType }

    return nonFunctionTypes + functionTypes
}

val GenerateFunction.generateTrailingLambdaOverload:String get() {
    val overloadOrder = this.arguments.noVarargs.overloadForTrailingLambda
    if (overloadOrder == null) return ""

    val overloadArgs = overloadOrder.map {
        "${it._name}: ${it.type.toKotlinType(it.name)}"
    }.joinToString(", ")
    val originalArgs = this.arguments.noVarargs
            .map {it.name}
            .joinToString(", ")
    return "fun $kotlinTypeParameters $name($overloadArgs) = $name($originalArgs)\n"
}

fun Arg(name: String, type: Type, vararg: Boolean = false) = GenerateAttribute(name, type, null, true, AttributeKind.ARGUMENT, false, vararg, false, false )

fun GenerateAttribute. generateKotlinSetter(parent: GenerateTraitOrClass): String {
    val kotlinType = type.toKotlinType(name)
    return "    set(value: $kotlinType) {\n" +
           (if (type.nullable) "      ${type.wrap("value")}\n" else "      var valueArg = value\n") +
    "      ${wasmSetterName(this, parent)}(" +
        (wasmReceiverArgs(parent) + Arg("value", type).wasmArgMapping()).joinToString(", ") +
        ")\n" + 
    "    }\n\n"
}

fun GenerateAttribute.generateKotlinGetter(parent: GenerateTraitOrClass): String {
    val wasmArgList = (wasmReceiverArgs(parent) + wasmReturnArg).joinToString(", ")
    return "    get() {\n" +
        generateKotlinGetterCallWithReturn(parent, wasmArgList) +
    "      return ${type.wasmReturnMapping("wasmRetVal")}\n"+
    "    }\n\n"
}

val GenerateAttribute._name: String get() = this.name._name

// TODO: Should we have the proper lists here?
val kotlinKeywords = setOf("interface", "is", "as", "object", "open", "internal", "class")
val jsKeywords = setOf("default")

val String._name: String get() {
    val nokeyword = if (this in kotlinKeywords || this in jsKeywords) "_$this" else this
    return nokeyword.replace('-', '_')
}

fun GenerateAttribute.generateKotlin(parent: GenerateTraitOrClass): String {
    assert(kind != ARGUMENT)
    if (type is FunctionType) return "  /* val $name omitted for now. Has function type */\n"

    val kotlinType = type.toKotlinType(name)
    val varOrVal = if (kind == VAL) "val" else "var"
    val overriden = if (parent.needsOverride(this)) "override " else ""

    return "  $overriden$varOrVal $kotlinTypeParameters ${_name}: $kotlinType\n" +
    generateKotlinGetter(parent) +
    if (kind == VAR) generateKotlinSetter(parent) else ""
}

val GenerateTraitOrClass.wasmTypedReceiverArgs get() =
    listOf("_arena: Int", "_index: Int")

fun GenerateAttribute.wasmTypedReceiverArgs(parent: GenerateTraitOrClass) =
    if (static) emptyList() else parent.wasmTypedReceiverArgs

fun GenerateFunction.wasmTypedReceiverArgs(parent: GenerateTraitOrClass) =
    if (static) emptyList() else parent.wasmTypedReceiverArgs


fun GenerateFunction.generateWasmStub(parent: GenerateTraitOrClass): String {
    val wasmName = wasmFunctionName(this, parent)
    val allArgs = (wasmTypedReceiverArgs(parent) + arguments.noVarargs.toList().wasmTypedMapping() + wasmTypedReturnMapping).joinToString(", ")
    return "@SymbolName(\"$wasmName\")\n" +
    "external public fun $wasmName($allArgs): ${returnType.wasmReturnTypeMapping()}\n\n"
}
fun GenerateAttribute.generateWasmSetterStub(parent: GenerateTraitOrClass): String {
    val wasmSetter = wasmSetterName(this, parent)
    val allArgs = (wasmTypedReceiverArgs(parent) + Arg("value", this.type).wasmTypedMapping()).joinToString(", ")
    return "@SymbolName(\"$wasmSetter\")\n" +
    "external public fun $wasmSetter($allArgs): Unit\n\n"
}
fun GenerateAttribute.generateWasmGetterStub(parent: GenerateTraitOrClass): String {
    val wasmGetter = wasmGetterName(this, parent)
    val allArgs = (wasmTypedReceiverArgs(parent) + wasmTypedReturnMapping).joinToString(", ")
    return "@SymbolName(\"$wasmGetter\")\n" +
    "external public fun $wasmGetter($allArgs): Int\n\n"
}
fun GenerateAttribute.generateWasmStubs(parent: GenerateTraitOrClass) =
    generateWasmGetterStub(parent) +
    if (kind == VAR) generateWasmSetterStub(parent) else ""

fun GenerateAttribute.wasmTypedMapping(): String {
    assert(kind == ARGUMENT)
    return this.wasmArgNames().map { "$it: Int" }.joinToString(", ")
}

// TODO: Optimize for simple types.
fun Type.wasmTypedReturnMapping(): String = "resultArena: Int"

val GenerateFunction.wasmTypedReturnMapping get() = returnType.wasmTypedReturnMapping()

val GenerateAttribute.wasmTypedReturnMapping get() = type.wasmTypedReturnMapping()

fun List<GenerateAttribute>.wasmTypedMapping()
    = this.map{it.wasmTypedMapping()}

// TODO: more complex return types, such as returning a pair of Ints
// will require a more complex approach.
fun Type.wasmReturnTypeMapping()
    = if (this == UnitType) "Unit" else "Int"

fun GenerateTraitOrClass.generateMemberWasmStubs() =
    memberAttributes.merge.map {
        it.generateWasmStubs(this)
    }.joinToString("") +
    "\n" +
    memberFunctions.map {
        it.generateWasmStub(this)
    }.joinToString("")

val List<GenerateAttribute>.merge: List<GenerateAttribute> get() {
    val grouped =  this.groupBy { it.name } 
    val reduced = grouped.reduceValues{ a,b -> merge(a, b) } 
    return reduced.values.toList()
}

fun GenerateTraitOrClass.needsOverride(member: GenerateFunction): Boolean {
    this.superTypes.forEach { superTypeName ->
        val superType = nameToInterface[superTypeName]
        if (superType == null) return@forEach
        superType.memberFunctions.forEach {
            if (it.name == member.name) return true
        }
        if (superType.needsOverride(member)) return true
    }
    return false
}

fun GenerateTraitOrClass.needsOverride(member: GenerateAttribute): Boolean {
    this.superTypes.forEach { superTypeName ->
        val superType = nameToInterface[superTypeName]
        if (superType == null) return@forEach
        superType.memberAttributes.forEach {
            if (it.name == member.name) return true
        }
        if (superType.needsOverride(member)) return true
    }
    return false
}

fun GenerateTraitOrClass.generateKotlinMembers() =
    memberAttributes.merge.filterNot { it.static } .map {
        it.generateKotlin(this)
    }.joinToString("") +
    "\n" +
    memberFunctions.filterNot { it.static } .map {
        it.generateKotlin(this)
    }.joinToString("")

fun GenerateTraitOrClass.generateKotlinCompanion() =
    "    companion object {\n" +
        memberAttributes.filter { it.static } .map {
            it.generateKotlin(this)
        }.joinToString("") +
        "\n" +
        memberFunctions.filter { it.static } .map {
            it.generateKotlin(this)
        }.joinToString("") +
    "    }\n" 

fun GenerateTraitOrClass.generateKotlinClassHeader(): String {
    val superTypes = (this.superTypes.filterNot{it.startsWith("ItemArrayLike")} + "JsValue").joinToString(", ")

    return "class ${name}Impl(override val _arena: Int, override val _index: Int): $name\n" +
           "interface $name: $superTypes {\n"
}

fun GenerateTraitOrClass.generateKotlinClassFooter() =
    "}\n\n"

fun GenerateTraitOrClass.generateKotlinClassConverter() =
    "val JsValue.as$name: $name\n" +
    "  get() {\n" +
    "    return ${name}Impl(this._arena, this._index)\n"+
    "  }\n"

fun GenerateTraitOrClass.generateKotlin(): String {
    val result = generateMemberWasmStubs() +
        generateKotlinClassHeader()+
        generateKotlinMembers() +
        generateKotlinCompanion() +
        generateKotlinClassFooter() +
        generateKotlinClassConverter()
    return result
}

fun GenerateUnionTypes.generateKotlin(): String {
    return this.typedefsMarkersMap.keys.map {
        allTypes.add(it)
        "interface $it : JsValue\n" +
              "class ${it}Impl(_arena: Int, _index: Int) : JsValueImpl(_arena, _index), $it\n"

    }.joinToString("\n")
}

lateinit var enumsSet: Set<String>

val Type.isEnum: Boolean get() {
    return !nullable && enumsSet.contains((this as SimpleType).type)
}

val Type.isNullableEnum: Boolean get() {
    return nullable && enumsSet.contains((this as SimpleType).type)
}

val String.treatEmpty get() = if (this == "") "_empty" else this

fun EnumDefinition.generateKotlin(): String {
    allTypes.add(this.name)
    return "enum class ${this.name}(val value: String) {\n" +
        this.entries.map { "  ${it.treatEmpty._name}(\"$it\")" } . joinToString(",\n") +
        "\n}\n"
}

fun List<EnumDefinition>.generateKotlin(): String {
    return this.map { it.generateKotlin() } . joinToString("\n")
}

fun generateKotlin(pkg: String, allPackages: Sequence<String>, interfaces: List<GenerateTraitOrClass>, unions: GenerateUnionTypes, enums: List<EnumDefinition>): String {

    enumsSet = enums.map{it.name}.toSet()

    interfaces.forEach {
        nameToInterface.put(it.name, it)
    }

    return kotlinHeader(pkg, allPackages) +
            unions.generateKotlin() + "\n" +
            enums.generateKotlin() + "\n" +
            interfaces.map {
                allTypes.add(it.name)
                it.generateKotlin()
            }.joinToString("\n") + "\n" +
            declareAbsentTypes(interfaces) + "\n" +
            if (pkg == "org.w3c.dom") {
                "public val document = DocumentImpl(ArenaManager.globalArena, pushDocumentToArena(ArenaManager.globalArena))\n"+
                "public val window = WindowImpl(ArenaManager.globalArena, pushWindowToArena(ArenaManager.globalArena))\n"
            } else ""
}

/////////////////////////////////////////////////////////

fun GenerateAttribute.composeWasmArgs(): String = when (type) {
    is UnitType -> error("An arg can not be UnitType")
    BooleanType -> ""
    IntType -> ""
    FloatType -> ""
    DoubleType ->
        "    var $_name = twoIntsToDouble(${name}Upper, ${name}Lower);\n"
    StringType ->
        "    var $_name = toUTF16String(${name}Ptr, ${name}Len);\n"
    //is AnyType, is DynamicType, is UnionType, is PromiseType -> "TODO(\"implement me: $type\");\n"
    is AnyType, is DynamicType, is UnionType, is PromiseType -> "var $_name = kotlinObject(${name}Arena, ${name}Index);\n"
    is FunctionType ->
        "    var $_name = konan_dependencies.env.Konan_js_wrapLambda(lambdaResultArena, ${name}Index);\n"

    is SimpleType -> if (type.isEnum) "    var $_name = toUTF16String(${name}Ptr, ${name}Len);\n"
                     else "var $_name = kotlinObject(${name}Arena, ${name}Index);\n"
    is ArrayType -> "var $_name = kotlinObject(${name}Arena, ${name}Index);\n"


    else -> error("Unexpected type")
}

val GenerateTraitOrClass.receiver get() =
    "kotlinObject(arena, obj)."

fun GenerateFunction.receiver(parent: GenerateTraitOrClass) =
    if (static) "${parent.name}." else parent.receiver

fun GenerateAttribute.receiver(parent: GenerateTraitOrClass) =
    if (static) "${parent.name}." else parent.receiver

val GenerateTraitOrClass.wasmReceiverArgName get() =
    listOf("arena", "obj")


fun GenerateFunction.wasmReceiverArgName(parent: GenerateTraitOrClass) =
    if (static) emptyList() else parent.wasmReceiverArgName

fun GenerateAttribute.wasmReceiverArgName(parent: GenerateTraitOrClass) =
    if (static) emptyList() else parent.wasmReceiverArgName

val GenerateFunction.wasmReturnArgName get() =
    returnType.wasmReturnArgName

val GenerateAttribute.wasmReturnArgName get() =
    type.wasmReturnArgName

val Type.wasmReturnArgName get() =
    when (this) {
        is UnitType -> emptyList()
        IntType -> emptyList()
        BooleanType -> emptyList()
        FloatType -> emptyList()
        DoubleType -> emptyList()
        StringType -> emptyList()
        is AnyType, is DynamicType, is UnionType, is PromiseType -> listOf("resultArena")
        is SimpleType -> listOf("resultArena")
        is ArrayType -> listOf("resultArena")
        is FunctionType -> listOf("resultArena")
        else -> error("Unexpected type: $this")
    }

val Type.wasmReturnExpression get() =
    when(this) {
        is UnitType -> ""
        BooleanType -> "result"
        IntType -> "result"
        FloatType -> "result" // TODO: can we really pass floats as is?
        DoubleType -> "doubleToReturnSlot(result)"
        StringType -> "stringToReturnVar(result)"
        is AnyType, is DynamicType, is UnionType, is PromiseType -> "toArena(resultArena, result)"
        is SimpleType -> if (this.isEnum) "stringToReturnVar(result)"
                         else "toArena(resultArena, result)"
        is ArrayType -> "toArena(resultArena, result)" // is it right?
        is FunctionType -> "toArena(resultArena, result)" // is it right?
        else -> error("Unexpected type: $this")
    }

val Type.unwrapNullable get() = 
    "function unwrapNullable(arena, index) {\n" +
    "    result = kotlinObject[arena][index]\n" +
    "    return ${this.wasmReturnExpression};\n" +
    "}"

fun GenerateFunction.generateJs(parent: GenerateTraitOrClass): String {
    val allArgs = wasmReceiverArgName(parent) + arguments.noVarargs.map { it.wasmArgNames() }.flatten() + wasmReturnArgName
    val wasmMapping = allArgs.joinToString(", ")
    val argList = arguments.noVarargs.map { it._name }. joinToString(", ")
    val composedArgsList = arguments.noVarargs.map { it.composeWasmArgs() }. joinToString("")

    return "\n  ${wasmFunctionName(this, parent)}: function($wasmMapping) {\n" +
        composedArgsList +
        "    var result = ${receiver(parent)}$name($argList);\n" +
        "    return ${returnType.wasmReturnExpression};\n" +
    "  }"
}

fun GenerateAttribute.generateJsSetter(parent: GenerateTraitOrClass): String {
    val valueArg = Arg("value", type)
    val allArgs = wasmReceiverArgName(parent) + valueArg.wasmArgNames()
    val wasmMapping = allArgs.joinToString(", ")
    return "\n  ${wasmSetterName(this, parent)}: function($wasmMapping) {\n" +
        valueArg.composeWasmArgs() +
        "    ${receiver(parent)}$name = value;\n" +
    "  }"
}

fun GenerateAttribute.generateJsGetter(parent: GenerateTraitOrClass): String {
    val allArgs = wasmReceiverArgName(parent) + wasmReturnArgName
    val wasmMapping = allArgs.joinToString(", ")
    return "\n  ${wasmGetterName(this, parent)}: function($wasmMapping) {\n" +
        "    var result = ${receiver(parent)}$name;\n" +
        "    return ${type.wasmReturnExpression};\n" +
    "  }"
}

fun GenerateAttribute.generateJs(parent: GenerateTraitOrClass) =
    generateJsGetter(parent) + 
    if (kind == VAR) ",\n${generateJsSetter(parent)}" else ""

fun generateJs(interfaces: List<GenerateTraitOrClass>): String =
    "konan.libraries.push ({\n" +
    interfaces.map { interf ->
        interf.memberAttributes.map { member ->
            member.generateJs(interf) 
        } +
        interf.memberFunctions.map { member ->
            member.generateJs(interf)
        }

    }.flatten() .joinToString(",\n") +
    "\n})\n"


fun processIdlLib(args: Array<String>): Array<String> {
    val arguments = parseCommandLine(args, JsInteropArguments())

    val userDir = System.getProperty("user.dir")
    val kotlinDir = File(arguments.generated ?: userDir)
    kotlinDir.mkdirs()

    val jsDir = File(arguments.natives ?: userDir)
    jsDir.mkdirs()

    val idlDir = arguments.idl?.let { File(it) } ?: error("Please sepcify -idl flag")
    val cacheFile = arguments.cache?.let { File(it) } ?: File(idlDir, "cache.txt")

    val webIdl = BuildWebIdl(cacheFile, idlDir)
    println("Running stub generator")
    webIdl.wasmStubGenerator(kotlinDir, jsDir)

    File(arguments.manifest!!).writeText("") // The manifest is currently unused for wasm.
    return argsToCompiler(arguments.staticLibrary, arguments.libraryPath)
}

var allTypes = mutableSetOf<String>()

fun declareAbsentTypes(interfaces: List<GenerateTraitOrClass>): String {
    val unknownTypes = mutableSetOf<String>()

    fun List<SimpleType>.collectUnknownTypes() = this
           .filterNot { it.isPrimitive }
           .filterNot { it.type in allTypes }
           .forEach { unknownTypes.add(it.type) }

    fun List<GenerateAttribute>.collectUnknownTypes() {
        return this.map { it.type }
            .filterIsInstance<SimpleType>()
            .collectUnknownTypes()
    }

    // TODO: make a full visitor pass here?
    interfaces.forEach { iface -> 
        iface.memberAttributes.collectUnknownTypes()

        iface.memberFunctions.forEach {
            it.arguments.collectUnknownTypes()
        }

        iface.memberFunctions
            .map { it.returnType }
            .filterIsInstance<SimpleType>()
            .collectUnknownTypes()

        iface.superTypes
            .filterNot { it.startsWith("ItemArrayLike") }
            .filterNot { it in allTypes }
            .forEach { unknownTypes.add(it) }

    }

    return unknownTypes.map {
        "interface $it : JsValue\n" +
        "class ${it}Impl(_arena: Int, _index: Int) : JsValueImpl(_arena, _index), $it\n" +
        "val JsValue.as$it: $it get() = ${it}Impl(this._arena, this._index)"

    }.joinToString("\n")
}

val nameToInterface = mutableMapOf<String, GenerateTraitOrClass>()

fun BuildWebIdl.wasmStubGenerator(kotlinDir: File, jsDir: File) {
    allPackages.forEach { pkg ->
        println("Generating stubs for package '$pkg'...")

        File(kotlinDir, "$pkg.kt")
            .writeText(generateKotlin(pkg, allPackages, definitions, unions, repository.enums.values.toList()))
        File(jsDir, "$pkg.js")
            .writeText(generateJs(definitions))
    }
}

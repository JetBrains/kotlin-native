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

// This tool generates all arithmetic operations needed as extensions.
enum class Kind(val signed: Boolean = true,
                val numeric: Boolean = true,
                val integral: Boolean = true,
                val floating: Boolean = false) {
    UNSIGNED(signed = false),
    SIGNED,
    FLOATING(floating = true, integral = false),
    // Yes, CHAR is numeric, but not integral.
    CHAR(integral = false),
    BOOLEAN(numeric = false)
}

enum class Type(val typeName: String, val kind: Kind, val cTypeName: String) {
    BYTE("Byte", Kind.SIGNED, "KByte"),
    SHORT("Short", Kind.SIGNED, "KShort"),
    INT("Int", Kind.SIGNED,  "KInt"),
    LONG("Long", Kind.SIGNED, "KLong"),
    CHAR("Char", Kind.CHAR,  "KChar"),
    BOOLEAN("Boolean", Kind.BOOLEAN,  "KBoolean"),
    FLOAT("Float", Kind.FLOATING,  "KFloat"),
    DOUBLE("Double", Kind.FLOATING, "KDouble"),
    UBYTE("UByte", Kind.UNSIGNED, "KUByte"),
    USHORT("UShort", Kind.UNSIGNED, "KUShort"),
    UINT("UInt", Kind.UNSIGNED, "KUInt"),
    ULONG("ULong", Kind.UNSIGNED, "KULong");

    public override fun toString(): String = typeName
}

val signedTypes = listOf(Type.BYTE, Type.SHORT, Type.INT, Type.LONG)
val unsignedTypes = listOf(Type.UBYTE, Type.USHORT, Type.UINT, Type.ULONG)
val floatTypes = listOf(Type.FLOAT, Type.DOUBLE)
val allNumericTypes = signedTypes + floatTypes + Type.CHAR
val typePromotion = mapOf(
        Type.BYTE to Type.INT, Type.UBYTE to Type.UINT,
        Type.SHORT to Type.INT, Type.USHORT to Type.UINT,
        Type.INT to Type.INT, Type.UINT to Type.UINT,
        Type.LONG to Type.LONG, Type.ULONG to Type.ULONG,
        Type.FLOAT to Type.FLOAT,
        Type.DOUBLE to Type.DOUBLE
)
val typeRank = mapOf(
        Type.INT to 1,
        Type.UINT to 2,
        Type.LONG to 3,
        Type.ULONG to 4,
        Type.FLOAT to 5,
        Type.DOUBLE to 6
)
val intTypes = signedTypes + unsignedTypes
//val intAndFloatTypes = intTypes + floatTypes
val intAndFloatTypes = signedTypes + floatTypes
val incrementOps = listOf("inc", "dec")
val unaryArithOps = listOf("unaryPlus", "unaryMinus")
val unaryBitOps = listOf("inv")
val binaryArithOps = listOf("plus", "minus", "times", "div", "rem")
val binaryBitOps = listOf("and", "or", "xor")
val binaryShiftOps = listOf("shl", "shr", "ushr")

fun generateRange(type: Type, otherType: Type) {
    val valueType = maxType(maxType(type, otherType), Type.INT)
    println("public operator fun rangeTo(other: $otherType) = ${valueType}Range(this.to$valueType(), other.to$valueType())")
}

fun generateOverrideHeader(function: String, args: List<Type>, retType: Type,
                           kind: String = "") {
    var index = 0
    var arguments = args.joinToString { arg -> "arg${index++}: $arg" }
    println("external public override$kind fun $function($arguments): $retType")
}

fun modf(type1: Type, type2: Type): String {
    if (type1 != Type.DOUBLE && type2 != Type.DOUBLE)
        return "fmodf"
    else
        return "fmod"
}

fun promotedToInt32(type: Type): Boolean {
    val promoted = typePromotion[type]
    return promoted == Type.INT || promoted == Type.UINT
}

fun promotedToInt64(type: Type): Boolean {
    val promoted = typePromotion[type]
    return promoted == Type.LONG || promoted == Type.ULONG
}

fun generateCImpl(symbol: String, function: String, type: Type, args: List<Type>, retType: Type) {
    var index = 0
    var arguments = (listOf(type) + args).joinToString { arg -> "${arg.cTypeName} arg${index++}" }
    val body = when {
        function == "plus" -> "return arg0 + arg1;"
        function == "minus" -> "return arg0 - arg1;"
        function == "times" -> "return arg0 * arg1;"
        function == "div" && maxType(type, args[0]).kind.floating  -> "return arg0 / arg1;"
        function == "div" -> "return div<${retType.cTypeName}>(arg0, arg1);"
        function == "rem" && maxType(type, args[0]).kind.floating  -> "return ${modf(type, args[0])}(arg0, arg1);"
        function == "rem" -> "return arg0 % arg1;"
        function == "compareTo" -> "if (arg0 == arg1) return 0; return (arg0 < arg1) ? -1 : 1;"
        function == "and" -> "return arg0 & arg1;"
        function == "or" -> "return arg0 | arg1;"
        function == "xor" -> "return arg0 ^ arg1;"
        function == "inv" -> "return ~arg0;"
        function == "inc" -> "return ++arg0;"
        function == "dec" -> "return --arg0;"
        function == "unaryPlus" -> "return +arg0;"
        function == "unaryMinus" -> "return -arg0;"
        function == "shl" && promotedToInt32(type) -> "return arg0 << (arg1 & 31);"
        function == "shl" && promotedToInt64(type) -> "return arg0 << (arg1 & 63);"
        function == "shr" && promotedToInt32(type) -> "return arg0 >> (arg1 & 31);"
        function == "shr" && promotedToInt64(type) -> "return arg0 >> (arg1 & 63);"
        function == "ushr" && typePromotion[type] == Type.INT -> "return static_cast<uint32_t>(arg0) >> (arg1 & 31);"
        function == "ushr" && typePromotion[type] == Type.LONG -> "return static_cast<uint64_t>(arg0) >> (arg1 & 63);"
        function in setOf("toByte", "toShort", "toInt", "toLong", "toFloat", "toDouble", "toChar") -> "return arg0;"
        else -> throw Error("Function $function is not implemented yet")
    }
    println("${retType.cTypeName} $symbol($arguments) { $body }")
}

fun makeSymbolName(type: Type, function: String, args: List<Type>) =
        "Kotlin_${type}_${(listOf(function) + args.map { arg -> arg.toString() }).joinToString("_")}"

fun generateExternal(type: Type, function: String, args: List<Type>, retType: Type, kind: String, kt: Boolean) {
    val symbol = makeSymbolName(type, function, args)

    if (!kt) {
        generateCImpl(symbol, function, type, args, retType)
        return
    }
    println("@SymbolName(\"$symbol\")")
    var index = 0
    var arguments = args.joinToString { arg -> "arg${index++}: $arg" }
    println("external public$kind fun $type.$function($arguments): $retType")
}

fun maxType(type1: Type, type2: Type): Type{
    val promoted1 = typePromotion[type1]!!
    val promoted2 = typePromotion[type2]!!
    val rank1 = typeRank[promoted1]!!
    val rank2 = typeRank[promoted2]!!
    if (rank1 > rank2)
        return promoted1
    else
        return promoted2
}


fun generateOverrides(type: Type) {
    println("// Generated overrides.")
    for (other in allNumericTypes) {
        val op = "to$other"
        println("@SymbolName(\"${makeSymbolName(type, op, listOf())}\")")
        generateOverrideHeader(op, listOf(), other)
    }

    for (other in listOf(type)) {
        val op = "compareTo"
        println("@SymbolName(\"${makeSymbolName(type, op, listOf(other))}\")")
        generateOverrideHeader(op, listOf(other), Type.INT, " operator")
    }

    if (type.kind == Kind.SIGNED) {
        for (otherType in signedTypes) {
            generateRange(type, otherType)
        }
    }
}

fun generateArith(kind: Kind, kt: Boolean) {
    val types = when (kind) {
        Kind.UNSIGNED -> unsignedTypes
        Kind.SIGNED -> signedTypes
        Kind.FLOATING -> floatTypes
        Kind.CHAR -> listOf(Type.CHAR)
        Kind.BOOLEAN -> listOf(Type.BOOLEAN)
    }
    val isIntegral = (kind == Kind.UNSIGNED || kind == Kind.SIGNED)
    for (type in types) {
        for (op in binaryArithOps) {
            for (otherType in intAndFloatTypes) {
                generateExternal(type, op, listOf(otherType), maxType(type, otherType), " operator", kt = kt)
            }
        }

        for (otherType in intAndFloatTypes) {
            if (type != otherType || kt == false)
                generateExternal(type, "compareTo", listOf(otherType), Type.INT, " operator", kt = kt)
        }

        if (isIntegral) {
            for (op in binaryBitOps) {
                generateExternal(type, op, listOf(type), maxType(type, type), " infix", kt = kt)
            }
            for (op in binaryShiftOps) {
                if (op == "ushr" && !type.kind.signed) continue
                generateExternal(type, op, listOf(Type.INT), maxType(type, Type.INT), " infix", kt = kt)
            }
            for (op in unaryBitOps) {
                generateExternal(type, op, listOf(), type, "", kt = kt)
            }
        }
        if (kind != Kind.BOOLEAN) {
            for (op in incrementOps) {
                generateExternal(type, op, listOf(), type, " operator", kt = kt)
            }
        }
        if (kind != Kind.BOOLEAN && kind != Kind.CHAR) {
            for (op in unaryArithOps) {
                generateExternal(type, op, listOf(), typePromotion[type]!!, " operator", kt = kt)
            }
        }

        // Also generate toNumber() definitions for C.
        if (!kt) {
            for (other in allNumericTypes) {
                generateExternal(type, "to$other", listOf(), other, " operator", kt = false)
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("operators action1 action2 .. actionN")
    }

    for (action in args) {
        when (action) {
            "overrides_Byte" -> generateOverrides(Type.BYTE)
            "overrides_Short" -> generateOverrides(Type.SHORT)
            "overrides_Int" -> generateOverrides(Type.INT)
            "overrides_Long" -> generateOverrides(Type.LONG)
            "overrides_Float" -> generateOverrides(Type.FLOAT)
            "overrides_Double" -> generateOverrides(Type.DOUBLE)
            "overrides_UByte" -> generateOverrides(Type.UBYTE)
            "overrides_UShort" -> generateOverrides(Type.USHORT)
            "overrides_UInt" -> generateOverrides(Type.UINT)
            "overrides_ULong" -> generateOverrides(Type.ULONG)
            "signed_kt" -> generateArith(Kind.SIGNED, kt = true)
            "unsigned_kt" -> generateArith(Kind.UNSIGNED, kt = true)
            "floating_kt" -> generateArith(Kind.FLOATING, kt = true)
            "char_kt" -> generateArith(Kind.CHAR, kt = true)
            "boolean_kt" -> generateArith(Kind.BOOLEAN, kt = true)
            "signed_c" -> generateArith(Kind.SIGNED, kt = false)
            "unsigned_c" -> generateArith(Kind.UNSIGNED, kt = false)
            "floating_c" -> generateArith(Kind.FLOATING, kt = false)
            "char_c" -> generateArith(Kind.CHAR, kt = false)
            "boolean_c" -> generateArith(Kind.BOOLEAN, kt = false)
            else -> throw Error("Unknown action: $action")
        }
    }
}



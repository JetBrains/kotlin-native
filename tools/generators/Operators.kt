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

val signedTypes = listOf("Byte", "Short", "Int", "Long")
val unsignedTypes = listOf("UByte", "UShort", "UInt", "ULong")
val floatTypes = listOf("Float", "Double")
val typePromotion = mapOf(
        "Byte" to "Int", "UByte" to "UInt",
        "Short" to "Int", "UShort" to "UInt",
        "Int" to "Int", "UInt" to "UInt",
        "Long" to "Long", "ULong" to "ULong",
        "Float" to "Float",
        "Double" to "Double"
)
val typeRank = mapOf(
        "Int" to 1,
        "UInt" to 2,
        "Long" to 3,
        "ULong" to 4,
        "Float" to 5,
        "Double" to 6
)
val intTypes = signedTypes + unsignedTypes
val intAndFloatTypes = intTypes + floatTypes
val incrementOps = listOf("inc", "dec")
val unaryArithOps = listOf("unaryPlus", "unaryMinus")
val unaryBitOps = listOf("inv")
val binaryArithOps = listOf("plus", "minus", "times", "div", "rem")
val binaryBitOps = listOf("and", "or", "xor")
val binaryShiftOps = listOf("shl", "shr", "ushr")


fun generateExternal(type: String, function: String,
                     args: List<String>, retType: String = type,
                     kind: String = "operator") {
    val symbol = "Kotlin_${type}_${function}_${args.joinToString("_")}"
    println("@SymbolName(\"$symbol\")")
    var arguments = args.joinToString { arg -> "_: $arg" }
    println("external public $kind fun $type.$function($arguments): $retType")
}

fun maxType(type1: String, type2: String): String {
    val promoted1 = typePromotion[type1]!!
    val promoted2 = typePromotion[type2]!!
    val rank1 = typeRank[promoted1]!!
    val rank2 = typeRank[promoted2]!!
    if (rank1 > rank2)
        return promoted1
    else
        return promoted2
}

enum class Kind {
    UNSIGNED,
    SIGNED,
    FLOAT,
    CHAR,
    BOOLEAN
}

fun generateArithKt(kind: Kind) {
    val types = when (kind) {
        Kind.UNSIGNED -> unsignedTypes
        Kind.SIGNED -> signedTypes
        Kind.FLOAT -> floatTypes
        Kind.CHAR -> listOf("Char")
        Kind.BOOLEAN -> listOf("Boolean")
    }
    for (type in types) {
        for (op in binaryArithOps) {
            for (otherType in types) {
                generateExternal(type, op, listOf(otherType), maxType(type, otherType))
            }
        }
        if (kind == Kind.UNSIGNED || kind == Kind.SIGNED) {
            for (op in binaryBitOps) {
                generateExternal(type, op, listOf(type), maxType(type, type))
            }
            for (op in binaryShiftOps) {
                generateExternal(type, op, listOf("Int"), maxType(type, "Int"))
            }
        }
        if (kind != Kind.BOOLEAN) {
            for (op in incrementOps) {
                generateExternal(type, op, listOf(), type)
            }
        }
        if (kind != Kind.BOOLEAN && kind != Kind.CHAR) {
            for (op in unaryArithOps) {
                generateExternal(type, op, listOf(), typePromotion[type]!!)
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
            "signed_kt" -> generateArithKt(Kind.SIGNED)
            "unsigned_kt" -> generateArithKt(Kind.UNSIGNED)
            "float_kt" -> generateArithKt(Kind.FLOAT)
            "char_kt" -> generateArithKt(Kind.CHAR)
            "boolean_kt" -> generateArithKt(Kind.BOOLEAN)
        }
    }
}



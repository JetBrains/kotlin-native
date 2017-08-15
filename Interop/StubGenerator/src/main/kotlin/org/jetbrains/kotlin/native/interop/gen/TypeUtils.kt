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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

val EnumDef.isAnonymous: Boolean
    get() = spelling.contains("(anonymous ") // TODO: it is a hack

/**
 * Returns the expression which could be used for this type in C code.
 * Note: the resulting string doesn't exactly represent this type, but it is enough for current purposes.
 *
 * TODO: use libclang to implement?
 */
fun Type.getStringRepresentation(): String = when (this) {
    is VoidType -> "void"
    is CharType -> "char"
    is BoolType -> "BOOL"
    is IntegerType -> this.spelling
    is FloatingType -> this.spelling

    is PointerType, is ArrayType -> "void*"

    is RecordType -> this.decl.spelling

    is EnumType -> if (this.def.isAnonymous) {
        this.def.baseType.getStringRepresentation()
    } else {
        this.def.spelling
    }

    is Typedef -> this.def.aliased.getStringRepresentation()

    is ObjCPointer -> when (this) {
        is ObjCIdType -> "id$protocolQualifier"
        is ObjCClassPointer -> "Class$protocolQualifier"
        is ObjCObjectPointer -> "${def.name}$protocolQualifier*"
        is ObjCInstanceType -> TODO(this.toString()) // Must have already been handled.
    }

    else -> throw kotlin.NotImplementedError()
}

private val ObjCQualifiedPointer.protocolQualifier: String
    get() = if (this.protocols.isEmpty()) "" else " <${protocols.joinToString { it.name }}>"

tailrec fun Type.unwrapTypedefs(): Type = if (this is Typedef) {
    this.def.aliased.unwrapTypedefs()
} else {
    this
}
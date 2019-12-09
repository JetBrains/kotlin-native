package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

fun tryRenderStructOrUnion(def: StructDef, nestingLevel: Int = 0): String? = when (def.kind) {
    StructDef.Kind.STRUCT -> tryRenderStruct(def, nestingLevel)
    StructDef.Kind.UNION -> tryRenderUnion(def, nestingLevel)
}

private fun tryRenderStruct(def: StructDef, nestingLevel: Int = 0): String? {
    val isPackedStruct = def.fields.any { !it.isAligned }

    // The only case when offset starts from non-zero is a inner anonymous struct or union
    var offset = def.members.filterIsInstance<Field>().firstOrNull()?.offsetBytes ?: 0L

    return buildString {
        append("struct")
        if (isPackedStruct) append(" __attribute__((packed))")
        append(" { ")

        def.members.forEachIndexed { i, it ->
            val index = i + (100 * nestingLevel)
            val name = "p$index"
            val decl = when (it) {
                is Field -> {
                    val defaultAlignment = if (isPackedStruct) 1L else it.typeAlign
                    val alignment = guessAlignment(offset, it.offsetBytes, defaultAlignment) ?: return null

                    offset = it.offsetBytes + it.typeSize

                    tryRenderVar(it.type, name)
                            ?.plus(if (alignment == defaultAlignment) "" else "__attribute__((aligned($alignment)))")
                }

                is BitField, // TODO: tryRenderVar(it.type, name)?.plus(" : ${it.size}")
                is IncompleteField -> null // e.g. flexible array member.
                is AnonymousInnerRecord -> {
                    offset = it.offset / 8 + it.typeSize
                    tryRenderVar(it.type, "", nestingLevel + 1)
                }

            } ?: return null
            append("$decl; ")
        }

        append("}")
    }
}

private fun guessAlignment(offset: Long, paddedOffset: Long, defaultAlignment: Long): Long? =
        longArrayOf(defaultAlignment, 1L, 2L, 4L, 8L, 16L, 32L).firstOrNull {
            alignUp(offset, it) == paddedOffset
        }

private fun alignUp(x: Long, alignment: Long): Long = (x + alignment - 1) and ((alignment - 1).inv())

private fun tryRenderUnion(def: StructDef, nestingLevel: Int = 0): String? =
        buildString {
            append("union { ")
            def.members.forEachIndexed { i, it ->
                val index = i + (100 * nestingLevel)
                val decl = when (it) {
                    is Field -> tryRenderVar(it.type, "p$index")
                    is BitField, is IncompleteField -> null
                    is AnonymousInnerRecord -> tryRenderVar(it.type, "", nestingLevel + 1)
                } ?: return null

                append("$decl; ")
            }
            append("}")
        }

private fun tryRenderVar(type: Type, name: String, nestingLevel: Int = 0): String? = when (type) {
    CharType, is BoolType -> "char $name"
    is IntegerType -> "${type.spelling} $name"
    is FloatingType -> "${type.spelling} $name"
    is VectorType -> "${type.spelling} $name"
    is RecordType -> "${tryRenderStructOrUnion(type.decl.def!!, nestingLevel)} $name"
    is EnumType -> tryRenderVar(type.def.baseType, name)
    is PointerType -> "void* $name"
    is ConstArrayType -> tryRenderVar(type.elemType, "$name[${type.length}]")
    is IncompleteArrayType -> tryRenderVar(type.elemType, "$name[]")
    is Typedef -> tryRenderVar(type.def.aliased, name)
    is ObjCPointer -> "void* $name"
    else -> null
}

private val Field.offsetBytes: Long get() {
    require(this.offset % 8 == 0L)
    return this.offset / 8
}

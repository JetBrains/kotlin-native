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

interface DeclarationMapper {
    fun getKotlinClassForPointed(structDecl: StructDecl): Classifier
    fun isMappedToStrict(enumDef: EnumDef): Boolean
    fun getKotlinNameForValue(enumDef: EnumDef): String
    fun getPackageFor(declaration: TypeDeclaration): String

    val useUnsignedTypes: Boolean
}

fun DeclarationMapper.isMappedToSigned(integerType: IntegerType): Boolean = integerType.isSigned || !useUnsignedTypes

fun DeclarationMapper.getKotlinClassFor(
        objCClassOrProtocol: ObjCClassOrProtocol,
        isMeta: Boolean = false
): Classifier {
    val pkg = if (objCClassOrProtocol.isForwardDeclaration) {
        when (objCClassOrProtocol) {
            is ObjCClass -> "objcnames.classes"
            is ObjCProtocol -> "objcnames.protocols"
        }
    } else {
        this.getPackageFor(objCClassOrProtocol)
    }
    val className = objCClassOrProtocol.kotlinClassName(isMeta)
    return Classifier.topLevel(pkg, className)
}

fun PrimitiveType.getKotlinType(declarationMapper: DeclarationMapper): KotlinClassifierType = when (this) {
    is CharType -> KotlinTypes.byte

    is BoolType -> KotlinTypes.boolean

// TODO: C primitive types should probably be generated as type aliases for Kotlin types.
    is IntegerType -> if (declarationMapper.isMappedToSigned(this)) {
        when (this.size) {
            1 -> KotlinTypes.byte
            2 -> KotlinTypes.short
            4 -> KotlinTypes.int
            8 -> KotlinTypes.long
            else -> TODO(this.toString())
        }
    } else {
        when (this.size) {
            1 -> KotlinTypes.uByte
            2 -> KotlinTypes.uShort
            4 -> KotlinTypes.uInt
            8 -> KotlinTypes.uLong
            else -> TODO(this.toString())
        }
    }

    is FloatingType -> when (this.size) {
        4 -> KotlinTypes.float
        8 -> KotlinTypes.double
        else -> TODO(this.toString())
    }

    else -> throw NotImplementedError()
}

private fun PrimitiveType.getBridgedType(declarationMapper: DeclarationMapper): BridgedType {
    val kotlinType = this.getKotlinType(declarationMapper)
    return BridgedType.values().single {
        it.kotlinType == kotlinType
    }
}

internal val ObjCPointer.isNullable: Boolean
    get() = this.nullability != ObjCPointer.Nullability.NonNull

/**
 * Describes the Kotlin types used to represent some C type.
 */
sealed class TypeMirror(val pointedType: KotlinClassifierType, val info: TypeInfo) {
    /**
     * Type to be used in bindings for argument or return value.
     */
    abstract val argType: KotlinType

    /**
     * Mirror for C type to be represented in Kotlin as by-value type.
     */
    class ByValue(
            pointedType: KotlinClassifierType,
            info: TypeInfo,
            val valueType: KotlinType,
            val nullable: Boolean = (info is TypeInfo.Pointer)
    ) : TypeMirror(pointedType, info) {

        override val argType: KotlinType
            get() = valueType.makeNullableAsSpecified(nullable)
    }

    /**
     * Mirror for C type to be represented in Kotlin as by-ref type.
     */
    class ByRef(pointedType: KotlinClassifierType, info: TypeInfo) : TypeMirror(pointedType, info) {
        override val argType: KotlinType get() = KotlinTypes.cValue.typeWith(pointedType)
    }
}

/**
 * Describes various type conversions for [TypeMirror].
 */
sealed class TypeInfo {
    abstract val bridgedType: BridgedType

    /**
     * If this info is for [TypeMirror.ByValue], then this method describes how to
     * construct pointed-type from value type.
     */
    abstract fun constructPointedType(valueType: KotlinType): KotlinClassifierType

    class Primitive(override val bridgedType: BridgedType, val varClass: Classifier) : TypeInfo() {

        override fun constructPointedType(valueType: KotlinType) = varClass.typeWith(valueType)
    }

    class Boolean : TypeInfo() {

        override val bridgedType: BridgedType get() = BridgedType.BYTE

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.booleanVarOf.typeWith(valueType)
    }

    class Enum(val clazz: Classifier, override val bridgedType: BridgedType) : TypeInfo() {
        override fun constructPointedType(valueType: KotlinType) =
                clazz.nested("Var").type // TODO: improve

    }

    class Pointer(val pointee: KotlinType, val cPointee: Type) : TypeInfo() {

        override val bridgedType: BridgedType
            get() = BridgedType.NATIVE_PTR

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.cPointerVarOf.typeWith(valueType)
    }

    class ObjCPointerInfo(val kotlinType: KotlinType, val type: ObjCPointer) : TypeInfo() {

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.objCObjectVar.typeWith(valueType)
    }

    class ObjCBlockPointerInfo(val kotlinType: KotlinFunctionType, val type: ObjCBlockPointer) : TypeInfo() {

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        override fun constructPointedType(valueType: KotlinType): KotlinClassifierType {
            return Classifier.topLevel("kotlinx.cinterop", "ObjCBlockVar").typeWith(valueType)
        }
    }

    class ByRef(val pointed: KotlinType) : TypeInfo() {

        override val bridgedType: BridgedType get() = error(pointed)

        // TODO: this method must not exist.
        override fun constructPointedType(valueType: KotlinType): KotlinClassifierType = error(pointed)
    }
}

private fun mirrorPrimitiveType(type: PrimitiveType, declarationMapper: DeclarationMapper): TypeMirror.ByValue {
    val varClassName = when (type) {
        is CharType -> "ByteVar"
        is BoolType -> "BooleanVar"
        is IntegerType -> if (declarationMapper.isMappedToSigned(type)) {
            when (type.size) {
                1 -> "ByteVar"
                2 -> "ShortVar"
                4 -> "IntVar"
                8 -> "LongVar"
                else -> TODO(type.toString())
            }
        } else {
            when (type.size) {
                1 -> "UByteVar"
                2 -> "UShortVar"
                4 -> "UIntVar"
                8 -> "ULongVar"
                else -> TODO(type.toString())
            }
        }
        is FloatingType -> when (type.size) {
            4 -> "FloatVar"
            8 -> "DoubleVar"
            else -> TODO(type.toString())
        }
        else -> TODO(type.toString())
    }

    val varClass = Classifier.topLevel("kotlinx.cinterop", varClassName)
    val varClassOf = Classifier.topLevel("kotlinx.cinterop", "${varClassName}Of")

    val info = if (type is BoolType) {
        TypeInfo.Boolean()
    } else {
        TypeInfo.Primitive(type.getBridgedType(declarationMapper), varClassOf)
    }
    return TypeMirror.ByValue(varClass.type, info, type.getKotlinType(declarationMapper))
}

private fun byRefTypeMirror(pointedType: KotlinClassifierType) : TypeMirror.ByRef {
    val info = TypeInfo.ByRef(pointedType)
    return TypeMirror.ByRef(pointedType, info)
}

fun mirror(declarationMapper: DeclarationMapper, type: Type): TypeMirror = when (type) {
    is PrimitiveType -> mirrorPrimitiveType(type, declarationMapper)

    is RecordType -> byRefTypeMirror(declarationMapper.getKotlinClassForPointed(type.decl).type)

    is EnumType -> {
        val pkg = declarationMapper.getPackageFor(type.def)
        val kotlinName = declarationMapper.getKotlinNameForValue(type.def)

        when {
            declarationMapper.isMappedToStrict(type.def) -> {
                val bridgedType = (type.def.baseType.unwrapTypedefs() as PrimitiveType).getBridgedType(declarationMapper)
                val clazz = Classifier.topLevel(pkg, kotlinName)
                val info = TypeInfo.Enum(clazz, bridgedType)
                TypeMirror.ByValue(clazz.nested("Var").type, info, clazz.type)
            }
            !type.def.isAnonymous -> {
                val baseTypeMirror = mirror(declarationMapper, type.def.baseType)
                TypeMirror.ByValue(
                        Classifier.topLevel(pkg, kotlinName + "Var").type,
                        baseTypeMirror.info,
                        Classifier.topLevel(pkg, kotlinName).type
                )
            }
            else -> mirror(declarationMapper, type.def.baseType)
        }
    }

    is PointerType -> {
        val pointeeType = type.pointeeType
        val unwrappedPointeeType = pointeeType.unwrapTypedefs()
        if (unwrappedPointeeType is VoidType) {
            val info = TypeInfo.Pointer(KotlinTypes.cOpaque, pointeeType)
            TypeMirror.ByValue(KotlinTypes.cOpaquePointerVar, info, KotlinTypes.cOpaquePointer)
        } else if (unwrappedPointeeType is ArrayType) {
            mirror(declarationMapper, pointeeType)
        } else {
            val pointeeMirror = mirror(declarationMapper, pointeeType)
            val info = TypeInfo.Pointer(pointeeMirror.pointedType, pointeeType)
            TypeMirror.ByValue(
                    KotlinTypes.cPointerVar.typeWith(pointeeMirror.pointedType),
                    info,
                    KotlinTypes.cPointer.typeWith(pointeeMirror.pointedType)
            )
        }
    }

    is ArrayType -> {
        // TODO: array type doesn't exactly correspond neither to pointer nor to value.
        val elemTypeMirror = mirror(declarationMapper, type.elemType)
        if (type.elemType.unwrapTypedefs() is ArrayType) {
            elemTypeMirror
        } else {
            val info = TypeInfo.Pointer(elemTypeMirror.pointedType, type.elemType)
            TypeMirror.ByValue(
                    KotlinTypes.cArrayPointerVar.typeWith(elemTypeMirror.pointedType),
                    info,
                    KotlinTypes.cArrayPointer.typeWith(elemTypeMirror.pointedType)
            )
        }
    }

    is FunctionType -> byRefTypeMirror(KotlinTypes.cFunction.typeWith(getKotlinFunctionType(declarationMapper, type)))

    is Typedef -> {
        val baseType = mirror(declarationMapper, type.def.aliased)
        val pkg = declarationMapper.getPackageFor(type.def)

        val name = type.def.name
        when (baseType) {
            is TypeMirror.ByValue -> TypeMirror.ByValue(
                    Classifier.topLevel(pkg, "${name}Var").type,
                    baseType.info,
                    Classifier.topLevel(pkg, name).type,
                    nullable = baseType.nullable
            )

            is TypeMirror.ByRef -> TypeMirror.ByRef(Classifier.topLevel(pkg, name).type, baseType.info)
        }

    }

    is ObjCPointer -> objCPointerMirror(declarationMapper, type)

    else -> TODO(type.toString())
}

internal tailrec fun ObjCClass.isNSStringOrSubclass(): Boolean = when (this.name) {
    "NSMutableString", // fast path and handling for forward declarations.
    "NSString" -> true
    else -> {
        val baseClass = this.baseClass
        if (baseClass != null) {
            baseClass.isNSStringOrSubclass()
        } else {
            false
        }
    }
}

internal fun ObjCClass.isNSStringSubclass(): Boolean = this.baseClass?.isNSStringOrSubclass() == true

private fun objCPointerMirror(declarationMapper: DeclarationMapper, type: ObjCPointer): TypeMirror.ByValue {
    if (type is ObjCObjectPointer && type.def.isNSStringOrSubclass()) {
        val valueType = KotlinTypes.string
        return objCMirror(valueType, TypeInfo.ObjCPointerInfo(valueType, type), type.isNullable)
    }

    val valueType = when (type) {
        is ObjCIdType -> {
            type.protocols.firstOrNull()?.let { declarationMapper.getKotlinClassFor(it) }?.type
                    ?: KotlinTypes.any
        }
        is ObjCClassPointer -> KotlinTypes.objCClass.type
        is ObjCObjectPointer -> {
            when (type.def.name) {
                "NSArray" -> KotlinTypes.list.typeWith(StarProjection)
                "NSMutableArray" -> KotlinTypes.mutableList.typeWith(KotlinTypes.any.makeNullable())
                "NSSet" -> KotlinTypes.set.typeWith(StarProjection)
                "NSDictionary" -> KotlinTypes.map.typeWith(KotlinTypes.any.makeNullable(), StarProjection)
                else -> declarationMapper.getKotlinClassFor(type.def).type
            }
        }
        is ObjCInstanceType -> TODO(type.toString()) // Must have already been handled.
        is ObjCBlockPointer -> return objCBlockPointerMirror(declarationMapper, type)
    }

    return objCMirror(valueType, TypeInfo.ObjCPointerInfo(valueType, type), type.isNullable)
}

private fun objCBlockPointerMirror(declarationMapper: DeclarationMapper, type: ObjCBlockPointer): TypeMirror.ByValue {
    val returnType = if (type.returnType.unwrapTypedefs() is VoidType) {
        KotlinTypes.unit
    } else {
        mirror(declarationMapper, type.returnType).argType
    }
    val kotlinType = KotlinFunctionType(
            type.parameterTypes.map { mirror(declarationMapper, it).argType },
            returnType
    )

    val info = TypeInfo.ObjCBlockPointerInfo(kotlinType, type)
    return objCMirror(kotlinType, info, type.isNullable)
}

private fun objCMirror(valueType: KotlinType, info: TypeInfo, nullable: Boolean) = TypeMirror.ByValue(
        info.constructPointedType(valueType.makeNullableAsSpecified(nullable)),
        info,
        valueType.makeNullable(), // All typedefs to Objective-C pointers would be nullable for simplicity
        nullable
)

fun getKotlinFunctionType(declarationMapper: DeclarationMapper, type: FunctionType): KotlinFunctionType {
    val returnType = if (type.returnType.unwrapTypedefs() is VoidType) {
        KotlinTypes.unit
    } else {
        mirror(declarationMapper, type.returnType).argType
    }
    return KotlinFunctionType(
            type.parameterTypes.map { mirror(declarationMapper, it).argType },
            returnType,
            nullable = false
    )
}


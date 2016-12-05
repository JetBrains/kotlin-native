package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.KotlinType

internal fun ContextUtils.getLLVMType(type: KotlinType): LLVMTypeRef {
    return when {
        KotlinBuiltIns.isBoolean(type) -> LLVMInt1Type()
        KotlinBuiltIns.isByte(type) -> LLVMInt8Type()
        KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isChar(type) -> LLVMInt16Type()
        KotlinBuiltIns.isInt(type) -> LLVMInt32Type()
        KotlinBuiltIns.isLong(type) -> LLVMInt64Type()
        KotlinBuiltIns.isUnit(type) -> LLVMVoidType() // TODO: handle Unit parameter case
        KotlinBuiltIns.isFloat(type) -> LLVMFloatType()
        KotlinBuiltIns.isDouble(type) -> LLVMDoubleType()
        !KotlinBuiltIns.isNullableAny(type) &&
                (KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) -> this.kArrayHeaderPtr
        // Nullable arrays also are represented as ObjHeader, otherwise it's hard to represent nullable arrays.
        !KotlinBuiltIns.isPrimitiveType(type) -> this.kObjHeaderPtr
        else -> throw NotImplementedError(type.toString() + " is not supported")
    }!!
}

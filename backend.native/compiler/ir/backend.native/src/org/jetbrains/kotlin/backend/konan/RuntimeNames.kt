package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName

object RuntimeNames {
    val symbolName = FqName("kotlin.native.SymbolName")
    val exportForCppRuntime = FqName("kotlin.native.internal.ExportForCppRuntime")
    val exportForCompilerAnnotation = FqName("kotlin.native.internal.ExportForCompiler")
    val exportTypeInfoAnnotation = FqName("kotlin.native.internal.ExportTypeInfo")
    val cCall = FqName("kotlinx.cinterop.internal.CCall")
    val objCMethodImp = FqName("kotlinx.cinterop.ObjCMethodImp")
    val independent = FqName("kotlin.native.internal.Independent")
    val filterExceptions = FqName("kotlin.native.internal.FilterExceptions")
    val kotlinNativeInternalPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
    val associatedObjectKey = FqName("kotlin.reflect.AssociatedObjectKey")
}

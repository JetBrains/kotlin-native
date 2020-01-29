package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import java.lang.StringBuilder

abstract class AbstractKonanIrMangler : IrBasedKotlinManglerImpl() {
    override fun getExportChecker(): IrExportCheckerVisitor = KonanIrExportChecker()

    override fun getMangleComputer(prefix: String): IrMangleComputer = KonanIrManglerComputer(StringBuilder(256), false)

    private class KonanIrExportChecker : IrExportCheckerVisitor() {
        override fun IrDeclaration.isPlatformSpecificExported(): Boolean {
            if (this is IrSimpleFunction) if (isFakeOverride) return false

            // TODO: revise
            if (annotations.hasAnnotation(RuntimeNames.symbolNameAnnotation)) {
                // Treat any `@SymbolName` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.exportForCppRuntime)) {
                // Treat any `@ExportForCppRuntime` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.cnameAnnotation)) {
                // Treat `@CName` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.exportForCompilerAnnotation)) {
                return true
            }

            return false
        }

    }

    private class KonanIrManglerComputer(builder: StringBuilder, skipSig: Boolean) : IrMangleComputer(builder, skipSig) {
        override fun copy(skipSig: Boolean): IrMangleComputer = KonanIrManglerComputer(builder, skipSig)

        override fun IrFunction.platformSpecificFunctionName(): String? {
            (if (this is IrConstructor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()
                    ?.let {
                        return buildString {
                            if (extensionReceiverParameter != null) {
                                append(extensionReceiverParameter!!.type.getClass()!!.name)
                                append(".")
                            }

                            append("objc:")
                            append(it.selector)
                            if (this@platformSpecificFunctionName is IrConstructor && this@platformSpecificFunctionName.isObjCConstructor) append("#Constructor")

                            if ((this@platformSpecificFunctionName as? IrSimpleFunction)?.correspondingPropertySymbol != null) {
                                append("#Accessor")
                            }
                        }
                    }
            return null
        }

        override fun IrFunction.specialValueParamPrefix(param: IrValueParameter): String {
            // TODO: there are clashes originating from ObjectiveC interop.
            // kotlinx.cinterop.ObjCClassOf<T>.create(format: kotlin.String): T defined in platform.Foundation in file Foundation.kt
            // and
            // kotlinx.cinterop.ObjCClassOf<T>.create(string: kotlin.String): T defined in platform.Foundation in file Foundation.kt

            return if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${param.name}:" else ""
        }


    }
}

object KonanManglerIr : AbstractKonanIrMangler()
package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

abstract class AbstractKonanDescriptorMangler : DescriptorBasedKotlinManglerImpl() {
    override fun getExportChecker(): DescriptorExportCheckerVisitor = KonanDescriptorExportChecker()

    override fun getMangleComputer(prefix: String): DescriptorMangleComputer =
            KonanDescriptorMangleComputer(StringBuilder(256), prefix, false)

    private class KonanDescriptorExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported(): Boolean {
            if (this is SimpleFunctionDescriptor) {
                if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) return false
            }
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

    private class KonanDescriptorMangleComputer(builder: StringBuilder, prefix: String, skipSig: Boolean) : DescriptorMangleComputer(builder, prefix, skipSig) {
        override fun copy(skipSig: Boolean): DescriptorMangleComputer = KonanDescriptorMangleComputer(builder, specialPrefix, skipSig)

        override fun FunctionDescriptor.platformSpecificFunctionName(): String? {
            (if (this is ConstructorDescriptor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()
                    ?.let {
                        return buildString {
                            if (extensionReceiverParameter != null) {
                                append(extensionReceiverParameter!!.type.constructor.declarationDescriptor!!.name)
                                append(".")
                            }

                            append("objc:")
                            append(it.selector)
                            if (this@platformSpecificFunctionName is ConstructorDescriptor && this@platformSpecificFunctionName.isObjCConstructor) append("#Constructor")

                            if (this@platformSpecificFunctionName is PropertyAccessorDescriptor) {
                                append("#Accessor")
                            }
                        }
                    }
            return null
        }

        override fun FunctionDescriptor.specialValueParamPrefix(param: ValueParameterDescriptor): String {
            return if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${param.name}:" else ""
        }
    }
}

object KonanManglerDesc : AbstractKonanDescriptorMangler()
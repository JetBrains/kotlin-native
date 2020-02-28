package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class KonanIdSignaturer(private val mangler: KotlinMangler.DescriptorMangler) : IdSignatureDescriptor(mangler) {

    override fun createSignatureBuilder(): DescriptorBasedSignatureBuilder =
            KonanDescriptorBasedSignatureBuilder(mangler)

    private class KonanDescriptorBasedSignatureBuilder(
            mangler: KotlinMangler.DescriptorMangler
    ) : DescriptorBasedSignatureBuilder(mangler) {

        /**
         * We need a way to distinguish interop declarations from usual ones
         * to be able to link against them. We do it by marking them with
         * [IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY] flag.
         */
        private fun markInteropDeclaration(descriptor: DeclarationDescriptor) {
            if (descriptor.module.isFromInteropLibrary()) {
                mask = mask or IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(true)

                //
                if (hashId == null) hashId = extractDescriptorUniqId(descriptor)
            }
        }

        private fun writeInteropUniqId(descriptor: DeclarationDescriptor) {
            if (descriptor.module.isFromInteropLibrary()) {
                if (hashId == null) hashId = extractDescriptorUniqId(descriptor)
            }
        }

        override fun platformSpecificAlias(descriptor: TypeAliasDescriptor) {
            markInteropDeclaration(descriptor)
            writeInteropUniqId(descriptor)
        }

        override fun platformSpecificClass(descriptor: ClassDescriptor) {
            markInteropDeclaration(descriptor)
            writeInteropUniqId(descriptor)
        }

        override fun platformSpecificConstructor(descriptor: ConstructorDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificFunction(descriptor: FunctionDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificGetter(descriptor: PropertyGetterDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificPackage(descriptor: PackageFragmentDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificProperty(descriptor: PropertyDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificSetter(descriptor: PropertySetterDescriptor) {
            markInteropDeclaration(descriptor)
        }
    }
}
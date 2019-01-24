package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.irasdescriptors.isAccessor
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isGetter
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isSetter
import org.jetbrains.kotlin.backend.konan.irasdescriptors.name
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.resolve.descriptorUtil.classId


class DescriptorReferenceSerializer(val declarationTable: DeclarationTable) {

    // Not all exported descriptors are deserialized, some a synthesized anew during metadata deserialization.
    // Those created descriptors can't carry the uniqIdIndex, since it is available only for deserialized descriptors.
    // So we record the uniq id of some other "discoverable" descriptor for which we know for sure that it will be
    // available as deserialized descriptor, plus the path to find the needed descriptor from that one.
    fun serializeDescriptorReference(declaration: IrDeclaration): KonanIr.DescriptorReference? {

        val descriptor = declaration.descriptor

        if (!declaration.isExported() && !((declaration as? IrDeclarationWithVisibility)?.visibility == Visibilities.INVISIBLE_FAKE)) {
            return null
        }
        if (declaration is IrAnonymousInitializer) return null

        if (descriptor is ParameterDescriptor || (descriptor is VariableDescriptor && descriptor !is PropertyDescriptor) || descriptor is TypeParameterDescriptor) return null

        val containingDeclaration = descriptor.containingDeclaration!!

        val (packageFqName, classFqName) = when (containingDeclaration) {
            is ClassDescriptor -> {
                val classId = containingDeclaration.classId ?: return null
                Pair(classId.packageFqName.toString(), classId.relativeClassName.toString())
            }
            is PackageFragmentDescriptor -> Pair(containingDeclaration.fqName.toString(), "")
            else -> return null
        }

        val isAccessor = declaration.isAccessor
        val isBackingField = declaration is IrField && declaration.correspondingProperty != null
        val isFakeOverride = declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE
        val isDefaultConstructor =
            descriptor is ClassConstructorDescriptor && containingDeclaration is ClassDescriptor && containingDeclaration.kind == ClassKind.OBJECT
        val isEnumEntry = descriptor is ClassDescriptor && descriptor.kind == ClassKind.ENUM_ENTRY
        val isEnumSpecial = declaration.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER


        val realDeclaration = if (isFakeOverride) {
            when (declaration) {
                is IrSimpleFunction -> declaration.resolveFakeOverrideMaybeAbstract()
                is IrField -> declaration.resolveFakeOverrideMaybeAbstract()
                is IrProperty -> declaration.resolveFakeOverrideMaybeAbstract()
                else -> error("Unexpected fake override declaration")
            }
        } else {
            declaration
        }

        val discoverableDescriptorsDeclaration: IrDeclaration? = if (isAccessor) {
            (realDeclaration as IrSimpleFunction).correspondingProperty!!
        } else if (isBackingField) {
            (realDeclaration as IrField).correspondingProperty!!
        } else if (isDefaultConstructor || isEnumEntry) {
            null
        } else {
            realDeclaration
        }

        val uniqId = discoverableDescriptorsDeclaration?.let { declarationTable.uniqIdByDeclaration(it) }
        uniqId?.let { declarationTable.descriptors.put(discoverableDescriptorsDeclaration.descriptor, it) }

        val proto = KonanIr.DescriptorReference.newBuilder()
            .setPackageFqName(packageFqName)
            .setClassFqName(classFqName)
            .setName(descriptor.name.toString())

        if (uniqId != null) proto.setUniqId(protoUniqId(uniqId))

        if (isFakeOverride) {
            proto.setIsFakeOverride(true)
        }

        if (isBackingField) {
            proto.setIsBackingField(true)
        }

        if (isAccessor) {
            if (declaration.isGetter)
                proto.setIsGetter(true)
            else if (declaration.isSetter)
                proto.setIsSetter(true)
            else
                error("A property accessor which is neither a getter, nor a setter: $descriptor")
        } else if (isDefaultConstructor) {
            proto.setIsDefaultConstructor(true)
        } else if (isEnumEntry) {
            proto.setIsEnumEntry(true)
        } else if (isEnumSpecial) {
            proto.setIsEnumSpecial(true)
        }

        return proto.build()
    }
}



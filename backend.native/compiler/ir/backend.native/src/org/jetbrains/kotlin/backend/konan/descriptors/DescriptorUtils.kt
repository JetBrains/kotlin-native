package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * List of all implemented interfaces (including those which implemented by a super class)
 */
internal val ClassDescriptor.implementedInterfaces: List<ClassDescriptor>
    get() {
        val superClassImplementedInterfaces = this.getSuperClassNotAny()?.implementedInterfaces ?: emptyList()
        val superInterfaces = this.getSuperInterfaces()
        val superInterfacesImplementedInterfaces = superInterfaces.flatMap { it.implementedInterfaces }
        return (superClassImplementedInterfaces +
                superInterfacesImplementedInterfaces +
                superInterfaces).distinctBy { it.classId }
    }


/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun <T : CallableMemberDescriptor> T.resolveFakeOverride(): T {
    if (this.kind.isReal) {
        return this
    } else {
        val overridden = OverridingUtil.getOverriddenDeclarations(this)
        val filtered = OverridingUtil.filterOutOverridden(overridden)
        // TODO: is it correct to take first?
        return filtered.first { it.modality != Modality.ABSTRACT } as T
    }
}

private val intrinsicAnnotation = FqName("konan.internal.Intrinsic")

// TODO: check it is external?
internal val FunctionDescriptor.isIntrinsic: Boolean
    get() = this.annotations.findAnnotation(intrinsicAnnotation) != null

private val intrinsicTypes = setOf(
        "kotlin.Boolean", "kotlin.Char",
        "kotlin.Byte", "kotlin.Short",
        "kotlin.Int", "kotlin.Long",
        "kotlin.Float", "kotlin.Double"
)

private val arrayTypes = setOf(
        "kotlin.Array",
        "kotlin.ByteArray",
        "kotlin.CharArray",
        "kotlin.ShortArray",
        "kotlin.IntArray",
        "kotlin.LongArray",
        "kotlin.FloatArray",
        "kotlin.DoubleArray",
        "kotlin.BooleanArray"
)

internal val ClassDescriptor.isIntrinsic: Boolean
    get() = this.fqNameSafe.asString() in intrinsicTypes


internal val ClassDescriptor.isArray: Boolean
    get() = this.fqNameSafe.asString() in arrayTypes


internal val ClassDescriptor.isInterface: Boolean
    get() = (this.kind == ClassKind.INTERFACE)

private val konanInternalPackageName = FqName.fromSegments(listOf("konan", "internal"))

/**
 * @return `konan.internal` member scope
 */
internal val KonanBuiltIns.konanInternal: MemberScope
    get() = this.builtInsModule.getPackage(konanInternalPackageName).memberScope

/**
 * @return built-in class `konan.internal.$name` or
 * `null` if no such class is available (e.g. when compiling `link` test without stdlib).
 *
 * TODO: remove this workaround after removing compilation without stdlib.
 */
internal fun KonanBuiltIns.getKonanInternalClassOrNull(name: String): ClassDescriptor? {
    val classifier = konanInternal.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
    return classifier as? ClassDescriptor
}

/**
 * @return built-in class `konan.internal.$name`
 */
internal fun KonanBuiltIns.getKonanInternalClass(name: String): ClassDescriptor =
        getKonanInternalClassOrNull(name) ?: TODO(name)

internal fun KonanBuiltIns.getKonanInternalFunctions(name: String): List<FunctionDescriptor> {
    return konanInternal.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).toList()
}

internal fun KotlinType.isUnboundCallableReference() = this.isRepresentedAs(ValueType.UNBOUND_CALLABLE_REFERENCE)

internal val CallableDescriptor.allValueParameters: List<ParameterDescriptor>
    get() {
        val receivers = mutableListOf<ParameterDescriptor>()

        if (this is ConstructorDescriptor)
            receivers.add(this.constructedClass.thisAsReceiverParameter)

        val dispatchReceiverParameter = this.dispatchReceiverParameter
        if (dispatchReceiverParameter != null)
            receivers.add(dispatchReceiverParameter)

        val extensionReceiverParameter = this.extensionReceiverParameter
        if (extensionReceiverParameter != null)
            receivers.add(extensionReceiverParameter)

        return receivers + this.valueParameters
    }

internal val KotlinType.isFunctionOrKFunctionType: Boolean
    get() {
        val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
        return kind == FunctionClassDescriptor.Kind.Function || kind == FunctionClassDescriptor.Kind.KFunction
    }

internal val KotlinType.isKFunctionType: Boolean
    get() {
        val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
        return kind == FunctionClassDescriptor.Kind.KFunction
    }

internal val FunctionDescriptor.isFunctionInvoke: Boolean
    get() {
        val dispatchReceiver = dispatchReceiverParameter ?: return false
        assert(!dispatchReceiver.type.isKFunctionType)

        return dispatchReceiver.type.isFunctionType &&
                this.isOperator && this.name == OperatorNameConventions.INVOKE
    }

internal fun ClassDescriptor.isUnit() = this.defaultType.isUnit()

internal class OverriddenFunctionDescriptor(val descriptor: FunctionDescriptor, overriddenDescriptor: FunctionDescriptor) {
    val overriddenDescriptor = overriddenDescriptor.original

    val needBridge: Boolean
        get() {
            if (descriptor.modality == Modality.ABSTRACT) return false
            if (descriptor !is PropertySetterDescriptor) {
                return descriptor.needBridgeTo(overriddenDescriptor) || descriptor.target.needBridgeTo(overriddenDescriptor)
            } else {
                val property = descriptor.correspondingProperty
                val overriddenProperty = (overriddenDescriptor as PropertySetterDescriptor).correspondingProperty
                return property.needBridgeTo(overriddenProperty) || property.target.needBridgeTo(overriddenProperty)
            }
        }

    override fun toString(): String {
        return "(descriptor=$descriptor, overriddenDescriptor=$overriddenDescriptor)"
    }
}

internal val <T : CallableMemberDescriptor> T.allOverriddenDescriptors: List<T>
    get() {
        val result = mutableListOf<T>()
        fun traverse(descriptor: T) {
            result.add(descriptor)
            descriptor.overriddenDescriptors.forEach { traverse(it as T) }
        }
        traverse(this)
        return result
    }

internal val ClassDescriptor.contributedMethods: List<FunctionDescriptor>
    get () {
        val contributedDescriptors = unsubstitutedMemberScope.getContributedDescriptors()
        // (includes declarations from supers)

        val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

        val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
        val getters = properties.mapNotNull { it.getter }
        val setters = properties.mapNotNull { it.setter }

        val allMethods = (functions + getters + setters).sortedBy {
            // TODO: use local hash instead, but it needs major refactoring.
            it.functionName.hashCode()
        }

        return allMethods
    }

internal val ClassDescriptor.contributedMethodsWithOverridden: List<OverriddenFunctionDescriptor>
    get() {
        return contributedMethods.flatMap { method ->
            method.allOverriddenDescriptors.map { OverriddenFunctionDescriptor(method, it) }
        }.distinctBy {
            Triple(it.overriddenDescriptor.functionName, it.descriptor, it.needBridge)
        }.sortedBy {
            // TODO: use local hash instead, but it needs major refactoring.
            it.overriddenDescriptor.functionName.hashCode()
        }
    }

fun ClassDescriptor.isAbstract() = this.modality == Modality.SEALED || this.modality == Modality.ABSTRACT
        || this.kind == ClassKind.ENUM_CLASS

// TODO: optimize
internal val ClassDescriptor.vtableEntries: List<OverriddenFunctionDescriptor>
    get() {
        assert(!this.isInterface)

        val superVtableEntries = if (KotlinBuiltIns.isSpecialClassWithNoSupertypes(this)) {
            emptyList()
        } else {
            this.getSuperClassOrAny().vtableEntries
        }

        val methods = this.contributedMethods
        val newVtableSlots = mutableListOf<OverriddenFunctionDescriptor>()

        val inheritedVtableSlots = superVtableEntries.map { superMethod ->
            val overridingMethod = methods.singleOrNull { OverridingUtil.overrides(it, superMethod.descriptor) }
            if (overridingMethod == null) {
                superMethod
            } else {
                if (superMethod.descriptor.original != superMethod.overriddenDescriptor)
                    newVtableSlots.add(OverriddenFunctionDescriptor(overridingMethod, superMethod.descriptor))
                newVtableSlots.add(OverriddenFunctionDescriptor(overridingMethod, overridingMethod))
                OverriddenFunctionDescriptor(overridingMethod, superMethod.overriddenDescriptor)
            }
        }

        methods.filterNot { method -> inheritedVtableSlots.any { it.descriptor == method } } // Find newly defined methods.
                .mapTo(newVtableSlots) { OverriddenFunctionDescriptor(it, it) }

        val list = inheritedVtableSlots + newVtableSlots.filter { it.descriptor.isOverridable }.sortedBy {
            // TODO: use local hash instead, but it needs major refactoring.
            it.overriddenDescriptor.functionName.hashCode()
        }

        return list
    }

internal fun ClassDescriptor.vtableIndex(function: FunctionDescriptor): Int {
    val target = function.target
    val index = this.vtableEntries.indexOfFirst { it.overriddenDescriptor == target }
    if (index < 0) throw Error(function.toString() + " not in vtable of " + this.toString())
    return index
}

internal val ClassDescriptor.methodTableEntries: List<OverriddenFunctionDescriptor>
    get() {
        assert(!this.isAbstract())

        return this.contributedMethodsWithOverridden.filter {
            // We check that either method is open, or one of declarations it overrides
            // is open.
            it.overriddenDescriptor.isOverridable || DescriptorUtils.getAllOverriddenDeclarations(it.overriddenDescriptor).any { it.isOverridable }
        }
        // TODO: probably method table should contain all accessible methods to improve binary compatibility
    }

internal enum class BridgeDirection {
    FROM_VALUE_TYPE,
    TO_VALUE_TYPE
}

internal fun KotlinType.isValueType() = this.isPrimitiveNumberType() || this.isBoolean()

internal fun CallableMemberDescriptor.returnsValueType() = returnType.let { it != null && it.isValueType() }

internal fun CallableMemberDescriptor.needBridgeTo(target: CallableMemberDescriptor)
        = returnsValueType() xor target.returnsValueType()

private fun CallableMemberDescriptor.overridesReturningReference()
        = allOverriddenDescriptors.any { it.original.returnType.let { it != null && !it.isValueType() } }

private fun CallableMemberDescriptor.overridesReturningValueType()
        = allOverriddenDescriptors.any { it.original.returnType.let { it != null && it.isValueType() } }

internal val <T : CallableMemberDescriptor> T.target: T
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original as T

internal val CallableMemberDescriptor.bridgeDirection: BridgeDirection?
    get() {
        if (this is PropertySetterDescriptor)
            return this.correspondingProperty.bridgeDirection

        if (modality == Modality.ABSTRACT) return null
        if (kind.isReal) {
            if (returnsValueType() && overridesReturningReference())
                return BridgeDirection.FROM_VALUE_TYPE
            return null
        }

        val target = this.target
        val ourDirection: BridgeDirection? =
                when {
                    returnsValueType() && target.returnsValueType() && overridesReturningReference() -> BridgeDirection.FROM_VALUE_TYPE
                    returnsValueType() && !target.returnsValueType() && overridesReturningValueType() -> BridgeDirection.TO_VALUE_TYPE
                    else -> null
                }

        if (ourDirection == target.bridgeDirection)
            return null // Bridge is inherited from supers.
        return ourDirection
    }

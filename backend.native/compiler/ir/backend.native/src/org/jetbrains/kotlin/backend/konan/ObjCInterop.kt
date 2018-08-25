/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.backend.konan.descriptors.getStringValue
import org.jetbrains.kotlin.backend.konan.irasdescriptors.constructedClass
import org.jetbrains.kotlin.backend.konan.irasdescriptors.getExternalObjCMethodInfo
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isReal
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.supertypes

private val interopPackageName = InteropFqNames.packageName
private val objCObjectFqName = interopPackageName.child(Name.identifier("ObjCObject"))
private val objCClassFqName = interopPackageName.child(Name.identifier("ObjCClass"))
internal val externalObjCClassFqName = interopPackageName.child(Name.identifier("ExternalObjCClass"))
private val objCMethodFqName = interopPackageName.child(Name.identifier("ObjCMethod"))
private val objCConstructorFqName = FqName("kotlinx.cinterop.ObjCConstructor")
private val objCFactoryFqName = interopPackageName.child(Name.identifier("ObjCFactory"))
private val objCBridgeFqName = interopPackageName.child(Name.identifier("ObjCBridge"))

fun ClassDescriptor.isObjCClass(): Boolean =
        this.getAllSuperClassifiers().any { it.fqNameSafe == objCObjectFqName } &&
                this.containingDeclaration.fqNameSafe != interopPackageName

fun KotlinType.isObjCObjectType(): Boolean =
        (this.supertypes() + this).any { TypeUtils.getClassDescriptor(it)?.fqNameSafe == objCObjectFqName }

fun ClassDescriptor.isExternalObjCClass(): Boolean = this.isObjCClass() &&
        this.parentsWithSelf.filterIsInstance<ClassDescriptor>().any {
            it.annotations.findAnnotation(externalObjCClassFqName) != null
        }

fun ClassDescriptor.isObjCMetaClass(): Boolean = this.getAllSuperClassifiers().any {
    it.fqNameSafe == objCClassFqName
}

fun FunctionDescriptor.isObjCClassMethod() =
        this.containingDeclaration.let { it is ClassDescriptor && it.isObjCClass() }

fun FunctionDescriptor.isExternalObjCClassMethod() =
        this.containingDeclaration.let { it is ClassDescriptor && it.isExternalObjCClass() }

// Special case: methods from Kotlin Objective-C classes can be called virtually from bridges.
fun FunctionDescriptor.canObjCClassMethodBeCalledVirtually(overriddenDescriptor: FunctionDescriptor) =
        overriddenDescriptor.isOverridable && this.kind.isReal && !this.isExternalObjCClassMethod()

fun ClassDescriptor.isKotlinObjCClass(): Boolean = this.isObjCClass() && !this.isExternalObjCClass()

data class ObjCMethodInfo(val bridge: FunctionDescriptor,
                          val selector: String,
                          val encoding: String,
                          val imp: String)

private fun CallableDescriptor.getBridgeAnnotation() =
        this.annotations.findAnnotation(objCBridgeFqName)

private fun FunctionDescriptor.decodeObjCMethodAnnotation(): ObjCMethodInfo? {
    assert (this.kind.isReal)

    val methodAnnotation = this.annotations.findAnnotation(objCMethodFqName) ?: return null
    val packageFragment = this.findPackage()

    val bridgeName = methodAnnotation.getStringValue("bridge")

    return objCMethodInfoByBridge(packageFragment, bridgeName)
}

private fun objCMethodInfoByBridge(packageFragment: PackageFragmentDescriptor, bridgeName: String): ObjCMethodInfo {
    val bridge = packageFragment.getMemberScope()
            .getContributedFunctions(Name.identifier(bridgeName), NoLookupLocation.FROM_BACKEND)
            .single()

    val bridgeAnnotation = bridge.getBridgeAnnotation()!!
    return ObjCMethodInfo(
            bridge = bridge,
            selector = bridgeAnnotation.getStringValue("selector"),
            encoding = bridgeAnnotation.getStringValue("encoding"),
            imp = bridgeAnnotation.getStringValue("imp")
    )
}

/**
 * @param onlyExternal indicates whether to accept overriding methods from Kotlin classes
 */
private fun FunctionDescriptor.getObjCMethodInfo(onlyExternal: Boolean): ObjCMethodInfo? {
    if (this.kind.isReal) {
        this.decodeObjCMethodAnnotation()?.let { return it }

        if (onlyExternal) {
            return null
        }
    }

    return this.overriddenDescriptors.asSequence().mapNotNull { it.getObjCMethodInfo(onlyExternal) }.firstOrNull()
}

fun FunctionDescriptor.getExternalObjCMethodInfo(): ObjCMethodInfo? = this.getObjCMethodInfo(onlyExternal = true)

fun FunctionDescriptor.getObjCMethodInfo(): ObjCMethodInfo? = this.getObjCMethodInfo(onlyExternal = false)

fun IrFunction.isObjCBridgeBased(): Boolean {
    assert(this.isReal)

    return this.descriptor.annotations.hasAnnotation(objCMethodFqName) ||
            this.descriptor.annotations.hasAnnotation(objCFactoryFqName) ||
            this.descriptor.annotations.hasAnnotation(objCConstructorFqName)
}

/**
 * Describes method overriding rules for Objective-C methods.
 *
 * This class is applied at [org.jetbrains.kotlin.resolve.OverridingUtil] as configured with
 * `META-INF/services/org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition` resource.
 */
class ObjCOverridabilityCondition : ExternalOverridabilityCondition {

    override fun getContract() = ExternalOverridabilityCondition.Contract.BOTH

    override fun isOverridable(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor,
            subClassDescriptor: ClassDescriptor?
    ): ExternalOverridabilityCondition.Result {

        if (superDescriptor.name != subDescriptor.name) {
            return ExternalOverridabilityCondition.Result.UNKNOWN
        }

        val superClass = superDescriptor.containingDeclaration as? ClassDescriptor
        val subClass = subDescriptor.containingDeclaration as? ClassDescriptor

        if (superClass == null || !superClass.isObjCClass() || subClass == null) {
            return ExternalOverridabilityCondition.Result.UNKNOWN
        }

        return if (areSelectorsEqual(superDescriptor, subDescriptor)) {
            // Also check the method signatures if the subclass is user-defined:
            if (subClass.isExternalObjCClass()) {
                ExternalOverridabilityCondition.Result.OVERRIDABLE
            } else {
                ExternalOverridabilityCondition.Result.UNKNOWN
            }
        } else {
            ExternalOverridabilityCondition.Result.INCOMPATIBLE
        }

    }

    private fun areSelectorsEqual(first: CallableDescriptor, second: CallableDescriptor): Boolean {
        // The original Objective-C method selector is represented as
        // function name and parameter names (except first).

        if (first.valueParameters.size != second.valueParameters.size) {
            return false
        }

        first.valueParameters.forEachIndexed { index, parameter ->
            if (index > 0 && parameter.name != second.valueParameters[index].name) {
                return false
            }
        }

        return true
    }

}

fun ConstructorDescriptor.objCConstructorIsDesignated(): Boolean {
    val annotation = this.annotations.findAnnotation(objCConstructorFqName)!!
    val value = annotation.allValueArguments[Name.identifier("designated")]!!

    return (value as BooleanValue).value
}

fun ConstructorDescriptor.getObjCInitMethod(): FunctionDescriptor? {
    return this.annotations.findAnnotation(objCConstructorFqName)?.let {
        val initSelector = it.getStringValue("initSelector")
        this.constructedClass.unsubstitutedMemberScope.getContributedDescriptors().asSequence()
                .filterIsInstance<FunctionDescriptor>()
                .single { it.getExternalObjCMethodInfo()?.selector == initSelector }
    }
}

fun IrConstructor.isObjCConstructor(): Boolean = this.descriptor.annotations.hasAnnotation(objCConstructorFqName)

fun IrConstructor.getObjCInitMethod(): IrSimpleFunction? {
    return this.descriptor.annotations.findAnnotation(objCConstructorFqName)?.let {
        val initSelector = it.getStringValue("initSelector")
        this.constructedClass.declarations.asSequence()
                .filterIsInstance<IrSimpleFunction>()
                .single { it.getExternalObjCMethodInfo()?.selector == initSelector }
    }
}

fun FunctionDescriptor.getObjCFactoryInitMethodInfo(): ObjCMethodInfo? {
    val factoryAnnotation = this.annotations.findAnnotation(objCFactoryFqName) ?: return null
    val bridgeName = factoryAnnotation.getStringValue("bridge")

    return objCMethodInfoByBridge(this.findPackage(), bridgeName)
}

fun inferObjCSelector(descriptor: FunctionDescriptor): String = if (descriptor.valueParameters.isEmpty()) {
    descriptor.name.asString()
} else {
    buildString {
        append(descriptor.name)
        append(':')
        descriptor.valueParameters.drop(1).forEach {
            append(it.name)
            append(':')
        }
    }
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName

open class DeepVisitor<DataType>(
        private val worker: DeclarationDescriptorVisitor<Boolean, DataType>
) : DeclarationDescriptorVisitor<Boolean, DataType> {

    open fun visitChildren(descriptors: Collection<DeclarationDescriptor>, data: DataType): Boolean {
        for (descriptor in descriptors) {
            if (!descriptor.accept(this, data)) return false
        }
        return true
    }

    open fun visitChild(descriptor: DeclarationDescriptor?, data: DataType): Boolean {
        if (descriptor == null) return true

        return descriptor.accept(this, data)
    }

    private fun <DescriptorType : DeclarationDescriptor> processDescriptor(
            descriptor: DescriptorType,
            data: DataType,
            visitChildrenBlock: DeepVisitor<DataType>.(DescriptorType) -> Boolean = { true }
    ): Boolean {
        // first, process the descriptor itself
        if (!descriptor.accept(worker, data)) return false

        // then, visit children
        return visitChildrenBlock(descriptor)
    }

    private fun <DescriptorType : CallableDescriptor> processCallableDescriptor(
            descriptor: DescriptorType,
            data: DataType,
            visitChildrenBlock: DeepVisitor<DataType>.(DescriptorType) -> Boolean = { true }
    ): Boolean {
        return processDescriptor(descriptor, data) {
            visitChildren(descriptor.typeParameters, data)
                    && visitChild(descriptor.extensionReceiverParameter, data)
                    && visitChildren(descriptor.valueParameters, data)
                    && visitChildrenBlock(descriptor)
        }
    }

    private val PackageViewDescriptor.fragmentsToRecurse: List<PackageFragmentDescriptor>
        get() = fragments.filter { it.containingDeclaration == module }

    private val ModuleDescriptor.fragmentsToRecurse: List<PackageFragmentDescriptor>
        get() {
            val fragments = mutableListOf<PackageFragmentDescriptor>()

            fun recurse(packageView: PackageViewDescriptor) {
                fragments += packageView.fragmentsToRecurse
                getSubPackagesOf(packageView.fqName) { true }.forEach { recurse(getPackage(it)) }
            }

            recurse(getPackage(FqName.ROOT))

            return fragments.sortedBy { it.fqName.asString() }
        }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: DataType) =
            processDescriptor(descriptor, data) {
                visitChildren(descriptor.getMemberScope().getContributedDescriptors(), data)
            }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: DataType) =
            processDescriptor(descriptor, data) {
                visitChildren(descriptor.fragmentsToRecurse, data)
            }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: DataType) =
            processCallableDescriptor(descriptor, data)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: DataType) =
            processCallableDescriptor(descriptor, data)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: DataType) =
            processCallableDescriptor(descriptor, data)

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: DataType) =
            processDescriptor(descriptor, data)

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: DataType) =
            processDescriptor(descriptor, data) {
                visitChild(descriptor.thisAsReceiverParameter, data)
                        && visitChildren(descriptor.constructors, data)
                        && visitChildren(descriptor.typeConstructor.parameters, data)
                        && visitChildren(descriptor.defaultType.memberScope.getContributedDescriptors(), data)
            }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: DataType) =
            processDescriptor(descriptor, data) {
                visitChildren(descriptor.declaredTypeParameters, data)
            }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: DataType) =
            processDescriptor(descriptor, data) {
                visitChildren(descriptor.fragmentsToRecurse, data)
            }

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: DataType) =
            visitFunctionDescriptor(constructorDescriptor, data)

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: DataType) =
            visitClassDescriptor(scriptDescriptor, data)

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: DataType) =
            visitVariableDescriptor(descriptor, data)

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: DataType) =
            visitFunctionDescriptor(descriptor, data)

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: DataType) =
            visitFunctionDescriptor(descriptor, data)

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: DataType) =
            processDescriptor(descriptor, data)
}

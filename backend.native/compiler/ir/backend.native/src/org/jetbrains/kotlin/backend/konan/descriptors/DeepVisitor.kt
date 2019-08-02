/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName

class TraversalContext private constructor(
        val parent: TraversalContext?,
        val fieldName: String?,
        val lastChild: Boolean
) {
    constructor() : this(null, null, true)

    val depth: Int = parent?.let { it.depth + 1 } ?: 0

    fun levelDown(fieldName: String? = null, lastChild: Boolean) = TraversalContext(this, fieldName, lastChild)
}

class DeepVisitor(
        private val wrapped: DeclarationDescriptorVisitor<Boolean, TraversalContext>
) : DeclarationDescriptorVisitor<Boolean, TraversalContext> {

    private interface Collector {
        fun add(fieldName: String, child: DeclarationDescriptor?)
        fun add(fieldName: String, children: Collection<DeclarationDescriptor>)
    }

    private fun <DescriptorType : DeclarationDescriptor> processDescriptor(
            descriptor: DescriptorType,
            context: TraversalContext,
            collectChildren: Collector.() -> Unit = {}
    ): Boolean {
        // first, process the descriptor itself
        if (!descriptor.accept(wrapped, context)) return false

        // need to collect all children before traversing down in order to know which child is the "last one"
        val collector = object : Collector {
            val children = mutableListOf<Pair<String, DeclarationDescriptor>>()

            override fun add(fieldName: String, child: DeclarationDescriptor?) {
                if (child != null) children += fieldName to child
            }

            override fun add(fieldName: String, children: Collection<DeclarationDescriptor>) {
                for (child in children) add(fieldName, child)
            }
        }

        collectChildren(collector)

        for ((index, entry) in collector.children.withIndex()) {
            val lastChild = index + 1 == collector.children.size
            val (fieldName, child) = entry
            if (!child.accept(this, context.levelDown(fieldName, lastChild))) return false
        }

        return true
    }

    private fun <DescriptorType : CallableDescriptor> processCallableDescriptor(
            descriptor: DescriptorType,
            context: TraversalContext,
            collectChildren: Collector.() -> Unit = {}
    ): Boolean {
        return processDescriptor(descriptor, context) {
            add("typeParameter", descriptor.typeParameters)
            add("extensionReceiverParameter", descriptor.extensionReceiverParameter)
            add("valueParameter", descriptor.valueParameters)
            collectChildren()
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

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: TraversalContext) =
            processDescriptor(descriptor, data) {
                add("contributedDescriptor", descriptor.getMemberScope().getContributedDescriptors())
            }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: TraversalContext) =
            processDescriptor(descriptor, data) {
                add("packageFragment", descriptor.fragmentsToRecurse)
            }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: TraversalContext) =
            processCallableDescriptor(descriptor, data)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: TraversalContext) =
            processCallableDescriptor(descriptor, data)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: TraversalContext) =
            processCallableDescriptor(descriptor, data)

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: TraversalContext) =
            processDescriptor(descriptor, data)

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: TraversalContext) =
            processDescriptor(descriptor, data) {
                add("thisAsReceiverParameter", descriptor.thisAsReceiverParameter)
                add("constructor", descriptor.constructors)
                add("typeConstructor.parameter", descriptor.typeConstructor.parameters)
                add("contributedDescriptor", descriptor.defaultType.memberScope.getContributedDescriptors())
            }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: TraversalContext) =
            processDescriptor(descriptor, data) {
                add("declaredTypeParameter", descriptor.declaredTypeParameters)
            }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: TraversalContext) =
            processDescriptor(descriptor, data) {
                add("packageFragment", descriptor.fragmentsToRecurse)
            }

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: TraversalContext) =
            visitFunctionDescriptor(constructorDescriptor, data)

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: TraversalContext) =
            visitClassDescriptor(scriptDescriptor, data)

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: TraversalContext) =
            visitVariableDescriptor(descriptor, data)

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: TraversalContext) =
            visitFunctionDescriptor(descriptor, data)

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: TraversalContext) =
            visitFunctionDescriptor(descriptor, data)

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: TraversalContext) =
            processDescriptor(descriptor, data)
}

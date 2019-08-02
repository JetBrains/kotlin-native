/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import java.util.*

class PrintVisitor(private val showTreeBranches: Boolean): DeclarationDescriptorVisitor<Boolean, TraversalContext> {

    private fun StringBuilder.appendEmptySpacePrefix(context: TraversalContext) {
        for (i in 0 until context.depth) append("    ")
    }

    private fun StringBuilder.appendTreeBranchesPrefix(context: TraversalContext) {
        if (context.depth == 0) return

        val stack = ArrayDeque<String>(context.depth)
        stack.push(if (context.lastChild) "└───" else "├───")

        var parent = context.parent
        while (parent != null) {
            stack.push(if (parent.lastChild) "    " else "│   ")
            parent = parent.parent
        }

        stack.forEach { append(it) }
    }

    private inline fun <reified DescriptorType : DeclarationDescriptor> printDescriptor(
            descriptor: DescriptorType,
            context: TraversalContext
    ): Boolean {
        val description = buildString {
            if (showTreeBranches) appendTreeBranchesPrefix(context) else appendEmptySpacePrefix(context)
            context.fieldName?.let { fieldName -> append(fieldName).append(" -> ") }
            append('[').append(DescriptorType::class.java.simpleName).append("] ")
            append(DescriptorRenderer.DEBUG_TEXT.render(descriptor))
        }

        println(description)

        return true
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitScriptDescriptor(descriptor: ScriptDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: TraversalContext): Boolean
        = printDescriptor(descriptor, data)
}



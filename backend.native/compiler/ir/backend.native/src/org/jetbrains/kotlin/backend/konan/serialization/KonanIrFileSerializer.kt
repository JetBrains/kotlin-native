package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation

class KonanIrFileSerializer(
    messageCollector: MessageCollector,
    declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    skipExpects: Boolean,
    bodiesOnlyForInlines: Boolean = false
): IrFileSerializer(messageCollector, declarationTable, expectDescriptorToSymbol, skipExpects = skipExpects, bodiesOnlyForInlines = bodiesOnlyForInlines) {

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean {
        val fqn = when (node) {
            is IrFunction -> RuntimeNames.exportForCppRuntime
            is IrClass -> RuntimeNames.exportTypeInfoAnnotation
            else -> return false
        }

        return node.annotations.hasAnnotation(fqn)
    }

    override fun backendSpecificSerializeAllMembers(irClass: IrClass) = !KonanFakeOverrideClassFilter.constructFakeOverrides(irClass)
}
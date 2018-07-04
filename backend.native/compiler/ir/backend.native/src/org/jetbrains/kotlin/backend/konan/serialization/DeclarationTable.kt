package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.irasdescriptors.fqNameSafe
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

/*
class DeclarationTable(val builtins: IrBuiltIns) {
    val descriptorTable = DescriptorTable(builtins)

    val reverse = mutableMapOf<Long, IrSymbolOwner>()

    fun indexByDeclaration(declaration: IrSymbolOwner): Long {

        val index = if (declaration is IrDeclaration) descriptorTable.indexByValue(declaration.descriptor)

        reverse.getOrPut(index) { declaration }
        return index
    }

    fun declarationByIndex(index: Long) = reverse[index]
}
        */

internal fun IrDeclaration.symbolName(): String = when (this) {
    is IrFunction
    -> {
        println("### this.symbolNBame = ${this.symbolName}")
        this.uniqFunctionName
    }
    is IrProperty
    -> this.symbolName
    is IrClass
    -> this.typeInfoSymbolName
    is IrField
    -> this.symbolName
    is IrEnumEntry
    -> this.symbolName
    else -> error("Unexpected exported declaration: $this")
}

internal val IrDeclaration.uniqId
    get() = this.symbolName().localHash.value


// TODO: We don't manage id clashes anyhow now.
class DeclarationTable(val builtIns: IrBuiltIns) {

    val table = mutableMapOf<IrDeclaration, Long>()
    val reverse = mutableMapOf<Long, IrDeclaration>() // TODO: remove me. Only needed during the development.
    var currentIndex = 17L

    init {
        builtIns.knownBuiltins.forEach {
            table.put(it, it.uniqId)
        }
    }

    fun indexByValue(value: IrDeclaration): Long {
        val index = table.getOrPut(value) {
            if (!value.isExported()
                    || value is IrVariable
                    || value is IrTypeParameter
                    || value is IrValueParameter
                    || value is IrAnonymousInitializerImpl
                    || value is IrFunction && value.origin == IrDeclarationOrigin.FAKE_OVERRIDE
                    || value is IrProperty && value.origin == IrDeclarationOrigin.FAKE_OVERRIDE
            ) {
                currentIndex++
            } else {
                value.uniqId
            }
        }
        reverse.getOrPut(index) { value }
        return index
    }

    fun valueByIndex(index: Long) = reverse[index]  // TODO: remove me. Only needed during the development.
}
/*
class IrDeserializationDescriptorIndex(irBuiltIns: IrBuiltIns) {

    val map = mutableMapOf<Long, DeclarationDescriptor>()

    init {
        irBuiltIns.knownBuiltins.forEach {
            map.put(it.uniqId, it)
        }
    }

}
*/
val IrBuiltIns.knownBuiltins // TODO: why do we have this list??? We need the complete list!
    get() = (lessFunByOperandType.values +
            lessOrEqualFunByOperandType.values +
            greaterOrEqualFunByOperandType.values +
            greaterFunByOperandType.values +
            ieee754equalsFunByOperandType.values +
            eqeqeqFun + eqeqFun + throwNpeFun + booleanNotFun + noWhenBranchMatchedExceptionFun + enumValueOfFun)


internal val IrProperty.symbolName: String
    get() {
        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kprop:$containingDeclarationPart$name"
    }

internal val IrEnumEntry.symbolName: String
    get() {
        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kenumentry:$containingDeclarationPart$name"
    }

// This is basicly the same as .symbolName, but diambiguates external functions with the same C name
internal val IrFunction.uniqFunctionName: String
    get() {
        println("### symbolName for $this")
        if (!this.isExported()) {
            throw AssertionError(this.descriptor.toString())
        }

        val parent = this.parent

        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }

        println("### symbolName = kfun:$containingDeclarationPart$functionName")
        return "kfun:$containingDeclarationPart$functionName"
    }
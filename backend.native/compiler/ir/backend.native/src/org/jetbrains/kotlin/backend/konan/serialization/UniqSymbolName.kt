package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.irasdescriptors.fqNameSafe
import org.jetbrains.kotlin.backend.konan.irasdescriptors.name
import org.jetbrains.kotlin.backend.konan.llvm.extensionReceiverNamePart
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.symbolName
import org.jetbrains.kotlin.backend.konan.llvm.typeInfoSymbolName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// This is a little extension over what's used in real mangling
// since some declarations never appear in the bitcode symbols.

internal fun IrDeclaration.uniqSymbolName(): String = when (this) {
    is IrFunction
    -> this.uniqFunctionName
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

private val IrDeclarationParent.fqNameUnique: FqName
    get() = when(this) {
        is IrPackageFragment -> this.fqName
        is IrDeclaration -> this.parent.fqNameUnique.child(this.uniqName)
        else -> error(this)
    }

private val IrDeclaration.uniqName: Name
    get() = when (this) {
        is IrSimpleFunction -> Name.special("<${this.uniqFunctionName}>")
        else -> this.name
    }

private val IrProperty.symbolName: String
    get() {
        val extensionReceiver: String = getter!!.extensionReceiverParameter ?. extensionReceiverNamePart ?: ""

        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kprop:$containingDeclarationPart$extensionReceiver$name"
    }

private val IrEnumEntry.symbolName: String
    get() {
        val containingDeclarationPart = parent.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kenumentry:$containingDeclarationPart$name"
    }

// This is basicly the same as .symbolName, but disambiguates external functions with the same C name.
// In addition functions appearing in fq sequence appear as <full signature>.
private val IrFunction.uniqFunctionName: String
    get() {
        val parent = this.parent

        val containingDeclarationPart = parent.fqNameUnique.let {
            if (it.isRoot) "" else "$it."
        }

        return "kfun:$containingDeclarationPart#$functionName"
    }

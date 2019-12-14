package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys.Companion.COUNT_BOX_OPERATIONS
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.getBoxFunction
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.setDeclarationsParent

internal class IrBoxCounterField private constructor(val context: Context, val suffix: String = "") {

    val field: IrField by lazy {
        val descriptor = WrappedFieldDescriptor()
        val expression = IrConstImpl.int(
                1, 1, context.irBuiltIns.intType, 0
        )
        val irField = IrFieldImpl(
                1, 1,
                IrDeclarationOrigin.DEFINED,
                IrFieldSymbolImpl(descriptor),
                "boxingCounter".synthesizedName,
                context.irBuiltIns.intType,
                Visibilities.PUBLIC,
                isFinal = false,
                isExternal = false,
                isStatic = true,
                isFakeOverride = false
        ).apply {
            descriptor.bind(this)
            expression.setDeclarationsParent(this)
        }
        irField
    }

    fun tryAddTo(file: IrFile) {
        if (containingFile != null) return
        file.addChild(field)
        containingFile = file
    }

    companion object {
        private val counters = mutableMapOf<Context, IrBoxCounterField>()
        private var containingFile: IrFile? = null

        fun get(context: Context, suffix: String = "") = counters.getOrPut(context) { IrBoxCounterField(context, suffix) }
    }
}

internal fun IrBuilderWithScope.irInc(counter: IrBoxCounterField): IrSetFieldImpl {
    val context = counter.context
    val stdlib = context.irModules.filterKeys { it.endsWith("stdlib") }.values.firstOrNull()
            ?: context.ir.irModule
    val intClass = stdlib.files
            .first { it.fqName.toString() == "kotlin" && it.fileEntry.name.endsWith("Primitives.kt") }
            .declarations
            .first { it.nameForIrSerialization.toString() == "Int" } as IrClass
    val increment = intClass.declarations.first { it.nameForIrSerialization.toString() == "inc" } as IrFunction

    return irSetField(null, counter.field, irCall(increment).also { it.dispatchReceiver = irGetField(null, counter.field) })
}

internal fun IrField.isBoxingCounter() = name.toString() == "${"$"}boxingCounter"
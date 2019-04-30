package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.konan.descriptors.createAnnotation
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

internal class CFunctionBuilder {
    private val parameters = mutableListOf<CVariable>()
    private lateinit var returnType: CType

    var variadic: Boolean = false

    fun setReturnType(type: CType) {
        require(!::returnType.isInitialized)
        returnType = type
    }

    fun addParameter(type: CType): CVariable {
        val result = CVariable(type, "p${counter++}")
        parameters += result
        return result
    }

    val numberOfParameters: Int get() = parameters.size

    private var counter = 1

    fun getType(): CType = CTypes.function(returnType, parameters.map { it.type }, variadic)

    fun buildSignature(name: String): String = returnType.render(buildString {
        append(name)
        append('(')
        parameters.joinTo(this)
        if (parameters.isEmpty()) {
            if (!variadic) append("void")
        } else {
            if (variadic) append(", ...")
        }
        append(')')
    })

}

internal class KotlinBridgeBuilder(
        startOffset: Int,
        endOffset: Int,
        cName: String,
        stubs: KotlinStubs,
        isExternal: Boolean
) {
    private var counter = 0
    private val bridge: IrFunction = createKotlinBridge(startOffset, endOffset, cName, stubs.symbols, isExternal)
    val irBuilder: IrBuilderWithScope = irBuilder(stubs.irBuiltIns, bridge.symbol).at(startOffset, endOffset)

    fun addParameter(type: IrType): IrValueParameter {
        val index = counter++
        val descriptor = WrappedValueParameterDescriptor()

        return IrValueParameterImpl(
                bridge.startOffset, bridge.endOffset, bridge.origin,
                IrValueParameterSymbolImpl(descriptor),
                Name.identifier("p$index"), index, type,
                null, false, false
        ).apply {
            descriptor.bind(this)
            parent = bridge
            bridge.valueParameters += this
        }
    }

    fun setReturnType(type: IrType) {
        bridge.returnType = type
    }

    fun build(): IrFunction = bridge
}

private fun createKotlinBridge(
        startOffset: Int,
        endOffset: Int,
        cBridgeName: String,
        symbols: KonanSymbols,
        isExternal: Boolean
): IrFunctionImpl {
    val bridgeAnnotations = Annotations.create(
            listOf(
                    if (isExternal) {
                        createAnnotation(symbols.symbolName.descriptor, "value" to cBridgeName)
                    } else {
                        createAnnotation(symbols.exportForCppRuntime.descriptor, "name" to cBridgeName)
                    }
            )
    )
    val bridgeDescriptor = WrappedSimpleFunctionDescriptor(bridgeAnnotations)
    val bridge = IrFunctionImpl(
            startOffset,
            endOffset,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(bridgeDescriptor),
            Name.identifier(cBridgeName),
            Visibilities.PRIVATE,
            Modality.FINAL,
            IrUninitializedType,
            isInline = false,
            isExternal = isExternal,
            isTailrec = false,
            isSuspend = false
    )
    bridgeDescriptor.bind(bridge)
    if (isExternal) {
        val constructor = symbols.filterExceptions.owner.constructors.single()
        bridge.annotations +=
                IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, constructor.returnType, constructor.symbol)
    }
    return bridge
}

internal class KotlinCBridgeBuilder(
        startOffset: Int,
        endOffset: Int,
        cName: String,
        stubs: KotlinStubs,
        isKotlinToC: Boolean
) {
    private val kotlinBridgeBuilder = KotlinBridgeBuilder(startOffset, endOffset, cName, stubs, isExternal = isKotlinToC)
    private val cBridgeBuilder = CFunctionBuilder()

    val kotlinIrBuilder: IrBuilderWithScope get() = kotlinBridgeBuilder.irBuilder

    fun addParameter(kotlinType: IrType, cType: CType): Pair<IrValueParameter, CVariable> {
        return kotlinBridgeBuilder.addParameter(kotlinType) to cBridgeBuilder.addParameter(cType)
    }

    fun setReturnType(kotlinReturnType: IrType, cReturnType: CType) {
        kotlinBridgeBuilder.setReturnType(kotlinReturnType)
        cBridgeBuilder.setReturnType(cReturnType)
    }

    fun buildCSignature(name: String): String = cBridgeBuilder.buildSignature(name)

    fun buildKotlinBridge() = kotlinBridgeBuilder.build()
}

internal class KotlinCallBuilder(private val irBuilder: IrBuilderWithScope, private val symbols: KonanSymbols) {
    val prepare = mutableListOf<IrStatement>()
    val arguments = mutableListOf<IrExpression>()
    val cleanup = mutableListOf<IrStatement>()

    private var memScope: IrVariable? = null

    fun getMemScope(): IrExpression = with(irBuilder) {
        memScope?.let { return irGet(it) }

        val newMemScope = scope.createTemporaryVariable(irCall(symbols.interopMemScope.owner.constructors.single()))
        memScope = newMemScope

        prepare += newMemScope

        val clearImpl = symbols.interopMemScope.owner.simpleFunctions().single { it.name.asString() == "clearImpl" }
        cleanup += irCall(clearImpl).apply {
            dispatchReceiver = irGet(memScope!!)
        }

        irGet(newMemScope)
    }

    fun build(
            function: IrFunction,
            transformCall: (IrMemberAccessExpression) -> IrExpression = { it }
    ): IrExpression {
        val arguments = this.arguments.toMutableList()

        val kotlinCall = irBuilder.irCall(function).run {
            if (function.dispatchReceiverParameter != null) {
                dispatchReceiver = arguments.removeAt(0)
            }
            if (function.extensionReceiverParameter != null) {
                extensionReceiver = arguments.removeAt(0)
            }
            assert(arguments.size == function.valueParameters.size)
            arguments.forEachIndexed { index, it -> putValueArgument(index, it) }

            transformCall(this)
        }

        return if (prepare.isEmpty() && cleanup.isEmpty()) {
            kotlinCall
        } else {
            irBuilder.irBlock(kotlinCall) {
                prepare.forEach { +it }
                if (cleanup.isEmpty()) {
                    +kotlinCall
                } else {
                    // Note: generating try-catch as finally blocks are already lowered.
                    +IrTryImpl(startOffset, endOffset, kotlinCall.type).apply {
                        tryResult = kotlinCall
                        catches += irCatch(context.irBuiltIns.throwableType).apply {
                            result = irBlock(kotlinCall) {
                                cleanup.forEach { +it }
                                +irThrow(irGet(catchParameter))
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class CCallBuilder {
    val arguments = mutableListOf<String>()

    fun build(function: String) = buildString {
        append(function)
        append('(')
        arguments.joinTo(this)
        append(')')
    }
}
package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.IrOriginToSpec
import org.jetbrains.kotlin.backend.konan.boxing.SpecializationEncoder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class ReplaceOriginsWithSpecializationsLowering(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transform(Replacer(), data = null)
    }

    private inner class Replacer(
            val symbolRemapper: SymbolRemapperForReplacer = SymbolRemapperForReplacer(),
            typeRemapper: TypeRemapper = TypeRemapperForReplacer(),
            val encoder: SpecializationEncoder = SpecializationEncoder(context)
    ) : IrElementTransformerVoid() {
        private val delegate = DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper)

        override fun visitFile(declaration: IrFile): IrFile {
            declaration.acceptVoid(symbolRemapper)
            return super.visitFile(declaration)
        }

        override fun visitCall(expression: IrCall): IrExpression {
            val origin = expression.symbol.owner
            val encodedFunctionName = encoder.encode(origin, expression.typeSubstitutionMap) ?: return super.visitCall(expression)
            val dispatchReceiver = expression.dispatchReceiver?.replaced()
            val specialization = IrOriginToSpec.forFunction(origin, encoder.decode(encodedFunctionName))
                    ?: dispatchReceiver?.let { IrOriginToSpec.forMemberFunction(origin, it.type) }

            return specialization?.let {
                context.createIrBuilder(expression.symbol).run {
                    (irCall(it) as IrCall).also { call ->
                        call.extensionReceiver = expression.extensionReceiver?.replaced()
                        call.dispatchReceiver = dispatchReceiver
                        for (i in 0 until expression.valueArgumentsCount) {
                            call.putValueArgument(i, expression.getValueArgument(i)?.replaced())
                        }
                    }
                }
            } ?: return super.visitCall(expression)
        }

        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            val classSpec = IrOriginToSpec.forClass(expression.type) ?: return super.visitConstructorCall(expression)
            return IrOriginToSpec.forConstructor(expression.symbol.owner, classSpec)?.let {
                context.createIrBuilder(expression.symbol).run {
                    irConstructorCall(delegate.visitConstructorCall(expression), it)
                }
            } ?: super.visitConstructorCall(expression)
        }

        override fun visitGetValue(expression: IrGetValue): IrExpression =
                IrOriginToSpec.forClass(expression.type)?.let {
                    delegate.visitGetValue(expression)
                } ?: super.visitGetValue(expression)

        override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
            return IrOriginToSpec.forClass(declaration.type)?.let {
                delegate.visitValueParameter(declaration).apply {
                    parent = declaration.parent
                    defaultValue = declaration.defaultValue?.replaced()
                }
            } ?: super.visitValueParameter(declaration)
        }

        override fun visitVariable(declaration: IrVariable): IrStatement =
                IrOriginToSpec.forClass(declaration.type)?.let {
                    delegate.visitVariable(declaration).apply {
                        parent = declaration.parent
                        initializer = declaration.initializer?.replaced()
                    }
                } ?: super.visitVariable(declaration)

        private inline fun <reified T : IrElement> T.replaced(): T = accept(this@Replacer, null) as T
    }

    private class SymbolRemapperForReplacer(
            val descriptorsRemapper: DescriptorsRemapper = DescriptorsRemapper.Default
    ) : IrElementVisitorVoid, SymbolRemapper {

        private val remappedVariables = mutableMapOf<IrValueSymbol, IrVariableSymbol>()
        private val remappedValueParameters = mutableMapOf<IrValueSymbol, IrValueParameterSymbol>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitValueParameter(declaration: IrValueParameter) {
            if (declaration.type in IrOriginToSpec) {
                remappedValueParameters[declaration.symbol] = IrValueParameterSymbolImpl(descriptorsRemapper.remapDeclaredValueParameter(declaration.descriptor))
            }
            super.visitValueParameter(declaration)
        }

        override fun visitVariable(declaration: IrVariable) {
            if (declaration.type in IrOriginToSpec) {
                remappedVariables[declaration.symbol] = IrVariableSymbolImpl(descriptorsRemapper.remapDeclaredVariable(declaration.descriptor))
            }
            super.visitVariable(declaration)
        }

        override fun getDeclaredClass(symbol: IrClassSymbol): IrClassSymbol = symbol
        override fun getDeclaredConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = symbol
        override fun getDeclaredEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = symbol
        override fun getDeclaredExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol): IrExternalPackageFragmentSymbol = symbol
        override fun getDeclaredField(symbol: IrFieldSymbol): IrFieldSymbol = symbol
        override fun getDeclaredFile(symbol: IrFileSymbol): IrFileSymbol = symbol
        override fun getDeclaredFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = symbol
        override fun getDeclaredLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol = symbol
        override fun getDeclaredProperty(symbol: IrPropertySymbol): IrPropertySymbol = symbol
        override fun getDeclaredTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAliasSymbol = symbol
        override fun getDeclaredTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameterSymbol = symbol
        override fun getDeclaredValueParameter(symbol: IrValueParameterSymbol): IrValueParameterSymbol = remappedValueParameters[symbol] ?: symbol
        override fun getDeclaredVariable(symbol: IrVariableSymbol): IrVariableSymbol = remappedVariables[symbol] ?: symbol

        override fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol = symbol
        override fun getReferencedClassOrNull(symbol: IrClassSymbol?): IrClassSymbol? = symbol
        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol = symbol
        override fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = symbol
        override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = symbol
        override fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol = symbol
        override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol = symbol
        override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol = symbol
        override fun getReferencedProperty(symbol: IrPropertySymbol): IrPropertySymbol = symbol
        override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol): IrReturnableBlockSymbol = symbol
        override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = symbol
        override fun getReferencedTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAliasSymbol = symbol
        override fun getReferencedValue(symbol: IrValueSymbol): IrValueSymbol = remappedVariables[symbol] ?: (remappedValueParameters[symbol] ?: symbol)
        override fun getReferencedVariable(symbol: IrVariableSymbol): IrVariableSymbol = remappedVariables[symbol] ?: symbol
    }

    private class TypeRemapperForReplacer : TypeRemapper {
        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        }

        override fun leaveScope() {
        }

        override fun remapType(type: IrType): IrType {
            return IrOriginToSpec.forClass(type) ?: type
        }
    }
}
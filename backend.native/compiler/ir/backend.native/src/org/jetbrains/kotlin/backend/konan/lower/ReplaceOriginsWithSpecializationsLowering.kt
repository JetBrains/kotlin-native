package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.IrOriginToSpec
import org.jetbrains.kotlin.backend.konan.boxing.SpecializationEncoder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class ReplaceOriginsWithSpecializationsLowering(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
//        irFile.transform(Replacer(), data = null)
    }

    private inner class Replacer(
            val symbolRemapper: SymbolRemapperForReplacer = SymbolRemapperForReplacer(),
            typeRemapper: TypeRemapper = TypeRemapperForReplacer(),
            val encoder: SpecializationEncoder = SpecializationEncoder(context)
    ) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper), NonCopyingTransformer {

        override fun visitFile(declaration: IrFile): IrFile {
            declaration.acceptVoid(symbolRemapper)
            return super<NonCopyingTransformer>.visitFile(declaration, data = null)
        }

        override fun visitClass(declaration: IrClass): IrClass {
            return super<NonCopyingTransformer>.visitClass(declaration, data = null) as IrClass
        }

        override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
            return super<NonCopyingTransformer>.visitDeclaration(declaration, data = null)
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrAnonymousInitializer {
            return super<NonCopyingTransformer>.visitAnonymousInitializer(declaration, data = null) as IrAnonymousInitializer
        }

        override fun visitBlock(expression: IrBlock): IrBlock {
            return super<NonCopyingTransformer>.visitBlock(expression, data = null) as IrBlock
        }

        override fun visitBlockBody(body: IrBlockBody): IrBlockBody {
            return super<NonCopyingTransformer>.visitBlockBody(body, data = null) as IrBlockBody
        }

        override fun visitBody(body: IrBody): IrBody {
            return super<NonCopyingTransformer>.visitBody(body, data = null)
        }

        override fun visitBranch(branch: IrBranch): IrBranch {
            return super<NonCopyingTransformer>.visitBranch(branch, data = null)
        }

        override fun visitBreak(jump: IrBreak): IrBreak {
            return super<NonCopyingTransformer>.visitBreak(jump, data = null) as IrBreak
        }

        override fun visitCatch(aCatch: IrCatch): IrCatch {
            return super<NonCopyingTransformer>.visitCatch(aCatch, data = null)
        }

        override fun visitClassReference(expression: IrClassReference): IrClassReference {
            return super<NonCopyingTransformer>.visitClassReference(expression, data = null) as IrClassReference
        }

        override fun visitComposite(expression: IrComposite): IrComposite {
            return super<NonCopyingTransformer>.visitComposite(expression, data = null) as IrComposite
        }

        override fun <T> visitConst(expression: IrConst<T>): IrConst<T> {
            @Suppress("UNCHECKED_CAST")
            return super<NonCopyingTransformer>.visitConst(expression, data = null) as IrConst<T>
        }

        override fun visitConstructor(declaration: IrConstructor): IrConstructor {
            return super<NonCopyingTransformer>.visitConstructor(declaration, data = null) as IrConstructor
        }

        override fun visitContinue(jump: IrContinue): IrContinue {
            return super<NonCopyingTransformer>.visitContinue(jump, data = null) as IrContinue
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrDelegatingConstructorCall {
            return super<NonCopyingTransformer>.visitDelegatingConstructorCall(expression, data = null) as IrDelegatingConstructorCall
        }

        override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrDoWhileLoop {
            return super<NonCopyingTransformer>.visitDoWhileLoop(loop, data = null) as IrDoWhileLoop
        }

        override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression): IrDynamicMemberExpression {
            return super<NonCopyingTransformer>.visitDynamicMemberExpression(expression, data = null) as IrDynamicMemberExpression
        }

        override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression): IrDynamicOperatorExpression {
            return super<NonCopyingTransformer>.visitDynamicOperatorExpression(expression, data = null) as IrDynamicOperatorExpression
        }

        override fun visitElement(element: IrElement): IrElement {
            return super<NonCopyingTransformer>.visitElement(element, data = null)
        }

        override fun visitElseBranch(branch: IrElseBranch): IrElseBranch {
            return super<NonCopyingTransformer>.visitElseBranch(branch, data = null)
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrEnumConstructorCall {
            return super<NonCopyingTransformer>.visitEnumConstructorCall(expression, data = null) as IrEnumConstructorCall
        }

        override fun visitEnumEntry(declaration: IrEnumEntry): IrEnumEntry {
            return super<NonCopyingTransformer>.visitEnumEntry(declaration, data = null) as IrEnumEntry
        }

        override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrErrorCallExpression {
            return super<NonCopyingTransformer>.visitErrorCallExpression(expression, data = null) as IrErrorCallExpression
        }

        override fun visitErrorDeclaration(declaration: IrErrorDeclaration): IrErrorDeclaration {
            return super<NonCopyingTransformer>.visitErrorDeclaration(declaration, data = null) as IrErrorDeclaration
        }

        override fun visitErrorExpression(expression: IrErrorExpression): IrErrorExpression {
            return super<NonCopyingTransformer>.visitErrorExpression(expression, data = null) as IrErrorExpression
        }

        override fun visitExpression(expression: IrExpression): IrExpression {
            return super<NonCopyingTransformer>.visitExpression(expression, data = null)
        }

        override fun visitExpressionBody(body: IrExpressionBody): IrExpressionBody {
            return super<NonCopyingTransformer>.visitExpressionBody(body, data = null) as IrExpressionBody
        }

        override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment {
            return super<NonCopyingTransformer>.visitExternalPackageFragment(declaration, data = null)
        }

        override fun visitField(declaration: IrField): IrField {
            return super<NonCopyingTransformer>.visitField(declaration, data = null) as IrField
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression): IrFunctionExpression {
            return super<NonCopyingTransformer>.visitFunctionExpression(expression, data = null) as IrFunctionExpression
        }

        override fun visitFunctionReference(expression: IrFunctionReference): IrFunctionReference {
            return super<NonCopyingTransformer>.visitFunctionReference(expression, data = null) as IrFunctionReference
        }

        override fun visitGetClass(expression: IrGetClass): IrGetClass {
            return super<NonCopyingTransformer>.visitGetClass(expression, data = null) as IrGetClass
        }

        override fun visitGetEnumValue(expression: IrGetEnumValue): IrGetEnumValue {
            return super<NonCopyingTransformer>.visitGetEnumValue(expression, data = null) as IrGetEnumValue
        }

        override fun visitGetField(expression: IrGetField): IrGetField {
            return super<NonCopyingTransformer>.visitGetField(expression, data = null) as IrGetField
        }

        override fun visitGetObjectValue(expression: IrGetObjectValue): IrGetObjectValue {
            return super<NonCopyingTransformer>.visitGetObjectValue(expression, data = null) as IrGetObjectValue
        }

        override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrInstanceInitializerCall {
            return super<NonCopyingTransformer>.visitInstanceInitializerCall(expression, data = null) as IrInstanceInitializerCall
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty {
            return super<NonCopyingTransformer>.visitLocalDelegatedProperty(declaration, data = null) as IrLocalDelegatedProperty
        }

        override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrLocalDelegatedPropertyReference {
            return super<NonCopyingTransformer>.visitLocalDelegatedPropertyReference(expression, data = null) as IrLocalDelegatedPropertyReference
        }

        override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
            return super<NonCopyingTransformer>.visitModuleFragment(declaration, data = null)
        }

        override fun visitProperty(declaration: IrProperty): IrProperty {
            return super<NonCopyingTransformer>.visitProperty(declaration, data = null) as IrProperty
        }

        override fun visitPropertyReference(expression: IrPropertyReference): IrPropertyReference {
            return super<NonCopyingTransformer>.visitPropertyReference(expression, data = null) as IrPropertyReference
        }

        override fun visitReturn(expression: IrReturn): IrReturn {
            return super<NonCopyingTransformer>.visitReturn(expression, data = null) as IrReturn
        }

        override fun visitScript(declaration: IrScript): IrStatement {
            return super<NonCopyingTransformer>.visitScript(declaration, data = null)
        }

        override fun visitSetField(expression: IrSetField): IrSetField {
            return super<NonCopyingTransformer>.visitSetField(expression, data = null) as IrSetField
        }

        override fun visitSetVariable(expression: IrSetVariable): IrSetVariable {
            return super<NonCopyingTransformer>.visitSetVariable(expression, data = null) as IrSetVariable
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
            return (super<NonCopyingTransformer>.visitSimpleFunction(declaration, data = null) as IrSimpleFunction)
        }

        override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement {
            return super<NonCopyingTransformer>.visitSpreadElement(spread, data = null)
        }

        override fun visitStringConcatenation(expression: IrStringConcatenation): IrStringConcatenation {
            return super<NonCopyingTransformer>.visitStringConcatenation(expression, data = null) as IrStringConcatenation
        }

        override fun visitSyntheticBody(body: IrSyntheticBody): IrSyntheticBody {
            return super<NonCopyingTransformer>.visitSyntheticBody(body, data = null) as IrSyntheticBody
        }

        override fun visitThrow(expression: IrThrow): IrThrow {
            return super<NonCopyingTransformer>.visitThrow(expression, data = null) as IrThrow
        }

        override fun visitTry(aTry: IrTry): IrTry {
            return super<NonCopyingTransformer>.visitTry(aTry, data = null) as IrTry
        }

        override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias {
            return super<NonCopyingTransformer>.visitTypeAlias(declaration, data = null) as IrTypeAlias
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall {
            return super<NonCopyingTransformer>.visitTypeOperator(expression, data = null) as IrTypeOperatorCall
        }

        override fun visitTypeParameter(declaration: IrTypeParameter): IrTypeParameter {
            return super<NonCopyingTransformer>.visitTypeParameter(declaration, data = null) as IrTypeParameter
        }

        override fun visitVararg(expression: IrVararg): IrVararg {
            return super<NonCopyingTransformer>.visitVararg(expression, data = null) as IrVararg
        }

        override fun visitWhen(expression: IrWhen): IrWhen {
            return super<NonCopyingTransformer>.visitWhen(expression, data = null) as IrWhen
        }

        override fun visitWhileLoop(loop: IrWhileLoop): IrWhileLoop {
            return super<NonCopyingTransformer>.visitWhileLoop(loop, data = null) as IrWhileLoop
        }

        override fun visitCall(expression: IrCall): IrCall {
            val origin = expression.symbol.owner
            val encodedFunctionName = encoder.encode(origin, expression.typeSubstitutionMap) ?: return super<NonCopyingTransformer>.visitCall(expression, data = null) as IrCall
            val dispatchReceiver = expression.dispatchReceiver?.replaced()
            val specialization = IrOriginToSpec.forFunction(origin, encoder.decode(encodedFunctionName))
                    ?: dispatchReceiver?.let { IrOriginToSpec.forMemberFunction(origin, it.type) }

            return specialization?.let {
                context.createIrBuilder(expression.symbol).run {
                    (expression.origin?.let { o -> irCall(it, o) } ?: irCall(it) as IrCall).also { call ->
                        call.extensionReceiver = expression.extensionReceiver?.replaced()
                        call.dispatchReceiver = dispatchReceiver
                        for (i in 0 until expression.valueArgumentsCount) {
                            call.putValueArgument(i, expression.getValueArgument(i)?.replaced())
                        }
                    }
                }
            } ?: return super<NonCopyingTransformer>.visitCall(expression, data = null) as IrCall
        }

        override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
            val classSpec = IrOriginToSpec.forClass(expression.type) ?: return super<NonCopyingTransformer>.visitConstructorCall(expression, data = null) as IrConstructorCall
            return IrOriginToSpec.forConstructor(expression.symbol.owner, classSpec)?.let {
                context.createIrBuilder(expression.symbol).run {
                    irConstructorCall(super<DeepCopyIrTreeWithSymbols>.visitConstructorCall(expression), it)
                }
            } ?: super<NonCopyingTransformer>.visitConstructorCall(expression, data = null) as IrConstructorCall
        }

        override fun visitGetValue(expression: IrGetValue): IrGetValue =
                IrOriginToSpec.forClass(expression.type)?.let {
                    super<DeepCopyIrTreeWithSymbols>.visitGetValue(expression)
                } ?: super<NonCopyingTransformer>.visitGetValue(expression, data = null) as IrGetValue

        override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter {
            return IrOriginToSpec.forClass(declaration.type)?.let {
                super<DeepCopyIrTreeWithSymbols>.visitValueParameter(declaration).apply {
                    parent = declaration.parent
//                    defaultValue = declaration.defaultValue?.replaced()
                }
            } ?: super<NonCopyingTransformer>.visitValueParameter(declaration, data = null) as IrValueParameter
        }

        override fun visitVariable(declaration: IrVariable): IrVariable =
                IrOriginToSpec.forClass(declaration.type)?.let {
                    super<DeepCopyIrTreeWithSymbols>.visitVariable(declaration).apply {
                        parent = declaration.parent
//                        initializer = declaration.initializer?.replaced()
                    }
                } ?: super<NonCopyingTransformer>.visitVariable(declaration, data = null) as IrVariable

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

interface NonCopyingTransformer : IrElementTransformer<Nothing?>
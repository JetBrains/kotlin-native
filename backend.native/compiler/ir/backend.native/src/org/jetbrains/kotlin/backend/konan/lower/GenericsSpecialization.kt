package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DescriptorsToIrRemapper
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.WrappedDescriptorPatcher
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.TypeParameterEliminator
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

internal class GenericsSpecialization(val context: Context) : FileLoweringPass {

    private val transformer = SpecializationTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transform(transformer, null)
    }
}

/**
 * Finds calls to generic functions with primitive type arguments and creates specialized versions
 * of these functions (i.e. copies of functions which have some type parameters erased and replaced
 * with concrete primitive types) to try to reduce extra boxings.
 *
 * It will reduce boxings in some cases. For example:
 *
 * ```
 * fun <T> foo(x: T) = x
 * ```
 *
 * Specialized version for type [Int] can be the following:
 *
 * ```
 * fun foo(x: Int) = x
 * ```
 *
 * Therefore, calls `foo(1)` or `foo(4103934)` will not require box their arguments.
 *
 * On the other hand, specialization for the following function:
 *
 * ```
 * fun <T> foo(x: T) {
 *     when (x) {
 *         is String -> {}
 *         is List<*> -> {}
 *         is Int -> {}
 *     }
 * }
 * ```
 *
 * and the type [Int] will require 1 box for each `is` call.
 * Generic function requires 1 box at all -- variable `x` must be boxed before the function will be called.
 * Therefore, specialization here can significantly increase total number of box operations.
 */
internal class SpecializationTransformer(val context: Context): IrBuildingTransformer(context) {

    private val currentSpecializations = mutableMapOf<Pair<IrFunction, IrType>, IrFunction>()

    // scope -> specialization -> origin
    private val newFunctions = mutableMapOf<IrDeclarationParent, MutableMap<IrFunction, IrFunction>>()

    private val typeParameterMapping = mutableMapOf<IrTypeParameterSymbol, IrType>()
    private val symbolRemapper = DeepCopySymbolRemapper(DescriptorsToIrRemapper)
    private val symbolRenamer = SymbolRenamerWithSpecializedFunctions()
    private val typeParameterEliminator = TypeParameterEliminator(this, symbolRemapper, symbolRenamer, typeParameterMapping)

    override fun visitFile(declaration: IrFile): IrFile {
        return super.visitFile(declaration).also {
            addNewFunctionsToIr(declaration)
            newFunctions.forEach { (scope, _) ->
                addNewFunctionsToIr(scope)
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration).also {
            addNewFunctionsToIr(declaration)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.typeArgumentsCount != 1 || expression.getTypeArgument(0) !in context.irBuiltIns.primitiveIrTypes) {
            return expression
        }
        val primitiveTypeArgument = expression.getTypeArgument(0)!!
        val owner: IrFunction = expression.symbol.owner
        if (owner.typeParameters.size != 1) {
            return expression
        }
        val typeParameter = owner.typeParameters.first()
        if (typeParameter.superTypes.size != 1 || typeParameter.superTypes.first() != context.irBuiltIns.anyNType || typeParameter.variance != Variance.INVARIANT) {
            return expression
        }
        val newFunction = owner.getSpecialization(primitiveTypeArgument)
        return builder.at(expression).run {
            irCall(newFunction).apply {
                extensionReceiver = expression.extensionReceiver
                dispatchReceiver = expression.dispatchReceiver
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i))
                }
            }
        }
    }

    // TODO generalize
    private fun IrFunction.getSpecialization(type: IrType): IrFunction {
        if (this !is IrSimpleFunction) {
            return this
        }
        val function = currentSpecializations[this to type]
        if (function != null) {
            return function
        }
        // Include concrete primitive type to the name of specialization
        // to be able to easily spot such functions and to avoid extra overloads
        symbolRenamer.addNaming(this, "${nameForIrSerialization}-${type.toKotlinType()}")

        // Add remapping for all elements of original function to automatically assign new descriptors
        // to elements of new function
        acceptVoid(symbolRemapper)

        // Remember type parameter to be able to eliminate it in all possible places
        // TODO works only for functions with one type parameter and (likely) when specialization just for one type is required
        typeParameterMapping[typeParameters.first().symbol] = type

        return typeParameterEliminator.visitSimpleFunction(this).also {
            // Bind descriptors to their owners, assign parents
            it.acceptVoid(SafeWrappedDescriptorPatcher)
            it.patchDeclarationParents(this)
            currentSpecializations[this to type] = it
            newFunctions.getOrPut(parent) { mutableMapOf() }[it] = this
        }
    }

    /**
     * Adds new functions to given [parent]. This function must not be called
     * when visiting members inside the scope of [parent],
     * otherwise [ConcurrentModificationException] may be thrown.
     */
    private fun addNewFunctionsToIr(parent: IrDeclarationParent) {
        newFunctions[parent]?.forEach { (specialization, origin) ->
            when (val scope = origin.parent) {
                is IrDeclarationContainer -> scope.addChild(specialization)
                is IrStatementContainer -> {
                    val originIndex = scope.statements.indexOf(origin)
                    scope.statements.add(originIndex + 1, specialization)
                    specialization.parent = scope
                }
                is IrFunction -> {
                    val builder = context.createIrBuilder(scope.symbol)
                    val statements = scope.body?.statements?.toMutableList()
                    if (statements == null) {
                        scope.body = builder.irBlockBody {
                            +specialization
                        }
                    } else {
                        statements.add(statements.indexOf(origin), specialization)
                        specialization.parent = scope
                    }
                    scope.body = builder.irBlockBody(scope) {
                        statements?.forEach {
                            +it
                        }
                    }
                }
                else -> TODO()
            }
        }
        newFunctions[parent]?.clear()
    }

    private class SymbolRenamerWithSpecializedFunctions : SymbolRenamer {
        private val newFunctionNaming =  mutableMapOf<IrSimpleFunction, String>()

        fun addNaming(function: IrSimpleFunction, name: String) {
            newFunctionNaming[function] = name
        }

        override fun getFunctionName(symbol: IrSimpleFunctionSymbol): Name {
            return newFunctionNaming[symbol.owner]?.let { Name.identifier(it) } ?: super.getFunctionName(symbol)
        }
    }
}

/**
 * Copy of [WrappedDescriptorPatcher] with guarantees that descriptor will not be bound
 * to any declaration more than once.
 *
 * This guarantee is necessary in current algorithm because it is recursive
 * and some information about ownership must be known at the stage
 * when new function is not fully created.
 * Without it, tracking exact place where particular descriptor must be bound
 * can be tricky and require a lot of boilerplate code.
 */
internal object SafeWrappedDescriptorPatcher : IrElementVisitorVoid {

    private val patchedElements = mutableSetOf<IrElement>()

    private fun <E : IrElement> E.addAndRunIfAbsent(action: () -> Unit) {
        if (this in patchedElements) return
        action()
        patchedElements.add(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedClassDescriptor).bind(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedClassConstructorDescriptor).bind(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedClassDescriptor).bind(
                    declaration.correspondingClass ?: declaration.parentAsClass
            )
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitField(declaration: IrField) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedFieldDescriptor).bind(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedPropertyDescriptor).bind(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedSimpleFunctionDescriptor).bind(declaration as IrSimpleFunction)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as? WrappedValueParameterDescriptor)?.bind(declaration)
            (declaration.descriptor as? WrappedReceiverParameterDescriptor)?.bind(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedTypeParameterDescriptor).bind(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.addAndRunIfAbsent {
            (declaration.descriptor as WrappedVariableDescriptor).bind(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }
}
package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.ir.classIfConstructor
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.lower.DeepCopyIrTreeWithSymbolsForInliner
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

// Expected that transformer will not change entities of given IR elements, only internals,
// i.e. for receiver of type T it will return an element of type T.
@Suppress("UNCHECKED_CAST")
internal fun <T : IrElement> T.eliminateTypeParameters(transformer: NewTypeParameterEliminator): T = transformer.copy(this) as T

/**
 * Replaces certain type parameters with concrete types. Type parameters that must be removed
 * stored in [globalTypeParameterMapping], where each parameter is associated with corresponding concrete type.
 *
 * This class reuses copy algorithms for inliner (@see [DeepCopyIrTreeWithSymbolsForInliner]),
 * because inliner does the same work (it must replace type parameters too).
 *
 * But it has two differences.
 *
 * First, it provides its own [SymbolRenamer] to add concrete type names to the names of generated functions.
 *
 * Second, it handles constructors not in declaration order, but "more primary" first.
 * I.e. dependent constructor will be handled and copied after the constructor it depends.
 */
internal class NewTypeParameterEliminator(private val globalTypeParameterMapping: MutableMap<IrTypeParameterSymbol, IrType>,
                                          private val localTypeParameterMapping: Map<IrTypeParameterSymbol, IrType?>,
                                          private val encoder: SpecializationEncoder,
                                          context: Context,
                                          parent: IrDeclarationParent)
    : DeepCopyIrTreeWithSymbolsForInliner(context, globalTypeParameterMapping, parent) {

    private val specializingClasses = mutableSetOf<IrClass>()

    override fun copy(irElement: IrElement): IrElement {
        if (irElement is IrClass) {
            specializingClasses += irElement
        }
        localTypeParameterMapping.forEach { (symbol, type) ->
            if (type != null) {
                globalTypeParameterMapping[symbol] = type
            }
        }
        val result = super.copy(irElement)
        copier.evaluateDeferredMembers()
        localTypeParameterMapping.forEach { (symbol, type) ->
            if (type != null) {
                globalTypeParameterMapping.remove(symbol)
            }
        }
        return result
    }

    private val newFunctionNames = mutableMapOf<IrSymbol, Name>()
    private val newClassNames = mutableMapOf<IrSymbol, Name>()
    fun <T : IrTypeParametersContainer> addNewDeclarationName(declaration: T, name: String) {
        when (declaration) {
            is IrClass -> newClassNames[declaration.symbol] = Name.identifier(name)
            is IrFunction -> newFunctionNames[declaration.symbol] = Name.identifier(name)
        }
    }

    private inner class TypeParameterEliminatorSymbolRenamer : SymbolRenamer {
        override fun getClassName(symbol: IrClassSymbol) = newClassNames[symbol] ?: symbol.owner.name
        override fun getFunctionName(symbol: IrSimpleFunctionSymbol) = newFunctionNames[symbol] ?: symbol.owner.name
        override fun getFieldName(symbol: IrFieldSymbol) = symbol.owner.name
        override fun getFileName(symbol: IrFileSymbol) = symbol.owner.fqName
        override fun getExternalPackageFragmentName(symbol: IrExternalPackageFragmentSymbol) = symbol.owner.fqName
        override fun getEnumEntryName(symbol: IrEnumEntrySymbol) = symbol.owner.name
        override fun getVariableName(symbol: IrVariableSymbol) = symbol.owner.name
        override fun getTypeParameterName(symbol: IrTypeParameterSymbol) = symbol.owner.name
        override fun getValueParameterName(symbol: IrValueParameterSymbol) = symbol.owner.name
    }

    /*
     * Removes obsolete type arguments from types that refer to specialized declarations
     * (e.g. type arguments in `this` type and constructors return types).
     */
    private inner class EliminatorTypeRemapper(symbolRemapper: SymbolRemapper) : InlinerTypeRemapper(symbolRemapper, globalTypeParameterMapping) {
        override fun remapType(type: IrType): IrType {
            val newType = super.remapType(type)
            if (newType !is IrSimpleType || newType.classOrNull?.isBound != true) return newType

            val newTypeOwner = newType.classOrNull?.owner!!
            if (type.classOrNull?.owner in specializingClasses) {
                val types = encoder.decode(newTypeOwner.nameForIrSerialization.asString())
                return IrSimpleTypeImpl(
                        newType.classifier,
                        newType.hasQuestionMark,
                        remapTypeArguments(newType.arguments, types),
                        newType.annotations,
                        newType.abbreviation
                )
            }
            return newType
        }

        private fun remapTypeArguments(arguments: List<IrTypeArgument>, types: List<IrType?>): List<IrTypeArgument> {
            require(arguments.size == types.size) {
                "Expected ${arguments.size} elements but specialization has ${types.size}"
            }
            return arguments.filterIndexed { index, _ -> types[index] == null }
                    .map { argument ->
                        (argument as? IrTypeProjection)?.let { makeTypeProjection(remapType(it.type), it.variance) }
                                ?: argument
                    }
        }
    }

    internal inner class EliminatingCopier : DeepCopyIrTreeWithSymbols(
            symbolRemapper,
            EliminatorTypeRemapper(symbolRemapper),
            TypeParameterEliminatorSymbolRenamer()
    ) {
        private val constructorsCopier = ConstructorsCopier()
        private val deferredMembers = mutableListOf<Pair<IrDeclaration, Triple<List<IrType?>, () -> IrType?, IrDeclaration>>>()

        fun evaluateDeferredMembers() {
            deferredMembers.forEach { (origin, triple) ->
                IrOriginToSpec.newSpec(origin, triple.first, triple.second(), triple.third)
            }
        }

        // Assumed that classes do not need specified dispatchers to store.
        // TODO consider this in case if failures, maybe use parent instead of dispatcher
        override fun visitClass(declaration: IrClass): IrClass {
            constructorsCopier.prepare(declaration)
            return super.visitClass(declaration).withEliminatedTypeParameters(declaration).also {
                val types = localTypeParameterMapping.values.toList()
                deferredMembers += declaration to Triple(types, { null }, it)
            }
        }

        override fun visitConstructor(declaration: IrConstructor): IrConstructor {
            return constructorsCopier.visitConstructor(declaration).also {
                val specDispatchType = it.dispatchReceiverParameter?.type
                val types = localTypeParameterMapping.values.toList()
                deferredMembers += declaration to Triple(types, { specDispatchType }, it)
            }
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
            return super.visitSimpleFunction(declaration).withEliminatedTypeParameters(declaration).also {
                it.saveAsSpecialized(declaration)
            }
        }

        private fun IrSimpleFunction.saveAsSpecialized(originalFunction: IrSimpleFunction) {
            val types = encoder.decode(name.asString())
            val specDispatchType = dispatchReceiverParameter?.type
            deferredMembers += originalFunction to Triple(types, { specDispatchType }, this)

            // Assumed that non-encoded function name can belong only to non-specialized variant,
            // and hence it has correct overriding methods info.
            // TODO consider this condition when errors with inconsistent methods table will occur.
            if (types.isNotEmpty()) {
                overriddenSymbols.replaceAll {
                    IrOriginToSpec.forSpec(it.owner, types, it.owner.dispatchReceiverParameter?.type)?.symbol
                            ?: it
                }
            }
        }

        private fun <E : IrTypeParametersContainer> E.withEliminatedTypeParameters(declaration: E): E = apply {
            require(declaration.typeParameters.size == typeParameters.size) {
                "Must be called only for origin and its copy," +
                        " but origin has ${declaration.typeParameters.size} type parameters," +
                        " and copy has ${typeParameters.size}"
            }
            val typeParametersToEliminate = typeParameters.filterIndexed { index, _ ->
                declaration.typeParameters[index].symbol in globalTypeParameterMapping
            }
            typeParameters.removeAll(typeParametersToEliminate)
        }

        private inner class ConstructorsCopier {
            private val constructorsInDelegationOrder = mutableListOf<IrConstructor>()
            private val copiedConstructors = mutableMapOf<IrConstructor, IrConstructor>()

            fun prepare(declaration: IrClass) {
                with (constructorsInDelegationOrder) {
                    clear()
                    addAll(NewConstructorDelegationPartialOrder(declaration).sort())
                    copiedConstructors.clear()
                }
            }

            fun visitConstructor(oldConstructor: IrConstructor): IrConstructor {
                if (copiedConstructors.isEmpty()) {
                    copiedConstructors.putAll(constructorsInDelegationOrder.map {
                        it to super@EliminatingCopier.visitConstructor(it).also { ctor ->
                            (ctor.descriptor as WrappedClassConstructorDescriptor).bind(ctor)
                        }
                    })
                }
                return copiedConstructors[oldConstructor]!!
            }
        }
    }

    override val copier = EliminatingCopier()
}

class NewConstructorDelegationPartialOrder(val clazz: IrClass) {
    private val constructorsAndDelegates = clazz.constructors
            .map {
                it to it.body?.statements?.filterIsInstance<IrDelegatingConstructorCall>()?.single()?.symbol?.owner
            }
            .toMap()

    fun sort(): List<IrConstructor> {
        if (constructorsAndDelegates.isEmpty()) {
            return emptyList()
        }

        class Node(val constructor: IrConstructor, val dependentConstructors: MutableList<Node> = mutableListOf())

        // Breadth-first traversal of dependency tree gives the right order of copying (delegate before)
        fun traverse(root: Node): List<IrConstructor> {
            val queue = LinkedList<Node>()
            queue.addAll(root.dependentConstructors)
            val result = mutableListOf<IrConstructor>()
            while (queue.isNotEmpty()) {
                val current = queue.pop()!!
                result += current.constructor
                queue.addAll(current.dependentConstructors)
            }
            return result
        }

        val nodes = constructorsAndDelegates.keys.keysToMap { Node(it) }.toMutableMap()

        // Main constructors are the ones that do not call another constructors
        // defined in the same class. They can be either without delegates
        // or with delegation to superclass constructor.
        val mainConstructors = mutableListOf<IrConstructor>()
        constructorsAndDelegates.forEach { (constructor, delegate) ->
            if (constructor.classIfConstructor === delegate?.classIfConstructor) {
                nodes[delegate]?.dependentConstructors?.add(nodes[constructor]!!)
            } else {
                mainConstructors += constructor
            }
        }

        assert(mainConstructors.isNotEmpty()) { "There must be at least one main constructor ${clazz.dump()}" }

        // Root is fake; first parameter is dummy and must not be used anywhere
        val root = Node(mainConstructors.first(), mainConstructors.map { nodes[it]!! }.toMutableList())
        return traverse(root)
    }
}
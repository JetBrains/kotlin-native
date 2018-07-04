/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.descriptors.substitute
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.TypeUtils
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingStrategy
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinType
import java.lang.reflect.Proxy

//TODO: delete file on next kotlin dependency update
internal fun IrExpression.isNullConst() = this is IrConst<*> && this.kind == IrConstKind.Null

private var topLevelInitializersCounter = 0

internal fun IrFile.addTopLevelInitializer(expression: IrExpression, context: CommonBackendContext) {
    val fieldDescriptor = PropertyDescriptorImpl.create(
            this.packageFragmentDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PRIVATE,
            false,
            "topLevelInitializer${topLevelInitializersCounter++}".synthesizedName,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false,
            false,
            false,
            false,
            false,
            false
    )

    val builtIns = fieldDescriptor.builtIns
    fieldDescriptor.setType(builtIns.unitType, emptyList(), null, null as KotlinType?)
    fieldDescriptor.initialize(null, null)

    val irField = IrFieldImpl(
            expression.startOffset, expression.endOffset,
            IrDeclarationOrigin.DEFINED, fieldDescriptor, context.irBuiltIns.unitType
    )

    val initializer = IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset, context.irBuiltIns.unitType,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, context.irBuiltIns.unitType, context.ir.symbols.unit, expression
    )

    irField.initializer = IrExpressionBodyImpl(expression.startOffset, expression.endOffset, initializer)

    this.addChild(irField)
}

fun IrClass.addFakeOverrides(symbolTable: SymbolTable) {

    val startOffset = this.startOffset
    val endOffset = this.endOffset

    descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            .forEach {
                this.addChild(createFakeOverride(it, startOffset, endOffset, symbolTable))
            }
}

private fun createFakeOverride(
        descriptor: CallableMemberDescriptor,
        overriddenDeclarations: List<IrDeclaration>,
        irClass: IrClass
): IrDeclaration {

    fun IrSimpleFunction.copyFake(descriptor: FunctionDescriptor): IrSimpleFunction = IrFunctionImpl(
            irClass.startOffset, irClass.endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, descriptor
    ).also {
        it.returnType = returnType

        it.createDispatchReceiverParameter(irClass)

        it.extensionReceiverParameter = this.extensionReceiverParameter?.let {
            IrValueParameterImpl(
                    it.startOffset,
                    it.endOffset,
                    IrDeclarationOrigin.DEFINED,
                    it.descriptor.extensionReceiverParameter!!,
                    it.type, // FIXME: substitute
                    null
            )
        }

        this.valueParameters.mapTo(it.valueParameters) { oldParameter ->
            // FIXME: substitute
            oldParameter.copyAsValueParameter(it.descriptor.valueParameters[oldParameter.index])
        }

        this.typeParameters.mapTo(it.typeParameters) { oldParameter ->
            IrTypeParameterImpl(
                    irClass.startOffset,
                    irClass.endOffset,
                    IrDeclarationOrigin.DEFINED,
                    it.descriptor.typeParameters[oldParameter.index]
            ).apply {
                superTypes += oldParameter.upperBounds // FIXME: substitute
            }
        }
    }

    val copiedDeclaration = overriddenDeclarations.first()

    return when (copiedDeclaration) {
        is IrSimpleFunction -> copiedDeclaration.copyFake(descriptor as FunctionDescriptor)
        is IrProperty -> IrPropertyImpl(irClass.startOffset, irClass.endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, descriptor as PropertyDescriptor).apply {
            getter = copiedDeclaration.getter?.copyFake(descriptor.getter!!)
            setter = copiedDeclaration.setter?.copyFake(descriptor.setter!!)
        }
        else -> error(copiedDeclaration)
    }
}

private fun createFakeOverride(descriptor: CallableMemberDescriptor, startOffset: Int, endOffset: Int, symbolTable: SymbolTable): IrDeclaration {

    fun FunctionDescriptor.createFunction(): IrSimpleFunction = IrFunctionImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.FAKE_OVERRIDE, this
    ).apply {
        returnType = symbolTable.translateErased(this@createFunction.returnType!!) // FIXME
        createParameterDeclarations(symbolTable)
    }

    return when (descriptor) {
        is FunctionDescriptor -> descriptor.createFunction()
        is PropertyDescriptor ->
            IrPropertyImpl(
                    startOffset,
                    endOffset,
                    IrDeclarationOrigin.FAKE_OVERRIDE,
                    descriptor
            ).apply {
                // TODO: add field if getter is missing?
                getter = descriptor.getter?.createFunction() as IrSimpleFunction?
                setter = descriptor.setter?.createFunction() as IrSimpleFunction?
            }
        else -> TODO(descriptor.toString())
    }
}

fun IrFunction.createParameterDeclarations(symbolTable: SymbolTable) {

    assert(typeParameters.isEmpty())
    descriptor.typeParameters.mapTo(typeParameters) {
        IrTypeParameterImpl(
                innerStartOffset(it), innerEndOffset(it),
                IrDeclarationOrigin.DEFINED,
                it
        ).also { typeParameter ->
            typeParameter.parent = this
        }
    }

    val typeTranslator = TypeTranslator(this.descriptor.module, symbolTable)

    fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            innerStartOffset(this), innerEndOffset(this),
            IrDeclarationOrigin.DEFINED,
            this, symbolTable.translateErased(this.type), // FIXME: erased
            (this as? ValueParameterDescriptor)?.varargElementType?.let { symbolTable.translateErased(it) }
    ).also {
        it.parent = this@createParameterDeclarations
    }

    //typeTranslator.buildWithScope(this) {
        this.typeParameters.forEach {
            it.descriptor.upperBounds.mapTo(it.upperBounds, symbolTable::translateErased)
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty())
        descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }
    //}
}


fun IrSimpleFunction.setOverrides(symbolTable: SymbolTable) {
    assert(this.overriddenSymbols.isEmpty())

    this.descriptor.overriddenDescriptors.mapTo(this.overriddenSymbols) {
        symbolTable.referenceSimpleFunction(it.original)
    }
}

fun IrClass.simpleFunctions(): List<IrSimpleFunction> = this.declarations.flatMap {
    when (it) {
        is IrSimpleFunction -> listOf(it)
        is IrProperty -> listOfNotNull(it.getter as IrSimpleFunction?, it.setter as IrSimpleFunction?)
        else -> emptyList()
    }
}

fun IrClass.createParameterDeclarations() {
    thisReceiver = IrValueParameterImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.INSTANCE_RECEIVER,
            descriptor.thisAsReceiverParameter, this.symbol.defaultType, null
    ).also { valueParameter ->
        valueParameter.parent = this
    }

    assert(typeParameters.isEmpty())
    assert(descriptor.declaredTypeParameters.isEmpty())
}

fun IrFunction.createDispatchReceiverParameter(parent: IrClass) {
    assert(this.dispatchReceiverParameter == null)

    val descriptor = this.descriptor.dispatchReceiverParameter ?: return

    this.dispatchReceiverParameter = IrValueParameterImpl(
            startOffset,
            endOffset,
            IrDeclarationOrigin.DEFINED,
            descriptor,
            parent.symbol.defaultType,
            null
    ).also { it.parent = this }
}

private fun IrDeclaration.createTypeTranslator(symbolTable: SymbolTable): TypeTranslator {
    val result = TypeTranslator(this.descriptor.module, symbolTable)
    generateSequence(this, { it.parent as? IrDeclaration })
            .filterIsInstance<IrTypeParametersContainer>().toList().reversed()
            .forEach { result.enterScope(it) }

    return result
}

fun IrClass.createParameterDeclarations(symbolTable: SymbolTable) {
    this.thisReceiver = IrValueParameterImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.DEFINED,
            this.descriptor.thisAsReceiverParameter,
            this.symbol.defaultType,
            null
    )

    this.descriptor.declaredTypeParameters.mapTo(this.typeParameters) {
        IrTypeParameterImpl(startOffset, endOffset, IrDeclarationOrigin.DEFINED, it).apply {
            it.upperBounds.mapTo(this.superTypes, symbolTable::translateErased)
        }
    }
}

fun IrClass.setSuperSymbols(supers: List<IrClass>) {
    assert(this.superDescriptors().toSet() == supers.map { it.descriptor }.toSet())
    assert(this.superTypes.isEmpty())
    supers.mapTo(this.superTypes) { it.symbol.defaultType } // FIXME: substitute.

    println("### setSuperSymbols for $this ${this.name}")

    println("### supers:")
    supers.forEach { println(it.name) }

    val superMembers = supers.flatMap {
        it.simpleFunctions()
    }.associateBy {
        it.descriptor
    }

    println("### superMembers:")
    superMembers.forEach {
        println("${it.key} -> ${it.value}")
    }

    println("### simpleFunctions:")
    this.simpleFunctions().forEach {
        assert(it.overriddenSymbols.isEmpty())

        println("   member: ${it.name}")

        it.descriptor.overriddenDescriptors.mapTo(it.overriddenSymbols) {
            println("   overridden by ${it.name}")
            val superMember = superMembers[it.original] ?: error(it.original)
            superMember.symbol
        }
    }
}

private fun IrClass.superDescriptors() =
        this.descriptor.typeConstructor.supertypes.map { it.constructor.declarationDescriptor as ClassDescriptor }

fun IrClass.setSuperSymbols(symbolTable: SymbolTable) {
    assert(this.superTypes.isEmpty())
    val typeTranslator = TypeTranslator(this.descriptor.module, symbolTable)
    this.descriptor.typeConstructor.supertypes.mapTo(this.superTypes) { typeTranslator.translateType(it) }

    this.simpleFunctions().forEach {
        it.setOverrides(symbolTable)
    }
}

fun IrClass.setSuperSymbolsAndAddFakeOverrides(supers: List<IrClass>) {
    val overriddenSuperMembers = this.declarations.map { it.descriptor }
            .filterIsInstance<CallableMemberDescriptor>().flatMap { it.overriddenDescriptors.map { it.original } }.toSet()

    val unoverriddenSuperMembers = supers.flatMap {
        it.declarations.filter { it.descriptor !in overriddenSuperMembers }.mapNotNull {
            when (it) {
                is IrSimpleFunction -> it.descriptor to it
                is IrProperty -> it.descriptor to it
                else -> null
            }
        }
    }.toMap()

    println("### overriddenSuperMembers:")
    overriddenSuperMembers.forEach {
        println("   $it")
    }

    println("### unoverriddenSuperMembers:")
    unoverriddenSuperMembers.forEach {
        println("   ${it.key} -> ${it.value.name}")
    }

    val irClass = this

    val overridingStrategy = object : OverridingStrategy() {
        override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
            val overriddenDeclarations =
                    fakeOverride.overriddenDescriptors.map { unoverriddenSuperMembers[it]!! }

            assert(overriddenDeclarations.isNotEmpty())

            irClass.addChild(createFakeOverride(fakeOverride, overriddenDeclarations, this@setSuperSymbolsAndAddFakeOverrides))
        }

        override fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
            error("inheritance conflict in synthesized class ${irClass.descriptor}:\n  $first\n  $second")
        }

        override fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
            error("override conflict in synthesized class ${irClass.descriptor}:\n  $fromSuper\n  $fromCurrent")
        }
    }

    unoverriddenSuperMembers.keys.groupBy { it.name }.forEach { (name, members) ->
        OverridingUtil.generateOverridesInFunctionGroup(
                name,
                members,
                emptyList(),
                this.descriptor,
                overridingStrategy
        )
    }

    this.setSuperSymbols(supers)
}

private fun IrElement.innerStartOffset(descriptor: DeclarationDescriptorWithSource): Int =
        descriptor.startOffset ?: this.startOffset

private fun IrElement.innerEndOffset(descriptor: DeclarationDescriptorWithSource): Int =
        descriptor.endOffset ?: this.endOffset

inline fun <reified T> stub(name: String): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) {
        _ /* proxy */, method, _ /* methodArgs */ ->
        if (method.name == "toString" && method.parameterCount == 0) {
            "${T::class.simpleName} stub for $name"
        } else {
            error("${T::class.simpleName}.${method.name} is not supported for $name")
        }
    } as T
}

fun IrDeclarationContainer.addChildren(declarations: List<IrDeclaration>) {
    declarations.forEach { this.addChild(it) }
}

fun IrDeclarationContainer.addChild(declaration: IrDeclaration) {
    this.declarations += declaration
    declaration.accept(SetDeclarationsParentVisitor, this)
}

object SetDeclarationsParentVisitor : IrElementVisitor<Unit, IrDeclarationParent> {
    override fun visitElement(element: IrElement, data: IrDeclarationParent) {
        if (element !is IrDeclarationParent) {
            element.acceptChildren(this, data)
        }
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent) {
        declaration.parent = data
        super.visitDeclaration(declaration, data)
    }
}

fun IrModuleFragment.checkDeclarationParents() {
    this.accept(CheckDeclarationParentsVisitor, null)
    this.dependencyModules.forEach { dependencyModule ->
        dependencyModule.externalPackageFragments.forEach {
            it.accept(CheckDeclarationParentsVisitor, null)
        }
    }
}

object CheckDeclarationParentsVisitor : IrElementVisitor<Unit, IrDeclarationParent?> {

    override fun visitElement(element: IrElement, data: IrDeclarationParent?) {
        element.acceptChildren(this, element as? IrDeclarationParent ?: data)
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent?) {
        if (declaration !is IrVariable && declaration !is IrValueParameter && declaration !is IrTypeParameter) {
            checkParent(declaration, data)
        } else {
            // Don't check IrVariable parent.
        }

        super.visitDeclaration(declaration, data)
    }

    private fun checkParent(declaration: IrDeclaration, expectedParent: IrDeclarationParent?) {
        val parent = try {
            declaration.parent
        } catch (e: Throwable) {
            error("$declaration for ${declaration.descriptor} has no parent")
        }

        if (parent != expectedParent) {
            error("$declaration for ${declaration.descriptor} has unexpected parent $parent")
        }
    }
}

tailrec fun IrDeclaration.getContainingFile(): IrFile? {
    val parent = this.parent

    return when (parent) {
        is IrFile -> parent
        is IrDeclaration -> parent.getContainingFile()
        else -> null
    }
}

internal fun KonanBackendContext.report(declaration: IrDeclaration, message: String, isError: Boolean) {
    val irFile = declaration.getContainingFile()
    this.report(
            declaration,
            irFile,
            if (irFile != null) {
                message
            } else {
                val renderer = org.jetbrains.kotlin.renderer.DescriptorRenderer.COMPACT_WITH_SHORT_TYPES
                "$message\n${renderer.render(declaration.descriptor)}"
            },
            isError
    )
    if (isError) throw KonanCompilationException()
}

fun IrBuilderWithScope.irForceNotNull(expression: IrExpression): IrExpression {
    if (!TypeUtils.isNullableType(expression.type)) {
        return expression
    }

    return irBlock {
        val temporary = irTemporaryVar(expression)
        +irIfNull(
                expression.type,
                subject = irGet(temporary),
                thenPart = irThrowNpe(IrStatementOrigin.EXCLEXCL),
                elsePart = irGet(temporary)
        )
    }
}

fun IrFunctionAccessExpression.addArguments(args: Map<IrValueParameter, IrExpression>) {
    function.dispatchReceiverParameter?.let {
        val arg = args[it]
        if (arg != null) {
            this.dispatchReceiver = arg
        }
    }

    function.extensionReceiverParameter?.let {
        val arg = args[it]
        if (arg != null) {
            this.extensionReceiver = arg
        }
    }

    function.valueParameters.forEach {
        val arg = args[it]
        if (arg != null) {
            this.putValueArgument(it.index, arg)
        }
    }
}

private fun FunctionDescriptor.substitute(
        typeArguments: List<IrType>
): FunctionDescriptor = this.substitute(*typeArguments.map { it.toKotlinType() }.toTypedArray())

private fun IrFunction.substitutedReturnType(typeArguments: List<IrType>): IrType = this.returnType // FIXME

fun IrBuilderWithScope.irCall(symbol: IrFunctionSymbol, typeArguments: List<IrType> = emptyList()) =
        // FIXME: doesn't seem to be correct
        // FIXME: callers should call irCall(symbol.owner, ) instead.
        irCall(symbol.owner, typeArguments)

fun IrType.toKotlinType(): KotlinType = when (this) {
    is IrSimpleType -> {
        val classifier = this.classifier.descriptor
        val arguments = this.arguments.mapIndexed { index, it ->
            when (it) {
                is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
                is IrStarProjection -> StarProjectionImpl((classifier as ClassDescriptor).declaredTypeParameters[index])
                else -> error(it)
            }
        }

        classifier.defaultType.replace(newArguments = arguments).makeNullableAsSpecified(this.hasQuestionMark)
    }
    else -> TODO(this.toString())
}

// FIXME: optimize the empty args case.
fun IrBuilderWithScope.irCall(
        irFunction: IrFunction,
        typeArguments: List<IrType> = emptyList()
): IrCall = IrCallImpl(
        startOffset, endOffset, irFunction.substitutedReturnType(typeArguments),
        irFunction.symbol, irFunction.descriptor.substitute(typeArguments), typeArguments.size
).apply {
    typeArguments.forEachIndexed { index, irType ->
        this.putTypeArgument(index, irType)
    }
}

// FIXME: optimize the empty args case.
fun IrBuilderWithScope.irCall(
        irFunction: IrFunctionSymbol,
        type: IrType,
        typeArguments: List<IrType> = emptyList()
): IrCall = IrCallImpl(
        startOffset, endOffset, type,
        irFunction, irFunction.descriptor.substitute(typeArguments), typeArguments.size
).apply {
    typeArguments.forEachIndexed { index, irType ->
        this.putTypeArgument(index, irType)
    }
}

fun IrBuilderWithScope.irCallOp(
        callee: IrFunction,
        dispatchReceiver: IrExpression,
        argument: IrExpression
): IrCall =
        irCall(callee.symbol, callee.returnType).apply {
            this.dispatchReceiver = dispatchReceiver
            putValueArgument(0, argument)
        }

val IrFunction.explicitIrParameters: List<IrValueParameter>
    get() = (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters)

fun IrBuilderWithScope.irGetField(receiver: IrExpression?, irField: IrField) =
        IrGetFieldImpl(startOffset, endOffset, irField.symbol, irField.type, receiver)

fun IrBuilderWithScope.irSetVar(variable: IrVariable, value: IrExpression) =
        irSetVar(variable.symbol, value)

fun IrBuilderWithScope.irSetField(receiver: IrExpression, irField: IrField, value: IrExpression): IrExpression =
        IrSetFieldImpl(
                startOffset,
                endOffset,
                irField.symbol,
                receiver = receiver,
                value = value,
                type = context.irBuiltIns.unitType
        )

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrMemberAccessExpression.getArgumentsWithIr(): List<Pair<IrValueParameter, IrExpression>> {
    val res = mutableListOf<Pair<IrValueParameter, IrExpression>>()
    val irFunction = when (this) {
        is IrFunctionAccessExpression -> this.symbol.owner
        is IrFunctionReference -> this.symbol.owner
        else -> error(this)
    }

    dispatchReceiver?.let {
        res += (irFunction.dispatchReceiverParameter!! to it)
    }

    extensionReceiver?.let {
        res += (irFunction.extensionReceiverParameter!! to it)
    }

    irFunction.valueParameters.forEachIndexed { index, it ->
        val arg = getValueArgument(index)
        if (arg != null) {
            res += (it to arg)
        }
    }

    return res
}

fun IrValueDeclaration.copyAsValueParameter(newDescriptor: ParameterDescriptor): IrValueParameter {
//    assert(this.descriptor.type == newDescriptor.type)
    // FIXME: set parents

    return IrValueParameterImpl(
            startOffset,
            endOffset,
            IrDeclarationOrigin.DEFINED,
            newDescriptor,
            type,
            (this as? IrValueParameter)?.varargElementType
    )
}

fun CallableMemberDescriptor.createValueParameter(
        index: Int,
        name: String,
        type: IrType,
        startOffset: Int,
        endOffset: Int
): IrValueParameter {
    val descriptor = ValueParameterDescriptorImpl(
            this, null,
            index,
            Annotations.EMPTY,
            Name.identifier(name),
            type.toKotlinType(),
            false, false, false, null, SourceElement.NO_SOURCE
    )

    return IrValueParameterImpl(startOffset, endOffset, IrDeclarationOrigin.DEFINED, descriptor, type, null)
}

private tailrec fun KotlinType.getErasedTypeClass(): ClassDescriptor =
        org.jetbrains.kotlin.types.TypeUtils.getClassDescriptor(this)
                ?: this.constructor.supertypes.first().getErasedTypeClass()

fun SymbolTable.translateErased(type: KotlinType): IrSimpleType {
    val erasedClassDescriptor = type.getErasedTypeClass()
    val classSymbol = if (erasedClassDescriptor.kind == ClassKind.ENUM_ENTRY) {
        this.referenceClass(erasedClassDescriptor.containingDeclaration as ClassDescriptor) // FIXME
    } else {
        this.referenceClass(erasedClassDescriptor)
    }
    val nullable = org.jetbrains.kotlin.types.TypeUtils.isNullableType(type)

    val arguments = type.arguments.map {
        if (it.isStarProjection) {
            IrStarProjectionImpl
        } else {
            makeTypeProjection(this.translateErased(it.type), Variance.INVARIANT)
        }
    }

    return classSymbol.createType(nullable, arguments)
}

fun CommonBackendContext.createArrayOfExpression(
        arrayElementType: IrType,
        arrayElements: List<IrExpression>,
        startOffset: Int, endOffset: Int
): IrExpression {

    val genericArrayOfFunSymbol = ir.symbols.arrayOf
    val genericArrayOfFun = genericArrayOfFunSymbol.descriptor
    val substitutedArrayOfFun = genericArrayOfFun.substitute(listOf(arrayElementType))

    val resultType = ir.symbols.array.typeWith(arrayElementType)

    val arg0 = IrVarargImpl(startOffset, endOffset, resultType, arrayElementType, arrayElements)

    // FIXME: reuse builder.irCall(...)

    return IrCallImpl(
            startOffset,
            endOffset,
            resultType,
            genericArrayOfFunSymbol,
            substitutedArrayOfFun,
            typeArgumentsCount = 1
    ).apply {
        putTypeArgument(0, arrayElementType)
        putValueArgument(0, arg0)
    }
}

fun createField(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        name: Name,
        isMutable: Boolean,
        origin: IrDeclarationOrigin,
        owner: ClassDescriptor
): IrField {
    val descriptor = PropertyDescriptorImpl.create(
            /* containingDeclaration = */ owner,
            /* annotations           = */ Annotations.EMPTY,
            /* modality              = */ Modality.FINAL,
            /* visibility            = */ Visibilities.PRIVATE,
            /* isVar                 = */ isMutable,
            /* name                  = */ name,
            /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
            /* source                = */ SourceElement.NO_SOURCE,
            /* lateInit              = */ false,
            /* isConst               = */ false,
            /* isExpect              = */ false,
            /* isActual                = */ false,
            /* isExternal            = */ false,
            /* isDelegated           = */ false
    ).apply {
        initialize(null, null)

        val receiverType: KotlinType? = null
        setType(type.toKotlinType(), emptyList(), owner.thisAsReceiverParameter, receiverType)
    }

    return IrFieldImpl(startOffset, endOffset, origin, descriptor, type)
}
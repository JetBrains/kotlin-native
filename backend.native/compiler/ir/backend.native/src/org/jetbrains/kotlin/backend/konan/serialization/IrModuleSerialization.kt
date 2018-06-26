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

package org.jetbrains.kotlin.backend.konan.serialization


import org.jetbrains.kotlin.protobuf.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedInputStream.*
import org.jetbrains.kotlin.backend.konan.Context
//import org.jetbrains.kotlin.backend.konan.symbols.deserializedPropertyIfAccessor
//import org.jetbrains.kotlin.backend.konan.symbols.isDeserializableCallable
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.ir.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.backend.konan.irasdescriptors.companionObject
import org.jetbrains.kotlin.backend.konan.llvm.base64Decode
import org.jetbrains.kotlin.backend.konan.llvm.base64Encode
import org.jetbrains.kotlin.backend.konan.lower.DeepCopyIrTreeWithDescriptors
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
//import org.jetbrains.kotlin.symbols.*
//import org.jetbrains.kotlin.symbols.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFINED
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
//import org.jetbrains.kotlin.ir.symbols.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.toKotlinType as utilToKotlinType
//import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer
import org.jetbrains.kotlin.metadata.*
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.metadata.KonanIr.IrConst.ValueCase.*
import org.jetbrains.kotlin.metadata.KonanIr.IrDeclarator.DeclaratorCase.*
import org.jetbrains.kotlin.metadata.KonanIr.IrOperation.OperationCase.*
import org.jetbrains.kotlin.metadata.KonanIr.IrStatement.StatementCase
import org.jetbrains.kotlin.metadata.KonanIr.IrType.KindCase.*
import org.jetbrains.kotlin.metadata.KonanIr.IrTypeArgument.KindCase.*
import org.jetbrains.kotlin.metadata.KonanIr.IrVarargElement.VarargElementCase
import org.jetbrains.kotlin.metadata.KonanIr.IrModule.parseFrom
import org.jetbrains.kotlin.name.FqName
//import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.Name.identifier
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
//import org.jetbrains.kotlin.resolve.symbolUtil.parents
//import org.jetbrains.kotlin.serialization.deserialization.symbols.DeserializedClassConstructorDescriptor
//import org.jetbrains.kotlin.serialization.deserialization.symbols.DeserializedPropertyDescriptor
//import org.jetbrains.kotlin.serialization.deserialization.symbols.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.*
//import org.jetbrains.kotlin.types.Variance.*



internal class IrModuleSerialization(val context: Context/*,
                            descriptorTable: DescriptorTable,
                            stringTable: KonanStringTable,
                            rootFunctionSerializer: KonanDescriptorSerializer,
                            private var rootFunction: FunctionDescriptor*/) {

    private val loopIndex = mutableMapOf<IrLoop, Int>()
    private var currentLoopIndex = 0
/*
    private val localDeclarationSerializer
        = LocalDeclarationSerializer(context, rootFunctionSerializer)
    private val irDescriptorSerializer
        = IrDescriptorSerializer(context, descriptorTable, 
            stringTable, localDeclarationSerializer, rootFunction)

    fun serializeInlineBody(): String {
        val declaration = context.ir.originalModuleIndex.functions[rootFunction]!!
        context.log{"INLINE: ${ir2stringWhole(declaration)}"}
        return encodeDeclaration(declaration)
    }

    private fun serializeKotlinType(type: KotlinType): KonanIr.KotlinType {
        context.log{"### serializing KotlinType: " + type}
        return irDescriptorSerializer.serializeKotlinType(type)
    }

    private fun serializeIrSymbol(descriptor: DeclarationDescriptor): KonanIr.KotlinDescriptor {
        context.log{"### serializeIrSymbol $descriptor"}

        // Behind this call starts a large world of 
        // descriptor serialization for IR.
        return irDescriptorSerializer.serializeIrSymbol(descriptor)
    }
*/

    private fun serializeCoordinates(start: Int, end: Int): KonanIr.Coordinates {
        return KonanIr.Coordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build()
    }

    private fun serializeTypeArguments(call: IrMemberAccessExpression): KonanIr.TypeArguments {
        val proto = KonanIr.TypeArguments.newBuilder()
        for (i in 0 until call.typeArgumentsCount) {
            proto.addTypeArgument(serializeIrType(call.getTypeArgument(i)!!))
        }
        return proto.build()
     }

    /* ------- IrSymbols -------------------------------------------------------- */

    fun serializeIrSymbol(symbol: IrSymbol): KonanIr.IrSymbol {
        val proto =  KonanIr.IrSymbol.newBuilder()
            //.setSymbol(serializeIrSymbol(symbol.symbol))

        val kind = when(symbol) {
            is IrClassSymbol ->
                KonanIr.IrSymbolKind.CLASS_SYMBOL
            is IrConstructorSymbol ->
                KonanIr.IrSymbolKind.CONSTRUCTOR_SYMBOL
            is IrTypeParameterSymbol ->
                KonanIr.IrSymbolKind.TYPE_PARAMETER_SYMBOL
            is IrEnumEntrySymbol ->
                KonanIr.IrSymbolKind.ENUM_ENTRY_SYMBOL
            is IrVariableSymbol ->
                KonanIr.IrSymbolKind.VARIABLE_SYMBOL
            is IrValueParameterSymbol ->
                KonanIr.IrSymbolKind.VALUE_PARAMETER_SYMBOL
            is IrSimpleFunctionSymbol ->
                KonanIr.IrSymbolKind.FUNCTION_SYMBOL
            is IrReturnTargetSymbol ->
                KonanIr.IrSymbolKind.RETURN_TARGET_SYMBOL
            is IrFieldSymbol ->
                KonanIr.IrSymbolKind.FIELD_SYMBOL

            else ->
                TODO("Unexpected symbol kind: $symbol")
        }

        proto.kind = kind

        return proto.build()
    }

    /* ------- IrTypes ---------------------------------------------------------- */


    fun serializeIrTypeVariance(variance: Variance)= when(variance) {
        Variance.IN_VARIANCE -> KonanIr.IrTypeVariance.IN
        Variance.OUT_VARIANCE -> KonanIr.IrTypeVariance.OUT
        Variance.INVARIANT -> KonanIr.IrTypeVariance.INV
    }

    fun serializeIrTypeBase(type: IrType, kotlinType: KonanIr.KotlinType?): KonanIr.IrTypeBase {
        val typeBase = type as IrTypeBase // TODO: get rid of the cast.
        val proto = KonanIr.IrTypeBase.newBuilder()
            //.setKotlinType(kotlinType)
            .setVariance(serializeIrTypeVariance(typeBase.variance))
        typeBase.annotations.forEach {
            proto.addAnnotation(serializeCall(it))
        }
        return proto.build()
    }

    fun serializeIrTypeProjection(argument: IrTypeProjection)
        = KonanIr.IrTypeProjection.newBuilder()
            .setVariance(serializeIrTypeVariance(argument.variance))
            .setType(serializeIrType(argument.type))
            .build()

    fun serializeTypeArgument(argument: IrTypeArgument): KonanIr.IrTypeArgument {
        val proto = KonanIr.IrTypeArgument.newBuilder()
        when (argument) {
            is IrStarProjection ->
                proto.star = KonanIr.IrStarProjection.newBuilder().build() // TODO: Do we need a singletone here? Or just an enum?
            is IrTypeProjection ->
                proto.type = serializeIrTypeProjection(argument)
            else -> TODO("Unexpected type argument kind: $argument")
        }
        return proto.build()
    }

    fun serializeSimpleType(type: IrSimpleType, kotlinType: KonanIr.KotlinType?): KonanIr.IrSimpleType {
        val proto = KonanIr.IrSimpleType.newBuilder()
            .setBase(serializeIrTypeBase(type, kotlinType))
            .setClassifier(serializeIrSymbol(type.classifier))
            .setHasQuestionMark(type.hasQuestionMark)
        type.arguments.forEach {
            proto.addArgument(serializeTypeArgument(it))
        }
        return proto.build()
    }

    fun serializeDynamicType(type: IrDynamicType) = KonanIr.IrDynamicType.newBuilder()
        .setBase(serializeIrTypeBase(type, null))
        .build()

    fun serializeErrorType(type: IrErrorType)  = KonanIr.IrErrorType.newBuilder()
        .setBase(serializeIrTypeBase(type, null))
        .build()

    private fun serializeIrType(type: IrType) : KonanIr.IrType {
        context.log{"### serializing IrType: " + type}
        val kotlinType = null
        val proto = KonanIr.IrType.newBuilder()
        when (type) {
            is IrSimpleType ->
                proto.simple = serializeSimpleType(type, kotlinType)
            is IrDynamicType ->
                proto.dynamic = serializeDynamicType(type)
            is IrErrorType ->
                proto.error = serializeErrorType(type)
            else -> TODO("IrType serialization not implemented yet: $type.")
        }
        return proto.build()
    }

    /* -------------------------------------------------------------------------- */

    private fun serializeBlockBody(expression: IrBlockBody): KonanIr.IrBlockBody {
        val proto = KonanIr.IrBlockBody.newBuilder()
        expression.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeBranch(branch: IrBranch): KonanIr.IrBranch {
        val proto = KonanIr.IrBranch.newBuilder()

        proto.condition = serializeExpression(branch.condition)
        proto.result = serializeExpression(branch.result)

        return proto.build()
    }

    private fun serializeBlock(block: IrBlock): KonanIr.IrBlock {
        val isLambdaOrigin = 
            block.origin == IrStatementOrigin.LAMBDA ||
            block.origin == IrStatementOrigin.ANONYMOUS_FUNCTION
        val proto = KonanIr.IrBlock.newBuilder()
            .setIsLambdaOrigin(isLambdaOrigin)
        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeComposite(composite: IrComposite): KonanIr.IrComposite {
        val proto = KonanIr.IrComposite.newBuilder()
        composite.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeCatch(catch: IrCatch): KonanIr.IrCatch {
        val proto = KonanIr.IrCatch.newBuilder()
           .setCatchParameter(serializeDeclaration(catch.catchParameter))
           .setResult(serializeExpression(catch.result))
        return proto.build()
    }

    private fun serializeStringConcat(expression: IrStringConcatenation): KonanIr.IrStringConcat {
        val proto = KonanIr.IrStringConcat.newBuilder()
        expression.arguments.forEach {
            proto.addArgument(serializeExpression(it))
        }
        return proto.build()
    }

    private fun irCallToPrimitiveKind(call: IrCall): KonanIr.IrCall.Primitive = when (call) {
        is IrNullaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.NULLARY
        is IrUnaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.UNARY
        is IrBinaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.BINARY
        else
            -> KonanIr.IrCall.Primitive.NOT_PRIMITIVE
    }

    private fun serializeMemberAccessCommon(call: IrMemberAccessExpression): KonanIr.MemberAccessCommon {
        val proto = KonanIr.MemberAccessCommon.newBuilder()
        if (call.extensionReceiver != null) {
            proto.extensionReceiver = serializeExpression(call.extensionReceiver!!)
        }

        if (call.dispatchReceiver != null)  {
            proto.dispatchReceiver = serializeExpression(call.dispatchReceiver!!)
        }
        proto.typeArguments = serializeTypeArguments(call)

        for (index in 0 .. call.valueArgumentsCount-1) {
            val actual = call.getValueArgument(index)
            val argOrNull = KonanIr.NullableIrExpression.newBuilder()
            if (actual == null) {
                // Am I observing an IR generation regression?
                // I see a lack of arg for an empty vararg,
                // rather than an empty vararg node.

                // TODO: how do we assert that without descriptora?
                //assert(it.varargElementType != null || it.hasDefaultValue())
            } else {
                argOrNull.expression = serializeExpression(actual)
            }
            proto.addValueArgument(argOrNull)
        }
        return proto.build()
    }

    private fun serializeCall(call: IrCall): KonanIr.IrCall {
        val proto = KonanIr.IrCall.newBuilder()

        proto.kind = irCallToPrimitiveKind(call)
        proto.symbol = serializeIrSymbol(call.symbol)

        call.superQualifierSymbol ?. let {
            proto.`super` = serializeIrSymbol(it)
        }
        proto.memberAccess = serializeMemberAccessCommon(call)
        return proto.build()
    }

    private fun serializeFunctionReference(callable: IrFunctionReference): KonanIr.IrFunctionReference {
        val proto = KonanIr.IrFunctionReference.newBuilder()
            //.setSymbol(serializeIrSymbol(callable.symbol))
            .setTypeArguments(serializeTypeArguments(callable))
        callable.origin?.let { proto.origin = (it as IrStatementOriginImpl).debugName }
        return proto.build()
    }


    private fun serializePropertyReference(callable: IrPropertyReference): KonanIr.IrPropertyReference {
        val proto = KonanIr.IrPropertyReference.newBuilder()
                //.setSymbol(serializeIrSymbol(callable.symbol))
                .setTypeArguments(serializeTypeArguments(callable))
        callable.field?.let { proto.field = serializeIrSymbol(it) }
        callable.getter?.let { proto.getter = serializeIrSymbol(it) }
        callable.setter?.let { proto.setter = serializeIrSymbol(it) }
        callable.origin?.let { proto.origin = (it as IrStatementOriginImpl).debugName }
        return proto.build()
    }

    private fun serializeClassReference(expression: IrClassReference): KonanIr.IrClassReference {
        val proto = KonanIr.IrClassReference.newBuilder()
            .setClassSymbol(serializeIrSymbol(expression.symbol))
            .setType(serializeIrType(expression.type))
        return proto.build()
    }

    private fun serializeConst(value: IrConst<*>): KonanIr.IrConst {
        val proto = KonanIr.IrConst.newBuilder()
        when (value.kind) {
            IrConstKind.Null        -> proto.`null` = true
            IrConstKind.Boolean     -> proto.boolean = value.value as Boolean
            IrConstKind.Byte        -> proto.byte = (value.value as Byte).toInt()
            IrConstKind.Char        -> proto.char = (value.value as Char).toInt()
            IrConstKind.Short       -> proto.short = (value.value as Short).toInt()
            IrConstKind.Int         -> proto.int = value.value as Int
            IrConstKind.Long        -> proto.long = value.value as Long
            IrConstKind.String      -> proto.string = value.value as String
            IrConstKind.Float       -> proto.float = value.value as Float
            IrConstKind.Double      -> proto.double = value.value as Double
         }
        return proto.build()
    }

    private fun serializeDelegatingConstructorCall(call: IrDelegatingConstructorCall): KonanIr.IrDelegatingConstructorCall {
        val proto = KonanIr.IrDelegatingConstructorCall.newBuilder()
            .setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeDoWhile(expression: IrDoWhileLoop): KonanIr.IrDoWhile {
        val proto = KonanIr.IrDoWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    fun serializeEnumConstructorCall(call: IrEnumConstructorCall): KonanIr.IrEnumConstructorCall {
        val proto = KonanIr.IrEnumConstructorCall.newBuilder()
            //.setSymbol(serializeIrSymbol(call.symbol))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeGetClass(expression: IrGetClass): KonanIr.IrGetClass {
        val proto = KonanIr.IrGetClass.newBuilder()
            .setArgument(serializeExpression(expression.argument))
        return proto.build()
    }

    private fun serializeGetEnumValue(expression: IrGetEnumValue): KonanIr.IrGetEnumValue {
        val proto = KonanIr.IrGetEnumValue.newBuilder()
            .setType(serializeIrType(expression.type))
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeFieldAccessCommon(expression: IrFieldAccessExpression): KonanIr.FieldAccessCommon {
        val proto = KonanIr.FieldAccessCommon.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        expression.superQualifierSymbol?.let { proto.`super` = serializeIrSymbol(it) }
        expression.receiver?.let { proto.receiver = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeGetField(expression: IrGetField): KonanIr.IrGetField {
        val proto = KonanIr.IrGetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setType(serializeIrType(expression.type))
        return proto.build()
    }

    private fun serializeGetValue(expression: IrGetValue): KonanIr.IrGetValue {
        val type = (expression as IrGetValueImpl).type
        val proto = KonanIr.IrGetValue.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
            .setType(serializeIrType(type))
        return proto.build()
    }

    private fun serializeGetObject(expression: IrGetObjectValue): KonanIr.IrGetObject {
        val proto = KonanIr.IrGetObject.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
        return proto.build()
    }

    private fun serializeInstanceInitializerCall(call: IrInstanceInitializerCall): KonanIr.IrInstanceInitializerCall {
        val proto = KonanIr.IrInstanceInitializerCall.newBuilder()

        proto.symbol = serializeIrSymbol(call.classSymbol)

        return proto.build()
    }

    private fun serializeReturn(expression: IrReturn): KonanIr.IrReturn {
        val proto = KonanIr.IrReturn.newBuilder()
            .setReturnTarget(serializeIrSymbol(expression.returnTargetSymbol))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetField(expression: IrSetField): KonanIr.IrSetField {
        val proto = KonanIr.IrSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetVariable(expression: IrSetVariable): KonanIr.IrSetVariable {
        val proto = KonanIr.IrSetVariable.newBuilder()
            .setSymbol(serializeIrSymbol(expression.symbol))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSpreadElement(element: IrSpreadElement): KonanIr.IrSpreadElement {
        val coordinates = serializeCoordinates(element.startOffset, element.endOffset)
        return KonanIr.IrSpreadElement.newBuilder()
            .setExpression(serializeExpression(element.expression))
            .setCoordinates(coordinates)
            .build()
    }

    private fun serializeSyntheticBody(expression: IrSyntheticBody)
            = KonanIr.IrSyntheticBody.newBuilder()
                .setKind(when(expression.kind) {
                    IrSyntheticBodyKind.ENUM_VALUES -> KonanIr.IrSyntheticBodyKind.ENUM_VALUES
                    IrSyntheticBodyKind.ENUM_VALUEOF -> KonanIr.IrSyntheticBodyKind.ENUM_VALUEOF
                }
            )
            .build()

    private fun serializeThrow(expression: IrThrow): KonanIr.IrThrow {
        val proto = KonanIr.IrThrow.newBuilder()
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeTry(expression: IrTry): KonanIr.IrTry {
        val proto = KonanIr.IrTry.newBuilder()
            .setResult(serializeExpression(expression.tryResult))
        val catchList = expression.catches
        catchList.forEach {
            proto.addCatch(serializeStatement(it))
        }
        val finallyExpression = expression.finallyExpression
        if (finallyExpression != null) {
            proto.finally = serializeExpression(finallyExpression)
        }
        return proto.build()
    }

    private fun serializeTypeOperator(operator: IrTypeOperator): KonanIr.IrTypeOperator = when (operator) {
        IrTypeOperator.CAST
            -> KonanIr.IrTypeOperator.CAST
        IrTypeOperator.IMPLICIT_CAST
            -> KonanIr.IrTypeOperator.IMPLICIT_CAST
        IrTypeOperator.IMPLICIT_NOTNULL
            -> KonanIr.IrTypeOperator.IMPLICIT_NOTNULL
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
            -> KonanIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        IrTypeOperator.IMPLICIT_INTEGER_COERCION
            -> KonanIr.IrTypeOperator.IMPLICIT_INTEGER_COERCION
        IrTypeOperator.SAFE_CAST
            -> KonanIr.IrTypeOperator.SAFE_CAST
        IrTypeOperator.INSTANCEOF
            -> KonanIr.IrTypeOperator.INSTANCEOF
        IrTypeOperator.NOT_INSTANCEOF
            -> KonanIr.IrTypeOperator.NOT_INSTANCEOF
    }

    private fun serializeTypeOp(expression: IrTypeOperatorCall): KonanIr.IrTypeOp {
        val proto = KonanIr.IrTypeOp.newBuilder()
            .setOperator(serializeTypeOperator(expression.operator))
            .setOperand(serializeIrType(expression.typeOperand))
            .setArgument(serializeExpression(expression.argument))
        return proto.build()

    }

    private fun serializeVararg(expression: IrVararg): KonanIr.IrVararg {
        val proto = KonanIr.IrVararg.newBuilder()
            .setElementType(serializeIrType(expression.varargElementType))
        expression.elements.forEach {
            proto.addElement(serializeVarargElement(it))
        }
        return proto.build()
    }

    private fun serializeVarargElement(element: IrVarargElement): KonanIr.IrVarargElement {
        val proto = KonanIr.IrVarargElement.newBuilder()
        when (element) {
            is IrExpression
                -> proto.expression = serializeExpression(element)
            is IrSpreadElement
                -> proto.spreadElement = serializeSpreadElement(element)
            else -> TODO("Unknown vararg element kind")
        }
        return proto.build()
    }

    private fun serializeWhen(expression: IrWhen): KonanIr.IrWhen {
        val proto = KonanIr.IrWhen.newBuilder()

        val branches = expression.branches
        branches.forEach {
            proto.addBranch(serializeStatement(it))
        }

        return proto.build()
    }

    private fun serializeLoop(expression: IrLoop): KonanIr.Loop {
        val proto = KonanIr.Loop.newBuilder()
            .setCondition(serializeExpression(expression.condition))
        val label = expression.label
        if (label != null) {
            proto.label = label
        }

        proto.loopId = currentLoopIndex
        loopIndex[expression] = currentLoopIndex++

        val body = expression.body
        if (body != null) {
            proto.body = serializeExpression(body)
        }

        return proto.build()
    }

    private fun serializeWhile(expression: IrWhileLoop): KonanIr.IrWhile {
        val proto = KonanIr.IrWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    private fun serializeBreak(expression: IrBreak): KonanIr.IrBreak {
        val proto = KonanIr.IrBreak.newBuilder()
        val label = expression.label
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeContinue(expression: IrContinue): KonanIr.IrContinue {
        val proto = KonanIr.IrContinue.newBuilder()
        val label = expression.label
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeExpression(expression: IrExpression): KonanIr.IrExpression {
        context.log{"### serializing Expression: ${ir2string(expression)}"}

        val coordinates = serializeCoordinates(expression.startOffset, expression.endOffset)
        val proto = KonanIr.IrExpression.newBuilder()
            .setType(serializeIrType(expression.type))
            .setCoordinates(coordinates)

        val operationProto = KonanIr.IrOperation.newBuilder()
        
        when (expression) {
            is IrBlock       -> operationProto.block = serializeBlock(expression)
            is IrBreak       -> operationProto.`break` = serializeBreak(expression)
            is IrClassReference
                             -> operationProto.classReference = serializeClassReference(expression)
            is IrCall        -> operationProto.call = serializeCall(expression)

            is IrComposite   -> operationProto.composite = serializeComposite(expression)
            is IrConst<*>    -> operationProto.const = serializeConst(expression)
            is IrContinue    -> operationProto.`continue` = serializeContinue(expression)
            is IrDelegatingConstructorCall
                             -> operationProto.delegatingConstructorCall = serializeDelegatingConstructorCall(expression)
            is IrDoWhileLoop -> operationProto.doWhile = serializeDoWhile(expression)
            is IrEnumConstructorCall
                             -> operationProto.enumConstructorCall = serializeEnumConstructorCall(expression)
            is IrFunctionReference
                             -> operationProto.functionReference = serializeFunctionReference(expression)
            is IrGetClass    -> operationProto.getClass = serializeGetClass(expression)
            is IrGetField    -> operationProto.getField = serializeGetField(expression)
            is IrGetValue    -> operationProto.getValue = serializeGetValue(expression)
            is IrGetEnumValue    
                             -> operationProto.getEnumValue = serializeGetEnumValue(expression)
            is IrGetObjectValue    
                             -> operationProto.getObject = serializeGetObject(expression)
            is IrInstanceInitializerCall        
                             -> operationProto.instanceInitializerCall = serializeInstanceInitializerCall(expression)
            is IrPropertyReference
                             -> operationProto.propertyReference = serializePropertyReference(expression)
            is IrReturn      -> operationProto.`return` = serializeReturn(expression)
            is IrSetField    -> operationProto.setField = serializeSetField(expression)
            is IrSetVariable -> operationProto.setVariable = serializeSetVariable(expression)
            is IrStringConcatenation 
                             -> operationProto.stringConcat = serializeStringConcat(expression)
            is IrThrow       -> operationProto.`throw` = serializeThrow(expression)
            is IrTry         -> operationProto.`try` = serializeTry(expression)
            is IrTypeOperatorCall 
                             -> operationProto.typeOp = serializeTypeOp(expression)
            is IrVararg      -> operationProto.vararg = serializeVararg(expression)
            is IrWhen        -> operationProto.`when` = serializeWhen(expression)
            is IrWhileLoop   -> operationProto.`while` = serializeWhile(expression)
            else -> {
                TODO("Expression serialization not implemented yet: ${ir2string(expression)}.")
            }
        }
        proto.setOperation(operationProto)

        return proto.build()
    }

    private fun serializeStatement(statement: IrElement): KonanIr.IrStatement {
        context.log{"### serializing Statement: ${ir2string(statement)}"}

        val coordinates = serializeCoordinates(statement.startOffset, statement.endOffset)
        val proto = KonanIr.IrStatement.newBuilder()
            .setCoordinates(coordinates)

        when (statement) {
            is IrDeclaration -> { context.log{" ###Declaration "}; proto.declaration = serializeDeclaration(statement) }
            is IrExpression -> { context.log{" ###Expression "}; proto.expression = serializeExpression(statement) }
            is IrBlockBody -> { context.log{" ###BlockBody "}; proto.blockBody = serializeBlockBody(statement) }
            is IrBranch    -> { context.log{" ###Branch "}; proto.branch = serializeBranch(statement) }
            is IrCatch    -> { context.log{" ###Catch "}; proto.catch = serializeCatch(statement) }
            is IrSyntheticBody -> { context.log{" ###SyntheticBody "}; proto.syntheticBody = serializeSyntheticBody(statement) }
            else -> {
                TODO("Statement not implemented yet: ${ir2string(statement)}")
            }
        }
        return proto.build()
    }

    private fun serializeIrTypeAlias(typeAlias: IrTypeAlias) = KonanIr.IrTypeAlias.newBuilder().build()

    private fun serializeIrValueParameter(parameter: IrValueParameter): KonanIr.IrValueParameter {
        val proto = KonanIr.IrValueParameter.newBuilder()
                //.setUniqId()
                .setName(parameter.name.toString())
                .setIndex(parameter.index)
                .setType(serializeIrType(parameter.type))
                .setIsCrossinline(parameter.isCrossinline)
                .setIsNoinline(parameter.isNoinline)

        parameter.varargElementType ?. let { proto.setVarargElementType(serializeIrType(it))}
        return proto.build()
    }

    private fun serializeIrTypeParameter(parameter: IrTypeParameter): KonanIr.IrTypeParameter {
        val proto =  KonanIr.IrTypeParameter.newBuilder()
                .setName(parameter.name.toString())
                .setIndex(parameter.index)
                .setVariance(serializeIrTypeVariance(parameter.variance))
        parameter.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }
        return proto.build()
    }

    private fun serializeIrTypeParameterContainer(typeParameters: List<IrTypeParameter>): KonanIr.IrTypeParameterContainer {
        val proto = KonanIr.IrTypeParameterContainer.newBuilder()
        typeParameters.forEach {
            proto.addTypeParameter(serializeIrTypeParameter(it))
        }
        return proto.build()
    }

    private fun serializeIrFunctionBase(function: IrFunctionBase): KonanIr.IrFunctionBase {
        val proto = KonanIr.IrFunctionBase.newBuilder()
            .setName(function.name.toString())
            .setVisibility(function.visibility.name)
            .setIsInline(function.isInline)
            .setIsExternal(function.isExternal)
            .setReturnType(serializeIrType(function.returnType))
            .setTypeParameters(serializeIrTypeParameterContainer(function.typeParameters))

        function.dispatchReceiverParameter?.let { proto.setDispatchReceiver(serializeIrValueParameter(it)) }
        function.extensionReceiverParameter?.let { proto.setExtensionReceiver(serializeIrValueParameter(it)) }
        function.valueParameters.forEach {
            proto.addValueParameter(serializeIrValueParameter(it))
        }
        function.body?.let { proto.body = serializeStatement(it) }
        return proto.build()
    }

    private fun serializeModality(modality: Modality) = when (modality) {
        Modality.FINAL -> KonanIr.ModalityKind.FINAL_MODALITY
        Modality.SEALED -> KonanIr.ModalityKind.SEALED_MODALITY
        Modality.OPEN -> KonanIr.ModalityKind.OPEN_MODALITY
        Modality.ABSTRACT -> KonanIr.ModalityKind.ABSTRACT_MODALITY
    }

    private fun serializeIrConstructor(declaration: IrConstructor): KonanIr.IrConstructor {
        return KonanIr.IrConstructor.newBuilder()
            .setBase(serializeIrFunctionBase(declaration as IrFunctionBase))
            .setIsPrimary(declaration.isPrimary)
            .build()
    }

    private fun serializeIrFunction(declaration: IrSimpleFunction): KonanIr.IrFunction {
        val function = declaration// as IrFunctionImpl
        val proto = KonanIr.IrFunction.newBuilder()
            .setModality(serializeModality(function.modality))
            .setIsTailrec(function.isTailrec)
            .setIsSuspend(function.isSuspend)

        function.overriddenSymbols.forEach {
            proto.addOverridden(serializeIrSymbol(it))
        }

        // TODO!!!
        //function.correspondingProperty ?. let { proto.setCorrespondingProperty(serializeIrSymbol(it.symbol)) }

        val base = serializeIrFunctionBase(function as IrFunctionBase)
        proto.setBase(base)

        return proto.build()
    }

    private fun serializeIrAnonymousInit(declaration: IrAnonymousInitializer)
        = KonanIr.IrAnonymousInit.newBuilder()
            .setBody(serializeStatement(declaration.body))
            .build()

    private fun serializeVisibility(visibility: Visibility): String {
        return visibility.name
    }

    private fun serializeIrProperty(property: IrProperty): KonanIr.IrProperty {
        val proto = KonanIr.IrProperty.newBuilder()
            .setIsDelegated(property.isDelegated)
                .setName(property.name.toString())
                .setVisibility(serializeVisibility(property.visibility))
                .setModality(serializeModality(property.modality))
                .setIsVar(property.isVar)
                .setIsConst(property.isConst)
                .setIsLateinit(property.isLateinit)
                .setIsDelegated(property.isDelegated)
                .setIsExternal(property.isExternal)

        val backingField = property.backingField
        val getter = property.getter
        val setter = property.setter
        if (backingField != null)
            proto.backingField = serializeIrField(backingField)
        if (getter != null)
            proto.getter = serializeIrFunction(getter)
        if (setter != null)
            proto.setter = serializeIrFunction(setter)

        return proto.build()
    }

    private fun serializeIrField(field: IrField): KonanIr.IrField {
        val proto = KonanIr.IrField.newBuilder()
            .setName(field.name.toString())
            .setVisibility(field.visibility.displayName)
            .setIsFinal(field.isFinal)
            .setIsExternal(field.isExternal)
            .setType(serializeIrType(field.type))
        val initializer = field.initializer?.expression
        if (initializer != null) {
            proto.initializer = serializeExpression(initializer)
        }
        return proto.build()
    }

    private fun serializeIrVariable(variable: IrVariable): KonanIr.IrVariable {
        val proto = KonanIr.IrVariable.newBuilder()
            .setName(variable.name.toString())
            .setType(serializeIrType(variable.type))
            .setIsConst(variable.isConst)
            .setIsVar(variable.isVar)
            .setIsLateinit(variable.isLateinit)
        variable.initializer ?. let { proto.initializer = serializeExpression(it) }
        return proto.build()
    }

    private fun serializeIrDeclarationContainer(declarations: List<IrDeclaration>): KonanIr.IrDeclarationContainer {
        val proto = KonanIr.IrDeclarationContainer.newBuilder()
        declarations.forEach {
            proto.addDeclaration(serializeDeclaration(it))
        }
        return proto.build()
    }

    private fun serializeClassKind(kind: ClassKind) = when(kind) {
        CLASS -> KonanIr.ClassKind.CLASS
        INTERFACE -> KonanIr.ClassKind.INTERFACE
        ENUM_CLASS -> KonanIr.ClassKind.ENUM_CLASS
        ENUM_ENTRY -> KonanIr.ClassKind.ENUM_ENTRY
        ANNOTATION_CLASS -> KonanIr.ClassKind.ANNOTATION_CLASS
        OBJECT -> KonanIr.ClassKind.OBJECT
    }

    private fun serializeIrClass(clazz: IrClass): KonanIr.IrClass {

        val proto = KonanIr.IrClass.newBuilder()
                .setName(clazz.name.toString())
                .setKind(serializeClassKind(clazz.kind))
                .setVisibility(clazz.visibility.name)
                .setModality(serializeModality(clazz.modality))
                .setIsCompanion(clazz.isCompanion)
                .setIsInner(clazz.isInner)
                .setIsData(clazz.isData)
                .setIsExternal(clazz.isExternal)
                .setTypeParameters(serializeIrTypeParameterContainer(clazz.typeParameters))
                .setDeclarationContainer(serializeIrDeclarationContainer(clazz.declarations))
        clazz.superTypes.forEach {
            proto.addSuperType(serializeIrType(it))
        }
        clazz.thisReceiver?.let { proto.thisReceiver = serializeIrValueParameter(it) }

        return proto.build()
    }

    private fun serializeIrEnumEntry(enumEntry: IrEnumEntry): KonanIr.IrEnumEntry {
        val proto = KonanIr.IrEnumEntry.newBuilder()
                .setName(enumEntry.name.toString())
        val initializer = enumEntry.initializerExpression!!
        proto.initializer = serializeExpression(initializer)
        val correspondingClass = enumEntry.correspondingClass
        if (correspondingClass != null) {
            proto.correspondingClass = serializeDeclaration(correspondingClass)
        }
        return proto.build()
    }

    private fun serializeDeclaration(declaration: IrDeclaration): KonanIr.IrDeclaration {
        context.log{"### serializing Declaration: ${ir2string(declaration)}"}

/*
        if (descriptor != rootFunction &&
                declaration !is IrVariable) {
            localDeclarationSerializer.pushContext(descriptor)
        }
*/
        /*
        var kotlinDescriptor = serializeIrSymbol(descriptor)
        var realDescriptor: KonanIr.DeclarationDescriptor? = null
        if (descriptor != rootFunction) {
            realDescriptor = localDeclarationSerializer.serializeLocalDeclaration(descriptor)
        }
        */
        val declarator = KonanIr.IrDeclarator.newBuilder()

        when (declaration) {
            is IrTypeAlias
               -> declarator.irTypeAlias = serializeIrTypeAlias(declaration)
            is IrAnonymousInitializer
                -> declarator.irAnonymousInit = serializeIrAnonymousInit(declaration)
            is IrConstructor
                -> declarator.irConstructor = serializeIrConstructor(declaration)
            is IrField
                -> declarator.irField = serializeIrField(declaration)
            is IrSimpleFunction
                -> declarator.irFunction = serializeIrFunction(declaration)
            is IrVariable 
                -> declarator.irVariable = serializeIrVariable(declaration)
            is IrClass
                -> declarator.irClass = serializeIrClass(declaration)
            is IrEnumEntry
                -> declarator.irEnumEntry = serializeIrEnumEntry(declaration)
            is IrProperty
                -> declarator.irProperty = serializeIrProperty(declaration)
            else
                -> TODO("Declaration serialization not supported yet: $declaration")
        }
/*
        if (declaration !is IrVariable) {
            localDeclarationSerializer.popContext()
        }
*/
 /*
        if (descriptor != rootFunction) {
            val localDeclaration = KonanIr.LocalDeclaration
                .newBuilder()
                .setSymbol(realDescriptor!!)
                .build()
            kotlinDescriptor = kotlinDescriptor
                .toBuilder()
                .setIrLocalDeclaration(localDeclaration)
                .build()
        }
*/
        val coordinates = serializeCoordinates(declaration.startOffset, declaration.endOffset)
        val proto = KonanIr.IrDeclaration.newBuilder()
            //.setKind(declaration.irKind())
            //.setSymbol(kotlinDescriptor)
            .setCoordinates(coordinates)


        proto.setDeclarator(declarator)

        // TODO disabled for now.
        //val fileName = context.ir.originalModuleIndex.declarationToFile[declaration.descriptor]
        //proto.fileName = fileName

        proto.fileName = "some file name"

        return proto.build()
    }

    private fun encodeDeclaration(declaration: IrDeclaration): String {
        val proto = serializeDeclaration(declaration)
        val byteArray = proto.toByteArray()
        return base64Encode(byteArray)
    }





// ---------- Top level ------------------------------------------------------

    fun serializeFileEntry(entry: SourceManager.FileEntry)
        = KonanIr.FileEntry.newBuilder()
            .setName(entry.name)
            .build()

    fun serializeIrFile(file: IrFile) = KonanIr.IrFile.newBuilder()
            .setFileEntry(serializeFileEntry(file.fileEntry))
            .setFqName(file.fqName.toString())
            .setDeclarationContainer(serializeIrDeclarationContainer(file.declarations))
            .build()

    fun serializeModule(module: IrModuleFragment): KonanIr.IrModule {
        val proto = KonanIr.IrModule.newBuilder()
                .setName(module.name.toString())
        module.files.forEach {
            proto.addFile(serializeIrFile(it))
        }
        return proto.build()
    }

    fun serializedModule(module: IrModuleFragment): ByteArray {
        return serializeModule(module).toByteArray()
    }






}





// --------- Deserializer part -----------------------------

internal class IrModuleDeserialization(val context: Context, val builtins: IrBuiltIns, val symbolTable: SymbolTable/*,
                              private val rootFunction: FunctionDescriptor*/) {

    private val loopIndex = mutableMapOf<Int, IrLoop>()
/*
    private val rootMember = rootFunction.deserializedPropertyIfAccessor
    private val localDeserializer = LocalDeclarationDeserializer(rootMember)

    private val descriptorDeserializer = IrDescriptorDeserializer(
        context, rootMember, localDeserializer)

    private fun deserializeKotlinType(proto: KonanIr.KotlinType)
        = descriptorDeserializer.deserializeKotlinType(proto)
*/
//    private fun deserializeIrSymbol(proto: KonanIr.KotlinDescriptor)
//        = descriptorDeserializer.deserializeIrSymbol(proto)

    private fun deserializeTypeArguments(proto: KonanIr.TypeArguments): List<IrType> {
        context.log{"### deserializeTypeArguments"}
        val result = mutableListOf<IrType>()
        proto.typeArgumentList.forEach { typeProto ->
            val type = deserializeIrType(typeProto)
            result.add(type)
            context.log{"$type"}
        }
        return result
    }

    /* ----- IrTypes ------------------------------------------------ */

    //fun deserializeIrClassSymbol(proto: KonanIr.IrClassSymbol, descriptor: ClassDescriptor) = IrClassSymbolImpl(descriptor)
    //fun deserializeIrTypeArgumentSymbol(proto: KonanIr.IrTypeParameterSymbol, descriptor: TypeParameterDescriptor) = IrTypeParameterSymbolImpl(descriptor)


    val errorClassDescriptor = ErrorUtils.createErrorClass("the descriptor should not be needed")
    val dummyFunctionDescriptor = context.builtIns.any.unsubstitutedMemberScope.getContributedDescriptors().first { it is FunctionDescriptor} as FunctionDescriptor

    val dummyConstructorDescriptor = context.builtIns.any.getConstructors().first()

    val dummyPropertyDescriptor = context.builtIns.string.unsubstitutedMemberScope.getContributedDescriptors().first { it is PropertyDescriptor} as PropertyDescriptor
        /*PropertyDescriptorImpl.create(
            errorClassDescriptor,
            Annotations.EMPTY ,
            Modality.FINAL,
            Visibilities.PRIVATE,
            false,
            Name.identifier("the descriptor should not be needed"),
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false, false, false, false, false, false
    )*/

    val dummyVariableDescriptor = IrTemporaryVariableDescriptorImpl(
            errorClassDescriptor,
            Name.identifier("the descriptor should not be needed"),
            context.builtIns.unitType)

    val dummyParameterDescriptor = ValueParameterDescriptorImpl(
            dummyFunctionDescriptor,
            null,
            0,
            Annotations.EMPTY,
            Name.identifier("the descriptor should not be needed"),
            context.builtIns.unitType,
            false,
            false,
            false,
            null,
            SourceElement.NO_SOURCE)

    val dummyPackageFragmentDescriptor = EmptyPackageFragmentDescriptor(context.moduleDescriptor, FqName("THE_DESCRIPTOR_SHOULD_NOT_BE_NEEDED"))

    val dummyTypeParameterDescriptor = TypeParameterDescriptorImpl.createWithDefaultBound(
            errorClassDescriptor,
            Annotations.EMPTY,
            false,
            Variance.INVARIANT,
            Name.identifier("the descriptor should not be needed"),
            0)


    fun deserializeIrSymbol(proto: KonanIr.IrSymbol): IrSymbol {
        //val descriptor = deserializeIrSymbol(proto.symbol)
        val symbol = when (proto.kind) {
            KonanIr.IrSymbolKind.CLASS_SYMBOL ->
                IrClassSymbolImpl(errorClassDescriptor)
            KonanIr.IrSymbolKind.CONSTRUCTOR_SYMBOL ->
                IrConstructorSymbolImpl(dummyConstructorDescriptor)
            KonanIr.IrSymbolKind.TYPE_PARAMETER_SYMBOL ->
                IrTypeParameterSymbolImpl(dummyTypeParameterDescriptor)
            KonanIr.IrSymbolKind.ENUM_ENTRY_SYMBOL ->
                IrEnumEntrySymbolImpl(errorClassDescriptor)
            KonanIr.IrSymbolKind.FIELD_SYMBOL ->
                IrFieldSymbolImpl(dummyPropertyDescriptor)
            KonanIr.IrSymbolKind.FUNCTION_SYMBOL ->
                IrSimpleFunctionSymbolImpl(dummyFunctionDescriptor)
            //RETURN_TARGET ->
              //  IrReturnTargetSymbolImpl
            KonanIr.IrSymbolKind.VARIABLE_SYMBOL ->
                IrVariableSymbolImpl(dummyVariableDescriptor)
            KonanIr.IrSymbolKind.VALUE_PARAMETER_SYMBOL ->
                IrValueParameterSymbolImpl(dummyParameterDescriptor)
            else -> TODO("Unexpected classifier symbol kind: ${proto.kind}")
        }
        // symbol.bind()
        return symbol
    }

    /* ----- IrSymbols ---------------------------------------------- */


    fun deserializeIrTypeVariance(variance: KonanIr.IrTypeVariance) = when(variance) {
        KonanIr.IrTypeVariance.IN -> Variance.IN_VARIANCE
        KonanIr.IrTypeVariance.OUT -> Variance.OUT_VARIANCE
        KonanIr.IrTypeVariance.INV -> Variance.INVARIANT
    }

    fun deserializeIrTypeArgument(proto: KonanIr.IrTypeArgument) = when (proto.kindCase) {
        STAR -> IrStarProjectionImpl
        TYPE -> makeTypeProjection(
                        deserializeIrType(proto.type.type), deserializeIrTypeVariance(proto.type.variance))
        else -> TODO("Unexpected projection kind")

    }

    fun deserializeIrTypeAnnotations(type: KonanIr.IrTypeBase): List<IrCall> {
        return type.annotationList.map {
            deserializeCall(it, 0, 0, builtins.unitType) // TODO: need a proper deserialization here
        }
    }

    fun deserializeSimpleType(proto: KonanIr.IrSimpleType): IrSimpleType {
        val arguments = proto.argumentList.map { deserializeIrTypeArgument(it) }
        val annotations= deserializeIrTypeAnnotations(proto.base)
        //val kotlinType = deserializeKotlinType(proto.base.kotlinType)
        val symbol =  deserializeIrSymbol(proto.classifier) as IrClassifierSymbol
        context.log { "deserializeSimpleType: symbol=$symbol" }
        val result =  IrSimpleTypeImpl(
                null,
                symbol,
                proto.hasQuestionMark,
                arguments,
                annotations
        )
        context.log { "ir_type = $result; render = ${result.render()}"}
        return result
    }

    fun deserializeDynamicType(proto: KonanIr.IrDynamicType): IrDynamicType {
        val annotations= deserializeIrTypeAnnotations(proto.base)
        val variance = deserializeIrTypeVariance(proto.base.variance)
        return IrDynamicTypeImpl(null, annotations, variance)
    }

    fun deserializeErrorType(proto: KonanIr.IrErrorType): IrErrorType {
        val annotations= deserializeIrTypeAnnotations(proto.base)
        val variance = deserializeIrTypeVariance(proto.base.variance)
        return IrErrorTypeImpl(null, annotations, variance)
    }

    fun deserializeIrType(proto: KonanIr.IrType): IrType {
        return when (proto.kindCase) {
            SIMPLE -> deserializeSimpleType(proto.simple)
            DYNAMIC -> deserializeDynamicType(proto.dynamic)
            ERROR -> deserializeErrorType(proto.error)
            else -> TODO("Unexpected IrType kind: ${proto.kindCase}")
        }
    }

    /* -------------------------------------------------------------- */

    private fun deserializeBlockBody(proto: KonanIr.IrBlockBody,
                                     start: Int, end: Int): IrBlockBody {

        val statements = mutableListOf<IrStatement>()

        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        return IrBlockBodyImpl(start, end, statements)
    }

    private fun deserializeBranch(proto: KonanIr.IrBranch, start: Int, end: Int): IrBranch {

        val condition = deserializeExpression(proto.condition)
        val result = deserializeExpression(proto.result)

        return IrBranchImpl(start, end, condition, result)
    }

    private fun deserializeCatch(proto: KonanIr.IrCatch, start: Int, end: Int): IrCatch {
        val catchParameter = deserializeDeclaration(proto.catchParameter) as IrVariable
        val result = deserializeExpression(proto.result)

        return IrCatchImpl(start, end, catchParameter, result)
    }

    private fun deserializeSyntheticBody(proto: KonanIr.IrSyntheticBody, start: Int, end: Int): IrSyntheticBody {
        val kind = when (proto.kind) {
            KonanIr.IrSyntheticBodyKind.ENUM_VALUES -> IrSyntheticBodyKind.ENUM_VALUES
            KonanIr.IrSyntheticBodyKind.ENUM_VALUEOF -> IrSyntheticBodyKind.ENUM_VALUEOF
        }
        return IrSyntheticBodyImpl(start, end, kind)
    }

    private fun deserializeStatement(proto: KonanIr.IrStatement): IrElement {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val element = when (proto.statementCase) {
            StatementCase.BLOCK_BODY //proto.hasBlockBody()
                -> deserializeBlockBody(proto.blockBody, start, end)
            StatementCase.BRANCH //proto.hasBranch()
                -> deserializeBranch(proto.branch, start, end)
            StatementCase.CATCH //proto.hasCatch()
                -> deserializeCatch(proto.catch, start, end)
            StatementCase.DECLARATION // proto.hasDeclaration()
                -> deserializeDeclaration(proto.declaration)
            StatementCase.EXPRESSION // proto.hasExpression()
                -> deserializeExpression(proto.expression)
            StatementCase.SYNTHETIC_BODY // proto.hasSyntheticBody()
                -> deserializeSyntheticBody(proto.syntheticBody, start, end)
            else
                -> TODO("Statement deserialization not implemented: ${proto.statementCase}")
        }

        context.log{"### Deserialized statement: ${ir2string(element)}"}

        return element
    }

    private val KotlinType.ir: IrType get() = /*context.ir*/symbolTable.translateErased(this)
    private val KotlinType.brokenIr: IrType get() = context.ir.translateBroken(this)


    private fun deserializeBlock(proto: KonanIr.IrBlock, start: Int, end: Int, type: IrType): IrBlock {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        val isLambdaOrigin = if (proto.isLambdaOrigin) IrStatementOrigin.LAMBDA else null

        return IrBlockImpl(start, end, type, isLambdaOrigin, statements)
    }

    private fun deserializeMemberAccessCommon(access: IrMemberAccessExpression, proto: KonanIr.MemberAccessCommon) {

        println("valueArgumentsList.size = ${ proto.valueArgumentList.size}")
        proto.valueArgumentList.mapIndexed { i, arg ->
            /*
            val exprOrNull = if (arg.hasExpression())
                deserializeExpression(arg.expression)
            else null
            access.putValueArgument(i, exprOrNull)
            */
            println("index = $i")
            if (arg.hasExpression()) {
                val expr = deserializeExpression(arg.expression)
                access.putValueArgument(i, expr)
            }
        }

        deserializeTypeArguments(proto.typeArguments).forEachIndexed { index, type ->
            access.putTypeArgument(index, type)
        }

        if (proto.hasDispatchReceiver()) {
            access.dispatchReceiver = deserializeExpression(proto.dispatchReceiver)
        }
        if (proto.hasExtensionReceiver()) {
            access.extensionReceiver = deserializeExpression(proto.extensionReceiver)
        }
    }

    private fun deserializeClassReference(proto: KonanIr.IrClassReference, start: Int, end: Int, type: IrType): IrClassReference {
        val symbol = deserializeIrSymbol(proto.classSymbol) as IrClassifierSymbol
        val classType = deserializeIrType(proto.type)
        /** TODO: [createClassifierSymbolForClassReference] is internal function */
        return IrClassReferenceImpl(start, end, type, symbol, classType)
    }

    private fun deserializeCall(proto: KonanIr.IrCall, start: Int, end: Int, type: IrType): IrCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrFunctionSymbol

        val superSymbol = if (proto.hasSuper()) {
            deserializeIrSymbol(proto.`super`) as IrClassSymbol
        } else null

        val call: IrCall = when (proto.kind) {
            KonanIr.IrCall.Primitive.NOT_PRIMITIVE ->
                // TODO: implement the last three args here.
                IrCallImpl(start, end, type,
                            symbol, dummyFunctionDescriptor,
                            proto.memberAccess.valueArgumentList.size,
                            proto.memberAccess.typeArguments.typeArgumentCount,
                        null, superSymbol)
            KonanIr.IrCall.Primitive.NULLARY ->
                IrNullaryPrimitiveImpl(start, end, type, null, symbol)
            KonanIr.IrCall.Primitive.UNARY ->
                IrUnaryPrimitiveImpl(start, end, type, null, symbol)
            KonanIr.IrCall.Primitive.BINARY ->
                IrBinaryPrimitiveImpl(start, end, type, null, symbol)
            else -> TODO("Unexpected primitive IrCall.")
        }
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    private fun deserializeComposite(proto: KonanIr.IrComposite, start: Int, end: Int, type: IrType): IrComposite {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }
        return IrCompositeImpl(start, end, type, null, statements)
    }

    private fun deserializeDelegatingConstructorCall(proto: KonanIr.IrDelegatingConstructorCall, start: Int, end: Int): IrDelegatingConstructorCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrConstructorSymbol
        val call = IrDelegatingConstructorCallImpl(start, end, builtins.unitType, symbol, dummyConstructorDescriptor, proto.memberAccess.typeArguments.typeArgumentCount)

        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }



    fun deserializeEnumConstructorCall(proto: KonanIr.IrEnumConstructorCall, start: Int, end: Int, type: IrType): IrEnumConstructorCall {
        val symbol = IrConstructorSymbolImpl(dummyConstructorDescriptor)
        return IrEnumConstructorCallImpl(start, end, type, symbol, proto.memberAccess.typeArguments.typeArgumentList.size, proto.memberAccess.valueArgumentList.size)
    }



    private fun deserializeFunctionReference(proto: KonanIr.IrFunctionReference,
                                             start: Int, end: Int, type: IrType): IrFunctionReference {

        //val symbol = deserializeIrSymbol(proto.symbol)
        val symbol = IrSimpleFunctionSymbolImpl(dummyFunctionDescriptor)
        val callable= IrFunctionReferenceImpl(start, end, type, symbol, dummyFunctionDescriptor, proto.typeArguments.typeArgumentCount, null)

        deserializeTypeArguments(proto.typeArguments).forEachIndexed { index, argType ->
            callable.putTypeArgument(index, argType)
        }
        return callable
    }

    private fun deserializeGetClass(proto: KonanIr.IrGetClass, start: Int, end: Int, type: IrType): IrGetClass {
        val argument = deserializeExpression(proto.argument)
        return IrGetClassImpl(start, end, type, argument)
    }

    private fun deserializeGetField(proto: KonanIr.IrGetField, start: Int, end: Int): IrGetField {
        val access = proto.fieldAccess
        val symbol = deserializeIrSymbol(access.symbol) as IrFieldSymbol
        val type = deserializeIrType(proto.type)
        val superQualifier = if (access.hasSuper()) {
            deserializeIrSymbol(access.symbol) as IrClassSymbol
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null

        return IrGetFieldImpl(start, end, symbol, type, receiver, null, superQualifier)
    }

    private fun deserializeGetValue(proto: KonanIr.IrGetValue, start: Int, end: Int): IrGetValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrValueSymbol
        val type = deserializeIrType(proto.type)

        // TODO: origin!
        return IrGetValueImpl(start, end, type, symbol, null)
    }

    private fun deserializeGetEnumValue(proto: KonanIr.IrGetEnumValue, start: Int, end: Int): IrGetEnumValue {
        val type = deserializeIrType(proto.type)
        val symbol = deserializeIrSymbol(proto.symbol) as IrEnumEntrySymbol

        return IrGetEnumValueImpl(start, end, type, symbol)
    }

    private fun deserializeGetObject(proto: KonanIr.IrGetObject, start: Int, end: Int, type: IrType): IrGetObjectValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrGetObjectValueImpl(start, end, type, symbol)
    }

    private fun deserializeInstanceInitializerCall(proto: KonanIr.IrInstanceInitializerCall, start: Int, end: Int): IrInstanceInitializerCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrInstanceInitializerCallImpl(start, end, symbol, builtins.unitType)
    }

    private fun deserializePropertyReference(proto: KonanIr.IrPropertyReference,
                                             start: Int, end: Int, type: IrType): IrPropertyReference {

        //val symbol = deserializeIrSymbol(proto.symbol)
        //val symbol = IrPropertySymbolImpl(dummyPropertyDescriptor)
        val field = if (proto.hasField()) deserializeIrSymbol(proto.field) as IrFieldSymbol else null
        val getter = if (proto.hasGetter()) deserializeIrSymbol(proto.getter) as IrFunctionSymbol else null
        val setter = if (proto.hasSetter()) deserializeIrSymbol(proto.setter) as IrFunctionSymbol else null
        val callable= IrPropertyReferenceImpl(start, end, type,
                dummyPropertyDescriptor,
                proto.typeArguments.typeArgumentCount,
                field,
                getter,
                setter,
                null)

        deserializeTypeArguments(proto.typeArguments).forEachIndexed { index, argType ->
            callable.putTypeArgument(index, argType)
        }
        return callable
    }

    private fun deserializeReturn(proto: KonanIr.IrReturn, start: Int, end: Int, type: IrType): IrReturn {
        val symbol = deserializeIrSymbol(proto.returnTarget) as IrReturnTargetSymbol
        val value = deserializeExpression(proto.value)
        return IrReturnImpl(start, end, builtins.nothingType, symbol, value)
    }

    private fun deserializeSetField(proto: KonanIr.IrSetField, start: Int, end: Int): IrSetField {
        val access = proto.fieldAccess
        val symbol = deserializeIrSymbol(access.symbol) as IrFieldSymbol
        val superQualifier = if (access.hasSuper()) {
            deserializeIrSymbol(access.symbol) as IrClassSymbol
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null
        val value = deserializeExpression(proto.value)

        return IrSetFieldImpl(start, end, symbol, receiver, value, builtins.unitType, null, superQualifier)
    }

    private fun deserializeSetVariable(proto: KonanIr.IrSetVariable, start: Int, end: Int): IrSetVariable {
        val symbol = deserializeIrSymbol(proto.symbol) as IrVariableSymbol
        val value = deserializeExpression(proto.value)
        return IrSetVariableImpl(start, end, builtins.unitType, symbol, value, null)
    }

    private fun deserializeSpreadElement(proto: KonanIr.IrSpreadElement): IrSpreadElement {
        val expression = deserializeExpression(proto.expression)
        return IrSpreadElementImpl(proto.coordinates.startOffset, proto.coordinates.endOffset, expression)
    }

    private fun deserializeStringConcat(proto: KonanIr.IrStringConcat, start: Int, end: Int, type: IrType): IrStringConcatenation {
        val argumentProtos = proto.argumentList
        val arguments = mutableListOf<IrExpression>()

        argumentProtos.forEach {
            arguments.add(deserializeExpression(it))
        }
        return IrStringConcatenationImpl(start, end, type, arguments)
    }

    private fun deserializeThrow(proto: KonanIr.IrThrow, start: Int, end: Int, type: IrType): IrThrowImpl {
        return IrThrowImpl(start, end, builtins.nothingType, deserializeExpression(proto.value))
    }

    private fun deserializeTry(proto: KonanIr.IrTry, start: Int, end: Int, type: IrType): IrTryImpl {
        val result = deserializeExpression(proto.result)
        val catches = mutableListOf<IrCatch>()
        proto.catchList.forEach {
            catches.add(deserializeStatement(it) as IrCatch) 
        }
        val finallyExpression = 
            if (proto.hasFinally()) deserializeExpression(proto.getFinally()) else null
        return IrTryImpl(start, end, type, result, catches, finallyExpression)
    }

    private fun deserializeTypeOperator(operator: KonanIr.IrTypeOperator): IrTypeOperator {
        when (operator) {
            KonanIr.IrTypeOperator.CAST
                -> return IrTypeOperator.CAST
            KonanIr.IrTypeOperator.IMPLICIT_CAST
                -> return IrTypeOperator.IMPLICIT_CAST
            KonanIr.IrTypeOperator.IMPLICIT_NOTNULL
                -> return IrTypeOperator.IMPLICIT_NOTNULL
            KonanIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
                -> return IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
            KonanIr.IrTypeOperator.IMPLICIT_INTEGER_COERCION
                -> return IrTypeOperator.IMPLICIT_INTEGER_COERCION
            KonanIr.IrTypeOperator.SAFE_CAST
                -> return IrTypeOperator.SAFE_CAST
            KonanIr.IrTypeOperator.INSTANCEOF
                -> return IrTypeOperator.INSTANCEOF
            KonanIr.IrTypeOperator.NOT_INSTANCEOF
                -> return IrTypeOperator.NOT_INSTANCEOF
        }
    }

    private fun deserializeTypeOp(proto: KonanIr.IrTypeOp, start: Int, end: Int, type: IrType) : IrTypeOperatorCall {
        val operator = deserializeTypeOperator(proto.operator)
        val operand = deserializeIrType(proto.operand)//.brokenIr
        val argument = deserializeExpression(proto.argument)
        return IrTypeOperatorCallImpl(start, end, type, operator, operand).apply {
            this.argument = argument
            this.typeOperandClassifier = operand.classifierOrFail
        }
    }

    private fun deserializeVararg(proto: KonanIr.IrVararg, start: Int, end: Int, type: IrType): IrVararg {
        val elementType = deserializeIrType(proto.elementType)

        val elements = mutableListOf<IrVarargElement>()
        proto.elementList.forEach {
            elements.add(deserializeVarargElement(it))
        }
        return IrVarargImpl(start, end, type, elementType, elements)
    }

    private fun deserializeVarargElement(element: KonanIr.IrVarargElement): IrVarargElement {
        return when (element.varargElementCase) {
            VarargElementCase.EXPRESSION
                -> deserializeExpression(element.expression)
            VarargElementCase.SPREAD_ELEMENT
                -> deserializeSpreadElement(element.spreadElement)
            else 
                -> TODO("Unexpected vararg element")
        }
    }

    private fun deserializeWhen(proto: KonanIr.IrWhen, start: Int, end: Int, type: IrType): IrWhen {
        val branches = mutableListOf<IrBranch>()

        proto.branchList.forEach {
            branches.add(deserializeStatement(it) as IrBranch)
        }

        // TODO: provide some origin!
        return  IrWhenImpl(start, end, type, null, branches)
    }

    private fun deserializeLoop(proto: KonanIr.Loop, loop: IrLoopBase): IrLoopBase {
        val loopId = proto.loopId
        loopIndex.getOrPut(loopId){loop}

        val label = if (proto.hasLabel()) proto.label else null
        val body = if (proto.hasBody()) deserializeExpression(proto.body) else null
        val condition = deserializeExpression(proto.condition)

        loop.label = label
        loop.condition = condition
        loop.body = body

        return loop
    }

    private fun deserializeDoWhile(proto: KonanIr.IrDoWhile, start: Int, end: Int, type: IrType): IrDoWhileLoop {
        // we create the loop before deserializing the body, so that 
        // IrBreak statements have something to put into 'loop' field.
        val loop = IrDoWhileLoopImpl(start, end, type, null)
        deserializeLoop(proto.loop, loop)
        return loop
    }

    private fun deserializeWhile(proto: KonanIr.IrWhile, start: Int, end: Int, type: IrType): IrWhileLoop {
        // we create the loop before deserializing the body, so that 
        // IrBreak statements have something to put into 'loop' field.
        val loop = IrWhileLoopImpl(start, end, type, null)
        deserializeLoop(proto.loop, loop)
        return loop
    }

    private fun deserializeBreak(proto: KonanIr.IrBreak, start: Int, end: Int, type: IrType): IrBreak {
        val label = if(proto.hasLabel()) proto.label else null
        val loopId = proto.loopId
        val loop = loopIndex[loopId]!!
        val irBreak = IrBreakImpl(start, end, type, loop)
        irBreak.label = label

        return irBreak
    }

    private fun deserializeContinue(proto: KonanIr.IrContinue, start: Int, end: Int, type: IrType): IrContinue {
        val label = if(proto.hasLabel()) proto.label else null
        val loopId = proto.loopId
        val loop = loopIndex[loopId]!!
        val irContinue = IrContinueImpl(start, end, type, loop)
        irContinue.label = label

        return irContinue
    }

    private fun deserializeConst(proto: KonanIr.IrConst, start: Int, end: Int, type: IrType): IrExpression =
        when(proto.valueCase) {
            NULL
                -> IrConstImpl.constNull(start, end, type)
            BOOLEAN
                -> IrConstImpl.boolean(start, end, type, proto.boolean)
            BYTE
                -> IrConstImpl.byte(start, end, type, proto.byte.toByte())
            CHAR
                -> IrConstImpl.char(start, end, type, proto.char.toChar())
            SHORT
                -> IrConstImpl.short(start, end, type, proto.short.toShort())
            INT
                -> IrConstImpl.int(start, end, type, proto.int)
            LONG
                -> IrConstImpl.long(start, end, type, proto.long)
            STRING
                -> IrConstImpl.string(start, end, type, proto.string)
            FLOAT
                -> IrConstImpl.float(start, end, type, proto.float)
            DOUBLE
                -> IrConstImpl.double(start, end, type, proto.double)
            VALUE_NOT_SET
                -> error("Const deserialization error: ${proto.valueCase} ")
        }

    private fun deserializeOperation(proto: KonanIr.IrOperation, start: Int, end: Int, type: IrType): IrExpression =
        when (proto.operationCase) {
            BLOCK
                -> deserializeBlock(proto.block, start, end, type)
            BREAK
                -> deserializeBreak(proto.`break`, start, end, type)
            CLASS_REFERENCE
                -> deserializeClassReference(proto.classReference, start, end, type)
            CALL
                -> deserializeCall(proto.call, start, end, type)
            COMPOSITE
                -> deserializeComposite(proto.composite, start, end, type)
            CONST
                -> deserializeConst(proto.const, start, end, type)
            CONTINUE
                -> deserializeContinue(proto.`continue`, start, end, type)
            DELEGATING_CONSTRUCTOR_CALL
                -> deserializeDelegatingConstructorCall(proto.delegatingConstructorCall, start, end)
            DO_WHILE
                -> deserializeDoWhile(proto.doWhile, start, end, type)
            ENUM_CONSTRUCTOR_CALL
                -> deserializeEnumConstructorCall(proto.enumConstructorCall, start, end, type)
            FUNCTION_REFERENCE
                -> deserializeFunctionReference(proto.functionReference, start, end, type)
            GET_ENUM_VALUE
                -> deserializeGetEnumValue(proto.getEnumValue, start, end)
            GET_CLASS
                -> deserializeGetClass(proto.getClass, start, end, type)
            GET_FIELD
                -> deserializeGetField(proto.getField, start, end)
            GET_OBJECT
                -> deserializeGetObject(proto.getObject, start, end, type)
            GET_VALUE
                -> deserializeGetValue(proto.getValue, start, end)
            INSTANCE_INITIALIZER_CALL
                -> deserializeInstanceInitializerCall(proto.instanceInitializerCall, start, end)
            PROPERTY_REFERENCE
                -> deserializePropertyReference(proto.propertyReference, start, end, type)
            RETURN
                -> deserializeReturn(proto.`return`, start, end, type)
            SET_FIELD
                -> deserializeSetField(proto.setField, start, end)
            SET_VARIABLE
                -> deserializeSetVariable(proto.setVariable, start, end)
            STRING_CONCAT
                -> deserializeStringConcat(proto.stringConcat, start, end, type)
            THROW
                -> deserializeThrow(proto.`throw`, start, end, type)
            TRY
                -> deserializeTry(proto.`try`, start, end, type)
            TYPE_OP
                -> deserializeTypeOp(proto.typeOp, start, end, type)
            VARARG
                -> deserializeVararg(proto.vararg, start, end, type)
            WHEN
                -> deserializeWhen(proto.`when`, start, end, type)
            WHILE
                -> deserializeWhile(proto.`while`, start, end, type)
            OPERATION_NOT_SET
                -> error("Expression deserialization not implemented: ${proto.operationCase}")
        }

    private fun deserializeExpression(proto: KonanIr.IrExpression): IrExpression {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val type = deserializeIrType(proto.type)
        val operation = proto.operation
        val expression = deserializeOperation(operation, start, end, type)

        context.log{"### Deserialized expression: ${ir2string(expression)} ir_type=$type"}
        return expression
    }

    private fun deserializeIrClass(proto: KonanIr.IrClass, start: Int, end: Int, origin: IrDeclarationOrigin): IrClass {
        val members = mutableListOf<IrDeclaration>()

        proto.declarationContainer.declarationList.forEach {
            members.add(deserializeDeclaration(it))
        }

        val symbol = IrClassSymbolImpl(errorClassDescriptor)

        val clazz = IrClassImpl(start, end, origin, symbol)
        clazz.addAll(members)

        //val symbolTable = context.ir.symbols.symbolTable
        clazz.createParameterDeclarations(symbolTable)
        clazz.addFakeOverrides(symbolTable)
        clazz.setSuperSymbols(symbolTable)

        return clazz

    }

    private fun deserializeIrFunction(proto: KonanIr.IrFunction,
                                      start: Int, end: Int, origin: IrDeclarationOrigin): IrSimpleFunction {

        context.log{"function name = ${proto.base.name}"}
        val symbol = IrSimpleFunctionSymbolImpl(dummyFunctionDescriptor)
        val function = IrFunctionImpl(start, end, origin, symbol)

        function.returnType = deserializeIrType(proto.base.returnType)
        function.body = if (proto.base.hasBody()) deserializeStatement(proto.base.body) as IrBody else null

        function.createParameterDeclarations(symbolTable)
        function.setOverrides(symbolTable)

        return function
    }

    private fun deserializeIrVariable(proto: KonanIr.IrVariable,
                                      start: Int, end: Int, origin: IrDeclarationOrigin): IrVariable {

        val initializer = if (proto.hasInitializer()) {
            deserializeExpression(proto.initializer)
        } else null

        val symbol = IrVariableSymbolImpl(dummyVariableDescriptor)
        val type = deserializeIrType(proto.type)

        val variable = IrVariableImpl(start, end, origin, symbol, Name.identifier(proto.name), type, proto.isVar, proto.isConst, proto.isLateinit)
        variable.initializer = initializer
        return variable
    }

    private fun deserializeIrEnumEntry(proto: KonanIr.IrEnumEntry,
                                       start: Int, end: Int, origin: IrDeclarationOrigin): IrEnumEntry {
        val symbol = IrEnumEntrySymbolImpl(errorClassDescriptor)

        val enumEntry = IrEnumEntryImpl(start, end, origin, symbol, Name.identifier(proto.name))
        if (proto.hasCorrespondingClass()) {
            enumEntry.correspondingClass = deserializeDeclaration(proto.correspondingClass) as IrClass
        }
        enumEntry.initializerExpression = deserializeExpression(proto.initializer)

        return enumEntry
    }

    private fun deserializeIrAnonymousInit(proto: KonanIr.IrAnonymousInit, start: Int, end: Int, origin: IrDeclarationOrigin): IrAnonymousInitializer {
        val symbol = IrAnonymousInitializerSymbolImpl(errorClassDescriptor)
        val initializer = IrAnonymousInitializerImpl(start, end, origin, symbol)
            initializer.body = deserializeBlockBody(proto.body.blockBody, start, end)
        return initializer
    }

    private fun deserializeVisibility(value: String): Visibility {
        return Visibilities.DEFAULT_VISIBILITY // TODO: fixme
    }

    private fun deserializeIrConstructor(proto: KonanIr.IrConstructor, start: Int, end: Int, origin: IrDeclarationOrigin): IrConstructor {
        val symbol = IrConstructorSymbolImpl(dummyConstructorDescriptor)
        val constructor = IrConstructorImpl(start, end, origin,
            symbol,
            Name.identifier(proto.base.name),
            deserializeVisibility(proto.base.visibility),
            proto.base.isInline,
            proto.base.isExternal,
            proto.isPrimary
        )
        constructor.returnType = deserializeIrType(proto.base.returnType)
        constructor.body = if (proto.base.hasBody()) deserializeStatement(proto.base.body) as IrBody else null
        return constructor
    }

    private fun deserializeIrField(proto: KonanIr.IrField, start: Int, end: Int, origin: IrDeclarationOrigin): IrField {
        val symbol = IrFieldSymbolImpl(dummyPropertyDescriptor)
        val field = IrFieldImpl(start, end, origin,
            symbol,
            Name.identifier(proto.name),
            deserializeIrType(proto.type),
            deserializeVisibility(proto.visibility),
            proto.isFinal,
            proto.isExternal)
        return field
    }

    private fun deserializeModality(modality: KonanIr.ModalityKind) = when(modality) {
        KonanIr.ModalityKind.OPEN_MODALITY -> Modality.OPEN
        KonanIr.ModalityKind.SEALED_MODALITY -> Modality.SEALED
        KonanIr.ModalityKind.FINAL_MODALITY -> Modality.FINAL
        KonanIr.ModalityKind.ABSTRACT_MODALITY -> Modality.ABSTRACT
    }

    private fun deserializeIrProperty(proto: KonanIr.IrProperty, start: Int, end: Int, origin: IrDeclarationOrigin): IrProperty {
        //val symbol = IrPropertySymbolImpl(dummyPropertySymbol)
        val property = IrPropertyImpl(start, end, origin,
                dummyPropertyDescriptor,
                Name.identifier(proto.name),
                deserializeVisibility(proto.visibility),
                deserializeModality(proto.modality),
                proto.isVar,
                proto.isConst,
                proto.isLateinit,
                proto.isDelegated,
                proto.isExternal)
        property.backingField = if (proto.hasBackingField()) deserializeIrField(proto.backingField, start, end, origin) else null
        property.getter = if (proto.hasGetter()) deserializeIrFunction(proto.getter, start, end, origin) else null
        property.setter = if (proto.hasSetter()) deserializeIrFunction(proto.setter, start, end, origin) else null

        return property
    }

    private fun deserializeIrTypeAlias(proto: KonanIr.IrTypeAlias, start: Int, end: Int, origin: IrDeclarationOrigin): IrDeclaration { //IrTypeAlias {
        //val descriptor =
        //return IrTypeAliasImpl(start, end, origin, descriptor)
        return IrErrorDeclarationImpl(start, end, errorClassDescriptor)
    }

    private fun deserializeDeclaration(proto: KonanIr.IrDeclaration): IrDeclaration {

        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val origin = DEFINED // TODO: retore the real origins
        val declarator = proto.declarator

        val declaration: IrDeclaration = when (declarator.declaratorCase){
            IR_TYPE_ALIAS
                -> deserializeIrTypeAlias(declarator.irTypeAlias, start, end, origin)
            IR_ANONYMOUS_INIT
                -> deserializeIrAnonymousInit(declarator.irAnonymousInit, start, end, origin)
            IR_CONSTRUCTOR
                -> deserializeIrConstructor(declarator.irConstructor, start, end, origin)
            IR_FIELD
                -> deserializeIrField(declarator.irField, start, end, origin)
            IR_CLASS
                -> deserializeIrClass(declarator.irClass, start, end, origin)
            IR_FUNCTION
                -> deserializeIrFunction(declarator.irFunction, start, end, origin)
            IR_PROPERTY
                -> deserializeIrProperty(declarator.irProperty, start, end, origin)
            IR_VARIABLE
                -> deserializeIrVariable(declarator.irVariable, start, end, origin)
            IR_ENUM_ENTRY
                -> deserializeIrEnumEntry(declarator.irEnumEntry, start, end, origin)
            DECLARATOR_NOT_SET
                -> error("Declaration deserialization not implemented: ${declarator.declaratorCase}")
        }

        val sourceFileName = proto.fileName
        //context.ir.originalModuleIndex.declarationToFile[declaration.descriptor.original] = sourceFileName
/*
        if (!(descriptor is VariableDescriptor) && descriptor != rootFunction)
            localDeserializer.popContext(descriptor)
*/
        context.log{"### Deserialized declaration: ${ir2string(declaration)}"}
        return declaration
    }

/*
    // We run inline body deserializations after the public descriptor tree
    // deserialization is long gone. So we don't have the needed chain of
    // deserialization contexts available to take type parameters.
    // So typeDeserializer introduces a brand new set of DeserializadTypeParameterDescriptors
    // for the rootFunction.
    // This function takes the type parameters from the rootFunction descriptor
    // and substitutes them instead the deserialized ones.
    // TODO: consider lazy inline body deserialization during the public descriptors deserialization.
    // I tried to copy over TypeDeserializaer, MemberDeserializer, 
    // and the rest of what's needed, but it didn't work out.
    private fun adaptDeserializedTypeParameters(declaration: IrDeclaration): IrDeclaration {

        val rootFunctionTypeParameters = 
            localDeserializer.typeDeserializer.ownTypeParameters

        val realTypeParameters =
            rootFunction.deserializedPropertyIfAccessor.typeParameters

        val substitutionContext = rootFunctionTypeParameters.mapIndexed{
            index, param ->
            Pair(param.typeConstructor, TypeProjectionImpl(realTypeParameters[index].defaultType))
        }.associate{
            (key,value) ->
        key to value}

        return DeepCopyIrTreeWithDescriptors(rootFunction, rootFunction.parents.first(), context).copy(
            irElement       = declaration,
            typeSubstitutor = TypeSubstitutor.create(substitutionContext)
        ) as IrFunction
    }
    */
/*
    private val extractInlineProto: KonanLinkData.InlineIrBody
        get() = when (rootFunction) {
            is DeserializedSimpleFunctionDescriptor -> {
                rootFunction.proto.inlineIr
            }
            is DeserializedClassConstructorDescriptor -> {
                rootFunction.proto.constructorIr
            }
            is PropertyGetterDescriptor -> {
                (rootMember as DeserializedPropertyDescriptor).proto.getterIr
            }
            is PropertySetterDescriptor -> {
                (rootMember as DeserializedPropertyDescriptor).proto.setterIr
            }
            else -> error("Unexpected descriptor: $rootFunction")
        }

    fun decodeDeclaration(): IrDeclaration {
        assert(rootFunction.isDeserializableCallable)

        val inlineProto = extractInlineProto
        val base64 = inlineProto.encodedIr
        val byteArray = base64Decode(base64)
        val irProto = KonanIr.IrDeclaration.parseFrom(byteArray, KonanSerializerProtocol.extensionRegistry)
        val declaration = deserializeDeclaration(irProto)

        context.log {"BEFORE ADAPTATION: ${ir2stringWhole(declaration)}"}

        //return adaptDeserializedTypeParameters(declaration)
        return declaration
    }
    */

    /* ------- Top level --------------------------------------------*/

    fun deserializeIrFile(proto: KonanIr.IrFile): IrFile {
        val fileEntry = NaiveSourceBasedFileEntryImpl(proto.fileEntry.name)
        val symbol = IrFileSymbolImpl(dummyPackageFragmentDescriptor)
        val file = IrFileImpl(fileEntry, symbol , FqName(proto.fqName))
        proto.declarationContainer.declarationList.forEach {
            val declaration = deserializeDeclaration(it)
            file.declarations.add(declaration)
        }
        return file
    }

    fun deserializeIrModule(proto: KonanIr.IrModule): IrModuleFragment {

        val files = proto.fileList.map {
            deserializeIrFile(it)

        }
        return IrModuleFragmentImpl(context.moduleDescriptor, builtins, files)
    }

    fun deserializedIrModule(byteArray: ByteArray): IrModuleFragment {
        val codedInputStream = org.jetbrains.kotlin.protobuf.CodedInputStream.newInstance(byteArray)
        codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
        val proto = KonanIr.IrModule.parseFrom(codedInputStream, KonanSerializerProtocol.extensionRegistry)
        return deserializeIrModule(proto)
    }
}

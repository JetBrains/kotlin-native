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


import org.jetbrains.kotlin.backend.common.DeepCopyIrTreeWithDescriptors
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.deserializedPropertyIfAccessor
import org.jetbrains.kotlin.backend.konan.descriptors.isDeserializableCallable
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.backend.konan.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.llvm.base64Decode
import org.jetbrains.kotlin.backend.konan.llvm.base64Encode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFINED
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrEnumEntryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer
import org.jetbrains.kotlin.serialization.KonanIr
import org.jetbrains.kotlin.serialization.KonanIr.IrConst.ValueCase.*
import org.jetbrains.kotlin.serialization.KonanIr.IrOperation.OperationCase.*
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassConstructorDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor


internal class IrSerializer(val context: Context, 
    val descriptorTable: DescriptorTable,
    val stringTable: KonanStringTable, 
    val rootFunctionSerializer: KonanDescriptorSerializer,
    var rootFunction: FunctionDescriptor) {

    val loopIndex = mutableMapOf<IrLoop, Int>()
    var currentLoopIndex = 0
    val localDeclarationSerializer 
        = LocalDeclarationSerializer(context, rootFunctionSerializer)
    val irDescriptorSerializer 
        = IrDescriptorSerializer(context, descriptorTable, 
            stringTable, localDeclarationSerializer, rootFunction)

    fun serializeInlineBody(): String {
        val declaration = context.ir.originalModuleIndex.functions[rootFunction]!!
        context.log{"INLINE: ${ir2stringWhole(declaration)}"}
        return encodeDeclaration(declaration)
    }

    fun serializeKotlinType(type: KotlinType): KonanIr.KotlinType {
        context.log{"### serializing KotlinType: " + type}
        return irDescriptorSerializer.serializeKotlinType(type)
    }

    fun serializeDescriptor(descriptor: DeclarationDescriptor): KonanIr.KotlinDescriptor {
        context.log{"### serializeDescriptor $descriptor"}

        // Behind this call starts a large world of 
        // descriptor serialization for IR.
        return irDescriptorSerializer.serializeDescriptor(descriptor)
    }

    fun serializeCoordinates(start: Int, end: Int): KonanIr.Coordinates {
        val proto = KonanIr.Coordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build()
        return proto
    }

    fun serializeTypeMap(typeArguments: Map<TypeParameterDescriptor, KotlinType>): KonanIr.TypeMap {
        val proto = KonanIr.TypeMap.newBuilder()
        typeArguments.forEach { key, value ->
            val pair = KonanIr.TypeMap.Pair.newBuilder()
                .setDescriptor(serializeDescriptor(key))
                .setType(serializeKotlinType(value))
                .build()
            proto.addPair(pair)
        }
        return proto.build()
    }

    fun serializeTypeArguments(call: IrMemberAccessExpression): KonanIr.TypeMap {
 
        val typeMap = mutableMapOf<TypeParameterDescriptor, KotlinType>()
        call.descriptor.original.typeParameters.forEach {
            val type = call.getTypeArgument(it)
            if (type != null) typeMap.put(it, type)
        }
        return serializeTypeMap(typeMap)

    }


    /* -------------------------------------------------------------------------- */

    fun serializeBlockBody(expression: IrBlockBody): KonanIr.IrBlockBody {
        val proto = KonanIr.IrBlockBody.newBuilder()
        expression.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    fun serializeBranch(branch: IrBranch): KonanIr.IrBranch {
        val proto = KonanIr.IrBranch.newBuilder()

        proto.setCondition(serializeExpression(branch.condition))
        proto.setResult(serializeExpression(branch.result))

        return proto.build()
    }

    fun serializeBlock(block: IrBlock): KonanIr.IrBlock {
        val isLambdaOrigin = 
            block.origin == IrStatementOrigin.LAMBDA ||
            block.origin == IrStatementOrigin.ANONYMOUS_FUNCTION
        val proto = KonanIr.IrBlock.newBuilder()
            .setIsTransparentScope(block.isTransparentScope)
            .setIsLambdaOrigin(isLambdaOrigin)
        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    fun serializeCatch(catch: IrCatch): KonanIr.IrCatch {
        val proto = KonanIr.IrCatch.newBuilder()
           .setParameter(serializeDescriptor(catch.parameter))
           .setResult(serializeExpression(catch.result))
        return proto.build()
    }

    fun serializeStringConcat(expression: IrStringConcatenation): KonanIr.IrStringConcat {
        val proto = KonanIr.IrStringConcat.newBuilder()
        expression.arguments.forEach {
            proto.addArgument(serializeExpression(it))
        }
        return proto.build()
    }

    fun irCallToPrimitiveKind(call: IrCall): KonanIr.IrCall.Primitive = when (call) {
        is IrNullaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.NULLARY
        is IrUnaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.UNARY
        is IrBinaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.BINARY
        else
            -> KonanIr.IrCall.Primitive.NOT_PRIMITIVE
    }

    fun serializeMemberAccessCommon(call: IrMemberAccessExpression): KonanIr.MemberAccessCommon {
        val proto = KonanIr.MemberAccessCommon.newBuilder()
        if (call.extensionReceiver != null) {
            proto.setExtensionReceiver(serializeExpression(call.extensionReceiver!!))
        }

        if (call.dispatchReceiver != null)  {
            proto.setDispatchReceiver(serializeExpression(call.dispatchReceiver!!))
        }
        proto.setTypeMap(serializeTypeArguments(call))

        call.descriptor.valueParameters.forEach {
            val actual = call.getValueArgument(it.index)
            if (actual == null) {
                // Am I observing an IR generation regression?
                // I see a lack of arg for an empty vararg,
                // rather than an empty vararg node.
                assert(it.varargElementType != null ||
                    it.declaresDefaultValue())
            } else {
                val arg = actual
                val argProto = serializeExpression(arg)
                proto.addValueArgument(argProto)
            }
        }
        return proto.build()
    }

    fun serializeCall(call: IrCall): KonanIr.IrCall {
        val proto = KonanIr.IrCall.newBuilder()

        proto.setKind(irCallToPrimitiveKind(call))
        proto.setDescriptor(serializeDescriptor(call.descriptor))

        if (call.superQualifier != null) {
            proto.setSuper(serializeDescriptor(call.superQualifier!!))
        }
        proto.setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    fun serializeCallableReference(callable: IrCallableReference): KonanIr.IrCallableReference {
        val proto = KonanIr.IrCallableReference.newBuilder()
            .setDescriptor(serializeDescriptor(callable.descriptor))
            .setTypeMap(serializeTypeArguments(callable))
        return proto.build()
    }

    fun serializeConst(value: IrConst<*>): KonanIr.IrConst {
        val proto = KonanIr.IrConst.newBuilder()
        when (value.kind) {
            IrConstKind.Null        -> proto.setNull(true)
            IrConstKind.Boolean     -> proto.setBoolean(value.value as Boolean)
            IrConstKind.Byte        -> proto.setByte((value.value as Byte).toInt())
            IrConstKind.Short       -> proto.setShort((value.value as Short).toInt())
            IrConstKind.Int         -> proto.setInt(value.value as Int)
            IrConstKind.Long        -> proto.setLong(value.value as Long)
            IrConstKind.String      -> proto.setString(value.value as String)
            IrConstKind.Float       -> proto.setFloat(value.value as Float)
            IrConstKind.Double      -> proto.setDouble(value.value as Double)
            else -> {
                TODO("Const type serialization not implemented yet: ${ir2string(value)}")
            }
        }
        return proto.build()
    }

    fun serializeDelegatingConstructorCall(call: IrDelegatingConstructorCall): KonanIr.IrDelegatingConstructorCall {
        val proto = KonanIr.IrDelegatingConstructorCall.newBuilder()
            .setDescriptor(serializeDescriptor(call.descriptor))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    fun serializeEnumConstructorCall(call: IrEnumConstructorCall): KonanIr.IrEnumConstructorCall {
        val proto = KonanIr.IrEnumConstructorCall.newBuilder()
            .setDescriptor(serializeDescriptor(call.descriptor))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    fun serializeGetEnumValue(expression: IrGetEnumValue): KonanIr.IrGetEnumValue {
        val proto = KonanIr.IrGetEnumValue.newBuilder()
            .setType(serializeKotlinType(expression.type))
            .setDescriptor(serializeDescriptor(expression.descriptor))
        return proto.build()
    }

    fun serializeFieldAccessCommon(expression: IrFieldAccessExpression): KonanIr.FieldAccessCommon {
        val proto = KonanIr.FieldAccessCommon.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
        val superQualifier = expression.superQualifier
        if (superQualifier != null)
            proto.setSuper(serializeDescriptor(superQualifier))
        val receiver = expression.receiver
        if (receiver != null) 
            proto.setReciever(serializeExpression(receiver))
        return proto.build()
    }

    fun serializeGetField(expression: IrGetField): KonanIr.IrGetField {
        val proto = KonanIr.IrGetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
        return proto.build()
    }

    fun serializeGetValue(expression: IrGetValue): KonanIr.IrGetValue {
        val proto = KonanIr.IrGetValue.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
        return proto.build()
    }

    fun serializeGetObject(expression: IrGetObjectValue): KonanIr.IrGetObject {
        val proto = KonanIr.IrGetObject.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
        return proto.build()
    }

    fun serializeInstanceInitializerCall(call: IrInstanceInitializerCall): KonanIr.IrInstanceInitializerCall {
        val proto = KonanIr.IrInstanceInitializerCall.newBuilder()

        proto.setDescriptor(serializeDescriptor(call.classDescriptor))

        return proto.build()
    }

    fun serializeReturn(expression: IrReturn): KonanIr.IrReturn {
        val proto = KonanIr.IrReturn.newBuilder()
            .setReturnTarget(serializeDescriptor(expression.returnTarget))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    fun serializeSetField(expression: IrSetField): KonanIr.IrSetField {
        val proto = KonanIr.IrSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    fun serializeSetVariable(expression: IrSetVariable): KonanIr.IrSetVariable {
        val proto = KonanIr.IrSetVariable.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    fun serializeThrow(expression: IrThrow): KonanIr.IrThrow {
        val proto = KonanIr.IrThrow.newBuilder()
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    fun serializeTry(expression: IrTry): KonanIr.IrTry {
        val proto = KonanIr.IrTry.newBuilder()
            .setResult(serializeExpression(expression.tryResult))
        val catchList = expression.catches
        catchList.forEach {
            proto.addCatch(serializeStatement(it))
        }
        val finallyExpression = expression.finallyExpression
        if (finallyExpression != null) {
            proto.setFinally(serializeExpression(finallyExpression))
        }
        return proto.build()
    }

    fun serializeTypeOperator(operator: IrTypeOperator): KonanIr.IrTypeOperator = when (operator) {
        IrTypeOperator.CAST
            -> KonanIr.IrTypeOperator.CAST
        IrTypeOperator.IMPLICIT_CAST
            -> KonanIr.IrTypeOperator.IMPLICIT_CAST
        IrTypeOperator.IMPLICIT_NOTNULL
            -> KonanIr.IrTypeOperator.IMPLICIT_NOTNULL
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
            -> KonanIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        IrTypeOperator.SAFE_CAST
            -> KonanIr.IrTypeOperator.SAFE_CAST
        IrTypeOperator.INSTANCEOF
            -> KonanIr.IrTypeOperator.INSTANCEOF
        IrTypeOperator.NOT_INSTANCEOF
            -> KonanIr.IrTypeOperator.NOT_INSTANCEOF
        else -> TODO("Unknown type operator")
    }

    fun serializeTypeOp(expression: IrTypeOperatorCall): KonanIr.IrTypeOp {
        val proto = KonanIr.IrTypeOp.newBuilder()
            .setOperator(serializeTypeOperator(expression.operator))
            .setOperand(serializeKotlinType(expression.typeOperand))
            .setArgument(serializeExpression(expression.argument))
        return proto.build()

    }

    fun serializeVararg(expression: IrVararg): KonanIr.IrVararg {
        val proto = KonanIr.IrVararg.newBuilder()
            .setElementType(serializeKotlinType(expression.varargElementType))
        return proto.build()
    }

    fun serializeWhen(expression: IrWhen): KonanIr.IrWhen {
        val proto = KonanIr.IrWhen.newBuilder()

        val branches = expression.branches
        branches.forEach {
            proto.addBranch(serializeStatement(it))
        }

        return proto.build()
    }

    fun serializeWhile(expression: IrWhileLoop): KonanIr.IrWhile {
        val proto = KonanIr.IrWhile.newBuilder()
            .setCondition(serializeExpression(expression.condition))
        val label = expression.label
        if (label != null) {
            proto.setLabel(label)
        }

        proto.setLoopId(currentLoopIndex)
        loopIndex.put(expression, currentLoopIndex++)

        val body = expression.body
        if (body != null) {
            proto.setBody(serializeExpression(body))
        }
        return proto.build()

    }

    fun serializeBreak(expression: IrBreak): KonanIr.IrBreak {
        val proto = KonanIr.IrBreak.newBuilder()
        val label = expression.label
        if (label != null) {
            proto.setLabel(label)
        }
        val loopId = loopIndex[expression.loop]!!
        proto.setLoopId(loopId)

        return proto.build()
    }

    fun serializeContinue(expression: IrContinue): KonanIr.IrContinue {
        val proto = KonanIr.IrContinue.newBuilder()
        val label = expression.label
        if (label != null) {
            proto.setLabel(label)
        }
        val loopId = loopIndex[expression.loop]!!
        proto.setLoopId(loopId)

        return proto.build()
    }

    fun serializeExpression(expression: IrExpression): KonanIr.IrExpression {
        context.log{"### serializing Expression: ${ir2string(expression)}"}

        val coordinates = serializeCoordinates(expression.startOffset, expression.endOffset)
        val proto = KonanIr.IrExpression.newBuilder()
            .setType(serializeKotlinType(expression.type))
            .setCoordinates(coordinates)

        val operationProto = KonanIr.IrOperation.newBuilder()
        
        when (expression) {
            is IrBlock       -> operationProto.setBlock(serializeBlock(expression))
            is IrBreak       -> operationProto.setBreak(serializeBreak(expression))
            is IrCall        -> operationProto.setCall(serializeCall(expression))
            is IrCallableReference
                             -> operationProto.setCallableReference(serializeCallableReference(expression))
            is IrConst<*>    -> operationProto.setConst(serializeConst(expression))
            is IrContinue    -> operationProto.setContinue(serializeContinue(expression))
            is IrDelegatingConstructorCall
                             -> operationProto.setDelegatingConstructorCall(serializeDelegatingConstructorCall(expression))
            is IrGetField    -> operationProto.setGetField(serializeGetField(expression))
            is IrGetValue    -> operationProto.setGetValue(serializeGetValue(expression))
            is IrGetEnumValue    
                             -> operationProto.setGetEnumValue(serializeGetEnumValue(expression))
            is IrGetObjectValue    
                             -> operationProto.setGetObject(serializeGetObject(expression))
            is IrInstanceInitializerCall        
                             -> operationProto.setInstanceInitializerCall(serializeInstanceInitializerCall(expression))
            is IrReturn      -> operationProto.setReturn(serializeReturn(expression))
            is IrSetField    -> operationProto.setSetField(serializeSetField(expression))
            is IrSetVariable -> operationProto.setSetVariable(serializeSetVariable(expression))
            is IrStringConcatenation 
                             -> operationProto.setStringConcat(serializeStringConcat(expression))
            is IrThrow       -> operationProto.setThrow(serializeThrow(expression))
            is IrTry         -> operationProto.setTry(serializeTry(expression))
            is IrTypeOperatorCall 
                             -> operationProto.setTypeOp(serializeTypeOp(expression))
            is IrVararg      -> operationProto.setVararg(serializeVararg(expression))
            is IrWhen        -> operationProto.setWhen(serializeWhen(expression))
            is IrWhileLoop   -> operationProto.setWhile(serializeWhile(expression))
            else -> {
                TODO("Expression serialization not implemented yet: ${ir2string(expression)}.")
            }
        }
        proto.setOperation(operationProto)

        return proto.build()
    }

    fun serializeStatement(statement: IrElement): KonanIr.IrStatement {
        context.log{"### serializing Statement: ${ir2string(statement)}"}

        val coordinates = serializeCoordinates(statement.startOffset, statement.endOffset)
        val proto = KonanIr.IrStatement.newBuilder()
            .setCoordinates(coordinates)

        when (statement) {
            is IrDeclaration -> proto.setDeclaration(serializeDeclaration(statement))
            is IrExpression -> proto.setExpression(serializeExpression(statement))
            is IrBlockBody -> proto.setBlockBody(serializeBlockBody(statement))
            is IrBranch    -> proto.setBranch(serializeBranch(statement))
            is IrCatch    -> proto.setCatch(serializeCatch(statement))
            else -> {
                TODO("Statement not implemented yet: ${ir2string(statement)}")
            }
        }
        return proto.build()
    }

    fun serializeIrFunction(function: IrFunction): KonanIr.IrFunction {
        val proto = KonanIr.IrFunction.newBuilder()
        val body = function.body
        if (body != null)  proto.setBody(serializeStatement(body))

        function.descriptor.valueParameters.forEachIndexed { index, it ->
            val default = function.getDefault(it)
            if (default != null) {
                val pair = KonanIr.IrFunction.DefaultArgument.newBuilder()
                pair.position = index
                pair.value = serializeExpression(default.expression)
                proto.addDefaultArgument(pair)
            }
        }

        return proto.build()
    }

    fun serializeIrProperty(property: IrProperty): KonanIr.IrProperty {
        val proto = KonanIr.IrProperty.newBuilder()
            .setIsDelegated(property.isDelegated)
        val backingField = property.backingField
        val getter = property.getter
        val setter = property.setter
        if (backingField != null) 
            proto.setBackingField(serializeIrField(backingField))
        if (getter != null) 
            proto.setGetter(serializeIrFunction(getter))
        if (setter != null) 
            proto.setSetter(serializeIrFunction(setter))

        return proto.build()
    }

    fun serializeIrField(field: IrField): KonanIr.IrProperty.IrField {
        val proto = KonanIr.IrProperty.IrField.newBuilder()
        val field = field.initializer?.expression
        if (field != null) {
            proto.setInitializer(
                serializeExpression(field))
        }
        return proto.build()
    }

    fun serializeIrVariable(variable: IrVariable): KonanIr.IrVar {
        val proto = KonanIr.IrVar.newBuilder()
        val initializer = variable.initializer
        if (initializer != null) {
            proto.setInitializer(serializeExpression(initializer))
        }
        return proto.build()
    }

    fun serializeIrClass(clazz: IrClass): KonanIr.IrClass {
        val proto = KonanIr.IrClass.newBuilder()

        // TODO: As of now we get here only for anonymous local objects.
        // There is still some work needed to support them.
        // Until it is done, let's pretend all IrClasses are empty.
        // So that we don't have to deal with their type tables.
        /*
        val declarations = clazz.declarations
        declarations.forEach {
            val descriptor = it.descriptor
            if (descriptor !is CallableMemberDescriptor || descriptor.kind.isReal) {
                proto.addMember(serializeDeclaration(it))
            }
        }
        */
        return proto.build()
    }

    fun serializeIrEnumEntry(enumEntry: IrEnumEntry): KonanIr.IrEnumEntry {
        val proto = KonanIr.IrEnumEntry.newBuilder()
        val initializer = enumEntry.initializerExpression!!
        proto.setInitializer(serializeExpression(initializer))
        val correspondingClass = enumEntry.correspondingClass
        if (correspondingClass != null) {
            proto.setCorrespondingClass(serializeDeclaration(correspondingClass))
        }
        return proto.build()
    }

    fun serializeDeclaration(declaration: IrDeclaration): KonanIr.IrDeclaration {
        context.log{"### serializing Declaration: ${ir2string(declaration)}"}

        val descriptor = declaration.descriptor

        if (descriptor != rootFunction &&
            !(declaration is IrVariable)) {
            localDeclarationSerializer.pushContext(descriptor)
        }

        var kotlinDescriptor = serializeDescriptor(descriptor)
        var realDescriptor: KonanIr.DeclarationDescriptor? = null
        if (descriptor != rootFunction) {
            realDescriptor = localDeclarationSerializer.serializeLocalDeclaration(descriptor)
        }
        val declarator = KonanIr.IrDeclarator.newBuilder()

        when (declaration) {
            is IrFunction 
                -> declarator.setFunction(serializeIrFunction(declaration))
            is IrVariable 
                -> declarator.setVariable(serializeIrVariable(declaration))
            is IrClass
                -> declarator.setIrClass(serializeIrClass(declaration))
            is IrEnumEntry
                -> declarator.setIrEnumEntry(serializeIrEnumEntry(declaration))
            is IrProperty
                -> declarator.setIrProperty(serializeIrProperty(declaration))
            else 
                -> {
                TODO("Declaration serialization not supported yet: $declaration")
            }
        }

        if (!(declaration is IrVariable)) {
            localDeclarationSerializer.popContext(descriptor)
        }

        if (descriptor != rootFunction) {
            val localDeclaration = KonanIr.LocalDeclaration
                .newBuilder()
                .setDescriptor(realDescriptor!!)
                .build()
            kotlinDescriptor = kotlinDescriptor
                .toBuilder()
                .setIrLocalDeclaration(localDeclaration)
                .build()
        }

        val coordinates = serializeCoordinates(declaration.startOffset, declaration.endOffset)
        val proto = KonanIr.IrDeclaration.newBuilder()
            //.setKind(declaration.irKind())
            .setDescriptor(kotlinDescriptor)
            .setCoordinates(coordinates)


        proto.setDeclarator(declarator)

        return proto.build()
    }

    fun encodeDeclaration(declaration: IrDeclaration): String {
        val proto = serializeDeclaration(declaration)
        val byteArray = proto.toByteArray()
        val base64 = base64Encode(byteArray)
        return base64
    }


}

// --------- Deserializer part -----------------------------

internal class IrDeserializer(val context: Context, 
    val rootFunction: FunctionDescriptor) {

    val loopIndex = mutableMapOf<Int, IrLoop>()

    val rootMember = rootFunction.deserializedPropertyIfAccessor
    val localDeserializer = LocalDeclarationDeserializer(rootMember)

    val descriptorDeserializer = IrDescriptorDeserializer(
        context, rootMember, localDeserializer)

    fun deserializeKotlinType(proto: KonanIr.KotlinType) 
        = descriptorDeserializer.deserializeKotlinType(proto)

    fun deserializeDescriptor(proto: KonanIr.KotlinDescriptor) 
        = descriptorDeserializer.deserializeDescriptor(proto)

    fun deserializeTypeMap(descriptor: CallableDescriptor, proto: KonanIr.TypeMap): 
        Map<TypeParameterDescriptor, KotlinType> {
        val typeMap  = mutableMapOf<TypeParameterDescriptor, KotlinType>()
        val pairProtos = proto.getPairList()
        pairProtos.forEachIndexed { index, pair ->
            val typeParameter = descriptor.original.typeParameters[index]

            typeMap.put(typeParameter, 
                deserializeKotlinType(pair.getType()))
        }
        context.log{"### deserialized typeMap = $typeMap"}
        return typeMap
    }

    fun deserializeBlockBody(proto: KonanIr.IrBlockBody, 
        start: Int, end: Int): IrBlockBody {

        val statements = mutableListOf<IrStatement>()

        val statementProtos = proto.getStatementList()
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        return IrBlockBodyImpl(start, end, statements)
    }

    fun deserializeBranch(proto: KonanIr.IrBranch, start: Int, end: Int): IrBranch {

        val condition = deserializeExpression(proto.getCondition())
        val result = deserializeExpression(proto.getResult())

        return IrBranchImpl(start, end, condition, result)
    }

    fun deserializeCatch(proto: KonanIr.IrCatch, start: Int, end: Int): IrCatch {
        val parameter = deserializeDescriptor(proto.getParameter()) as VariableDescriptor
        val catchParameter = IrVariableImpl(start, end, IrDeclarationOrigin.CATCH_PARAMETER, parameter)
        val result = deserializeExpression(proto.getResult())

        return IrCatchImpl(start, end, catchParameter, result)
    }

    fun deserializeStatement(proto: KonanIr.IrStatement): IrElement {
        val start = proto.getCoordinates().getStartOffset()
        val end = proto.getCoordinates().getEndOffset()
        val element = when {
            proto.hasBlockBody() 
                -> deserializeBlockBody(proto.getBlockBody(), start, end)
            proto.hasBranch()
                -> deserializeBranch(proto.getBranch(), start, end)
            proto.hasCatch()
                -> deserializeCatch(proto.getCatch(), start, end)
            proto.hasDeclaration()
                -> deserializeDeclaration(proto.getDeclaration())
            proto.hasExpression()
                -> deserializeExpression(proto.getExpression())
            else -> {
                TODO("Statement deserialization not implemented")
            }
        }

        context.log{"### Deserialized statement: ${ir2string(element)}"}

        return element
    }

    fun deserializeBlock(proto: KonanIr.IrBlock, start: Int, end: Int, type: KotlinType): IrBlock {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.getStatementList()
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        val isLambdaOrigin = if (proto.isLambdaOrigin) IrStatementOrigin.LAMBDA else null

        val block = IrBlockImpl(start, end, type, isLambdaOrigin, statements)

        // TODO: Need to set isTransparentScope somehow
        return block
    }

    fun deserializeMemberAccessCommon(access: IrMemberAccessExpression, proto: KonanIr.MemberAccessCommon) {

        proto.valueArgumentList.mapIndexed { i, expr ->
            access.putValueArgument(i, deserializeExpression(expr))
        }

        if (proto.hasDispatchReceiver()) {
            access.dispatchReceiver = deserializeExpression(proto.dispatchReceiver)
        }
        if (proto.hasExtensionReceiver()) {
            access.extensionReceiver = deserializeExpression(proto.extensionReceiver)
        }
    }

    fun deserializeCall(proto: KonanIr.IrCall, start: Int, end: Int, type: KotlinType): IrCall {
        val descriptor = deserializeDescriptor(proto.getDescriptor()) as FunctionDescriptor

        val superDescriptor = if (proto.hasSuper()) {
            deserializeDescriptor(proto.getSuper()) as ClassDescriptor
        } else null

        val typeArgs = deserializeTypeMap(descriptor, proto.memberAccess.getTypeMap())
        val call: IrCall = when (proto.kind) {
            KonanIr.IrCall.Primitive.NOT_PRIMITIVE ->
                // TODO: implement the last three args here.
                IrCallImpl(start, end, type, descriptor, typeArgs , null, superDescriptor)
            KonanIr.IrCall.Primitive.NULLARY ->
                IrNullaryPrimitiveImpl(start, end, null, createFunctionSymbol(descriptor))
            KonanIr.IrCall.Primitive.UNARY ->
                IrUnaryPrimitiveImpl(start, end, null, descriptor)
            KonanIr.IrCall.Primitive.BINARY ->
                IrBinaryPrimitiveImpl(start, end, null, descriptor)
            else -> TODO("Unexpected primitive IrCall.")
        }
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    fun deserializeCallableReference(proto: KonanIr.IrCallableReference, 
        start: Int, end: Int, type: KotlinType): IrCallableReference {

        val descriptor = deserializeDescriptor(proto.getDescriptor()) as CallableDescriptor
        val typeMap = deserializeTypeMap(descriptor, proto.typeMap)
        val callable = when (descriptor) {
            is FunctionDescriptor -> IrFunctionReferenceImpl(start, end, type, descriptor, typeMap, null)
            else -> TODO()
        }
        return callable
    }

    fun deserializeDelegatingConstructorCall(proto: KonanIr.IrDelegatingConstructorCall, start: Int, end: Int, type: KotlinType): IrDelegatingConstructorCall {
        val descriptor = deserializeDescriptor(proto.getDescriptor()) as ClassConstructorDescriptor
        val typeArgs = deserializeTypeMap(descriptor, proto.memberAccess.getTypeMap())

        val call = IrDelegatingConstructorCallImpl(start, end, descriptor, typeArgs)

        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    fun deserializeEnumConstructorCall(proto: KonanIr.IrEnumConstructorCall, start: Int, end: Int, type: KotlinType): IrEnumConstructorCall {
        val descriptor = deserializeDescriptor(proto.getDescriptor()) as ClassConstructorDescriptor
        val call = IrEnumConstructorCallImpl(start, end, descriptor)
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    fun deserializeGetValue(proto: KonanIr.IrGetValue, start: Int, end: Int, type: KotlinType): IrGetValue {
        val descriptor = deserializeDescriptor(proto.descriptor) as ValueDescriptor

        // TODO: origin!
        return IrGetValueImpl(start, end, descriptor, null)
    }

    fun deserializeGetEnumValue(proto: KonanIr.IrGetEnumValue, start: Int, end: Int, type: KotlinType): IrGetEnumValue {
        val type = deserializeKotlinType(proto.type)
        val descriptor = deserializeDescriptor(proto.descriptor) as ClassDescriptor

        return IrGetEnumValueImpl(start, end, type, descriptor)
    }

    fun deserializeGetObject(proto: KonanIr.IrGetObject, start: Int, end: Int, type: KotlinType): IrGetObjectValue {
        val descriptor = deserializeDescriptor(proto.descriptor) as ClassDescriptor
        return IrGetObjectValueImpl(start, end, type, descriptor)
    }

    fun deserializeInstanceInitializerCall(proto: KonanIr.IrInstanceInitializerCall, start: Int, end: Int, type: KotlinType): IrInstanceInitializerCall {
        val descriptor = deserializeDescriptor(proto.getDescriptor()) as ClassDescriptor

        return IrInstanceInitializerCallImpl(start, end, descriptor)
    }

    fun deserializeReturn(proto: KonanIr.IrReturn, start: Int, end: Int, type: KotlinType): IrReturn {
        val descriptor = 
            deserializeDescriptor(proto.getReturnTarget()) as FunctionDescriptor
        val value = deserializeExpression(proto.getValue())
        return IrReturnImpl(start, end, type, descriptor, value)
    }

    fun deserializeSetVariable(proto: KonanIr.IrSetVariable, start: Int, end: Int, type: KotlinType): IrSetVariable {
        val descriptor = deserializeDescriptor(proto.getDescriptor()) as VariableDescriptor
        val value = deserializeExpression(proto.getValue())
        return IrSetVariableImpl(start, end, descriptor, value, null)
    }

    fun deserializeStringConcat(proto: KonanIr.IrStringConcat, start: Int, end: Int, type: KotlinType): IrStringConcatenation {
        val argumentProtos = proto.getArgumentList()
        val arguments = mutableListOf<IrExpression>()

        argumentProtos.forEach {
            arguments.add(deserializeExpression(it))
        }
        return IrStringConcatenationImpl(start, end, type, arguments)
    }

    fun deserializeThrow(proto: KonanIr.IrThrow, start: Int, end: Int, type: KotlinType): IrThrowImpl {
        return IrThrowImpl(start, end, type, deserializeExpression(proto.getValue()))
    }

    fun deserializeTry(proto: KonanIr.IrTry, start: Int, end: Int, type: KotlinType): IrTryImpl {
        val result = deserializeExpression(proto.getResult())
        val catches = mutableListOf<IrCatch>()
        proto.getCatchList().forEach {
            catches.add(deserializeStatement(it) as IrCatch) 
        }
        val finallyExpression = 
            if (proto.hasFinally()) deserializeExpression(proto.getFinally()) else null
        return IrTryImpl(start, end, type, result, catches, finallyExpression)
    }

    fun deserializeTypeOperator(operator: KonanIr.IrTypeOperator): IrTypeOperator {
        when (operator) {
            KonanIr.IrTypeOperator.CAST
                -> return IrTypeOperator.CAST
            KonanIr.IrTypeOperator.IMPLICIT_CAST
                -> return IrTypeOperator.IMPLICIT_CAST
            KonanIr.IrTypeOperator.IMPLICIT_NOTNULL
                -> return IrTypeOperator.IMPLICIT_NOTNULL
            KonanIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
                -> return IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
            KonanIr.IrTypeOperator.SAFE_CAST
                -> return IrTypeOperator.SAFE_CAST
            KonanIr.IrTypeOperator.INSTANCEOF
                -> return IrTypeOperator.INSTANCEOF
            KonanIr.IrTypeOperator.NOT_INSTANCEOF
                -> return IrTypeOperator.NOT_INSTANCEOF
            else -> TODO("Unknown type operator")
        }
    }

    fun deserializeTypeOp(proto: KonanIr.IrTypeOp, start: Int, end: Int, type: KotlinType) : IrTypeOperatorCall {
        val operator = deserializeTypeOperator(proto.getOperator())
        val operand = deserializeKotlinType(proto.getOperand())
        val argument = deserializeExpression(proto.getArgument())
        return IrTypeOperatorCallImpl(start, end, type, operator, operand, argument)
    }

    fun deserializeVararg(proto: KonanIr.IrVararg, start: Int, end: Int, type: KotlinType): IrVararg {
        val elementType = deserializeKotlinType(proto.getElementType())
        return IrVarargImpl(start, end, type, elementType)
    }

    fun deserializeWhen(proto: KonanIr.IrWhen, start: Int, end: Int, type: KotlinType): IrWhen {
        val branches = mutableListOf<IrBranch>()

        proto.getBranchList().forEach {
            branches.add(deserializeStatement(it) as IrBranch)
        }

        // TODO: provide some origin!
        return  IrWhenImpl(start, end, type, null, branches)
    }

    fun deserializeWhile(proto: KonanIr.IrWhile, start: Int, end: Int, type: KotlinType): IrWhileLoop {
        // we create the IrLoop before deserializing the body, so that 
        // IrBreak statements have something to put into 'loop' field.
        val loop = IrWhileLoopImpl(start, end, type, null)

        val loopId = proto.getLoopId()
        loopIndex.getOrPut(loopId){loop}

        val condition = deserializeExpression(proto.getCondition())
        val label = if (proto.hasLabel()) proto.getLabel() else null
        val body = if (proto.hasBody()) deserializeExpression(proto.getBody()) else null

        loop.label = label
        loop.condition = condition
        loop.body = body

        return loop
    }

    fun deserializeBreak(proto: KonanIr.IrBreak, start: Int, end: Int, type: KotlinType): IrBreak {
        val label = if(proto.hasLabel()) proto.getLabel() else null
        val loopId = proto.getLoopId()
        val loop = loopIndex[loopId]!!
        val irBreak = IrBreakImpl(start, end, type, loop)
        irBreak.label = label

        return irBreak
    }

    fun deserializeContinue(proto: KonanIr.IrContinue, start: Int, end: Int, type: KotlinType): IrContinue {
        val label = if(proto.hasLabel()) proto.getLabel() else null
        val loopId = proto.getLoopId()
        val loop = loopIndex[loopId]!!
        val irContinue = IrContinueImpl(start, end, type, loop)
        irContinue.label = label

        return irContinue
    }

    fun deserializeConst(proto: KonanIr.IrConst, start: Int, end: Int, type: KotlinType): IrExpression =
        when(proto.valueCase) {
            NULL
                -> IrConstImpl.constNull(start, end, type)
            BOOLEAN
                -> IrConstImpl.boolean(start, end, type, proto.boolean)
            BYTE
                -> IrConstImpl.byte(start, end, type, proto.byte.toByte())
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
            else -> {
                TODO("Not all const types have been implemented")
            }
        }

    fun deserializeOperation(proto: KonanIr.IrOperation, start: Int, end: Int, type: KotlinType): IrExpression =
        when (proto.operationCase) {
            BLOCK
                -> deserializeBlock(proto.block, start, end, type)
            BREAK
                -> deserializeBreak(proto.getBreak(), start, end, type)
            CALL
                -> deserializeCall(proto.call, start, end, type)
            CALLABLE_REFERENCE
                -> deserializeCallableReference(proto.callableReference, start, end, type)
            CONST
                -> deserializeConst(proto.const, start, end, type)
            CONTINUE
                -> deserializeContinue(proto.getContinue(), start, end, type)
            DELEGATING_CONSTRUCTOR_CALL
                -> deserializeDelegatingConstructorCall(proto.delegatingConstructorCall, start, end, type)
            GET_VALUE
                -> deserializeGetValue(proto.getValue, start, end, type)
            GET_ENUM_VALUE
                -> deserializeGetEnumValue(proto.getEnumValue, start, end, type)
            GET_OBJECT
                -> deserializeGetObject(proto.getObject, start, end, type)
            INSTANCE_INITIALIZER_CALL
                -> deserializeInstanceInitializerCall(proto.instanceInitializerCall, start, end, type)
            RETURN
                -> deserializeReturn(proto.getReturn(), start, end, type)
            SET_VARIABLE
                -> deserializeSetVariable(proto.setVariable, start, end, type)
            STRING_CONCAT
                -> deserializeStringConcat(proto.stringConcat, start, end, type)
            THROW
                -> deserializeThrow(proto.getThrow(), start, end, type)
            TRY
                -> deserializeTry(proto.getTry(), start, end, type)
            TYPE_OP
                -> deserializeTypeOp(proto.typeOp, start, end, type)
            VARARG
                -> deserializeVararg(proto.vararg, start, end, type)
            WHEN
                -> deserializeWhen(proto.getWhen(), start, end, type)
            WHILE
                -> deserializeWhile(proto.getWhile(), start, end, type)
            else -> {
                TODO("Expression deserialization not implemented}")
            }
        }

    fun deserializeExpression(proto: KonanIr.IrExpression): IrExpression {
        val start = proto.getCoordinates().getStartOffset()
        val end = proto.getCoordinates().getEndOffset()
        val type = deserializeKotlinType(proto.getType())
        val operation = proto.getOperation()
        val expression = deserializeOperation(operation, start, end, type)

        context.log{"### Deserialized expression: ${ir2string(expression)}"}
        return expression
    }

    fun deserializeIrClass(proto: KonanIr.IrClass, descriptor: ClassDescriptor, start: Int, end: Int, origin: IrDeclarationOrigin): IrClass {
        val members = mutableListOf<IrDeclaration>()

        proto.getMemberList().forEach {
            members.add(deserializeDeclaration(it))
        }

        val clazz = IrClassImpl(start, end, origin, descriptor, members)

        clazz.createParameterDeclarations()

        return clazz

    }

    fun deserializeIrFunction(proto: KonanIr.IrFunction, descriptor: FunctionDescriptor,
        start: Int, end: Int, origin: IrDeclarationOrigin): IrFunction {

        val body = deserializeStatement(proto.getBody())
        val function = IrFunctionImpl(start, end, origin, 
            descriptor, body as IrBody)

        function.createParameterDeclarations()

        proto.defaultArgumentList.forEach {
            val expr = deserializeExpression(it.value)

            function.putDefault(
                descriptor.valueParameters.get(it.position), 
                IrExpressionBodyImpl(start, end, expr))
        }

        return function
    }

    fun deserializeIrVariable(proto: KonanIr.IrVar, descriptor: VariableDescriptor, 
        start: Int, end: Int, origin: IrDeclarationOrigin): IrVariable {

        val initializer = deserializeExpression(proto.getInitializer())
        val variable = IrVariableImpl(start, end, origin, descriptor, initializer)

        return variable
    }

    fun deserializeIrEnumEntry(proto: KonanIr.IrEnumEntry, descriptor: ClassDescriptor,
        start: Int, end: Int, origin: IrDeclarationOrigin): IrEnumEntry {

        val enumEntry = IrEnumEntryImpl(start, end, origin, descriptor)
        if (proto.hasCorrespondingClass()) {
            enumEntry.correspondingClass = deserializeDeclaration(proto.correspondingClass) as IrClass
        }
        enumEntry.initializerExpression = deserializeExpression(proto.initializer)

        return enumEntry
    }

    fun deserializeDeclaration(proto: KonanIr.IrDeclaration): IrDeclaration {

        val descriptor = deserializeDescriptor(proto.descriptor)

        if (!(descriptor is VariableDescriptor) && descriptor != rootFunction)
            localDeserializer.pushContext(descriptor)

        val start = proto.getCoordinates().getStartOffset()
        val end = proto.getCoordinates().getEndOffset()
        val origin = DEFINED // TODO: retore the real origins
        val declarator = proto.getDeclarator()

        val declaration: IrDeclaration = when {
            declarator.hasIrClass()
                -> deserializeIrClass(declarator.irClass,
                    descriptor as ClassDescriptor, start, end, origin)
            declarator.hasFunction() 
                -> deserializeIrFunction(declarator.function, 
                    descriptor as FunctionDescriptor, start, end, origin)
            declarator.hasVariable()
                -> deserializeIrVariable(declarator.variable, 
                    descriptor as VariableDescriptor, start, end, origin)
            declarator.hasIrEnumEntry()
                -> deserializeIrEnumEntry(declarator.irEnumEntry, 
                    descriptor as ClassDescriptor, start, end, origin)
            else -> {
                TODO("Declaration deserialization not implemented")
            }
        }

        if (!(descriptor is VariableDescriptor) && descriptor != rootFunction)
            localDeserializer.popContext(descriptor)
        context.log{"### Deserialized declaration: ${ir2string(declaration)}"}
        return declaration
    }

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
    fun adaptDeserializedTypeParameters(declaration: IrDeclaration): IrDeclaration {

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

        val copyFunctionDeclaration = DeepCopyIrTreeWithDescriptors(rootFunction.containingDeclaration, context).copy(
            irElement       = declaration,      
            typeSubstitutor = TypeSubstitutor.create(substitutionContext)  
        ) as IrFunction

        return copyFunctionDeclaration
    }

    val extractInlineProto: KonanLinkData.InlineIrBody
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

        val copyFunctionDeclaration = adaptDeserializedTypeParameters(declaration)

        return copyFunctionDeclaration
    }
}

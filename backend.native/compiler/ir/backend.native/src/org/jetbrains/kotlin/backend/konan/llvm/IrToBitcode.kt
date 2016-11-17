package org.jetbrains.kotlin.backend.konan.llvm

import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.isArray
import org.jetbrains.kotlin.backend.konan.isInterface
import org.jetbrains.kotlin.backend.konan.isIntrinsic
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptorBase
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetterCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

fun emitLLVM(module: IrModuleFragment, runtimeFile: String, outFile: String) {
    val llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
    val runtime = Runtime(runtimeFile) // TODO: dispose
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    LLVMSetTarget(llvmModule, runtime.target)

    val context = Context(module, runtime, llvmModule) // TODO: dispose

    module.acceptVoid(RTTIGeneratorVisitor(context))
    println("\n--- Generate bitcode ------------------------------------------------------\n")
    module.acceptVoid(CodeGeneratorVisitor(context))
    memScoped {
        val errorRef = alloc(Int8Box.ref)
        // TODO: use LLVMDisposeMessage() on errorRef, once possible in interop.
        if (LLVMVerifyModule(
                llvmModule, LLVMVerifierFailureAction.LLVMPrintMessageAction, errorRef) == 1) {
            LLVMDumpModule(llvmModule)
            throw Error("Invalid module");
        }
    }
    LLVMWriteBitcodeToFile(llvmModule, outFile)
}

internal class RTTIGeneratorVisitor(context: Context) : IrElementVisitorVoid {
    val generator = RTTIGenerator(context)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any RTTI for annotation classes as a workaround for link errors
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        generator.generate(declaration.descriptor)
    }

}

//-------------------------------------------------------------------------//

internal class CodeGeneratorVisitor(val context: Context) : IrElementVisitorVoid {

    val codegen = CodeGenerator(context)
    val logger = Logger(codegen, context)
    val metadator = MetadataGenerator(context)

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitWhen(expression: IrWhen) {
        logger.log("visitWhen                  : ${ir2string(expression)}")
        var bbExit:LLVMOpaqueBasicBlock? = null             // By default "when" does not have "exit"
        if (!KotlinBuiltIns.isNothing(expression.type))     // If "when" has "exit".
            bbExit = codegen.basicBlock()                   // Create basic block to process "exit".

        expression.branches.forEach {                       // Iterate through "when" branches (clauses).
            var bbNext = bbExit                             // For last clause bbNext coincides with bbExit.
            if (it != expression.branches.last())           // If it is not last clause.
                bbNext = codegen.basicBlock()               // Create new basic block for next clause.
            generateWhenCase(it, bbNext, bbExit)            // Generate code for current clause.
        }
    }

    //-------------------------------------------------------------------------//

    override fun visitLoop(loop: IrLoop) {
        TODO()
    }

    //-------------------------------------------------------------------------//

    override fun visitWhileLoop(loop: IrWhileLoop) {
        val loopEnter = codegen.basicBlock()
        val loopBody  = codegen.basicBlock()
        val loopExit  = codegen.basicBlock()

        LLVMBuildBr(context.llvmBuilder, loopEnter)

        codegen.positionAtEnd(loopEnter!!)
        val condition = evaluateExpression(codegen.newVar(), loop.condition)
        LLVMBuildCondBr(context.llvmBuilder, condition, loopBody, loopExit)

        codegen.positionAtEnd(loopBody!!)
        evaluateExpression(codegen.newVar(), loop.body)

        LLVMBuildBr(context.llvmBuilder, loopEnter)
        codegen.positionAtEnd(loopExit!!)
    }

    //-------------------------------------------------------------------------//

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        val loopBody  = codegen.basicBlock()
        val loopCheck = codegen.basicBlock()
        val loopExit  = codegen.basicBlock()

        LLVMBuildBr(context.llvmBuilder, loopBody)

        codegen.positionAtEnd(loopBody!!)
        evaluateExpression(codegen.newVar(), loop.body)
        LLVMBuildBr(context.llvmBuilder, loopCheck)

        codegen.positionAtEnd(loopCheck!!)
        val condition = evaluateExpression(codegen.newVar(), loop.condition)
        LLVMBuildCondBr(context.llvmBuilder, condition, loopBody, loopExit)

        codegen.positionAtEnd(loopExit!!)
    }

    //-------------------------------------------------------------------------//

    override fun visitConstructor(declaration: IrConstructor) {
        codegen.initFunction(declaration)
        val thisValue = codegen.variable("this")
        //super.visitConstructor(declaration)
        /**
         *   %this = alloca i8*
         *   store i8* %0, i8** %this <- prolog
         *
         *   %tmp0 = load i8*, i8** %this <- epilog
         *   ret i8* %tmp0
         */
        codegen.ret(codegen.load(thisValue!!, codegen.newVar()))
        logger.log("visitConstructor           : ${ir2string(declaration)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitBlockBody(body: IrBlockBody) {
        super.visitBlockBody(body)
        if (KotlinBuiltIns.isUnit(codegen.currentFunction!!.returnType!!)) {
            codegen.ret(null)
        }
        logger.log("visitBlockBody             : ${ir2string(body)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        evaluateCall(codegen.newVar(), expression)
    }

   //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall) {
        logger.log("visitCall                  : ${ir2string(expression)}")
        val isUnit = KotlinBuiltIns.isUnit(expression.descriptor.returnType!!)
        val tmpVariable = if (isUnit) "" else codegen.newVar()
        evaluateExpression(tmpVariable, expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitFunction(declaration: IrFunction) {
        codegen.function(declaration)
        metadator.function(declaration)
        declaration.acceptChildrenVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitClass(declaration: IrClass) {
        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any code for annotation classes as a workaround for NotImplementedError
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        if (declaration.descriptor.isInterface) {
            // Do not generate any code for interfaces.
            return
        }

        super.visitClass(declaration)
    }

    //-------------------------------------------------------------------------//
    // Create new variable in LLVM ir

    override fun visitVariable(declaration: IrVariable) {
        logger.log("visitVariable              : ${ir2string(declaration)}")
        val variableName = declaration.descriptor.name.asString()
        val variableType = declaration.descriptor.type
        val newVariable  = codegen.alloca(variableType, variableName)          // Create LLVM variable.
        codegen.registerVariable(variableName, newVariable)                    // Map variableName -> LLVM variable.
        val value = evaluateExpression(variableName, declaration.initializer)  // Generate initialization code.
        codegen.store(value!!, codegen.variable(variableName)!!)               // Store init result in the variable
    }

    //-------------------------------------------------------------------------//

    override fun visitReturn(expression: IrReturn) {
        logger.log("visitReturn                : ${ir2string(expression)}")
        val tmpVarName = codegen.newVar()                                   // Generate new tmp name.
        val value      = evaluateExpression(tmpVarName, expression.value)   //
        codegen.ret(value)
    }

    //-------------------------------------------------------------------------//

    override fun visitSetVariable(expression: IrSetVariable) {
        logger.log("visitSetVariable           : ${ir2string(expression)}")
        val value = evaluateExpression(codegen.newVar(), expression.value)
        codegen.store(value!!, codegen.variable(expression.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//
    // top level setter for field
    override fun visitSetField(expression: IrSetField) {
        logger.log("visitSetField              : ${ir2string(expression)}")
        evaluateExpression("", expression)
    }

    //-------------------------------------------------------------------------//
    // top level getter for field:
    // normally GetField appears in following manner:
    //
    // PROPERTY private var globalValue: kotlin.Int
    //   FIELD PROPERTY_BACKING_FIELD private var globalValue: kotlin.Int
    //     EXPRESSION_BODY
    //       CONST Int type=kotlin.Int value='1'
    //   FUN DEFAULT_PROPERTY_ACCESSOR private fun <get-globalValue>(): kotlin.Int
    //     BLOCK_BODY
    //       RETURN type=kotlin.Nothing from='<get-globalValue>(): Int'
    //         GET_FIELD 'globalValue: Int' type=kotlin.Int origin=null
    //
    // and this case processed via evaluateExpression from RETURN processing,
    // where we process similar thing with generating temporal variable to return
    // alternatively top level IR, let's assume following IR:
    //
    // PROPERTY private var globalValue: kotlin.Int
    //   FUN DEFAULT_PROPERTY_ACCESSOR private fun <get-globalValue>(): kotlin.Int
    //     BLOCK_BODY
    //       GET_FIELD 'globalValue: Int' type=kotlin.Int origin=null
    //
    // in this situation our visitGetField will be charged, that why no
    // temporal variable is required.
    override fun visitGetField(expression: IrGetField) {
        logger.log("visitGetField              : ${ir2string(expression)}")
        evaluateExpression("", expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitField(expression: IrField) {
        logger.log("visitField                : ${ir2string(expression)}")
        val descriptor = expression.descriptor
        if (descriptor.containingDeclaration is PackageFragmentDescriptor) {
            val globalProperty = LLVMAddGlobal(context.llvmModule, getLLVMType(descriptor.type), descriptor.symbolName)
            LLVMSetInitializer(globalProperty, evaluateExpression("", expression.initializer))
            return
        }

        super.visitField(expression)

    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(tmpVariableName: String, value: IrElement?): LLVMOpaqueValue? {
        when (value) {
            is IrSetterCallImpl  -> return evaluateSetterCall  (tmpVariableName, value)
            is IrCall            -> return evaluateCall        (tmpVariableName, value)
            is IrGetValue        -> return evaluateGetValue    (tmpVariableName, value)
            is IrSetVariable     -> return evaluateSetVariable (                 value)
            is IrVariable        -> return evaluateVariable    (                 value)
            is IrGetField        -> return evaluateGetField    (                 value)
            is IrSetField        -> return evaluateSetField    (                 value)
            is IrConst<*>        -> return evaluateConst       (                 value)
            is IrReturn          -> return evaluateReturn      (                 value)
            is IrBlock           -> return evaluateBlock       (                 value)
            is IrExpressionBody  -> return evaluateExpression  (tmpVariableName, value.expression)
            null                 -> return null
            else                 -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetValue(tmpVariableName: String, value: IrGetValue): LLVMOpaqueValue {
        logger.log("evaluateGetValue           : $tmpVariableName = ${ir2string(value)}")
        when (value.descriptor) {
            is LocalVariableDescriptor,
            is ValueParameterDescriptor,
            is IrTemporaryVariableDescriptor -> {
                val variable = codegen.variable(value.descriptor.name.asString())
                return codegen.load(variable!!, tmpVariableName)
            }
            is LazyClassReceiverParameterDescriptor -> {
                if (value.descriptor.name.asString() == "<this>") {
                    return codegen.load(codegen.thisVariable(), tmpVariableName)
                }
                TODO()
            }
            else -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetVariable(value: IrSetVariable): LLVMOpaqueValue {
        logger.log("evaluateSetVariable        : ${ir2string(value)}")
        val ret = evaluateExpression(codegen.newVar(), value.value)
        return codegen.store(ret!!, codegen.variable(value.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateVariable(value: IrVariable): LLVMOpaqueValue {
        logger.log("evaluateVariable           : ${ir2string(value)}")
        val ret = evaluateExpression(codegen.newVar(), value.initializer)
        val variableName = value.descriptor.name.asString()
        val variable = codegen.alloca(LLVMTypeOf(ret), variableName)
        codegen.registerVariable(variableName, variable)
        return codegen.store(ret!!, variable)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetField(value: IrGetField): LLVMOpaqueValue {
        logger.log("evaluateGetField           : ${ir2string(value)}")
        if (value.descriptor.dispatchReceiverParameter != null) {
            val thisPtr = codegen.load(codegen.thisVariable(), codegen.newVar())
            return codegen.load(fieldPtrOfClass(thisPtr, value)!!, codegen.newVar())
        }
        else {
            val ptr = LLVMGetNamedGlobal(context.llvmModule, value.descriptor.symbolName)!!
            return codegen.load(ptr, codegen.newVar())
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetField(value: IrSetField): LLVMOpaqueValue {
        logger.log("evaluateSetField           : ${ir2string(value)}")
        val valueToAssign = evaluateExpression(codegen.newVar(), value.value)!!
        if (value.descriptor.dispatchReceiverParameter != null) {
            val thisPtr = codegen.load(codegen.thisVariable(), codegen.newVar())
            return codegen.store(valueToAssign, fieldPtrOfClass(thisPtr, value)!!)
        }
        else {
            val globalValue = LLVMGetNamedGlobal(context.llvmModule, value.descriptor.symbolName)
            return codegen.store(valueToAssign, globalValue!!)
        }
    }

    //-------------------------------------------------------------------------//

    /*
       in C:
       struct ObjHeader *header = (struct ObjHeader *)ptr;
       struct T* obj = (T*)&header[1];
       return &obj->fieldX;

       in llvm ir:
       %struct.ObjHeader = type { i32, i32 }
       %struct.Object = type { i32, i32 }

       ; Function Attrs: nounwind ssp uwtable
       define i32 @fooField2(i8*) #0 {
         %2 = alloca i8*, align 8
         %3 = alloca %struct.ObjHeader*, align 8
         %4 = alloca %struct.Object*, align 8
         store i8* %0, i8** %2, align 8
         %5 = load i8*, i8** %2, align 8
         %6 = bitcast i8* %5 to %struct.ObjHeader*
         store %struct.ObjHeader* %6, %struct.ObjHeader** %3, align 8
         %7 = load %struct.ObjHeader*, %struct.ObjHeader** %3, align 8

         %8 = getelementptr inbounds %struct.ObjHeader, %struct.ObjHeader* %7, i64 1; <- (T*)&header[1];

         %9 = bitcast %struct.ObjHeader* %8 to %struct.Object*
         store %struct.Object* %9, %struct.Object** %4, align 8
         %10 = load %struct.Object*, %struct.Object** %4, align 8
         %11 = getelementptr inbounds %struct.Object, %struct.Object* %10, i32 0, i32 0 <-  &obj->fieldX
         %12 = load i32, i32* %11, align 4
         ret i32 %12
       }

    */
    private fun fieldPtrOfClass(thisPtr: LLVMOpaqueValue, value: IrFieldAccessExpression): LLVMOpaqueValue? {
        val objHeaderPtr = codegen.bitcast(kObjHeaderPtr, thisPtr, codegen.newVar())
        val typePtr = pointerType(codegen.classType(codegen.currentClass!!))
        memScoped {
            val args = allocNativeArrayOf(LLVMOpaqueValue, Int32(1).getLlvmValue())
            val objectPtr = LLVMBuildGEP(codegen.context.llvmBuilder, objHeaderPtr,  args[0], 1, codegen.newVar())
            val typedObjPtr = codegen.bitcast(typePtr, objectPtr!!, codegen.newVar())
            val fieldPtr = LLVMBuildStructGEP(codegen.context.llvmBuilder, typedObjPtr, codegen.indexInClass(value.descriptor), codegen.newVar())
            return fieldPtr
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConst(value: IrConst<*>): LLVMOpaqueValue? {
        logger.log("evaluateConst              : ${ir2string(value)}")
        when (value.kind) {
            IrConstKind.Null -> TODO() // LLVMConstPointerNull
            IrConstKind.Boolean -> when (value.value) {
                true  -> return LLVMConstInt(LLVMInt1Type(), 1, 1)
                false -> return LLVMConstInt(LLVMInt1Type(), 0, 1)
            }
            IrConstKind.Char   -> return LLVMConstInt(LLVMInt16Type(), (value.value as Char).toLong(),  0)
            IrConstKind.Byte   -> return LLVMConstInt(LLVMInt8Type(),  (value.value as Byte).toLong(),  1)
            IrConstKind.Short  -> return LLVMConstInt(LLVMInt16Type(), (value.value as Short).toLong(), 1)
            IrConstKind.Int    -> return LLVMConstInt(LLVMInt32Type(), (value.value as Int).toLong(),   1)
            IrConstKind.Long   -> return LLVMConstInt(LLVMInt64Type(), value.value as Long,             1)
            IrConstKind.String ->
                return context.staticData.kotlinStringLiteral(value as IrConst<String>).getLlvmValue()
            IrConstKind.Float  -> return LLVMConstRealOfString(LLVMFloatType(), (value.value as Float).toString())
            IrConstKind.Double -> return LLVMConstRealOfString(LLVMDoubleType(), (value.value as Double).toString())
        }
        TODO()
    }

    //-------------------------------------------------------------------------//

    private fun evaluateReturn(value: IrReturn): LLVMOpaqueValue? {
        logger.log("evaluateReturn             : ${ir2string(value)}")
        val ret = evaluateExpression(codegen.newVar(), value.value)
        return codegen.ret(ret)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateBlock(value: IrBlock): LLVMOpaqueValue? {
        logger.log("evaluateBlock              : ${ir2string(value)}")
        value.statements.dropLast(1).forEach {
            evaluateExpression(codegen.newVar(), it)
        }
        return evaluateExpression(codegen.newVar(), value.statements.lastOrNull())
    }

    //-------------------------------------------------------------------------//

    private fun evaluateCall(tmpVariableName: String, value: IrMemberAccessExpression?): LLVMOpaqueValue? {
        logger.log("evaluateCall               : $tmpVariableName = ${ir2string(value)}")
        val args = mutableListOf<LLVMOpaqueValue?>()                            // Create list of function args.
        value!!.acceptChildrenVoid(object: IrElementVisitorVoid {               // Iterate args of the function.
            override fun visitElement(element: IrElement) {                     // Visit arg.
                val tmp = codegen.newVar()                                      // Create variable representing the arg in codegen
                args.add(evaluateExpression(tmp, element as IrExpression))      // Evaluate expression and get LLVM arg
            }
        })

        when {
            value.descriptor is FunctionDescriptor -> return evaluateFunctionCall(tmpVariableName, value as IrCall, args)
            value is IrDelegatingConstructorCall -> return superCall(tmpVariableName, value.descriptor, args)
            else -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionCall(tmpVariableName: String, callee: IrCall, args: List<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        val descriptor:FunctionDescriptor = callee.descriptor as FunctionDescriptor
        when (descriptor) {
            is IrBuiltinOperatorDescriptorBase -> return evaluateOperatorCall      (tmpVariableName, callee,     args)
            is ClassConstructorDescriptor      -> return evaluateConstructorCall   (tmpVariableName, callee,     args)
            else                               -> return evaluateSimpleFunctionCall(tmpVariableName, descriptor, args)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(tmpVariableName: String, descriptor: FunctionDescriptor, args: List<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        //logger.log("evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}")
        if (descriptor.isOverridable)
            return callVirtual(descriptor, args, tmpVariableName)
        else
            return callDirect(descriptor, args, tmpVariableName)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetterCall(tmpVariableName: String, value: IrSetterCallImpl): LLVMOpaqueValue? {
        val descriptor = value.descriptor as FunctionDescriptor
        val args       = mutableListOf<LLVMOpaqueValue?>()
        if (descriptor.dispatchReceiverParameter != null)
            args.add(evaluateExpression(codegen.newVar(), value.dispatchReceiver))         //add this ptr
        args.add(evaluateExpression(codegen.newVar(), value.getValueArgument(0)))
        return evaluateSimpleFunctionCall(tmpVariableName, descriptor, args)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConstructorCall(variableName: String, callee: IrCall, args: List<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        logger.log("evaluateConstructorCall    : $variableName = ${ir2string(callee)}")
        memScoped {
            val containingClass = (callee.descriptor as ClassConstructorDescriptor).containingDeclaration
            val typeInfo = codegen.typeInfoValue(containingClass)
            val allocHint = Int32(1).getLlvmValue()
            val thisValue = if (containingClass.isArray) {
                assert(args.size == 1 && LLVMTypeOf(args[0]) == LLVMInt32Type())
                codegen.call(context.allocArrayFunction, listOf(typeInfo, allocHint, args[0]), variableName)
            } else {
                codegen.call(context.allocInstanceFunction, listOf(typeInfo, allocHint), variableName)
            }
            val constructorParams: MutableList<LLVMOpaqueValue?> = mutableListOf()
            constructorParams += thisValue
            constructorParams += args
            return evaluateSimpleFunctionCall(variableName, callee.descriptor as FunctionDescriptor, constructorParams)
        }
    }

    //-------------------------------------------------------------------------//
    private val kEqeq        = Name.identifier("EQEQ")
    private val ktEqeqeq     = Name.identifier("EQEQEQ")
    private val kGt0         = Name.identifier("GT0")
    private val kGteq0       = Name.identifier("GTEQ0")
    private val kLt0         = Name.identifier("LT0")
    private val kLteq0       = Name.identifier("LTEQ0")
    private val kNot         = Name.identifier("NOT")
    private val kImmZero     = LLVMConstInt(LLVMInt32Type(),  0, 1)!!
    private val kTrue        = LLVMConstInt(LLVMInt1Type(),   1, 1)!!
    private val kFalse       = LLVMConstInt(LLVMInt1Type(),   0, 1)!!


    private fun evaluateOperatorCall(tmpVariableName: String, callee: IrCall, args: List<LLVMOpaqueValue?>): LLVMOpaqueValue {
        logger.log("evaluateCall $tmpVariableName origin:$callee")
        val descriptor = callee.descriptor
        when (descriptor.name) {
            kEqeq  -> return evaluateOperatorEqeq(callee as IrBinaryPrimitiveImpl, args[0]!!, args[1]!!, tmpVariableName)
            ktEqeqeq ->
                return evaluateOperatorEqeqeq(callee as IrBinaryPrimitiveImpl, args[0]!!, args[1]!!, tmpVariableName)
            kGt0   -> return codegen.icmpGt(args[0]!!, kImmZero, tmpVariableName)
            kGteq0 -> return codegen.icmpGe(args[0]!!, kImmZero, tmpVariableName)
            kLt0   -> return codegen.icmpLt(args[0]!!, kImmZero, tmpVariableName)
            kLteq0 -> return codegen.icmpLe(args[0]!!, kImmZero, tmpVariableName)
            kNot   -> return codegen.icmpNe(args[0]!!, kTrue,    tmpVariableName)
            else -> {
                TODO(descriptor.name.toString())
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateOperatorEqeq(callee: IrBinaryPrimitiveImpl, arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, tmpVariableName: String):LLVMOpaqueValue {
        val arg0Type = callee.argument0.type
        val isFp  = KotlinBuiltIns.isDouble(arg0Type) || KotlinBuiltIns.isFloat(arg0Type)
        val isInt = KotlinBuiltIns.isPrimitiveType(arg0Type) && !isFp
        assert(arg0Type == callee.argument1.type)
        when {
            isFp  -> return codegen.fcmpEq(arg0, arg1, tmpVariableName)
            isInt -> return codegen.icmpEq(arg0, arg1, tmpVariableName)
            else  -> {
                val functions = arg0Type.memberScope.getContributedFunctions(Name.identifier("equals"), NoLookupLocation.FROM_BACKEND).filter {
                    it.valueParameters.size == 1 && KotlinBuiltIns.isAnyOrNullableAny(it.valueParameters[0].type)
                }
                assert(functions.size == 1)
                val descriptor = functions.first()
                return evaluateSimpleFunctionCall(tmpVariableName, descriptor, listOf(arg0, arg1))!!
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateOperatorEqeqeq(callee: IrBinaryPrimitiveImpl,
                                       arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue,
                                       tmpVariableName: String):LLVMOpaqueValue {

        val arg0Type = callee.argument0.type
        val arg1Type = callee.argument1.type

        assert (arg0Type == arg1Type)

        return when {
            KotlinBuiltIns.isPrimitiveType(arg0Type) -> TODO()
            else -> codegen.icmpEq(arg0, arg1, tmpVariableName)
        }
    }

    //-------------------------------------------------------------------------//

    private fun generateWhenCase(branch: IrBranch, bbNext: LLVMOpaqueBasicBlock?, bbExit: LLVMOpaqueBasicBlock?) {
        if (isUnconditional(branch)) {                                               // It is the "else" clause.
            evaluateExpression(codegen.newVar(), branch.result)                      // Generate clause body.
            if (bbExit == null) return                                               // If "when" does not have exit - return.
            codegen.br(bbExit)                                                       // Generate branch to bbExit.
            codegen.positionAtEnd(bbExit)                                            // Switch generation to bbExit.
        } else {                                                                     // It is conditional clause.
            val bbCurr = codegen.basicBlock()                                        // Create block for clause body.
            val condition = evaluateExpression(codegen.newVar(), branch.condition)   // Generate cmp instruction.
            codegen.condBr(condition, bbCurr, bbNext)                                // Conditional branch depending on cmp result.
            codegen.positionAtEnd(bbCurr!!)                                          // Switch generation to block for clause body.
            evaluateExpression(codegen.newVar(), branch.result)                      // Generate clause body.
            if (!KotlinBuiltIns.isNothing(branch.result.type))                       // If clause code does not contain "return".
                codegen.br(bbExit!!)                                                 // Generate branch to bbExit.
            codegen.positionAtEnd(bbNext!!)                                          // Switch generation to bbNextClause.
        }
    }

    //-------------------------------------------------------------------------//
    // Checks if the branch is unconditional

    private fun isUnconditional(branch: IrBranch): Boolean =
        branch.condition is IrConst<*>                            // If branch condition is constant.
            && (branch.condition as IrConst<*>).value as Boolean  // If condition is "true"

    //-------------------------------------------------------------------------//

    fun callDirect(descriptor: FunctionDescriptor, args: List<LLVMOpaqueValue?>, result: String?): LLVMOpaqueValue? {
        val llvmFunction = codegen.functionLlvmValue(descriptor)
        return codegen.call(llvmFunction, args, result)
    }

    //-------------------------------------------------------------------------//

    /* Runtime constant */
    private val kTypeInfo     = LLVMGetTypeByName(context.llvmModule, "struct.TypeInfo")!!
    private val kObjHeader    = LLVMGetTypeByName(context.llvmModule, "struct.ObjHeader")!!
    private val kObjHeaderPtr = pointerType(kObjHeader)!!
    private val kTypeInfoPtr  = pointerType(kTypeInfo)
    private val kInt8Ptr      = pointerType(LLVMInt8Type())
    private val kInt8PtrPtr   = pointerType(kInt8Ptr)

    //-------------------------------------------------------------------------//

    fun callVirtual(descriptor: FunctionDescriptor, args: List<LLVMOpaqueValue?>, result: String?): LLVMOpaqueValue? {
        val thisI8PtrPtr    = codegen.bitcast(kInt8PtrPtr, args[0]!!, codegen.newVar())        // Cast "this (i8*)" to i8**.
        val typeInfoI8Ptr   = codegen.load(thisI8PtrPtr!!, codegen.newVar())                   // Load TypeInfo address.
        val typeInfoPtr     = codegen.bitcast(kTypeInfoPtr, typeInfoI8Ptr, codegen.newVar())   // Cast TypeInfo (i8*) to TypeInfo*.
        val methodHash      = codegen.functionHash(descriptor)                                 // Calculate hash of the method to be invoked
        val lookupArgs      = listOf(typeInfoPtr, methodHash)                                  // Prepare args for lookup
        val llvmMethod      = codegen.call(
                              context.lookupOpenMethodFunction,
                              lookupArgs,
                              codegen.newVar())                                                 // Get method ptr to be invoked

        val functionPtrType = pointerType(getLlvmFunctionType(descriptor))                      // Construct type of the method to be invoked
        val function        = codegen.bitcast(functionPtrType, llvmMethod!!, codegen.newVar())  // Cast method address to the type
        return codegen.call(function, args, result)                                             // Invoke the method
    }

    //-------------------------------------------------------------------------//

    fun  superCall(result:String, descriptor:ClassConstructorDescriptor, args:List<LLVMOpaqueValue?> ):LLVMOpaqueValue? {
        val tmp = codegen.load(codegen.thisVariable(), codegen.newVar())
        val rargs =
                if (args.isNotEmpty())
                    listOf<LLVMOpaqueValue?>(tmp, *args.toTypedArray())
                else
                    listOf<LLVMOpaqueValue?>(tmp)
        return callDirect(descriptor, rargs!!, result)
    }

}

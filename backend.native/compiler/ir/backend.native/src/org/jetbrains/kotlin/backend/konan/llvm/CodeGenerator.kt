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

package org.jetbrains.kotlin.backend.konan.llvm


import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType

internal class CodeGenerator(override val context: Context) : ContextUtils {

    fun llvmFunction(function: FunctionDescriptor): LLVMValueRef = function.llvmFunction
    val intPtrType = LLVMIntPtrType(llvmTargetData)!!
    internal val immOneIntPtrType = LLVMConstInt(intPtrType, 1, 1)!!

    //-------------------------------------------------------------------------//

    /* to class descriptor */
    fun typeInfoValue(descriptor: ClassDescriptor): LLVMValueRef = descriptor.llvmTypeInfoPtr

    fun param(fn: FunctionDescriptor, i: Int): LLVMValueRef {
        assert(i >= 0 && i < countParams(fn))
        return LLVMGetParam(fn.llvmFunction, i)!!
    }

    private fun countParams(fn: FunctionDescriptor) = LLVMCountParams(fn.llvmFunction)

    fun functionLlvmValue(descriptor: FunctionDescriptor) = descriptor.llvmFunction
    fun functionEntryPointAddress(descriptor: FunctionDescriptor) = descriptor.entryPointAddress.llvm
    fun functionHash(descriptor: FunctionDescriptor): LLVMValueRef = descriptor.functionName.localHash.llvm
}

val LLVMValueRef.name:String?
    get() = LLVMGetValueName(this)?.toKString()

val LLVMValueRef.isConst:Boolean
    get() = (LLVMIsConstant(this) == 1)


internal inline fun<R> generateFunction(codegen: CodeGenerator,
                                        descriptor: FunctionDescriptor,
                                        startLocation: LocationInfo? = null,
                                        endLocation: LocationInfo? = null,
                                        code:FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    val llvmFunction = codegen.llvmFunction(descriptor)

    generateFunctionBody(FunctionGenerationContext(
            llvmFunction,
            codegen,
            startLocation,
            endLocation,
            descriptor), code)
}


internal inline fun<R> generateFunction(codegen: CodeGenerator, function: LLVMValueRef, code:FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    generateFunctionBody(FunctionGenerationContext(function, codegen), code)
}

inline private fun <R> generateFunctionBody(functionGenerationContext: FunctionGenerationContext, code: FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    functionGenerationContext.prologue()
    functionGenerationContext.code(functionGenerationContext)
    if (!functionGenerationContext.isAfterTerminator()) {
        if (functionGenerationContext.returnType == voidType)
            functionGenerationContext.ret(null)
        else
            functionGenerationContext.unreachable()
    }
    functionGenerationContext.epilogue()
    functionGenerationContext.resetDebugLocation()
}

internal class FunctionGenerationContext(val function: LLVMValueRef,
                                         val codegen:CodeGenerator,
                                         startLocation:LocationInfo? = null,
                                         endLocation:LocationInfo? = null,
                                         internal val functionDescriptor: FunctionDescriptor? = null):ContextUtils {
    override val context = codegen.context
    val vars = VariableManager(this)
    private val basicBlockToLastLocation = mutableMapOf<LLVMBasicBlockRef, LocationInfo>()

    private fun update(block:LLVMBasicBlockRef, locationInfo: LocationInfo?) {
        locationInfo ?: return
        basicBlockToLastLocation.put(block, locationInfo)
    }

    var returnType: LLVMTypeRef? = LLVMGetReturnType(getFunctionType(function))
    private val returns: MutableMap<LLVMBasicBlockRef, LLVMValueRef> = mutableMapOf()
    // TODO: remove, to make CodeGenerator descriptor-agnostic.
    val constructedClass: ClassDescriptor?
        get() = (functionDescriptor as? ClassConstructorDescriptor)?.constructedClass
    private var returnSlot: LLVMValueRef? = null
    private var slotsPhi: LLVMValueRef? = null
    private var slotCount = 0
    private var localAllocs = 0
    private var arenaSlot: LLVMValueRef? = null

    private val prologueBb        = basicBlockInFunction("prologue", startLocation)
    private val localsInitBb      = basicBlockInFunction("locals_init", startLocation)
    private val entryBb           = basicBlockInFunction("entry", startLocation)
    private val epilogueBb        = basicBlockInFunction("epilogue", endLocation)
    private val cleanupLandingpad = basicBlockInFunction("cleanup_landingpad", endLocation)

    init {
        functionDescriptor?.let {
            if (!functionDescriptor.isExported()) {
                LLVMSetLinkage(function, LLVMLinkage.LLVMInternalLinkage)
                // (Cannot do this before the function body is created).
            }
        }
    }

    private fun basicBlockInFunction(name: String, locationInfo: LocationInfo?): LLVMBasicBlockRef {
        val bb = LLVMAppendBasicBlock(function, name)!!
        update(bb, locationInfo)
        return bb
    }

    fun basicBlock(name:String = "label_" , locationInfo:LocationInfo?):LLVMBasicBlockRef {
        val result = LLVMInsertBasicBlock(this.currentBlock, name)!!
        update(result, locationInfo)
        LLVMMoveBasicBlockAfter(result, this.currentBlock)
        return result
    }

    fun alloca(type: LLVMTypeRef?, name: String = ""): LLVMValueRef {
        if (isObjectType(type!!)) {
            appendingTo(localsInitBb) {
                return gep(slotsPhi!!, Int32(slotCount++).llvm, name)
            }
        }

        appendingTo(prologueBb) {
            return LLVMBuildAlloca(builder, type, name)!!
        }
    }


    fun ret(value: LLVMValueRef?): LLVMValueRef {
        val res = LLVMBuildBr(builder, epilogueBb)!!
        if (returns.containsKey(currentBlock)) {
            // TODO: enable error throwing.
            throw Error("ret() in the same basic block twice! in ${function.name}")
        }

        if (value != null)
            returns[currentBlock] = value

        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun load(value: LLVMValueRef, name: String = ""): LLVMValueRef {
        val result = LLVMBuildLoad(builder, value, name)!!
        // Use loadSlot() API for that.
        assert(!isObjectRef(value))
        return result
    }

    fun loadSlot(address: LLVMValueRef, isVar: Boolean, name: String = ""): LLVMValueRef {
        val value = LLVMBuildLoad(builder, address, name)!!
        if (isObjectRef(value) && isVar) {
            val slot = alloca(LLVMTypeOf(value))
            storeAnyLocal(value, slot)
        }
        return value
    }

    fun store(value: LLVMValueRef, ptr: LLVMValueRef) {
        // Use updateRef() or storeAny() API for that.
        assert(!isObjectRef(value))
        LLVMBuildStore(builder, value, ptr)
    }

    fun storeAnyLocal(value: LLVMValueRef, ptr: LLVMValueRef) {
        if (isObjectRef(value)) {
            updateRef(value, ptr)
        } else {
            LLVMBuildStore(builder, value, ptr)
        }
    }

    fun storeAnyGlobal(value: LLVMValueRef, ptr: LLVMValueRef) {
        if (isObjectRef(value)) {
            updateRef(value, ptr)
        } else {
            LLVMBuildStore(builder, value, ptr)
        }
    }

    private fun updateReturnRef(value: LLVMValueRef, address: LLVMValueRef) {
        call(context.llvm.updateReturnRefFunction, listOf(address, value))
    }

    // Only use ignoreOld, when sure that memory is freshly inited and have no value.
    private fun updateRef(value: LLVMValueRef, address: LLVMValueRef, ignoreOld: Boolean = false) {
        call(if (ignoreOld) context.llvm.setRefFunction else context.llvm.updateRefFunction,
                listOf(address, value))
    }

    //-------------------------------------------------------------------------//

    fun callAtFunctionScope(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
                            lifetime: Lifetime) =
            call(llvmFunction, args, lifetime, { cleanupLandingpad })

    fun call(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
             resultLifetime: Lifetime = Lifetime.IRRELEVANT,
             lazyLandingpad: () -> LLVMBasicBlockRef? = { null }): LLVMValueRef {
        val callArgs = if (!isObjectReturn(llvmFunction.type)) {
            args
        } else {
            // If function returns an object - create slot for the returned value or give local arena.
            // This allows appropriate rootset accounting by just looking at the stack slots,
            // along with ability to allocate in appropriate arena.
            val resultSlot = when (resultLifetime.slotType) {
                SlotType.ARENA -> {
                    localAllocs++
                    arenaSlot!!
                }

                SlotType.RETURN -> returnSlot!!

                SlotType.ANONYMOUS -> vars.createAnonymousSlot()

                SlotType.RETURN_IF_ARENA -> returnSlot.let {
                    if (it != null)
                        call(context.llvm.getReturnSlotIfArenaFunction, listOf(it, vars.createAnonymousSlot()))
                    else {
                        // Return type is not an object type - can allocate locally.
                        localAllocs++
                        arenaSlot!!
                    }
                }

                is SlotType.PARAM_IF_ARENA ->
                    call(context.llvm.getParamSlotIfArenaFunction,
                            listOf(vars.load(resultLifetime.slotType.parameter), vars.createAnonymousSlot()))

                else -> throw Error("Incorrect slot type: ${resultLifetime.slotType}")
            }
            args + resultSlot
        }
        return callRaw(llvmFunction, callArgs, lazyLandingpad)
    }

    private fun callRaw(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
                        lazyLandingpad: () -> LLVMBasicBlockRef?): LLVMValueRef {
        val rargs = args.toCValues()
        if (LLVMIsAFunction(llvmFunction) != null /* the function declaration */ &&
                (LLVMGetFunctionAttr(llvmFunction) and LLVMNoUnwindAttribute) != 0) {

            return LLVMBuildCall(builder, llvmFunction, rargs, args.size, "")!!
        } else {
            val landingpad = lazyLandingpad()

            if (landingpad == null) {
                // When calling a function that is not marked as nounwind (can throw an exception),
                // it is required to specify a landingpad to handle exceptions properly.
                // Runtime C++ function can be marked as non-throwing using `RUNTIME_NOTHROW`.
                val functionName = llvmFunction.name
                val message = "no landingpad specified when calling function $functionName without nounwind attr"
                throw IllegalArgumentException(message)
            }

            val success = basicBlock("call_success", position())
            val result = LLVMBuildInvoke(builder, llvmFunction, rargs, args.size, success, landingpad, "")!!
            positionAtEnd(success)
            return result
        }
    }

    //-------------------------------------------------------------------------//

    fun phi(type: LLVMTypeRef, name: String = ""): LLVMValueRef {
        return LLVMBuildPhi(builder, type, name)!!
    }

    fun addPhiIncoming(phi: LLVMValueRef, vararg incoming: Pair<LLVMBasicBlockRef, LLVMValueRef>) {
        memScoped {
            val incomingValues = incoming.map { it.second }.toCValues()
            val incomingBlocks = incoming.map { it.first }.toCValues()

            LLVMAddIncoming(phi, incomingValues, incomingBlocks, incoming.size)
        }
    }

    fun assignPhis(vararg phiToValue: Pair<LLVMValueRef, LLVMValueRef>) {
        phiToValue.forEach {
            addPhiIncoming(it.first, currentBlock to it.second)
        }
    }

    fun allocInstance(typeInfo: LLVMValueRef, lifetime: Lifetime): LLVMValueRef {
        return call(context.llvm.allocInstanceFunction, listOf(typeInfo), lifetime)
    }

    fun allocArray(
            typeInfo: LLVMValueRef, count: LLVMValueRef, lifetime: Lifetime): LLVMValueRef {
        return call(context.llvm.allocArrayFunction, listOf(typeInfo, count), lifetime)
    }

    fun unreachable(): LLVMValueRef? {
        val res = LLVMBuildUnreachable(builder)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun br(bbLabel: LLVMBasicBlockRef): LLVMValueRef {
        val res = LLVMBuildBr(builder, bbLabel)!!
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun condBr(condition: LLVMValueRef?, bbTrue: LLVMBasicBlockRef?, bbFalse: LLVMBasicBlockRef?): LLVMValueRef? {
        val res = LLVMBuildCondBr(builder, condition, bbTrue, bbFalse)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun blockAddress(bbLabel: LLVMBasicBlockRef): LLVMValueRef {
        return LLVMBlockAddress(function, bbLabel)!!
    }

    fun or(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildOr(builder, arg0, arg1, name)!!

    /* integers comparisons */
    fun icmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntEQ, arg0, arg1, name)!!

    fun icmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGT, arg0, arg1, name)!!
    fun icmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGE, arg0, arg1, name)!!
    fun icmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLT, arg0, arg1, name)!!
    fun icmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLE, arg0, arg1, name)!!
    fun icmpNe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntNE, arg0, arg1, name)!!

    /* floating-point comparisons */
    fun fcmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOEQ, arg0, arg1, name)!!

    fun bitcast(type: LLVMTypeRef?, value: LLVMValueRef, name: String = "") = LLVMBuildBitCast(builder, value, type, name)!!

    fun intToPtr(value: LLVMValueRef?, DestTy: LLVMTypeRef, Name: String = "") = LLVMBuildIntToPtr(builder, value, DestTy, Name)!!
    fun ptrToInt(value: LLVMValueRef?, DestTy: LLVMTypeRef, Name: String = "") = LLVMBuildPtrToInt(builder, value, DestTy, Name)!!
    fun gep(base: LLVMValueRef, index: LLVMValueRef, name: String = ""): LLVMValueRef {
        return LLVMBuildGEP(builder, base, cValuesOf(index), 1, name)!!
    }

    fun gxxLandingpad(numClauses: Int, name: String = ""): LLVMValueRef {
        val personalityFunction = LLVMConstBitCast(context.llvm.gxxPersonalityFunction, int8TypePtr)

        // Type of `landingpad` instruction result (depends on personality function):
        val landingpadType = structType(int8TypePtr, int32Type)

        return LLVMBuildLandingPad(builder, landingpadType, personalityFunction, numClauses, name)!!
    }

    internal fun debugLocation(locationInfo: LocationInfo): DILocationRef? {
        if (!context.shouldContainDebugInfo()) return null
        update(currentBlock, locationInfo)
        return LLVMBuilderSetDebugLocation(
                builder,
                locationInfo.line,
                locationInfo.column,
                locationInfo.scope)
    }

    fun indirectBr(address: LLVMValueRef, destinations: Collection<LLVMBasicBlockRef>): LLVMValueRef? {
        val indirectBr = LLVMBuildIndirectBr(builder, address, destinations.size)
        destinations.forEach { LLVMAddDestination(indirectBr, it) }
        currentPositionHolder.setAfterTerminator()
        return indirectBr
    }
    fun resetDebugLocation() {
        if (!context.shouldContainDebugInfo()) return
        if (!currentPositionHolder.isAfterTerminator)
            LLVMBuilderResetDebugLocation(builder)
    }

    private fun position() = basicBlockToLastLocation[currentBlock]


    internal fun prologue() {
        assert(returns.isEmpty())
        if (isObjectType(returnType!!)) {
            returnSlot = LLVMGetParam(function, numParameters(function.type) - 1)
        }
        positionAtEnd(localsInitBb)
        slotsPhi = phi(kObjHeaderPtrPtr)
        // First slot can be assigned to keep pointer to frame local arena.
        slotCount = 1
        localAllocs = 0
        // Is removed by DCE trivially, if not needed.
        arenaSlot = intToPtr(
                or(ptrToInt(slotsPhi, codegen.intPtrType), codegen.immOneIntPtrType), kObjHeaderPtrPtr)
        positionAtEnd(entryBb)
    }

    internal fun epilogue() {
        appendingTo(prologueBb) {
            val slots = if (needSlots)
                LLVMBuildArrayAlloca(builder, kObjHeaderPtr, Int32(slotCount).llvm, "")!!
            else
                kNullObjHeaderPtrPtr
            if (needSlots) {
                // Zero-init slots.
                val slotsMem = bitcast(kInt8Ptr, slots)
                val pointerSize = LLVMABISizeOfType(llvmTargetData, kObjHeaderPtr).toInt()
                val alignment = LLVMABIAlignmentOfType(llvmTargetData, kObjHeaderPtr)
                call(context.llvm.memsetFunction,
                        listOf(slotsMem, Int8(0).llvm,
                                Int32(slotCount * pointerSize).llvm, Int32(alignment).llvm,
                                Int1(0).llvm))
            }
            addPhiIncoming(slotsPhi!!, prologueBb to slots)
            br(localsInitBb)
        }

        appendingTo(localsInitBb) {
            br(entryBb)
        }

        appendingTo(epilogueBb) {
            when {
                returnType == voidType -> {
                    releaseVars()
                    assert(returnSlot == null)
                    LLVMBuildRetVoid(builder)
                }
                returns.isNotEmpty() -> {
                    val returnPhi = phi(returnType!!)
                    addPhiIncoming(returnPhi, *returns.toList().toTypedArray())
                    if (returnSlot != null) {
                        updateReturnRef(returnPhi, returnSlot!!)
                    }
                    releaseVars()
                    LLVMBuildRet(builder, returnPhi)
                }
            // Do nothing, all paths throw.
                else -> LLVMBuildUnreachable(builder)
            }
        }

        appendingTo(cleanupLandingpad) {
            val landingpad = gxxLandingpad(numClauses = 0)
            LLVMSetCleanup(landingpad, 1)
            releaseVars()
            LLVMBuildResume(builder, landingpad)
        }

        returns.clear()
        vars.clear()
        returnSlot = null
        slotsPhi = null
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the mutable position of instructions being inserted.
     *
     * This class is introduced to workaround unreachable code handling.
     */
    inner class PositionHolder {
        private val builder: LLVMBuilderRef = LLVMCreateBuilder()!!


        fun getBuilder(): LLVMBuilderRef {
            if (isAfterTerminator) {
                positionAtEnd(basicBlock("unreachable", null))
            }

            return builder
        }

        /**
         * Should be `true` iff the position is located after terminator instruction.
         */
        var isAfterTerminator: Boolean = false
            private set

        fun setAfterTerminator() {
            isAfterTerminator = true
        }

        val currentBlock: LLVMBasicBlockRef
            get() = LLVMGetInsertBlock(builder)!!

        fun positionAtEnd(block: LLVMBasicBlockRef) {
            LLVMPositionBuilderAtEnd(builder, block)
            basicBlockToLastLocation[block]?.let(this@PositionHolder::debugLocation)
            val lastInstr = LLVMGetLastInstruction(block)
            isAfterTerminator = lastInstr != null && (LLVMIsATerminatorInst(lastInstr) != null)
        }

        fun dispose() {
            LLVMDisposeBuilder(builder)
        }

        fun debugLocation(locationInfo: LocationInfo): DILocationRef? {
            if (!context.shouldContainDebugInfo()) return null
            return LLVMBuilderSetDebugLocation(
                    builder,
                    locationInfo.line,
                    locationInfo.column,
                    locationInfo.scope)
        }

        fun resetDebugLocation() {
            if (!context.shouldContainDebugInfo()) return
            LLVMBuilderResetDebugLocation(builder)
        }
    }

    private var currentPositionHolder: PositionHolder = PositionHolder()

    /**
     * Returns `true` iff the current code generation position is located after terminator instruction.
     */
    fun isAfterTerminator() = currentPositionHolder.isAfterTerminator

    val currentBlock: LLVMBasicBlockRef
        get() = currentPositionHolder.currentBlock

    /**
     * The builder representing the current code generation position.
     *
     * Note that it shouldn't be positioned directly using LLVM API due to some hacks.
     * Use e.g. [positionAtEnd] instead. See [PositionHolder] for details.
     */
    val builder: LLVMBuilderRef
        get() = currentPositionHolder.getBuilder()

    fun positionAtEnd(bbLabel: LLVMBasicBlockRef) = currentPositionHolder.positionAtEnd(bbLabel)

    inline private fun <R> preservingPosition(code: () -> R): R {
        val oldPositionHolder = currentPositionHolder
        val newPositionHolder = PositionHolder()
        currentPositionHolder = newPositionHolder
        try {
            return code()
        } finally {
            currentPositionHolder = oldPositionHolder
            newPositionHolder.dispose()
        }
    }

    inline fun <R> appendingTo(block: LLVMBasicBlockRef, code: FunctionGenerationContext.() -> R) = preservingPosition {
        positionAtEnd(block)
        code()
    }

    private val needSlots: Boolean
        get() {
            return slotCount > 1 || localAllocs > 0 ||
                    // Prevent empty cleanup on mingw to workaround LLVM bug:
                    context.config.targetManager.target == KonanTarget.MINGW
        }

    private fun releaseVars() {
        if (needSlots) {
            call(context.llvm.leaveFrameFunction,
                    listOf(slotsPhi!!, Int32(slotCount).llvm))
        }
    }
}




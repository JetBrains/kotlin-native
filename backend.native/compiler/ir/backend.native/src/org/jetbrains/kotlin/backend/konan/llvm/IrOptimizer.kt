package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.llvm.ir2string
import org.jetbrains.kotlin.backend.konan.llvm.*

import org.jetbrains.kotlin.backend.konan.*
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
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing

fun optimizeIR(module: IrModuleFragment) {
    specialize(module)
}

//
// Specialization is a two step process:
//
// 1. We need to get specialized bodies 
//
// 2. We need to substitute specialized entities (calls, what else?)
// 
// The (1) is non-existent, for now, so two make things separate,
// I want to make the substitution pass first, 
// obtaining bodies from the user (== the library author)
//
// The idea here is for the user to provide specialized bodies of generic
// classes/functions and to instruct the compiler useing an annotation:
//
// class Atrray<T> {
//      ...
// }
//
// @Specialization("Array", "Byte")
// class Array_Byte {
//      ...
// }
//
// The ((specialized class) signature) should match 
// the (specialized (class signature))
// that's user's responsibility 
//
fun specialize(module: IrModuleFragment) {

    // Disabled for now
    // generateSpecializedBodies()

    applyUserProvidedBodies(module)
}

fun applyUserProvidedBodies(module: IrModuleFragment) {
    val specializations = Specializations()
    module.acceptVoid(CollectUserProvidedBodiesVisitor(specializations))



    if (!specializations.mapping.isEmpty()) {
        rewriteBodies(module, specializations)
        println("### Module Rewritten: " + ir2string(module))
    }

}

class Specializations {
    val mapping = mutableMapOf<Pair<String, List<String>>, FunctionDescriptor>()
}

class CollectUserProvidedBodiesVisitor(var specializations: Specializations): IrElementVisitorVoid {

    var mapping = specializations.mapping

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(callee: IrCall) {
        val descriptor = callee.descriptor.original as FunctionDescriptor

        val annotation = descriptor.specializationAnnotation
        if (annotation == null) return

        //println("specialization key: " + annotation)

        mapping[annotation] = descriptor
    }
}

private fun IrCallWithNewDescriptor(call: IrCall, newDescriptor: CallableDescriptor): IrCall {
    var newCall =  IrCallImpl(call.startOffset, 
                              call.endOffset, 
                              call.type, 
                              newDescriptor,                                // This one is new
                              mapOf<TypeParameterDescriptor, KotlinType>(), // And this one is empty
                              call.origin, 
                              call.superQualifier)

    newDescriptor.valueParameters.mapIndexed { i, valueParameterDescriptor ->
        newCall.putValueArgument(i, call.getValueArgument(i))
    }

    return newCall
}

private fun keyByCallee(callee: IrCall): Pair<String, List<String>> {
    val descriptor = callee.descriptor.original as FunctionDescriptor

    val name = descriptor.symbolName
    val typeNames = descriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair("$name", typeNames)

    //println("callee key: " + pair)

    return pair
}

private fun rewriteBodies(module: IrModuleFragment, specializations: Specializations) {

    module.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(callee: IrCall): IrExpression {
            callee.transformChildrenVoid(this)

            val descriptor = callee.descriptor.original as FunctionDescriptor
            val oldDescriptor = descriptor
            val key = keyByCallee(callee)
            val newDescriptor = specializations.mapping[key] 
            if (newDescriptor != null) {
                println("Specialization match on key" + key)
                return IrCallWithNewDescriptor(callee, newDescriptor!!)
            } else {
                return callee
            }
        }
    })
}


package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.llvm.ir2string
import org.jetbrains.kotlin.backend.konan.llvm.*

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptorBase
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
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

    module.acceptVoid(CollectUserProvidedNamesVisitor(specializations))
    //module.acceptVoid(CollectUserProvidedClassesVisitor(specializations))

    if (!specializations.funcMapping.isEmpty()) {
        rewriteBodies(module, specializations)
        println("### Functions rewritten in module: " + ir2string(module))
    }
    if (!specializations.classMapping.isEmpty()) {
        rewriteClasses(module, specializations)
        println("### Classes rewritten in module: " + ir2string(module))
    }
}

class Specializations {
    val funcMapping = mutableMapOf<Pair<String, List<String>>, FunctionDescriptor>()

    val classMapping = mutableMapOf<Pair<String, List<String>>, ClassDescriptor>()
    val classHazards = mutableMapOf<Pair<String, List<String>>, Boolean>()
}

class CollectUserProvidedNamesVisitor(var specializations: Specializations): IrElementVisitorVoid {

    var funcMapping = specializations.funcMapping
    var classMapping = specializations.classMapping

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(clazz: IrClass) {
        val descriptor = clazz.descriptor.original as ClassDescriptor

        val annotation = descriptor.specializationAnnotation
        if (annotation == null) return

        println("specialization class key: " + annotation)

        classMapping[annotation] = descriptor
    }

    override fun visitCall(callee: IrCall) {
        val descriptor = callee.descriptor.original as FunctionDescriptor

        val annotation = descriptor.specializationAnnotation
        if (annotation == null) return

        println("specialization func key: " + annotation)

        funcMapping[annotation] = descriptor
    }
}

private fun changeReceiverParameter(oldDescriptor: FunctionDescriptor, receiverParameter: ReceiverParameterDescriptor? ): FunctionDescriptor {
    val newDescriptor = SimpleFunctionDescriptorImpl.create(
            oldDescriptor.containingDeclaration,
            oldDescriptor.annotations,
            oldDescriptor.name,
            oldDescriptor.kind,
            oldDescriptor.source
    )

    newDescriptor.initialize(
            receiverParameter?.type,
            receiverParameter,
            //oldDescriptor.receiverParameterType, // CHANGE!!!
            //oldDescriptor.dispatchReceiverParameter, // CHANGE !!!
            oldDescriptor.typeParameters, // CHANGE?
            oldDescriptor.valueParameters,
            oldDescriptor.returnType,
            oldDescriptor.modality,
            oldDescriptor.visibility
    )

    return newDescriptor

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

    println("HOW COME: " + newDescriptor.dispatchReceiverParameter)

    //newDescriptor.dispatchReceiverParameter = call.descriptor.dispatchReceiverParameter

    return newCall
}


// FIXME: shamelessly copied and pasted from RTTI generator
private fun ClassDescriptor.getContributedMethods(): List<FunctionDescriptor> {
    val contributedDescriptors = unsubstitutedMemberScope.getContributedDescriptors()
    // (includes declarations from supers)

    val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

    val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
    val getters = properties.mapNotNull { it.getter }
    val setters = properties.mapNotNull { it.setter }

    val allMethods = functions + getters + setters
    return allMethods
}

private fun allMembers(clazz: ClassDescriptor): List<DeclarationDescriptor> {
    val members = clazz.unsubstitutedMemberScope.getContributedDescriptors()
    val primaryConstructor = clazz.getUnsubstitutedPrimaryConstructor()
    val constructors = clazz.getConstructors()
    val getters = members.filterIsInstance<PropertyDescriptor>().map{it -> it.getter }
    val setters = members.filterIsInstance<PropertyDescriptor>().map{it -> it.setter }
    val allCombined =  members+listOf(primaryConstructor)+constructors+getters+setters
    return allCombined.filterNotNull()
}


private fun sameMember(newClassDescriptor: ClassDescriptor, 
                   calleeDescriptor: DeclarationDescriptor): DeclarationDescriptor {
    val newMembers =  allMembers(newClassDescriptor)
    //FIXME remove as
    val oldMembers = allMembers(calleeDescriptor.getContainingDeclaration() as ClassDescriptor)

    //if (oldMembers.size != newMembers.size) {
        oldMembers.forEach{ 
            println(" From: "+ it)
        }
        newMembers.forEach{
            println("   To: "+it)
        }
     //   throw Error("Generic and specializer members differ")
   // }


    oldMembers.zip(newMembers).forEach { (old, new) -> 
        println("COMPARING " + old.original + " WITH " + calleeDescriptor)
        if (old.original == calleeDescriptor) {
            println("REWRITE CALL: " + old + " -> " + new)
            return new
        }
    } 

    throw Error("Could not find matching member")

}

private fun IrCallWithNewClassDescriptor(call: IrCall, newClassDescriptor: ClassDescriptor, receiver: ReceiverParameterDescriptor?): IrCall {

// FIXME: remove 'as'
    val specializedDescriptor: FunctionDescriptor = sameMember(newClassDescriptor, call.descriptor.original as FunctionDescriptor) as FunctionDescriptor

    val newCalleeDescriptor = if (specializedDescriptor is ConstructorDescriptor)  {
        specializedDescriptor 
    } else {
        changeReceiverParameter(specializedDescriptor, receiver)
    }

    var newCall =  IrCallImpl(call.startOffset, 
                              call.endOffset, 
                              newCalleeDescriptor.returnType!!,
                              //call.type, 
                              newCalleeDescriptor,                                // This one is new
                              mapOf<TypeParameterDescriptor, KotlinType>(), // And this one is empty
                              call.origin, 
                              call.superQualifier)

    newCalleeDescriptor.valueParameters.mapIndexed { i, valueParameterDescriptor ->
        newCall.putValueArgument(i, call.getValueArgument(i))
    }

    newCall.dispatchReceiver = call.dispatchReceiver
    newCall.extensionReceiver = call.extensionReceiver

    return newCall
}

private fun changeType(old: ValueDescriptor, type: KotlinType): ValueDescriptor {
    val new = 
     when (old) {
         is LocalVariableDescriptor -> LocalVariableDescriptor(
                                            old.containingDeclaration,
                                            old.annotations,
                                            old.name, /// Shouldn't we introduce a new name?
                                            type,
                                            (old as LocalVariableDescriptor).isVar(),
                                            old.isDelegated(),
                                            old.source)
         is ValueParameterDescriptor,
         is IrTemporaryVariableDescriptor,
         is LazyClassReceiverParameterDescriptor -> old // CHANGE!!!!
         else -> {
             TODO()
         }
     }


    println("REWRITTEN VALUE: " + old + " -> " + new)
    return new
}

private fun IrGetVarWithNewType(value: IrGetValue, newClassDescriptor: ClassDescriptor): IrGetValue {
    val newValueDescriptor = changeType(value.descriptor, newClassDescriptor.getDefaultType())

    var newGetValue = IrGetValueImpl(value.startOffset,
                                     value.endOffset,
                                     newValueDescriptor,
                                     value.origin)
    return newGetValue

}

private fun IrSetVarWithNewType(value: IrSetVariable, newClassDescriptor: ClassDescriptor): IrSetVariable {
    val newValueDescriptor = changeType(value.descriptor, newClassDescriptor.getDefaultType())

    var newSetVariable = IrSetVariableImpl(value.startOffset,
                                     value.endOffset,
                                     newValueDescriptor as VariableDescriptor, // FIXME: remove as
                                     value.value,
                                     value.origin)
    return newSetVariable

}

private fun keyByCallee(callee: IrCall): Pair<String, List<String>> {
    val descriptor = callee.descriptor.original as FunctionDescriptor

    val name = descriptor.symbolName
    val typeNames = descriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair("$name", typeNames)

    println("callee key: " + pair)

    return pair
}


private fun keyByClassCallee(classDescriptor: ClassDescriptor, callee: IrCall): Pair<String, List<String>> {
    //val classDescriptor = clazz.descriptor.original as ClassDescriptor
    val calleeDescriptor = callee.descriptor.original as FunctionDescriptor

    val name = classDescriptor.symbolName
    val typeNames = calleeDescriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair("$name", typeNames)

    println("class key: " + pair)

    return pair
}

private fun keyByKotlinType(type: KotlinType): Pair<String, List<String>> {
    val name = "kclass:" + type.constructor
    val typeNames = type.arguments.map{it -> it.toString()}

    val pair = Pair("$name", typeNames)
    println("receiver key: " + pair)

    return pair

}

private fun rewriteBodies(module: IrModuleFragment, specializations: Specializations) {
    module.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(callee: IrCall): IrExpression {
            callee.transformChildrenVoid(this)

            val descriptor = callee.descriptor.original as FunctionDescriptor
            val key = keyByCallee(callee)
            val newDescriptor = specializations.funcMapping[key] 
            if (newDescriptor != null) {
                println("Specialization MATCH on key" + key)
                return IrCallWithNewDescriptor(callee, newDescriptor!!)
            } else {
                return callee
            }
        }
    })
}

private fun rewriteClasses(module: IrModuleFragment, specializations: Specializations) {
    module.transformChildrenVoid(object : IrElementTransformerVoid() {


        override fun visitCall(callee: IrCall): IrExpression {
            callee.transformChildrenVoid(this)
            println("")

            println("QQQ: " + callee.descriptor.original)

            val descriptor = callee.descriptor.original as FunctionDescriptor

            val receiver = callee.descriptor.dispatchReceiverParameter
            println("reciever: " + callee.descriptor.dispatchReceiverParameter?.type?.arguments ?: "no reciever param")
            val key = if (receiver!= null) {
                keyByKotlinType(callee.descriptor.dispatchReceiverParameter?.type!!)
            } else {

                val clazz = descriptor.getContainingDeclaration()
                if (clazz !is ClassDescriptor) return callee

                println("DESCRIPTOR: " + descriptor)
                println("CLASS: " + clazz)
                println(descriptor.typeParameters.map{callee.getTypeArgument(it).toString()})

                keyByClassCallee(clazz, callee)
            }
            val newClassDescriptor = specializations.classMapping[key] 
            if (newClassDescriptor != null) {
                println("Specialization MATCH on key" + key)
                // FIXME: CHANGE receiver should be with a new type
                return IrCallWithNewClassDescriptor(callee, newClassDescriptor!!, receiver)
            } else {
                return callee
            }
        }



        override fun visitGetValue(value: IrGetValue): IrExpression {
            value.transformChildrenVoid(this)
            println("")

            val descriptor = value.descriptor
            val type = descriptor.getType()
            val key = keyByKotlinType(type)
            val newClassDescriptor = specializations.classMapping[key]
            if (newClassDescriptor != null) {
                println("VARIABLE " + descriptor + " specialization MATCH on key" + key)
                return IrGetVarWithNewType(value, newClassDescriptor)
            } else {
                return value
            }
        }

        override fun visitSetVariable(value: IrSetVariable): IrExpression {
            value.transformChildrenVoid(this)
            println("")

            val descriptor = value.descriptor
            val type = descriptor.getType()
            val key = keyByKotlinType(type)
            val newClassDescriptor = specializations.classMapping[key]
            if (newClassDescriptor != null) {
                println("VARIABLE " + descriptor + " specialization MATCH on key" + key)
                return IrSetVarWithNewType(value, newClassDescriptor)
            } else {
                return value
            }
        }


    })
}


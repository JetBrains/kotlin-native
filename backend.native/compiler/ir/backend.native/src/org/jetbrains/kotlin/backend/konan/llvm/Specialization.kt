package org.jetbrains.kotlin.backend.konan.optimizer

import org.jetbrains.kotlin.backend.konan.llvm.ir2string
import org.jetbrains.kotlin.backend.konan.llvm.symbolName
import org.jetbrains.kotlin.backend.konan.*

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.types.KotlinType

//
// Specialization is a two step process:
//
// 1. We need to obtain specialized bodies 
//
// 2. We need to substitute specialized entities (calls, what else?)
// 
// The (1) is non-existent for now, so to make things separate,
// I want to make the substitution pass first, 
// obtaining bodies from the user (== the library author)
//
// The idea here is for the user to provide specialized bodies of generic
// classes/functions and to instruct the compiler using annotations:
//
// @Specialization("Array_Byte", "Byte")
// class Atrray<T> {
//      ...
// }
//
// class Array_Byte {
//      ...
// }
//
// The ((specialized class) members) should match 
// the (specialized (class members))
// that's user's responsibility 
//

fun specializer(module: IrModuleFragment) {

    // We don't have automatic specialization yet
    // generateSpecializedBodies()

    applyUserProvidedSpecializations(module)
}

fun applyUserProvidedSpecializations(module: IrModuleFragment) {

    val context = SpecializationContext()

    module.acceptVoid(CollectUserProvidedNamesVisitor(context))

    if (hazards(module)) return;

    if (!context.classMapping.isEmpty()) {
        rewriteClasses(module, context)
        println("### Classes specialized in module: " + ir2string(module))
    }

    if (!context.functionMapping.isEmpty()) {
        rewriteFunctions(module, context)
        println("### Functions specialized in module: " + ir2string(module))
    }

}

typealias ClassKey = Pair<ClassDescriptor?, List<String>>
typealias FunctionKey = Pair<FunctionDescriptor, List<String>>

class SpecializationContext {
    val functionMapping = mutableMapOf<FunctionKey, FunctionDescriptor>()
    val classMapping = mutableMapOf<ClassKey, ClassDescriptor>()
}


fun hazards(module: IrModuleFragment): Boolean {
    // For now we just blindly allow any specialization,
    // not caring to check for escapes or incompatibilities
    return false
}

fun findSisterFunctionByName(name: String, original: FunctionDescriptor): FunctionDescriptor {
    val scope = original.getContainingDeclaration() 
    println("SCOPE: " + scope)

    if (scope !is PackageFragmentDescriptor) {
        TODO()
    }

    val memberScope = scope.getMemberScope()
    memberScope.getContributedDescriptors().forEach{
        println("SISTER SEARCHING:" + (it as FunctionDescriptor).symbolName)
        if (it is FunctionDescriptor && 
            it.symbolName == name) {
            println("SISTER FOUND:" + it)
            return it
        }
    }
    throw(Error("Could not find specialization in the scope: " + name ))
}

fun findSisterClassByName(name: String, original: DeclarationDescriptor): ClassDescriptor {
    val scope = original.getContainingDeclaration() 

    if (scope !is PackageFragmentDescriptor) {
        TODO()
    }

    val memberScope = scope.getMemberScope()
    memberScope.getContributedDescriptors().forEach{
        if (it is ClassDescriptor && 
            it.symbolName == name) {
            println("SISTER FOUND:" + it)
            return it
        }
    }
    throw(Error("Could not find specialization in the scope: " + name))
}


private fun collectFunctionSpecializations(context: SpecializationContext, genericDescriptor: FunctionDescriptor) { 
    val annotations = genericDescriptor.specializationAnnotations
    if (annotations.isEmpty()) return

        annotations.forEach { 
            val (specificName, typeSubstitutions) = it
            val specificDescriptor = findSisterFunctionByName(specificName, genericDescriptor)
            val key = Pair(genericDescriptor, typeSubstitutions)
            println("SPECIALIZATION: known key function: " + key)
            context.functionMapping[key] = specificDescriptor
        }
}

private fun collectClassSpecializations(context: SpecializationContext, genericDescriptor: ClassDescriptor) { 
    val annotations = genericDescriptor.specializationAnnotations
    if (annotations.isEmpty()) return

        annotations.forEach { 
            val (specificName, typeSubstitutions) = it
            val specificDescriptor = findSisterClassByName(specificName, genericDescriptor)
            val key = Pair(genericDescriptor, typeSubstitutions)
            println("SPECIALIZATION: known key class: " + key)
            context.classMapping[key] = specificDescriptor
        }
}

class CollectUserProvidedNamesVisitor(var context: SpecializationContext): IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(callee: IrCall) {
        callee.acceptChildrenVoid(this)

        // FIXME: remove as
        val descriptor = callee.descriptor.original as FunctionDescriptor

        collectFunctionSpecializations(context, descriptor)
    }

    override fun visitClass(clazz: IrClass) {
        clazz.acceptChildrenVoid(this)

        val genericDescriptor = clazz.descriptor.original as ClassDescriptor
        collectClassSpecializations(context, genericDescriptor)
   }

   override fun visitVariable(value: IrVariable) {
        value.acceptChildrenVoid(this)

        val descriptor = value.descriptor
        val type = descriptor.getType()
        val typeDeclarationDescriptor = type.constructor.getDeclarationDescriptor()

        if (typeDeclarationDescriptor !is ClassDescriptor) return

        collectClassSpecializations(context, typeDeclarationDescriptor)
    }
}


internal fun keyByCallee(callee: IrCall): FunctionKey {
    val descriptor = callee.descriptor.original as FunctionDescriptor

    val typeNames = descriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair(descriptor, typeNames)

    println("SPECIALIZATION: key by callee: " + pair)

    return pair
}


internal fun keyByClassMember(classDescriptor: ClassDescriptor, callee: IrCall): ClassKey {
    val calleeDescriptor = callee.descriptor.original as FunctionDescriptor

    val typeNames = calleeDescriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair(classDescriptor, typeNames)

    println("SPECIALIZATION: key by class class member: " + pair)

    return pair
}

internal fun keyByKotlinType(type: KotlinType): ClassKey {
    val typeDeclarationDescriptor = type.constructor.getDeclarationDescriptor()

    if (typeDeclarationDescriptor !is ClassDescriptor) {
        return Pair(null, listOf())
    }

    val typeNames = type.arguments.map{it -> it.toString()}

    val pair = Pair(typeDeclarationDescriptor as ClassDescriptor, typeNames)
    println("SPECIALIZATION: key by receiver type: " + pair)

    return pair

}

internal fun rewriteFunctions(module: IrModuleFragment, context: SpecializationContext) {
    module.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(callee: IrCall): IrExpression {
            callee.transformChildrenVoid(this)

            val descriptor = callee.descriptor.original as FunctionDescriptor
            val key = keyByCallee(callee)
            val newDescriptor = context.functionMapping[key] 
            if (newDescriptor != null) {
                println("SPECIALIZATION: function MATCH on key" + key)
                return IrCallWithNewFunction(callee, newDescriptor!!)
            } else {
                return callee
            }
        }
    })
}

internal fun rewriteClasses(module: IrModuleFragment, context: SpecializationContext) {
    module.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(callee: IrCall): IrExpression {
            callee.transformChildrenVoid(this)

            val descriptor = callee.descriptor.original as FunctionDescriptor

            val receiver = callee.descriptor.dispatchReceiverParameter
            val key = if (receiver!= null) {
                keyByKotlinType(callee.descriptor.dispatchReceiverParameter?.type!!)
            } else {

                val clazz = descriptor.getContainingDeclaration()
                if (clazz !is ClassDescriptor) return callee
                keyByClassMember(clazz, callee)
            }
            val newClassDescriptor = context.classMapping[key] 
            if (newClassDescriptor != null) {
                println("SPECIALIZATION: member MATCH on key" + key)
                return IrCallWithNewClass(callee, newClassDescriptor!!, receiver)
            } else {
                return callee
            }
        }


        override fun visitGetValue(value: IrGetValue): IrExpression {
            value.transformChildrenVoid(this)

            val descriptor = value.descriptor
            val type = descriptor.getType()
            val key = keyByKotlinType(type)
            val newClassDescriptor = context.classMapping[key]
            if (newClassDescriptor != null) {
                return IrGetVarWithNewType(value, newClassDescriptor)
            } else {
                return value
            }
        }

        override fun visitSetVariable(value: IrSetVariable): IrExpression {
            value.transformChildrenVoid(this)

            val descriptor = value.descriptor
            val type = descriptor.getType()
            val key = keyByKotlinType(type)
            val newClassDescriptor = context.classMapping[key]
            if (newClassDescriptor != null) {
                return IrSetVarWithNewType(value, newClassDescriptor)
            } else {
                return value
            }
        }

        override fun visitVariable(value: IrVariable): IrStatement {
            value.transformChildrenVoid(this)

            val descriptor = value.descriptor
            val type = descriptor.getType()
            val key = keyByKotlinType(type)
            val newClassDescriptor = context.classMapping[key]
            if (newClassDescriptor != null) {
                return IrVarWithNewType(value, newClassDescriptor)
            } else {
                return value
            }
 
        }


    })
}


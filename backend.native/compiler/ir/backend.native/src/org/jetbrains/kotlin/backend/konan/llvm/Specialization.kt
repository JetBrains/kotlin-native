package org.jetbrains.kotlin.backend.konan.optimizer

import org.jetbrains.kotlin.backend.konan.llvm.ir2string
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.*

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.descriptors.ClassKind.*



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
fun specializer(module: IrModuleFragment) {

    // Disabled for now
    // generateSpecializedBodies()

    applyUserProvidedBodies(module)
}

fun applyUserProvidedBodies(module: IrModuleFragment) {
    val context = SpecializationContext()

    module.acceptVoid(CollectUserProvidedNamesVisitor(context))
    //module.acceptVoid(CollectUserProvidedClassesVisitor(context))

    if (hazards(module)) return;

    if (!context.classMapping.isEmpty()) {
        rewriteClasses(module, context)
        println("### Classes rewritten in module: " + ir2string(module))
    }

    if (!context.funcMapping.isEmpty()) {
        rewriteFunctions(module, context)
        println("### Functions rewritten in module: " + ir2string(module))
    }

}

class SpecializationContext {
    val funcMapping = mutableMapOf<Pair<String, List<String>>, FunctionDescriptor>()
    val classMapping = mutableMapOf<Pair<String, List<String>>, ClassDescriptor>()
}


fun hazards(module: IrModuleFragment): Boolean {
    // For now we just blindly allow ant specialization
    // not caring to check for escapes 
    return false
}

class CollectUserProvidedNamesVisitor(var context: SpecializationContext): IrElementVisitorVoid {

    var funcMapping = context.funcMapping
    var classMapping = context.classMapping

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(clazz: IrClass) {
        clazz.acceptChildrenVoid(this)

        val descriptor = clazz.descriptor.original as ClassDescriptor

        val annotation = descriptor.specializationAnnotation
        if (annotation == null) return


        println("SPECIALIZATION: known key class: " + annotation)
        classMapping[annotation] = descriptor
    }

    override fun visitCall(callee: IrCall) {
        callee.acceptChildrenVoid(this)

        // FIXME: remove as
        val descriptor = callee.descriptor.original as FunctionDescriptor

        val annotation = descriptor.specializationAnnotation
        if (annotation == null) return

        println("SPECIALIZATION: known key func: " + annotation)

        funcMapping[annotation] = descriptor

    }

    override fun visitVariable(value: IrVariable) {
        value.acceptChildrenVoid(this)

        val descriptor = value.descriptor
        val type = descriptor.getType()
        val typeDeclarationDescriptor = type.constructor.getDeclarationDescriptor()

        if (typeDeclarationDescriptor !is ClassDescriptor) return

        val annotation = (typeDeclarationDescriptor as ClassDescriptor).specializationAnnotation
        if (annotation == null) return

        println("SPECIALIZATION: known key class (from var): " + annotation)
        classMapping[annotation] = (typeDeclarationDescriptor as ClassDescriptor)
    }


}


internal fun keyByCallee(callee: IrCall): Pair<String, List<String>> {
    val descriptor = callee.descriptor.original as FunctionDescriptor

    val name = descriptor.symbolName
    val typeNames = descriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair("$name", typeNames)

    println("SPECIALIZATION: key by callee: " + pair)

    return pair
}


internal fun keyByClassMember(classDescriptor: ClassDescriptor, callee: IrCall): Pair<String, List<String>> {
    //val classDescriptor = clazz.descriptor.original as ClassDescriptor
    val calleeDescriptor = callee.descriptor.original as FunctionDescriptor

    val name = classDescriptor.symbolName
    val typeNames = calleeDescriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair("$name", typeNames)

    println("SPECIALIZATION: key by class class member: " + pair)

    return pair
}

internal fun keyByKotlinType(type: KotlinType): Pair<String, List<String>> {
    val typeDeclarationDescriptor = type.constructor.getDeclarationDescriptor()

    if (typeDeclarationDescriptor !is ClassDescriptor ||
        (typeDeclarationDescriptor as ClassDescriptor).kind == ANNOTATION_CLASS) {
        return Pair("%Irrelevant", listOf())
    }

    val name = (typeDeclarationDescriptor as ClassDescriptor).symbolName
    val typeNames = type.arguments.map{it -> it.toString()}

    val pair = Pair("$name", typeNames)
    println("SPECIALIZATION: key by receiver type: " + pair)

    return pair

}

internal fun rewriteFunctions(module: IrModuleFragment, context: SpecializationContext) {
    module.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(callee: IrCall): IrExpression {
            callee.transformChildrenVoid(this)

            val descriptor = callee.descriptor.original as FunctionDescriptor
            val key = keyByCallee(callee)
            val newDescriptor = context.funcMapping[key] 
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
                // FIXME: CHANGE receiver should be with a new type
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


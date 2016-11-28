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
        rewriteBodies(module, context)
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

        println("SPECIALIZER: visitClass")
        val descriptor = clazz.descriptor.original as ClassDescriptor

        val annotation = descriptor.specializationAnnotation
        if (annotation == null) return

        println("specialization class key: " + annotation)

        classMapping[annotation] = descriptor
    }

    override fun visitCall(callee: IrCall) {
        callee.acceptChildrenVoid(this)
        println("SPECIALIZER: visitCall")
        val descriptor = callee.descriptor.original as FunctionDescriptor

        val annotation = descriptor.specializationAnnotation
        //val annotation = (callee.descriptor as FunctionDescriptor).specializationAnnotation
        if (annotation == null) { println("NO ANNOTATIONS\n") }
        if (annotation == null) return

        println("specialization func key: " + annotation)

        funcMapping[annotation] = descriptor

    }

    override fun visitVariable(value: IrVariable) {
        value.acceptChildrenVoid(this)

        println(value)

        val descriptor = value.descriptor
        val type = descriptor.getType()
        val typeDeclarationDescriptor = type.constructor.getDeclarationDescriptor()

        println(typeDeclarationDescriptor)
        if (typeDeclarationDescriptor !is ClassDescriptor) return

        println((typeDeclarationDescriptor as ClassDescriptor).annotations.getAllAnnotations())
        val annotation = (typeDeclarationDescriptor as ClassDescriptor).specializationAnnotation
        println(annotation)
        if (annotation == null) return

        println("specialization class (from var) key: " + annotation)
        classMapping[annotation] = (typeDeclarationDescriptor as ClassDescriptor)
    }


}


internal fun keyByCallee(callee: IrCall): Pair<String, List<String>> {
    val descriptor = callee.descriptor.original as FunctionDescriptor

    val name = descriptor.symbolName
    val typeNames = descriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair("$name", typeNames)

    println("callee key: " + pair)

    return pair
}


internal fun keyByClassCallee(classDescriptor: ClassDescriptor, callee: IrCall): Pair<String, List<String>> {
    //val classDescriptor = clazz.descriptor.original as ClassDescriptor
    val calleeDescriptor = callee.descriptor.original as FunctionDescriptor

    val name = classDescriptor.symbolName
    val typeNames = calleeDescriptor.typeParameters.map{callee.getTypeArgument(it).toString()}
    val pair = Pair("$name", typeNames)

    println("class key: " + pair)

    return pair
}

internal fun keyByKotlinType(type: KotlinType): Pair<String, List<String>> {
    val typeDeclarationDescriptor = type.constructor.getDeclarationDescriptor()

    if (typeDeclarationDescriptor !is ClassDescriptor ||
        (typeDeclarationDescriptor as ClassDescriptor).kind == ANNOTATION_CLASS) {
        return Pair("%Irrelevant", listOf())
    }

    println(typeDeclarationDescriptor as ClassDescriptor)
    val name = (typeDeclarationDescriptor as ClassDescriptor).symbolName
    val typeNames = type.arguments.map{it -> it.toString()}

    val pair = Pair("$name", typeNames)
    println("receiver key: " + pair)

    return pair

}

internal fun rewriteBodies(module: IrModuleFragment, context: SpecializationContext) {
    module.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(callee: IrCall): IrExpression {
            callee.transformChildrenVoid(this)

            val descriptor = callee.descriptor.original as FunctionDescriptor
            val key = keyByCallee(callee)
            val newDescriptor = context.funcMapping[key] 
            if (newDescriptor != null) {
                println("Specialization MATCH on key" + key)
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
            println("")

            val descriptor = callee.descriptor.original as FunctionDescriptor
            println("QQQ: " + callee.descriptor.original)

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
            val newClassDescriptor = context.classMapping[key] 
            if (newClassDescriptor != null) {
                println("CALL specialization MATCH on key" + key)
                // FIXME: CHANGE receiver should be with a new type
                return IrCallWithNewClass(callee, newClassDescriptor!!, receiver)
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
            val newClassDescriptor = context.classMapping[key]
            if (newClassDescriptor != null) {
                println("GET_VAR " + descriptor + " specialization MATCH on key" + key)
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
            val newClassDescriptor = context.classMapping[key]
            if (newClassDescriptor != null) {
                println("SET_VAR " + descriptor + " specialization MATCH on key" + key)
                return IrSetVarWithNewType(value, newClassDescriptor)
            } else {
                return value
            }
        }

        override fun visitVariable(value: IrVariable): IrStatement {
            value.transformChildrenVoid(this)
            println("")

            val descriptor = value.descriptor
            val type = descriptor.getType()
            val key = keyByKotlinType(type)
            val newClassDescriptor = context.classMapping[key]
            if (newClassDescriptor != null) {
                println("VAR" + descriptor + " specialization MATCH on key" + key)
                return IrVarWithNewType(value, newClassDescriptor)
            } else {
                println("VAR" + descriptor + " no match " + key)
                return value
            }
 
        }


    })
}


package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

private val annotationName = FqName("kotlin.Specialization")

private fun values(annotation: AnnotationDescriptor): Collection<ConstantValue<*>> {
    return annotation.allValueArguments.values
}

private fun valuesToPair(values: Collection<ConstantValue<*>>): Pair<String, List<String>> {
    return Pair((values.first() as StringValue).value, 
                (values.drop(1).first() as ArrayValue).value.map{it->(it as StringValue).value})

}

internal val FunctionDescriptor.specializationAnnotation: Pair<String, List<String>>?
    get() {
        val specialization = this.annotations.findAnnotation(annotationName) 
        return if (specialization != null) valuesToPair(values(specialization!!)) else null
    }


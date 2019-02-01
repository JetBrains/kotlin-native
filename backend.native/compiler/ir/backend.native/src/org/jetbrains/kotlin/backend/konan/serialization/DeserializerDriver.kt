/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.backend.konan.descriptors.EmptyDescriptorVisitorVoid
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.serialization.*

internal class DeserializerDriver(val context: Context) {

    private val cache = mutableMapOf<FunctionDescriptor, IrDeclaration?>()

    internal fun deserializeInlineBody(descriptor: FunctionDescriptor): IrDeclaration? = cache.getOrPut(descriptor) {
        if (!descriptor.needsSerializedIr) return null
        if (!descriptor.isDeserializableCallable) return null

        var deserializedIr: IrDeclaration? = null
        context.log { "### IR deserialization attempt:\t$descriptor" }
        try {
            deserializedIr = IrDeserializer(context, descriptor).decodeDeclaration()
            context.log { "${deserializedIr!!.descriptor}" }
            context.log { ir2stringWhole(deserializedIr!!) }
            context.log { "IR deserialization SUCCESS:\t$descriptor" }
        } catch (e: Throwable) {
            context.log { "IR deserialization FAILURE:\t$descriptor" }
        }
        deserializedIr
    }

    internal fun dumpAllInlineBodies() {
        context.log{"Now deserializing all inlines for debugging purpose."}
        context.moduleDescriptor.accept(
            InlineBodiesPrinterVisitor(InlineBodyPrinter()), Unit)
    }


    inner class InlineBodiesPrinterVisitor(worker: EmptyDescriptorVisitorVoid): DeepVisitor<Unit>(worker)

    inner class InlineBodyPrinter: EmptyDescriptorVisitorVoid() {

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit): Boolean {
            if (descriptor.isDeserializableCallable) {
                this@DeserializerDriver.deserializeInlineBody(descriptor)
            }

            return true
        }
    }
}

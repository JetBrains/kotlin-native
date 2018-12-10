/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.CompilerPhase
import org.jetbrains.kotlin.backend.common.CompilerPhaseManager
import org.jetbrains.kotlin.backend.common.DefaultPhaseRunner
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.backend.konan.descriptors.EmptyDescriptorVisitorVoid
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.util.dump

// An ugly hack to squeeze the deserialize op into the CompilerPhase framework.
internal class DeserializeIrData(val descriptor: FunctionDescriptor) {
    var ir: IrDeclaration? = null

    fun perform(context: Context) {
        context.log{"### IR deserialization attempt:\t$descriptor"}
        try {
            ir = IrDeserializer(context, descriptor).decodeDeclaration()
            context.log{"${ir!!.descriptor}"}
            context.log{ ir2stringWhole(ir!!) }
            context.log{"IR deserialization SUCCESS:\t$descriptor"}
        } catch(e: Throwable) {
            context.log{"IR deserialization FAILURE:\t$descriptor"}
            if (context.inVerbosePhase) e.printStackTrace()
        }
    }
}

internal object DeserializerPhase : CompilerPhase<Context, DeserializeIrData> {
    override val name = "Deserializer"
    override val description = "Deserialize inline bodies"
    override fun invoke(manager: CompilerPhaseManager<Context, DeserializeIrData>, input: DeserializeIrData): DeserializeIrData {
        input.perform(manager.context)
        return input
    }
}

internal object DeserializeIrDataPhaseRunner : DefaultPhaseRunner<Context, DeserializeIrData>() {
    override fun dumpElement(input: DeserializeIrData, phase: CompilerPhase<Context, DeserializeIrData>, context: Context, beforeOrAfter: BeforeOrAfter) {
        println("Descriptor: ${input.descriptor}")
        if (input.ir == null) {
            println("No IR")
        } else {
            println("IR: ${input.ir!!.dump()}")
        }
    }

    override fun phases(context: Context) = context.phases
    override fun elementName(input: DeserializeIrData): String = input.descriptor.name.asString()
    override fun configuration(context: Context): CompilerConfiguration = context.config.configuration
}

internal class DeserializerDriver(val context: Context, val parentPhaseManager: CompilerPhaseManager<Context, *>) {

    private val cache = mutableMapOf<FunctionDescriptor, IrDeclaration?>()

    internal fun deserializeInlineBody(descriptor: FunctionDescriptor): IrDeclaration? = cache.getOrPut(descriptor) {
        if (!descriptor.needsSerializedIr) return null
        if (!descriptor.isDeserializableCallable) return null

        val deserializeIrData = DeserializeIrData(descriptor)
        parentPhaseManager.createChildManager(deserializeIrData, DeserializeIrDataPhaseRunner)
                .phase(DeserializerPhase, deserializeIrData)
        deserializeIrData.ir
    }

    internal fun dumpAllInlineBodies() {
        if (! context.inVerbosePhase) return
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

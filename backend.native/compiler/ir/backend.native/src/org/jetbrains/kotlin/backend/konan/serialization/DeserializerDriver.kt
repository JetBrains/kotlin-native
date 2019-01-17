/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.backend.konan.descriptors.EmptyDescriptorVisitorVoid
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.serialization.*
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

internal object DeserializerPhase : AbstractNamedCompilerPhase<Context, DeserializeIrData, DeserializeIrData>() {
    override val name = "Deserializer"
    override val description = "Deserialize inline bodies"
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: DeserializeIrData): DeserializeIrData {
        input.perform(context)
        return input
    }

    override fun dumpInput(context: Context, input: DeserializeIrData) {
        dump(input)
    }

    override fun dumpOutput(context: Context, output: DeserializeIrData) {
        dump(output)
    }

    override fun verifyInput(context: Context, input: DeserializeIrData) {}
    override fun verifyOutput(context: Context, output: DeserializeIrData) {}

    private fun dump(data: DeserializeIrData) {
        println("Descriptor: ${data.descriptor}")
        if (data.ir == null) {
            println("No IR")
        } else {
            println("IR: ${data.ir!!.dump()}")
        }
    }
}

internal class DeserializerDriver(val context: Context, val phaserState: PhaserState) {

    private val cache = mutableMapOf<FunctionDescriptor, IrDeclaration?>()

    internal fun deserializeInlineBody(descriptor: FunctionDescriptor): IrDeclaration? = cache.getOrPut(descriptor) {
        if (!descriptor.needsSerializedIr) return null
        if (!descriptor.isDeserializableCallable) return null

        val deserializeIrData = DeserializeIrData(descriptor)
        phaserState.downlevel {
            DeserializerPhase.invoke(context.phaseConfig, phaserState, context, deserializeIrData)
        }
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

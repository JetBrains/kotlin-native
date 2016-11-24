package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import java.io.StringWriter

internal class Logger(val generator: CodeGenerator, override val context: Context) : ContextUtils {
  private var logcounter: Int = LLVMPrintModuleToString(context.llvmModule)!!.asCString().toString().lines().size

  //---------------------------------------------------------------------------//

  fun log(msg: String) {
    // logIr()
    println(msg)
  }

  //---------------------------------------------------------------------------//

  fun logIr() {
    val function = generator.currentFunction ?: return

    val irFunctionDeclaration = LLVMPrintValueToString(function.llvmFunction.getLlvmValue())!!.asCString().toString()
    irFunctionDeclaration.lines().forEach { println("    $it") }
  }

}

//-----------------------------------------------------------------------------//

fun ir2string(ir: IrElement?): String = ir2stringWhole(ir).takeWhile { it != '\n' }

//-----------------------------------------------------------------------------//

fun ir2stringWhole(ir: IrElement?): String {
  val strWriter = StringWriter()

  ir?.accept(DumpIrTreeVisitor(strWriter), "")
  return strWriter.toString()
}


//-----------------------------------------------------------------------------//

fun llvm2string(value: LLVMValueRef?): String {
  if (value == null) return "<null>"
  return LLVMPrintValueToString(value)!!.asCString().toString()
}


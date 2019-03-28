package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMAddNamedMetadataOperand
import llvm.LLVMModuleRef

fun embedLlvmLinkOptions(module: LLVMModuleRef, options: List<List<String>>) {
    options.forEach {
        val node = node(*it.map { it.mdString() }.toTypedArray())
        LLVMAddNamedMetadataOperand(module, "llvm.linker.options", node)
    }
}
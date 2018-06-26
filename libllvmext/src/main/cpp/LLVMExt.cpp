/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "LLVMExt.h"
#include "CodeGen.h"

#include <llvm/Support/FileSystem.h>
#include <llvm/Support/TargetSelect.h>
#include <llvm/InitializePasses.h>
#include <llvm/IR/Module.h>
#include <llvm/Transforms/IPO/PassManagerBuilder.h>
#include <llvm/Target/TargetMachine.h>
#include <llvm/LTO/Config.h>
#include <llvm/Analysis/TargetLibraryInfo.h>
#include <llvm/Transforms/IPO.h>
#include <llvm/Transforms/IPO/AlwaysInliner.h>
#include <llvm/Support/TargetRegistry.h>
#include <llvm/Linker/Linker.h>
#include <llvm/IR/Verifier.h>
#include <llvm/IR/LegacyPassManager.h>
#include <llvm/IR/DiagnosticPrinter.h>
#include <llvm/CodeGen/Passes.h>
#include <llvm/Support/ToolOutputFile.h>
#include <llvm/Analysis/TargetTransformInfo.h>
#include <llvm/IR/IRPrintingPasses.h>
#include <llvm/MC/SubtargetFeature.h>
#include <llvm/Support/Host.h>

#include <memory>
#include <ctime>

using namespace llvm;

class ModuleLinker {
 public:
  explicit ModuleLinker(LLVMContext &context)
      : mergedModule(new Module("merged", context)),
        linker(*mergedModule) {}

  bool linkModule(std::unique_ptr<Module> module, bool onlyNeeded) {
    unsigned flags = Linker::Flags::None;
    if (onlyNeeded) {
      flags |= Linker::Flags::LinkOnlyNeeded;
    }

    std::string name = module->getName();
    if (linker.linkInModule(std::move(module), flags)) {
      return true;
    }
    return verifyModule(*mergedModule, &logging::error());
  }

  std::unique_ptr<Module> mergedModule;

 private:
  Linker linker;
};

// Links given modules together and disposes them.
static std::unique_ptr<Module> linkModules(LLVMContext &context,
                                           LLVMModuleRef programModuleRef,
                                           LLVMModuleRef runtimeModuleRef,
                                           LLVMModuleRef stdlibModuleRef) {

  std::unique_ptr<Module> programModule(unwrap(programModuleRef));
  std::unique_ptr<Module> runtimeModule(unwrap(runtimeModuleRef));
  std::unique_ptr<Module> stdlibModule(unwrap(stdlibModuleRef));

  ModuleLinker linker(context);
  if (linker.linkModule(std::move(programModule), false)) {
    logging::error() << "Cannot link program.\n";
    return nullptr;
  }
  if (linker.linkModule(std::move(runtimeModule), false)) {
    logging::error() << "Cannot link program with runtime.\n";
    return nullptr;
  }
  // stdlib is very fat. We link it last and take only required definitions.
  if (linker.linkModule(std::move(stdlibModule), true)) {
    logging::error() << "Cannot link standard library.\n";
    return nullptr;
  }
  return std::move(linker.mergedModule);
}

void setFunctionAttributes(StringRef cpu, StringRef features, Module &module) {
  for (auto &fn : module) {
    auto &context = fn.getContext();
    AttributeList Attrs = fn.getAttributes();
    AttrBuilder newAttrs;

    if (!cpu.empty())
      newAttrs.addAttribute("target-cpu", cpu);
    if (!features.empty())
      newAttrs.addAttribute("target-features", features);

    fn.setAttributes(Attrs.addAttributes(context, AttributeList::FunctionIndex, newAttrs));
  }
}

// Mostly copy'n'paste from opt and llc for now.
void initLLVM(PassRegistry *registry) {
  InitializeAllTargets();
  InitializeAllTargetMCs();
  InitializeAllAsmPrinters();
  InitializeAllAsmParsers();

  initializeCore(*registry);

  initializeScalarOpts(*registry);
  initializeObjCARCOpts(*registry);
  initializeVectorization(*registry);
  initializeIPO(*registry);
  initializeAnalysis(*registry);
  initializeTransformUtils(*registry);
  initializeInstCombine(*registry);
  initializeInstrumentation(*registry);
  initializeTarget(*registry);

  initializeCodeGenPreparePass(*registry);
  initializeCodeGen(*registry);
  initializeLoopStrengthReducePass(*registry);
  initializeLowerIntrinsicsPass(*registry);

  initializeScalarizeMaskedMemIntrinPass(*registry);
  initializeAtomicExpandPass(*registry);
  initializeRewriteSymbolsLegacyPassPass(*registry);
  initializeWinEHPreparePass(*registry);
  initializeDwarfEHPreparePass(*registry);
  initializeSafeStackLegacyPassPass(*registry);
  initializeSjLjEHPreparePass(*registry);
  initializePreISelIntrinsicLoweringLegacyPassPass(*registry);
  initializeGlobalMergePass(*registry);
  initializeInterleavedAccessPass(*registry);
  initializeUnreachableBlockElimLegacyPassPass(*registry);
  initializeExpandReductionsPass(*registry);

  initializeConstantHoistingLegacyPassPass(*registry);
  initializeExpandReductionsPass(*registry);
}

void LLVMExtDiagnosticHandlerCallback(const DiagnosticInfo &DI, void* context) {
  bool *errorDetected = static_cast<bool *>(context);
  if (DI.getSeverity() == DS_Error)
    *errorDetected = true;

  if (auto *Remark = dyn_cast<DiagnosticInfoOptimizationBase>(&DI)) {
    if (!Remark->isEnabled()) {
      return;
    }
  }

  DiagnosticPrinterRawOStream DP(logging::error());
  logging::error() << LLVMContext::getDiagnosticMessagePrefix(DI.getSeverity()) << ": ";
  DI.print(DP);
  logging::error() << "\n";
}

// Copied for LTOCodeGenerator. Should change to something more sophisticated.
std::string determineTargetCPU(const CodeGenConfig &configuration) {
  std::string cpu;
  Triple triple(configuration.targetTriple);
  if (configuration.compilingForHost) {
    cpu = llvm::sys::getHostCPUName();
  }
  if (cpu.empty() && triple.isOSDarwin()) {
    if (triple.getArch() == llvm::Triple::x86_64)
      cpu = "core2";
    else if (triple.getArch() == llvm::Triple::x86)
      cpu = "yonah";
    else if (triple.getArch() == llvm::Triple::aarch64)
      cpu = "cyclone";
  }
  return cpu;
}

std::string determineTargetFeatures(const CodeGenConfig &configuration) {
  Triple triple(configuration.targetTriple);
  SubtargetFeatures features;
  features.getDefaultSubtargetFeatures(triple);
  return features.getString();
}

extern "C" {

int LLVMFatLtoCodegen(LLVMContextRef contextRef,
                      LLVMModuleRef programModuleRef,
                      LLVMModuleRef runtimeModuleRef,
                      LLVMModuleRef stdlibModuleRef,
                      CodeGenConfig codeGenConfig) {

  // LLVM global variable that enables profiling.
  TimePassesIsEnabled = static_cast<bool>(codeGenConfig.shouldProfile);

  std::error_code EC;
  sys::fs::OpenFlags OpenFlags = sys::fs::F_None;
  std::unique_ptr<ToolOutputFile> output(new ToolOutputFile(codeGenConfig.fileName, EC, OpenFlags));

  if (EC) {
    logging::error() << EC.message();
    return 1;
  }

  std::unique_ptr<LLVMContext> context(unwrap(contextRef));

  bool errorDetected = false;
  context->setDiagnosticHandlerCallBack(LLVMExtDiagnosticHandlerCallback);

  initLLVM(PassRegistry::getPassRegistry());

  auto module = linkModules(*context, programModuleRef, runtimeModuleRef, stdlibModuleRef);
  if (module == nullptr) {
    logging::error() << "Module linkage failed.\n";
    return 1;
  }

  // LLVM heavily relies on proper function attributes placement. Let's make it happy.
  std::string cpu = determineTargetCPU(codeGenConfig);
  std::string targetFeatures = determineTargetFeatures(codeGenConfig);
  setFunctionAttributes(cpu, targetFeatures, *module);

  CodeGen backend(codeGenConfig);
  if (backend.compile(std::move(module), output->os())) {
    logging::error() << "LLVM Pass Manager failed.\n";
    if (*static_cast<bool *>(context->getDiagnosticContext())) {
      return 1;
    }
    return 1;
  }
  // Preserve compiler's output.
  output->keep();
  // Print profiling report.
  reportAndResetTimings();
  return 0;
}

}
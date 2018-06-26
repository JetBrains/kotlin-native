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

#include "CodeGen.h"

#include <llvm/IR/Verifier.h>

// It is usual LLVM invocation function.
// The only special thing is that it runs preparation passes that are required
// to make bitcode a little bit cleaner.
bool CodeGen::compile(std::unique_ptr<Module> module, raw_pwrite_stream &os) {
  if (createTargetMachine()) {
    return true;
  }
  module->setDataLayout(targetMachine->createDataLayout());

  legacy::PassManager preparationPasses;
  preparationPasses.add(
      createTargetTransformInfoWrapperPass(targetMachine->getTargetIRAnalysis()));

  // Right after linkage we have a lot of unused symbols.
  // We don't want compiler to spend time optimizing dead code.
  preparationPasses.add(createInternalizePass());
  preparationPasses.add(createEliminateAvailableExternallyPass());
  preparationPasses.add(createGlobalDCEPass());
  // Many functions in runtime are marked as always_inline so it is better to inline them asap.
  preparationPasses.add(createAlwaysInlinerLegacyPass());
  preparationPasses.run(*module);

  legacy::PassManager modulePasses;
  modulePasses.add(
      createTargetTransformInfoWrapperPass(targetMachine->getTargetIRAnalysis()));

  legacy::FunctionPassManager functionPasses(module.get());
  functionPasses.add(
      createTargetTransformInfoWrapperPass(targetMachine->getTargetIRAnalysis()));

  createPasses(modulePasses, functionPasses);

  legacy::PassManager codeGenPasses;
  codeGenPasses.add(
      createTargetTransformInfoWrapperPass(targetMachine->getTargetIRAnalysis()));

  switch (config.outputKind) {
    case OUTPUT_KIND_BITCODE:
      codeGenPasses.add(createBitcodeWriterPass(os));
      break;
    case OUTPUT_KIND_OBJECT_FILE:
      targetMachine->addPassesToEmitFile(codeGenPasses, os, getCodeGenFileType());
      break;
  }

  functionPasses.doInitialization();
  for (Function &F : *module)
    if (!F.isDeclaration())
      functionPasses.run(F);
  functionPasses.doFinalization();

  modulePasses.run(*module);

  // Let's verify module after all passes
  bool hasDebugInfoError = false;
  if (verifyModule(*module, &logging::error(), &hasDebugInfoError)) {
    logging::error() << "Module verification after all optimization passes failed.\n";
    return true;
  }
  if (hasDebugInfoError) {
    if (config.shouldPreserveDebugInfo) {
      logging::error() << "Invalid debug info found after all optimization passes.\n";
      return true;
    }
  }
  codeGenPasses.run(*module);

  return false;
}

Optional<Reloc::Model> CodeGen::getRelocModel() {
  switch (config.relocMode) {
    case LLVMRelocDefault:
      return None;
    case LLVMRelocStatic:
      return Reloc::Model::Static;
    case LLVMRelocPIC:
      return Reloc::Model::PIC_;
    case LLVMRelocDynamicNoPic:
      return Reloc::Model::DynamicNoPIC;
  }
}

bool CodeGen::createTargetMachine() {
  std::string error;
  const llvm::Target *target = TargetRegistry::lookupTarget(config.targetTriple, error);
  if (!target) {
    logging::error() << error;
    return true;
  }
  llvm::TargetOptions options;
  targetMachine.reset(target->createTargetMachine(config.targetTriple,
                                                  getCPU(),
                                                  getTargetFeatures(),
                                                  options,
                                                  getRelocModel(),
                                                  None,
                                                  getCodegenOptLevel()));

  return false;
}

TargetMachine::CodeGenFileType CodeGen::getCodeGenFileType() {
  switch (config.outputKind) {
    case OUTPUT_KIND_OBJECT_FILE:
      return TargetMachine::CodeGenFileType::CGFT_ObjectFile;

    default:
      logging::error() << "Unsupported codegen file type!\n";
      return TargetMachine::CodeGenFileType::CGFT_Null;
  }
}

CodeGenOpt::Level CodeGen::getCodegenOptLevel() {
  switch (config.optLevel) {
    case 0:
      return CodeGenOpt::Level::None;
    case 1:
      return CodeGenOpt::Level::Less;
    case 2:
      return CodeGenOpt::Level::Default;
    case 3:
      return CodeGenOpt::Level::Aggressive;
    default:
      return CodeGenOpt::Level::Default;
  }
}

std::string CodeGen::getTargetFeatures() {
  SubtargetFeatures features("");
  features.getDefaultSubtargetFeatures(triple);
  return features.getString();
}

// Populates module and function pass managers based on target machine and compilation flags.
void CodeGen::createPasses(legacy::PassManager &modulePasses,
                                           legacy::FunctionPassManager &functionPasses) {
  std::unique_ptr<TargetLibraryInfoImpl> tlii(new TargetLibraryInfoImpl(triple));

  PassManagerBuilder passManagerBuilder;
  if (optLevel > 1) {
    passManagerBuilder.Inliner = createFunctionInliningPass(optLevel, sizeLevel, false);
  } else {
    // AlwaysInliner is part of preparation passes for now.
    // passManagerBuilder.Inliner = createAlwaysInlinerLegacyPass();
  }
  passManagerBuilder.OptLevel = optLevel;
  passManagerBuilder.SizeLevel = sizeLevel;
  passManagerBuilder.SLPVectorize = optLevel > 1;
  passManagerBuilder.LoopVectorize = optLevel > 1;
  passManagerBuilder.PrepareForLTO = static_cast<bool>(config.shouldPerformLto);
  passManagerBuilder.PrepareForThinLTO = static_cast<bool>(config.shouldPerformLto);

  modulePasses.add(new TargetLibraryInfoWrapperPass(*tlii));

  targetMachine->adjustPassManager(passManagerBuilder);

  functionPasses.add(new TargetLibraryInfoWrapperPass(*tlii));
  passManagerBuilder.populateFunctionPassManager(functionPasses);
  passManagerBuilder.populateModulePassManager(modulePasses);
  passManagerBuilder.populateLTOPassManager(modulePasses);
}

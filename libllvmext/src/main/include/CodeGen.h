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
#ifndef LIBLLVMEXT_CODEGEN_H
#define LIBLLVMEXT_CODEGEN_H

#include "logging.h"

#include <memory>

#include <llvm/IR/Module.h>
#include <LLVMExt.h>
#include <llvm/Support/TargetRegistry.h>
#include <llvm/Target/TargetMachine.h>
#include <llvm/MC/SubtargetFeature.h>
#include <llvm/IR/LegacyPassManager.h>
#include <llvm/Analysis/TargetLibraryInfo.h>
#include <llvm/Analysis/TargetTransformInfo.h>
#include <llvm/Transforms/IPO.h>
#include <llvm/Transforms/IPO/PassManagerBuilder.h>
#include <llvm/Transforms/IPO/AlwaysInliner.h>
#include <llvm/Bitcode/BitcodeWriterPass.h>

using namespace llvm;

// Pretty much inspired by Clang's BackendUtil
class CodeGen {
 public:
  explicit CodeGen(const CodeGenConfig &config)
      : config(config), triple(config.targetTriple) {
    optLevel = static_cast<unsigned int>(config.optLevel);
    sizeLevel = static_cast<unsigned int>(config.sizeLevel);
  }

  bool compile(std::unique_ptr<Module> module, raw_pwrite_stream& os);

 private:
  unsigned int optLevel;
  unsigned int sizeLevel;
  const CodeGenConfig &config;
  Triple triple;

  std::unique_ptr<TargetMachine> targetMachine;

 private:
  Optional<Reloc::Model> getRelocModel();

  bool createTargetMachine();

  TargetMachine::CodeGenFileType getCodeGenFileType();

  CodeGenOpt::Level getCodegenOptLevel();

  std::string getTargetFeatures();

  // TODO: determine cpu correctly
  std::string getCPU() {
    return "";
  }

  void createPasses(legacy::PassManager &modulePasses, legacy::FunctionPassManager &functionPasses);
};

#endif //LIBLLVMEXT_CODEGEN_H

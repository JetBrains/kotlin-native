#include <llvm/IR/DebugInfo.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/DIBuilder.h>
#include <llvm/IR/DebugInfoMetadata.h>
#include <llvm/IR/Instruction.h>
#include <llvm/Support/Casting.h>
#include "DebugInfoC.h"


/** 
 * c++ --std=c++11 llvmDebugInfoC/src/DebugInfoC.cpp -IllvmDebugInfoC/include/ -Idependencies/all/clang+llvm-3.9.0-darwin-macos/include -Ldependencies/all/clang+llvm-3.9.0-darwin-macos/lib  -lLLVMCore -lLLVMSupport -lncurses -shared -o libLLVMDebugInfoC.dylib
 */

namespace llvm {
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIBuilder,        DIBuilderRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DICompileUnit,    DICompileUnitRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIFile,           DIFileRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIBasicType,      DIBasicTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIType,           DITypeOpaqueRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIModule,         DIModuleRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIScope,          DIScopeOpaqueRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DISubroutineType, DISubroutineTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DISubprogram,     DISubprogramRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocation,       DILocationRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocalVariable,  DILocalVariableRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIExpression,     DIExpressionRef)

// from Module.cpp
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(Module,        LLVMModuleRef)
}

extern "C" {

  DIBuilderRef DICreateBuilder(LLVMModuleRef module) {
    return llvm::wrap(new llvm::DIBuilder(* llvm::unwrap(module)));
  }

  void DIFinalize(DIBuilderRef builder) {
    llvm::unwrap(builder)->finalize();
  }
  
  DICompileUnitRef DICreateCompilationUnit(DIBuilderRef builder, unsigned int lang,
                                           const char *file, const char* dir,
                                           const char * producer, int isOptimized,
                                           const char * flags, unsigned int rv) {
    return llvm::wrap(llvm::unwrap(builder)->createCompileUnit(lang, file, dir, producer, isOptimized, flags, rv));
  }

  DIFileRef DICreateFile(DIBuilderRef builder, const char *filename, const char *directory) {
    return llvm::wrap(llvm::unwrap(builder)->createFile(filename, directory));
  }

  DIBasicTypeRef DICreateBasicType(DIBuilderRef builder, const char* name, uint64_t sizeInBits, uint64_t alignment, unsigned encoding) {
    return llvm::wrap(llvm::unwrap(builder)->createBasicType(name, sizeInBits, alignment, encoding));
  }

  DIModuleRef DICreateModule(DIBuilderRef builder, DIScopeOpaqueRef scope,
                             const char* name, const char* configurationMacro,
                             const char* includePath, const char *iSysRoot) {
    return llvm::wrap(llvm::unwrap(builder)->createModule(llvm::unwrap(scope), name, configurationMacro, includePath, iSysRoot));
  }

  DISubprogramRef DICreateFunction(DIBuilderRef builder, DIScopeOpaqueRef scope,
                                   const char* name, const char *linkageName,
                                   DIFileRef file, unsigned lineNo,
                                   DISubroutineTypeRef type, int isLocal,
                                   int isDefinition, unsigned scopeLine) {
    return llvm::wrap(llvm::unwrap(builder)->createFunction(llvm::unwrap(scope),
                                                            name,
                                                            linkageName,
                                                            llvm::unwrap(file),
                                                            lineNo,
                                                            llvm::unwrap(type),
                                                            isLocal,
                                                            isDefinition,
                                                            scopeLine));
  }

  /* */
  DISubroutineTypeRef DICreateSubroutineType(DIBuilderRef builder,
                                             DITypeOpaqueRef* types,
                                             unsigned typesCount) {
    std::vector<llvm::Metadata *> parameterTypes;
    for (int i = 0; i != typesCount; ++i) {
      parameterTypes.push_back(llvm::unwrap(types[i]));
    }
    llvm::DIBuilder *b = llvm::unwrap(builder);
    llvm::DITypeRefArray typeArray = b->getOrCreateTypeArray(parameterTypes);
    return llvm::wrap(b->createSubroutineType(typeArray));
  }

  void DIFunctionAddSubprogram(LLVMValueRef fn, DISubprogramRef sp) {
    auto f = llvm::cast<llvm::Function>(llvm::unwrap(fn));
    auto dsp = llvm::cast<llvm::DISubprogram>(llvm::unwrap(sp));
    f->setSubprogram(dsp);
    if (!dsp->describes(f)) {
      fprintf(stderr, "error!!! f:%s, sp:%s\n", f->getName(), dsp->getLinkageName()); 
    }
  }

  DILocalVariableRef DICreateAutoVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, DIFileRef file, unsigned line, DITypeOpaqueRef type) {
    return llvm::wrap(llvm::unwrap(builder)->createAutoVariable(
      llvm::unwrap(scope),
      name,
      llvm::unwrap(file),
      line,
      llvm::unwrap(type)));
  }

  DIExpressionRef DICreateEmptyExpression(DIBuilderRef builder) {
    return llvm::wrap(llvm::unwrap(builder)->createExpression());
  }

  void DIInsertDeclarationWithEmptyExpression(DIBuilderRef builder, LLVMValueRef value, DILocalVariableRef localVariable, DILocationRef location, LLVMBasicBlockRef bb) {
    auto di_builder = llvm::unwrap(builder);
    di_builder->insertDeclare(llvm::unwrap(value),
                              llvm::unwrap(localVariable),
                              di_builder->createExpression(),
                              llvm::unwrap(location),
                              llvm::unwrap(bb));
  }
  
  DILocationRef LLVMBuilderSetDebugLocation(LLVMBuilderRef builder, unsigned line,
                                   unsigned col, DIScopeOpaqueRef scope) {
    auto sp = llvm::unwrap(scope);
    auto llvmBuilder = llvm::unwrap(builder);
    auto location = llvm::DILocation::get(llvmBuilder->getContext(), line, col, sp, nullptr);
    llvmBuilder->SetCurrentDebugLocation(location);
    return llvm::wrap(location);
  }

  void LLVMBuilderResetDebugLocation(LLVMBuilderRef builder) {
    llvm::unwrap(builder)->SetCurrentDebugLocation(nullptr);
  }

  LLVMValueRef LLVMBuilderGetCurrentFunction(LLVMBuilderRef builder) {
    return llvm::wrap(llvm::unwrap(builder)->GetInsertBlock()->getParent());
  } 

  const char* LLVMBuilderGetCurrentBbName(LLVMBuilderRef builder) {
    return llvm::unwrap(builder)->GetInsertBlock()->getName().str().c_str();
  } 

  
  const char *DIGetSubprogramLinkName(DISubprogramRef sp) {
    return llvm::unwrap(sp)->getLinkageName().str().c_str();
  }

  int DISubprogramDescribesFunction(DISubprogramRef sp, LLVMValueRef fn) {
    return llvm::unwrap(sp)->describes(llvm::cast<llvm::Function>(llvm::unwrap(fn)));
  }

  void DIScopeDump(DIScopeOpaqueRef scope) {
    llvm::unwrap(scope)->dump();
  }
}


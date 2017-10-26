/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DICompositeType,  DICompositeTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIType,           DITypeOpaqueRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIDerivedType,    DIDerivedTypeRef)
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

/**
 * see [DIFlags::FlagFwdDecl].
 */
#define DI_FORWARD_DECLARAION (4)
extern "C" {

DIBuilderRef DICreateBuilder(LLVMModuleRef module) {
  return llvm::wrap(new llvm::DIBuilder(* llvm::unwrap(module)));
}

void DIFinalize(DIBuilderRef builder) {
  auto diBuilder = llvm::unwrap(builder);
  diBuilder->finalize();
  delete diBuilder;
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

DICompositeTypeRef DICreateStructType(DIBuilderRef refBuilder,
                                      DIScopeOpaqueRef scope, const char *name,
                                      DIFileRef file, unsigned lineNumber,
                                      uint64_t sizeInBits, uint64_t alignInBits,
                                      unsigned flags, DITypeOpaqueRef derivedFrom,
                                      DIDerivedTypeRef *elements,
                                      uint64_t elementsCount,
                                      DICompositeTypeRef refPlace) {
  auto builder = llvm::unwrap(refBuilder);
  if ((flags & DI_FORWARD_DECLARAION) != 0) {
    return llvm::wrap(builder->createStructType(llvm::unwrap(scope), name, NULL, 0, 0, 0, flags, NULL, NULL));
  }
  std::vector<llvm::Metadata *> typeElements;
  for(int i = 0; i < elementsCount; ++i) {
    typeElements.push_back(llvm::unwrap(elements[i]));
  }
  auto elementsArray = builder->getOrCreateArray(typeElements);
  auto composite = builder->createStructType(llvm::unwrap(scope),
                                              name, llvm::unwrap(file),
                                              lineNumber,
                                              sizeInBits, alignInBits, flags,
                                              llvm::unwrap(derivedFrom),
                                              elementsArray);
  builder->replaceTemporary(llvm::TempDIType(llvm::unwrap(refPlace)), composite);
  return llvm::wrap(composite);
}


DICompositeTypeRef DICreateArrayType(DIBuilderRef refBuilder,
                                      uint64_t size, uint64_t alignInBits,
                                      DITypeOpaqueRef refType,
                                     uint64_t elementsCount) {
  auto builder = llvm::unwrap(refBuilder);
  auto range = std::vector<llvm::Metadata*>({llvm::dyn_cast<llvm::Metadata>(builder->getOrCreateSubrange(0, size))});
  return llvm::wrap(builder->createArrayType(size, alignInBits, llvm::unwrap(refType),
                                             builder->getOrCreateArray(range)));
}


DIDerivedTypeRef DICreateMemberType(DIBuilderRef refBuilder,
                                    DIScopeOpaqueRef refScope,
                                    const char *name,
                                    DIFileRef file,
                                    unsigned lineNum,
                                    uint64_t sizeInBits,
                                    uint64_t alignInBits,
                                    uint64_t offsetInBits,
                                    unsigned flags,
                                    DITypeOpaqueRef type) {
  return llvm::wrap(llvm::unwrap(refBuilder)->createMemberType(
                      llvm::unwrap(refScope),
                      name,
                      llvm::unwrap(file),
                      lineNum,
                      sizeInBits,
                      alignInBits,
                      offsetInBits,
                      flags,
                      llvm::unwrap(type)));
}

DICompositeTypeRef DICreateReplaceableCompositeType(DIBuilderRef refBuilder,
                                                    int tag,
                                                    const char *name,
                                                    DIScopeOpaqueRef refScope,
                                                    DIFileRef refFile,
                                                    unsigned line) {
  return llvm::wrap(llvm::unwrap(refBuilder)->createReplaceableCompositeType(
                      tag, name, llvm::unwrap(refScope), llvm::unwrap(refFile), line));
}

DIDerivedTypeRef DICreateReferenceType(DIBuilderRef refBuilder, DITypeOpaqueRef refType) {
  return llvm::wrap(llvm::unwrap(refBuilder)->createReferenceType(
                      llvm::dwarf::DW_TAG_reference_type,
                      llvm::unwrap(refType)));
}

DIDerivedTypeRef DICreatePointerType(DIBuilderRef refBuilder, DITypeOpaqueRef refType) {
  return llvm::wrap(llvm::unwrap(refBuilder)->createReferenceType(
                      llvm::dwarf::DW_TAG_pointer_type,
                      llvm::unwrap(refType)));
}

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

DILocalVariableRef DICreateParameterVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, unsigned argNo, DIFileRef file, unsigned line, DITypeOpaqueRef type) {
  return llvm::wrap(llvm::unwrap(builder)->createParameterVariable(
    llvm::unwrap(scope),
    name,
    argNo,
    llvm::unwrap(file),
    line,
    llvm::unwrap(type)));
}

DIExpressionRef DICreateEmptyExpression(DIBuilderRef builder) {
  return llvm::wrap(llvm::unwrap(builder)->createExpression());
}

void DIInsertDeclaration(DIBuilderRef builder, LLVMValueRef value, DILocalVariableRef localVariable, DILocationRef location, LLVMBasicBlockRef bb, int64_t *expr, uint64_t exprCount) {
  auto di_builder = llvm::unwrap(builder);
  std::vector<int64_t> expression;
  for (uint64_t i = 0; i < exprCount; ++i)
    expression.push_back(expr[i]);
  di_builder->insertDeclare(llvm::unwrap(value),
                            llvm::unwrap(localVariable),
                            di_builder->createExpression(expression),
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
} /* extern "C" */


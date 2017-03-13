#ifndef __DEBUG_INFO_C_H__
# define __DEBUG_INFO_C_H__
#include <llvm-c/Core.h>
# ifdef __cplusplus
extern "C" {
# endif
  typedef struct DIBuilder          *DIBuilderRef;
  typedef struct DICompileUnit      *DICompileUnitRef;
  typedef struct DIFile             *DIFileRef;
  typedef struct DIBasicType        *DIBasicTypeRef;
  typedef struct DIType             *DITypeOpaqueRef;
  typedef struct DISubprogram       *DISubprogramRef;
  typedef struct DIModule           *DIModuleRef;
  typedef struct DIScope            *DIScopeOpaqueRef;
  typedef struct DISubroutineType   *DISubroutineTypeRef;
  typedef struct DISubprogram       *DISubprogramRef;
  typedef struct DILocation         *DILocationRef;
  typedef struct DILocalVariable    *DILocalVariableRef;
  typedef struct DIExpression       *DIExpressionRef;

  DIBuilderRef DICreateBuilder(LLVMModuleRef module);
  void DIFinalize(DIBuilderRef builder);

  DICompileUnitRef DICreateCompilationUnit(DIBuilderRef builder, unsigned int lang, const char *File, const char* dir, const char * producer, int isOptimized, const char * flags, unsigned int rv);

  DIFileRef DICreateFile(DIBuilderRef builder, const char *filename, const char *directory);

  DIBasicTypeRef DICreateBasicType(DIBuilderRef builder, const char* name, uint64_t sizeInBits, uint64_t alignment, unsigned encoding);

  DIModuleRef DICreateModule(DIBuilderRef builder, DIScopeOpaqueRef scope,
                             const char* name, const char* configurationMacro,
                             const char* includePath, const char *iSysRoot);
  
  DISubprogramRef DICreateFunction(DIBuilderRef builder, DIScopeOpaqueRef scope,
                                   const char* name, const char *linkageName,
                                   DIFileRef file, unsigned lineNo,
                                   DISubroutineTypeRef type, int isLocal,
                                   int isDefinition, unsigned scopeLine);
  
  DISubroutineTypeRef DICreateSubroutineType(DIBuilderRef builder,
                                             DITypeOpaqueRef* types,
                                             unsigned typesCount);

  DILocalVariableRef DICreateAutoVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, DIFileRef file, unsigned line, DITypeOpaqueRef type);
  void DIInsertDeclarationWithEmptyExpression(DIBuilderRef builder, LLVMValueRef value, DILocalVariableRef localVariable, DILocationRef location, LLVMBasicBlockRef bb);
  DIExpressionRef DICreateEmptyExpression(DIBuilderRef builder);
  void DIFunctionAddSubprogram(LLVMValueRef fn, DISubprogramRef sp);
  DILocationRef LLVMBuilderSetDebugLocation(LLVMBuilderRef builder, unsigned line, unsigned col, DIScopeOpaqueRef scope);
  void LLVMBuilderResetDebugLocation(LLVMBuilderRef builder);
  const char* LLVMBuilderGetCurrentBbName(LLVMBuilderRef builder);
  const char *DIGetSubprogramLinkName(DISubprogramRef sp);
  LLVMValueRef LLVMBuilderGetCurrentFunction(LLVMBuilderRef builder);
  int DISubprogramDescribesFunction(DISubprogramRef sp, LLVMValueRef fn);
  void DIScopeDump(DIScopeOpaqueRef scope);
# ifdef __cplusplus
}
# endif
#endif

#ifndef RUNTIME_TYPEINFO_H
#define RUNTIME_TYPEINFO_H

#include <cstdint>

#include "Names.h"

// An element of sorted by hash in-place array representing methods.
// For systems where introspection is not needed - only open methods are in
// this table.
struct MethodTableRecord {
    MethodNameHash nameSignature_;
    void* methodEntryPoint_;
};

// An element of sorted by hash in-place array representing field offsets.
struct FieldTableRecord {
    FieldNameHash nameSignature_;
    int fieldOffset_;
};

// This struct represents runtime type information and by itself is the compile time
// constant.
struct TypeInfo {
    ClassNameHash name_;
    // Negative value marks array class/string, and it is negated element size.
    int32_t instanceSize_;
    // Must be pointer to Any for array classes, and null for Any.
    const TypeInfo* superType_;
    const int* objOffsets_;
    int objOffsetsCount_;
    TypeInfo* const* implementedInterfaces_;
    int implementedInterfacesCount_;
    void* const* vtable_; // TODO: place vtable at the end of TypeInfo to eliminate the indirection
    const MethodTableRecord* openMethods_;
    uint32_t openMethodsCount_;
    const FieldTableRecord* fields_;
    uint32_t fieldsCount_;
};

#ifdef __cplusplus
extern "C" {
#endif
// Find offset of given hash in table.
int LookupFieldOffset(const TypeInfo* type_info, FieldNameHash hash);

// Find open method by its hash. Other methods are resolved in compile-time.
void* LookupOpenMethod(const TypeInfo* info, MethodNameHash nameSignature);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_TYPEINFO_H

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

#ifndef RUNTIME_TYPEINFO_H
#define RUNTIME_TYPEINFO_H

#include <cstdint>

#include "Common.h"
#include "Names.h"

#if KONAN_TYPE_INFO_HAS_WRITABLE_PART
struct WritableTypeInfo;
#endif

struct ObjHeader;

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

// Type for runtime representation of Konan object.
// Keep in sync with runtimeTypeMap in RTTIGenerator.
enum Konan_RuntimeType {
  RT_INVALID    = 0,
  RT_OBJECT     = 1,
  RT_INT8       = 2,
  RT_INT16      = 3,
  RT_INT32      = 4,
  RT_INT64      = 5,
  RT_FLOAT32    = 6,
  RT_FLOAT64    = 7,
  RT_NATIVE_PTR = 8,
  RT_BOOLEAN    = 9
};

enum Konan_TypeFlags {
  TF_IMMUTABLE = 1 << 0,
  TF_ACYCLIC   = 1 << 1
};

enum Konan_MetaFlags {
  MF_NEVER_FROZEN = 1 << 0
};

// Extended information about a type.
struct ExtendedTypeInfo {
  // Number of fields (negated Konan_RuntimeType for array types).
  int32_t fieldsCount_;
  // Offsets of all fields.
  const int32_t* fieldOffsets_;
  // Types of all fields.
  const uint8_t* fieldTypes_;
  // Names of all fields.
  const char** fieldNames_;
  // TODO: do we want any other info here?
};

// This struct represents runtime type information and by itself is the compile time
// constant.
struct TypeInfo {
    // Reference to self, to allow simple obtaining TypeInfo via meta-object.
    const TypeInfo* typeInfo_;
    // Hash of class name.
    ClassNameHash name_;
    // Negative value marks array class/string, and it is negated element size.
    int32_t instanceSize_;
    // Must be pointer to Any for array classes, and null for Any.
    const TypeInfo* superType_;
    // All object reference fields inside this object.
    const int32_t* objOffsets_;
    // Count of object reference fields inside this object.
    // 1 for kotlin.Array to mark it as non-leaf.
    int32_t objOffsetsCount_;
    const TypeInfo* const* implementedInterfaces_;
    int32_t implementedInterfacesCount_;
    // Null for abstract classes and interfaces.
    const MethodTableRecord* openMethods_;
    uint32_t openMethodsCount_;
    const FieldTableRecord* fields_;
    // Is negative to mark an interface.
    int32_t fieldsCount_;

    // String for the fully qualified dot-separated name of the package containing class,
    // or `null` if the class is local or anonymous.
    ObjHeader* packageName_;

    // String for the qualified class name relative to the containing package
    // (e.g. TopLevel.Nested1.Nested2), or simple class name if it is local,
    // or `null` if the class is anonymous.
    ObjHeader* relativeName_;

    // Various flags.
    int32_t flags_;

    // Extended RTTI.
    const ExtendedTypeInfo* extendedInfo_;

#if KONAN_TYPE_INFO_HAS_WRITABLE_PART
    WritableTypeInfo* writableInfo_;
#endif

    // vtable starts just after declared contents of the TypeInfo:
    // void* const vtable_[];
#ifdef __cplusplus
    inline const void* const * vtable() const {
      return reinterpret_cast<void * const *>(this + 1);
    }

    inline const void** vtable() {
      return reinterpret_cast<const void**>(this + 1);
    }
#endif
};

#ifdef __cplusplus
extern "C" {
#endif
// Find offset of given hash in table.
// Note, that we use attribute const, which assumes function doesn't
// dereference global memory, while this function does. However, it seems
// to be safe, as actual result of this computation depends only on 'type_info'
// and 'hash' numeric values and doesn't really depends on global memory state
// (as TypeInfo is compile time constant and type info pointers are stable).
int LookupFieldOffset(const TypeInfo* type_info, FieldNameHash hash) RUNTIME_CONST;

// Find open method by its hash. Other methods are resolved in compile-time.
// See comment in LookupFieldOffset().
void* LookupOpenMethod(const TypeInfo* info, MethodNameHash nameSignature) RUNTIME_CONST;

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_TYPEINFO_H

// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// Defines the type used for repeated fields.

#ifndef __ComGoogleProtobufRepeatedField_H__
#define __ComGoogleProtobufRepeatedField_H__

#import "JreEmulation.h"

#import "com/google/protobuf/Descriptors.h"

@protocol JavaUtilList;

// For compactness of empty repeated fields, the storage type of a repeated
// field (CGPRepeatedField) is a single pointer. When the repeated field becomes
// non-empty, we allocate CGPRepeatedFieldData, which contains the size, the
// allocated size and a buffer. This block is re-allocated as the field grows.
typedef struct CGPRepeatedFieldData {
  uint32_t size;
  uint32_t total_size;
  _Atomic(uint32_t) ref_count;
  void *buffer;
} CGPRepeatedFieldData;

typedef struct CGPRepeatedField {
  CGPRepeatedFieldData *data;
} CGPRepeatedField;

CF_EXTERN_C_BEGIN

CGP_ALWAYS_INLINE inline uint32_t CGPRepeatedFieldSize(CGPRepeatedField *field) {
  return field->data != NULL ? field->data->size : 0;
}

CGP_ALWAYS_INLINE inline uint32_t CGPRepeatedFieldTotalSize(CGPRepeatedField *field) {
  return field->data != NULL ? field->data->total_size : 0;
}

void CGPRepeatedFieldReserve(CGPRepeatedField *field, uint32_t new_size, size_t elemSize);

CGP_ALWAYS_INLINE inline void CGPRepeatedFieldReserveAdditionalCapacity(
    CGPRepeatedField *field, uint32_t size_to_add, size_t elemSize) {
  uint32_t new_size = CGPRepeatedFieldSize(field) + size_to_add;
  if (new_size > CGPRepeatedFieldTotalSize(field)) {
    CGPRepeatedFieldReserve(field, new_size, elemSize);
  }
}

void CGPRepeatedFieldCopyData(CGPRepeatedField *field, CGPFieldJavaType type);

void CGPRepeatedFieldAppendOther(
    CGPRepeatedField *field, CGPRepeatedField *other, CGPFieldJavaType type);

void CGPRepeatedFieldClear(CGPRepeatedField *field, CGPFieldJavaType type);

void CGPRepeatedFieldOutOfBounds(jint idx, uint32_t size);

CGP_ALWAYS_INLINE inline void CGPRepeatedFieldCheckBounds(CGPRepeatedField *field, jint idx) {
  uint32_t size = CGPRepeatedFieldSize(field);
  if (idx < 0 || size <= (uint32_t)idx) {
    CGPRepeatedFieldOutOfBounds(idx, size);
  }
}

#define REPEATED_FIELD_GETTER_IMP(NAME) \
  CGP_ALWAYS_INLINE inline TYPE_##NAME CGPRepeatedFieldGet##NAME( \
      CGPRepeatedField *field, jint idx) { \
    CGPRepeatedFieldCheckBounds(field, idx); \
    return ((TYPE_##NAME *)field->data->buffer)[idx]; \
  }

FOR_EACH_TYPE_NO_ENUM(REPEATED_FIELD_GETTER_IMP)

#undef REPEATED_FIELD_GETTER_IMP

#define REPEATED_FIELD_ADDER_IMP(NAME) \
  CGP_ALWAYS_INLINE inline void CGPRepeatedFieldAdd##NAME( \
      CGPRepeatedField *field, TYPE_##NAME value) { \
    uint32_t total_size = CGPRepeatedFieldTotalSize(field); \
    if (CGPRepeatedFieldSize(field) == total_size) { \
      CGPRepeatedFieldReserve(field, total_size + 1, sizeof(TYPE_##NAME)); \
    } \
    ((TYPE_##NAME *)field->data->buffer)[field->data->size++] = TYPE_RETAIN_##NAME(value); \
  }

FOR_EACH_TYPE_WITH_ENUM(REPEATED_FIELD_ADDER_IMP)

#undef REPEATED_FIELD_ADDER_IMP

CGP_ALWAYS_INLINE inline void CGPRepeatedFieldAddRetainedId(CGPRepeatedField *field, id value) {
  uint32_t total_size = CGPRepeatedFieldTotalSize(field);
  if (CGPRepeatedFieldSize(field) == total_size) {
    CGPRepeatedFieldReserve(field, total_size + 1, sizeof(id));
  }
  ((id *)field->data->buffer)[field->data->size++] = value;
}

#define REPEATED_FIELD_SETTER_IMP(NAME) \
  CGP_ALWAYS_INLINE inline void CGPRepeatedFieldSet##NAME( \
      CGPRepeatedField *field, jint idx, TYPE_##NAME value) { \
    CGPRepeatedFieldCheckBounds(field, idx); \
    TYPE_##NAME *ptr = &((TYPE_##NAME *)field->data->buffer)[idx]; \
    TYPE_ASSIGN_##NAME(*ptr, value); \
  } \

FOR_EACH_TYPE_WITH_ENUM(REPEATED_FIELD_SETTER_IMP)

#undef REPEATED_FIELD_SETTER_IMP

id<JavaUtilList> CGPNewRepeatedFieldList(CGPRepeatedField *field, CGPFieldJavaType type);

id CGPRepeatedFieldGet(CGPRepeatedField *field, jint index, CGPFieldDescriptor *descriptor);

void CGPRepeatedFieldAdd(CGPRepeatedField *field, id value, CGPFieldJavaType type);

void CGPRepeatedFieldSet(CGPRepeatedField *field, jint index, id value, CGPFieldJavaType type);

void CGPRepeatedFieldAssignFromList(
    CGPRepeatedField *field, id<JavaUtilList> list, CGPFieldJavaType type);

void CGPRepeatedMessageFieldRemove(CGPRepeatedField *field, jint index);

id<JavaUtilList> CGPRepeatedFieldCopyList(CGPRepeatedField *field, CGPFieldDescriptor *descriptor);

BOOL CGPRepeatedFieldIsEqual(CGPRepeatedField *a, CGPRepeatedField *b, CGPFieldJavaType type);

CF_EXTERN_C_END

#endif // __ComGoogleProtobufRepeatedField_H__

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

// Defines the type used for map fields. This data structure emulates as close
// as possible the functionality of com.google.protobuf.MapField which can be
// viewed as either a map or a list. Like c.g.p.MapField, CGPMapField can be
// structured as an array or a linked hash map (or both) and at least one of the
// structures must be valid at all times. Unlike c.g.p.MapField, CGPMapField
// shares entries between its array and its map. This avoids additional
// allocations when converting from map to array or from array to map.

#ifndef __ComGoogleProtobufMapField_H__
#define __ComGoogleProtobufMapField_H__

#include "JreEmulation.h"

#include "Descriptors_PackagePrivate.h"

@protocol JavaUtilList;
@protocol JavaUtilMap;

// The hash map entry.
typedef struct CGPMapFieldEntry {
  struct CGPMapFieldEntry *hashNext;
  struct CGPMapFieldEntry *prev;
  struct CGPMapFieldEntry *next;
  CGPValue key;
  CGPValue value;
  uint32_t hash;
} CGPMapFieldEntry;

// For compactness of empty map fields, the storage type of a map field
// (CGPMapField) is a single pointer. When the map field becomes non-empty, we
// allocate CGPMapFieldData, which contains all the data necessary to manage the
// map field.
typedef struct CGPMapFieldData {
  CGPMapFieldEntry header;
  CGPMapFieldEntry **array;
  CGPMapFieldEntry **hashArray;
  uint32_t numEntries;
  uint32_t arrayCapacity;
//  uint32_t nextIndex;
  uint32_t hashCapacity;  // Always a power of 2.
  uint32_t modCount;
  _Atomic(uint32_t) refCount;
  bool validArray;
  bool validHashMap;
} CGPMapFieldData;

typedef struct CGPMapField {
  CGPMapFieldData *data;
} CGPMapField;

CF_EXTERN_C_BEGIN

CGP_ALWAYS_INLINE inline uint32_t CGPMapFieldIsEmpty(CGPMapField *field) {
  return field->data == NULL || field->data->numEntries == 0;
}

CGP_ALWAYS_INLINE inline uint32_t CGPMapFieldListSize(CGPMapField *field) {
  return field->data != NULL ? field->data->numEntries : 0;
}

void CGPMapFieldEnsureValidMap(
    CGPMapFieldData *data, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

CGP_ALWAYS_INLINE inline uint32_t CGPMapFieldMapSize(
    CGPMapField *field, CGPFieldJavaType keyType, CGPFieldJavaType valueType) {
  CGPMapFieldData *data = field->data;
  if (data == NULL) {
    return 0;
  }
  if (!data->validHashMap) {
    CGPMapFieldEnsureValidMap(data, keyType, valueType);
  }
  return data->numEntries;
}

CGPMapFieldEntry *CGPMapFieldGetWithKey(
    CGPMapField *field, CGPValue key, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

// The caller will indicate whether the key and value are already retained.
void CGPMapFieldPut(
    CGPMapField *field, CGPValue key, CGPFieldJavaType keyType, CGPValue value,
    CGPFieldJavaType valueType, bool retainedKeyAndValue);

void CGPMapFieldRemove(
    CGPMapField *field, CGPValue key, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

void CGPMapFieldCopyData(CGPMapField *field, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

void CGPMapFieldAppendOther(
    CGPMapField *field, CGPMapField *other, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

void CGPMapFieldClear(CGPMapField *field, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

bool CGPMapFieldIsEqual(
    CGPMapField *fieldA, CGPMapField *fieldB, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

int CGPMapFieldHash(CGPMapField *field, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

id<JavaUtilList> CGPMapFieldCopyList(
    CGPMapField *field, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

id CGPMapFieldGetAtIndex(CGPMapField *field, jint idx, CGPFieldDescriptor *descriptor);

void CGPMapFieldAdd(CGPMapField *field, id object, CGPFieldDescriptor *descriptor);

void CGPMapFieldSet(CGPMapField *field, jint idx, id object, CGPFieldDescriptor *descriptor);

void CGPMapFieldAssignFromList(
    CGPMapField *field, id<JavaUtilList> list, CGPFieldDescriptor *descriptor);

id<JavaUtilMap> CGPMapFieldAsJavaMap(
    CGPMapField *field, CGPFieldJavaType keyType, CGPFieldJavaType valueType);

CF_EXTERN_C_END

#endif // __ComGoogleProtobufMapField_H__

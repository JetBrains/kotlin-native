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

#ifndef RUNTIME_TYPES_H
#define RUNTIME_TYPES_H

#include "Common.h"
#include "Memory.h"
#include "TypeInfo.h"

// Note that almost all types are signed.
typedef uint8_t KBoolean;
typedef int8_t  KByte;
typedef uint16_t KChar;
typedef int16_t KShort;
typedef int32_t KInt;
typedef int64_t KLong;
typedef float   KFloat;
typedef double  KDouble;
typedef void*   KNativePtr;

typedef ObjHeader* KRef;
typedef const ObjHeader* KConstRef;
typedef const ArrayHeader* KString;

#ifdef __cplusplus
extern "C" {
#endif

extern const TypeInfo* theAnyTypeInfo;
extern const TypeInfo* theCloneableTypeInfo;
extern const TypeInfo* theArrayTypeInfo;
extern const TypeInfo* theByteArrayTypeInfo;
extern const TypeInfo* theCharArrayTypeInfo;
extern const TypeInfo* theShortArrayTypeInfo;
extern const TypeInfo* theIntArrayTypeInfo;
extern const TypeInfo* theLongArrayTypeInfo;
extern const TypeInfo* theFloatArrayTypeInfo;
extern const TypeInfo* theDoubleArrayTypeInfo;
extern const TypeInfo* theBooleanArrayTypeInfo;
extern const TypeInfo* theStringTypeInfo;
extern const TypeInfo* theThrowableTypeInfo;

KBoolean IsInstance(const ObjHeader* obj, const TypeInfo* type_info) RUNTIME_PURE;
void CheckCast(const ObjHeader* obj, const TypeInfo* type_info);
KBoolean IsArray(KConstRef obj) RUNTIME_PURE;

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_TYPES_H

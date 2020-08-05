// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//
//  IOSMetadata.h
//  JreEmulation
//
//  Created by Tom Ball on 9/23/13.
//

#ifndef JreEmulation_IOSMetadata_h
#define JreEmulation_IOSMetadata_h

#import <Foundation/Foundation.h>

#import "J2ObjC_types.h"

// Current metadata structure version
#define J2OBJC_METADATA_VERSION 7

// A raw value is the union of all possible native types.
typedef union {
  void *asId;
  char asChar;
  unichar asUnichar;
  short asShort;
  int asInt;
  long long asLong;
  float asFloat;
  double asDouble;
  jboolean asBOOL;
} J2ObjcRawValue;

// C data structures that hold "raw" metadata for use by the methods that
// implement Java reflection. This information is necessary because not
// all information provided by the reflection API is discoverable via the
// Objective-C runtime.

typedef int16_t ptr_idx;

typedef struct J2ObjcMethodInfo {
  SEL selector;
  const char *returnType;
  uint16_t modifiers;
  ptr_idx javaNameIdx;
  ptr_idx paramsIdx;
  ptr_idx exceptionsIdx;
  ptr_idx genericSignatureIdx;
  ptr_idx annotationsIdx;
  ptr_idx paramAnnotationsIdx;
} J2ObjcMethodInfo;

typedef struct J2ObjcFieldInfo {
  const char *name;
  const char *type;
  J2ObjcRawValue constantValue;
  uint16_t modifiers;
  ptr_idx javaNameIdx;
  ptr_idx staticRefIdx;
  ptr_idx genericSignatureIdx;
  ptr_idx annotationsIdx;
} J2ObjcFieldInfo;

typedef struct J2ObjcClassInfo {
  const char *typeName;
  const char *packageName;
  const void **ptrTable;
  const J2ObjcMethodInfo *methods;
  const J2ObjcFieldInfo *fields;
  // Pointer types are above version for better packing.
  const uint16_t version;
  uint16_t modifiers;
  uint16_t methodCount;
  uint16_t fieldCount;
  ptr_idx enclosingClassIdx;
  ptr_idx innerClassesIdx;
  ptr_idx enclosingMethodIdx;
  ptr_idx genericSignatureIdx;
  ptr_idx annotationsIdx;
} J2ObjcClassInfo;

#endif  // JreEmulation_IOSMetadata_h

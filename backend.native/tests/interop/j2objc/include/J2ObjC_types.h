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

// Common definitions needed by J2ObjC and JNI.

#ifndef _J2OBJC_TYPES_H_
#define _J2OBJC_TYPES_H_

#ifdef __OBJC__
#import <Foundation/Foundation.h>
#else
#include <stdint.h>
#endif

// Typedefs for each of Java's primitive types. (as originally defined in jni.h)
// jboolean and jbyte are modified from the original jni.h to integrate better
// with Objective-C code.
typedef int8_t          jbyte;          /* signed 8 bits */
typedef uint16_t        jchar;          /* unsigned 16 bits */
typedef int16_t         jshort;         /* signed 16 bits */
typedef int32_t         jint;           /* signed 32 bits */
typedef int64_t         jlong;          /* signed 64 bits */
typedef float           jfloat;         /* 32-bit IEEE 754 */
typedef double          jdouble;        /* 64-bit IEEE 754 */

#if defined(__OBJC__) || defined(__cplusplus__)
typedef bool            jboolean;
#else
typedef uint8_t         jboolean;
#endif

// Typedefs for Java types declared as volatile.
typedef _Atomic(jbyte)     volatile_jbyte;
typedef _Atomic(jchar)     volatile_jchar;
typedef _Atomic(jshort)    volatile_jshort;
typedef _Atomic(jint)      volatile_jint;
typedef _Atomic(jlong)     volatile_jlong;
typedef _Atomic(jfloat)    volatile_jfloat;
typedef _Atomic(jdouble)   volatile_jdouble;
typedef _Atomic(jboolean)  volatile_jboolean;
// Volatile object access is guarded by spin locks because of reference counting
// so we don't use an atomic type. uintptr_t is used for the typedef mainly to
// prevent accidental usage as a regular id type.
typedef uintptr_t          volatile_id;

#endif // _J2OBJC_TYPES_H_

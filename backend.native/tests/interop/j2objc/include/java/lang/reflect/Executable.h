// Copyright 2011 Google Inc. All Rights Reserved.
//
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
//  Executable.h
//  JreEmulation
//
//  Created by Tom Ball on 11/11/11.
//

#ifndef _JavaLangReflectExecutable_H_
#define _JavaLangReflectExecutable_H_

#include "IOSMetadata.h"
#include "java/lang/reflect/AccessibleObject.h"
#include "java/lang/reflect/GenericDeclaration.h"
#include "java/lang/reflect/Member.h"

// The first arguments all messages have are self and _cmd.
// These are unmodified when specifying method-specific arguments.
#define SKIPPED_ARGUMENTS 2

@class IOSClass;
@class IOSObjectArray;
@protocol JavaLangReflectAnnotatedType;
@protocol JavaLangAnnotationAnnotation;

// Common parent of Member and Constructor with their shared functionality.
// This class isn't directly called from translated Java, since Java's
// Method and Constructor classes just duplicate their common code.
@interface JavaLangReflectExecutable : JavaLangReflectAccessibleObject
    < JavaLangReflectGenericDeclaration, JavaLangReflectMember > {
 @protected
  IOSClass *class_;
  const J2ObjcMethodInfo *metadata_;
  const void **ptrTable_;
}

- (instancetype)initWithDeclaringClass:(IOSClass *)aClass
                              metadata:(const J2ObjcMethodInfo *)metadata;

- (NSString *)getName;

// Returns the set of modifier flags, as defined by java.lang.reflect.Modifier.
- (jint)getModifiers;

// Returns the class this executable is a member of.
- (IOSClass *)getDeclaringClass;

// Returns the types of any declared exceptions.
- (IOSObjectArray *)getExceptionTypes;
- (IOSObjectArray *)getGenericExceptionTypes;

// Returns the parameter types for this executable member.
- (jint)getParameterCount;
- (IOSObjectArray *)getParameters;
- (IOSObjectArray *)getParameterTypes;
- (IOSObjectArray *)getGenericParameterTypes;
- (IOSObjectArray *)getTypeParameters;

// Returns true if this method has variable arguments.
- (jboolean)isVarArgs;

// Returns true if this method was added by j2objc.
- (jboolean)isSynthetic;

// Annotation accessors.
- (IOSObjectArray *)getAnnotationsByTypeWithIOSClass:(IOSClass *)cls;
- (IOSObjectArray *)getDeclaredAnnotations;
- (IOSObjectArray *)getParameterAnnotations;

// Empty implementations.
- (id<JavaLangReflectAnnotatedType>)getAnnotatedReturnType;
- (IOSObjectArray *)getAnnotatedParameterTypes;

- (NSString *)toGenericString;
- (IOSObjectArray *)getAllGenericParameterTypes;

// Internal methods.
- (IOSObjectArray *)getParameterTypesInternal;
- (SEL)getSelector;
- (jboolean)hasRealParameterData;

@end

#endif // _JavaLangReflectExecutable_H_

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
//  Constructor.h
//  JreEmulation
//
//  Created by Tom Ball on 11/11/11.
//

#ifndef _JAVA_LANG_REFLECT_CONSTRUCTOR_H
#define _JAVA_LANG_REFLECT_CONSTRUCTOR_H

#import "IOSMetadata.h"
#import "J2ObjC_common.h"
#import "java/lang/reflect/Executable.h"
#import "java/lang/reflect/GenericDeclaration.h"
#import "java/lang/reflect/Member.h"

// A native implementation of java.lang.reflect.Constructor.  Its methods are
// limited to those that can be derived from an Objective-C Method instance,
// so instances can be created and released as needed.
@interface JavaLangReflectConstructor : JavaLangReflectExecutable

+ (instancetype)constructorWithDeclaringClass:(IOSClass *)aClass
                                     metadata:(const J2ObjcMethodInfo *)metadata;

// Create a new instance using this constructor.
- (id)newInstanceWithNSObjectArray:(IOSObjectArray *)initArgs OBJC_METHOD_FAMILY_NONE;

// Faster version of Constructor.newInstance() for JNI code. This does not
// require boxing arguments.
- (id)jniNewInstance:(const J2ObjcRawValue *)args;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaLangReflectConstructor)

J2OBJC_TYPE_LITERAL_HEADER(JavaLangReflectConstructor)

#endif // _JAVA_LANG_REFLECT_CONSTRUCTOR_H

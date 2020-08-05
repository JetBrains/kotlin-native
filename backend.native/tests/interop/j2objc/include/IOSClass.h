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
//  IOSClass.h
//  JreEmulation
//
//  Created by Tom Ball on 10/18/11.
//

#ifndef _IOSClass_H_
#define _IOSClass_H_

#import "IOSMetadata.h"
#import "J2ObjC_common.h"
#import "java/io/Serializable.h"
#import "java/lang/reflect/AnnotatedElement.h"
#import "java/lang/reflect/GenericDeclaration.h"
#import "java/lang/reflect/Type.h"

@class IOSObjectArray;
@class JavaLangClassLoader;
@class JavaLangReflectConstructor;
@class JavaLangReflectField;
@class JavaLangReflectMethod;
@class JavaIoInputStream;
@class JavaNetURL;
@protocol JavaLangAnnotationAnnotation;

/**
 * The representation of a Java class, which serves as the starting
 * point for querying class-related information, a process usually
 * called "reflection". There are basically three types of @c Class
 * instances: those representing real classes and interfaces, those
 * representing primitive types, and those representing array classes.
 */
@interface IOSClass : NSObject <JavaLangReflectAnnotatedElement,
    JavaLangReflectGenericDeclaration, JavaIoSerializable,
    JavaLangReflectType, NSCopying> {
}

@property (readonly) Class objcClass;
@property (readonly) Protocol *objcProtocol;

- (instancetype)initWithMetadata:(const J2ObjcClassInfo *)metadata;

// IOSClass Getters.
+ (IOSClass *)classForIosName:(NSString *)iosName;
+ (IOSClass *)primitiveClassForChar:(unichar)c;

// Primitive class instance getters.
+ (IOSClass *)byteClass;
+ (IOSClass *)charClass;
+ (IOSClass *)doubleClass;
+ (IOSClass *)floatClass;
+ (IOSClass *)intClass;
+ (IOSClass *)longClass;
+ (IOSClass *)shortClass;
+ (IOSClass *)booleanClass;
+ (IOSClass *)voidClass;

// Class.newInstance()
- (id)newInstance NS_RETURNS_NOT_RETAINED;

// Class.getSuperclass()
- (IOSClass *)getSuperclass;
- (id<JavaLangReflectType>)getGenericSuperclass;

// Class.getDeclaringClass()
- (IOSClass *)getDeclaringClass;

// Class.isInstance(Object)
- (jboolean)isInstance:(id)object;

// These methods all return the same class name.
- (NSString *)getName;
- (NSString *)getSimpleName;
- (NSString *)getCanonicalName;

// Class.getModifiers()
- (jint)getModifiers;

// Class.getDeclaredConstructors()
- (IOSObjectArray *)getDeclaredConstructors;

// Class.getDeclaredMethods()
- (IOSObjectArray *)getDeclaredMethods;

// Class.getMethod(String, Class...)
- (JavaLangReflectMethod *)getMethod:(NSString *)name
                      parameterTypes:(IOSObjectArray *)types;

// Class.getDeclaredMethod(String, Class...)
- (JavaLangReflectMethod *)getDeclaredMethod:(NSString *)name
                              parameterTypes:(IOSObjectArray *)types;

// Class.getDeclaredConstructor(Class...)
- (JavaLangReflectConstructor *)getDeclaredConstructor:(IOSObjectArray *)types;

// Class.getConstructor(Class)
- (JavaLangReflectConstructor *)getConstructor:(IOSObjectArray *)types;

// Class.getConstructors()
- (IOSObjectArray *)getConstructors;

// Class.getMethods()
- (IOSObjectArray *)getMethods;

// Class.isAssignableFrom(Class)
- (jboolean) isAssignableFrom:(IOSClass *)cls;

// Class.asSubclass(Class)
- (IOSClass *)asSubclass:(IOSClass *)cls;

// Class.getComponentType()
- (IOSClass *)getComponentType;

// Class.forName
+ (IOSClass *)forName:(NSString *)className;
+ (IOSClass *)forName:(NSString *)className
           initialize:(jboolean)load
          classLoader:(JavaLangClassLoader *)loader;

// Class.cast(Object)
- (id)cast:(id)throwable;

// Class.getEnclosingClass()
- (IOSClass *)getEnclosingClass;

// Class.isMemberClass
- (jboolean)isMemberClass;
- (jboolean)isLocalClass;

- (jboolean)isArray;
- (jboolean)isEnum;
- (jboolean)isInterface;
- (jboolean)isPrimitive;
- (jboolean)isAnnotation;
- (jboolean)isSynthetic;

- (IOSObjectArray *)getInterfaces;
- (IOSObjectArray *)getGenericInterfaces;
- (IOSObjectArray *)getTypeParameters;

- (id<JavaLangAnnotationAnnotation>)
      getAnnotationWithIOSClass:(IOSClass *)annotationClass;
- (jboolean)isAnnotationPresentWithIOSClass:(IOSClass *)annotationType;
- (IOSObjectArray *)getAnnotations;
- (IOSObjectArray *)getDeclaredAnnotations;
- (id<JavaLangAnnotationAnnotation>)
      getDeclaredAnnotationWithIOSClass:(IOSClass *)annotationClass;
- (id)getPackage;
- (id)getClassLoader;

- (JavaLangReflectField *)getDeclaredField:(NSString *)name;
- (IOSObjectArray *)getDeclaredFields;
- (JavaLangReflectField *)getField:(NSString *)name;
- (IOSObjectArray *)getFields;

- (JavaLangReflectConstructor *)getEnclosingConstructor;
- (JavaLangReflectMethod *)getEnclosingMethod;
- (jboolean)isAnonymousClass;

- (jboolean)desiredAssertionStatus;

- (IOSObjectArray *)getEnumConstants;

// Class.getResource, getResourceAsStream
- (JavaNetURL *)getResource:(NSString *)name;
- (JavaIoInputStream *)getResourceAsStream:(NSString *)name;

- (IOSObjectArray *)getClasses;
- (IOSObjectArray *)getDeclaredClasses;

- (id)getProtectionDomain;
- (id)getSigners;

- (NSString *)toGenericString;

// Boxing and unboxing (internal)
- (id)__boxValue:(J2ObjcRawValue *)rawValue;
- (jboolean)__unboxValue:(id)value toRawValue:(J2ObjcRawValue *)rawValue;
- (void)__readRawValue:(J2ObjcRawValue *)rawValue fromAddress:(const void *)addr;
- (void)__writeRawValue:(J2ObjcRawValue *)rawValue toAddress:(const void *)addr;
- (jboolean)__convertRawValue:(J2ObjcRawValue *)rawValue toType:(IOSClass *)type;

// Internal methods
- (JavaLangReflectMethod *)getMethodWithSelector:(const char *)selector;
// Same as getInterfaces, but not a defensive copy.
- (IOSObjectArray *)getInterfacesInternal;
- (const J2ObjcClassInfo *)getMetadata;
- (NSString *)objcName;
- (NSString *)binaryName;
- (void)appendMetadataName:(NSMutableString *)str;
// Get the IOSArray subclass that would be used to hold this type.
- (Class)objcArrayClass;
- (size_t)getSizeof;
- (IOSObjectArray *)getEnumConstantsShared;

@end

CF_EXTERN_C_BEGIN

// Class.forName(String)
IOSClass *IOSClass_forName_(NSString *className);
// Class.forName(String, boolean, ClassLoader)
IOSClass *IOSClass_forName_initialize_classLoader_(
    NSString *className, jboolean load, JavaLangClassLoader *loader);

// Lookup a IOSClass from its associated ObjC class, protocol or component type.
IOSClass *IOSClass_fromClass(Class cls);
IOSClass *IOSClass_fromProtocol(Protocol *protocol);
IOSClass *IOSClass_arrayOf(IOSClass *componentType);
// Same as "arrayOf" but allows dimensions to be specified.
IOSClass *IOSClass_arrayType(IOSClass *componentType, jint dimensions);

// Primitive array type literals.
#define IOSClass_byteArray(DIM) IOSClass_arrayType([IOSClass byteClass], DIM)
#define IOSClass_charArray(DIM) IOSClass_arrayType([IOSClass charClass], DIM)
#define IOSClass_doubleArray(DIM) IOSClass_arrayType([IOSClass doubleClass], DIM)
#define IOSClass_floatArray(DIM) IOSClass_arrayType([IOSClass floatClass], DIM)
#define IOSClass_intArray(DIM) IOSClass_arrayType([IOSClass intClass], DIM)
#define IOSClass_longArray(DIM) IOSClass_arrayType([IOSClass longClass], DIM)
#define IOSClass_shortArray(DIM) IOSClass_arrayType([IOSClass shortClass], DIM)
#define IOSClass_booleanArray(DIM) IOSClass_arrayType([IOSClass booleanClass], DIM)

// Internal functions
const J2ObjcClassInfo *IOSClass_GetMetadataOrFail(IOSClass *iosClass);
IOSClass *IOSClass_NewProxyClass(Class cls);

// Return value is retained
IOSObjectArray *IOSClass_NewInterfacesFromProtocolList(
    Protocol **list, unsigned int count, bool excludeNSCopying);

CF_EXTERN_C_END

J2OBJC_STATIC_INIT(IOSClass)

J2OBJC_TYPE_LITERAL_HEADER(IOSClass)

#endif // _IOSClass_H_

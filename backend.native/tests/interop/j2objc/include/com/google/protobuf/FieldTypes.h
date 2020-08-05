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

//  Declares the FieldDescriptor.Type and FieldDescriptor.JavaType enums.
//  These are copied directly from the translation of
//  com.google.protobuf.Descriptors.java.

#ifndef __ComGoogleProtobufFieldTypes_H__
#define __ComGoogleProtobufFieldTypes_H__

#import "com/google/protobuf/ProtocolMessageEnum.h"
#import "java/lang/Boolean.h"
#import "java/lang/Double.h"
#import "java/lang/Enum.h"
#import "java/lang/Float.h"
#import "java/lang/Integer.h"
#import "java/lang/Long.h"

@class ComGoogleProtobufDescriptors_FieldDescriptor_JavaType;
@class ComGoogleProtobufDescriptorProtos_FieldDescriptorProto_Type;

#define TYPE_Int jint
#define TYPE_Long jlong
#define TYPE_Float jfloat
#define TYPE_Double jdouble
#define TYPE_Bool jboolean
#define TYPE_Enum id
#define TYPE_Id id
#define TYPE_Retainable id

#define TYPE_RETAIN_Int(value) value
#define TYPE_RETAIN_Long(value) value
#define TYPE_RETAIN_Float(value) value
#define TYPE_RETAIN_Double(value) value
#define TYPE_RETAIN_Bool(value) value
#define TYPE_RETAIN_Enum(value) value
#define TYPE_RETAIN_Retainable(value) [value retain]

#define TYPE_ASSIGN_Int(assignee, value) assignee = value
#define TYPE_ASSIGN_Long(assignee, value) assignee = value
#define TYPE_ASSIGN_Float(assignee, value) assignee = value
#define TYPE_ASSIGN_Double(assignee, value) assignee = value
#define TYPE_ASSIGN_Bool(assignee, value) assignee = value
#define TYPE_ASSIGN_Enum(assignee, value) assignee = value
#define TYPE_ASSIGN_Retainable(assignee, value) \
  ([assignee autorelease], assignee = [value retain])

#define HASH_Int(value) value
#define HASH_Long(value) (int)((uint64_t)value ^ ((uint64_t)value >> 32))
#define HASH_Float(value) *(int *)&value
#define HASH_Double(value) (int)(*(uint64_t *)&value ^ (*(uint64_t *)&value >> 32))
#define HASH_Bool(value) (value ? 1231 : 1237)
#define HASH_Id(value) (int)[value hash]

// Functions that box a value from its field storage type into an object type.
// For enums, the boxed type is the same as its storage type. (a Java enum
// object)
#define CGPBoxedValueInt(value) [JavaLangInteger valueOfWithInt:value]
#define CGPBoxedValueLong(value) [JavaLangLong valueOfWithLong:value]
#define CGPBoxedValueFloat(value) [JavaLangFloat valueOfWithFloat:value]
#define CGPBoxedValueDouble(value) [JavaLangDouble valueOfWithDouble:value]
#define CGPBoxedValueBool(value) [JavaLangBoolean valueOfWithBoolean:value]
#define CGPBoxedValueId(value) value

// Functions that unbox a value into its primitive type.
#define CGPUnboxValueInt(value) [value intValue]
#define CGPUnboxValueLong(value) [value longLongValue]
#define CGPUnboxValueFloat(value) [value floatValue]
#define CGPUnboxValueDouble(value) [value doubleValue]
#define CGPUnboxValueBool(value) [value booleanValue]
#define CGPUnboxValueEnum(value) value
#define CGPUnboxValueRetainable(value) value

// Functions that convert a value from its reflection to its storage type.
#define CGPFromReflectionTypeInt(value) [value intValue]
#define CGPFromReflectionTypeLong(value) [value longLongValue]
#define CGPFromReflectionTypeFloat(value) [value floatValue]
#define CGPFromReflectionTypeDouble(value) [value doubleValue]
#define CGPFromReflectionTypeBool(value) [value booleanValue]
#define CGPFromReflectionTypeEnum(value) ((CGPEnumValueDescriptor *)value)->enum_
#define CGPFromReflectionTypeRetainable(value) value

// Creates a switch statement over the java types grouping enums together with
// the other object types.
#define SWITCH_TYPES_NO_ENUM(type, CASE_MACRO) \
  switch (type) { \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_INT: \
      CASE_MACRO(Int) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_LONG: \
      CASE_MACRO(Long) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_FLOAT: \
      CASE_MACRO(Float) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_DOUBLE: \
      CASE_MACRO(Double) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_BOOLEAN: \
      CASE_MACRO(Bool) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_STRING: \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_BYTE_STRING: \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_ENUM: \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_MESSAGE: \
      CASE_MACRO(Id) \
  }

// Creates a switch statement over the java types separating enums from the
// other object types.
#define SWITCH_TYPES_WITH_ENUM(type, CASE_MACRO) \
  switch (type) { \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_INT: \
      CASE_MACRO(Int) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_LONG: \
      CASE_MACRO(Long) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_FLOAT: \
      CASE_MACRO(Float) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_DOUBLE: \
      CASE_MACRO(Double) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_BOOLEAN: \
      CASE_MACRO(Bool) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_STRING: \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_BYTE_STRING: \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_MESSAGE: \
      CASE_MACRO(Retainable) \
    case ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_ENUM: \
      CASE_MACRO(Enum) \
  }

// Declares the given macro once for each java field type, grouping enums and
// the other object types together as "Id".
#define FOR_EACH_TYPE_NO_ENUM(MACRO) \
  MACRO(Int) \
  MACRO(Long) \
  MACRO(Float) \
  MACRO(Double) \
  MACRO(Bool) \
  MACRO(Id)

// Declares the given macro once for each java field type, declaring it
// separately for enums (as "Enum) and the other object types (as "Retainable").
#define FOR_EACH_TYPE_WITH_ENUM(MACRO) \
  MACRO(Int) \
  MACRO(Long) \
  MACRO(Float) \
  MACRO(Double) \
  MACRO(Bool) \
  MACRO(Enum) \
  MACRO(Retainable)

// The remainder of this file is copied from the translation of the types
// FieldDescriptor.Type and FieldDescriptor.JavaType in Descriptor.java.

typedef NS_ENUM(NSUInteger, ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum) {
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_DOUBLE = 0,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_FLOAT = 1,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_INT64 = 2,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_UINT64 = 3,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_INT32 = 4,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_FIXED64 = 5,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_FIXED32 = 6,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_BOOL = 7,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_STRING = 8,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_GROUP = 9,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_MESSAGE = 10,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_BYTES = 11,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_UINT32 = 12,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_ENUM = 13,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_SFIXED32 = 14,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_SFIXED64 = 15,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_SINT32 = 16,
  ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum_SINT64 = 17,
};

typedef ComGoogleProtobufDescriptors_FieldDescriptor_Type_Enum CGPFieldType;

@interface ComGoogleProtobufDescriptors_FieldDescriptor_Type : JavaLangEnum < NSCopying >

#pragma mark Public

- (ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *)getJavaType;

- (ComGoogleProtobufDescriptorProtos_FieldDescriptorProto_Type *)toProto;

+ (ComGoogleProtobufDescriptors_FieldDescriptor_Type *)valueOfWithComGoogleProtobufDescriptorProtos_FieldDescriptorProto_Type:(ComGoogleProtobufDescriptorProtos_FieldDescriptorProto_Type *)type;

#pragma mark Package-Private

+ (IOSObjectArray *)values;

+ (ComGoogleProtobufDescriptors_FieldDescriptor_Type *)valueOfWithNSString:(NSString *)name;

- (id)copyWithZone:(NSZone *)zone;

@end

J2OBJC_STATIC_INIT(ComGoogleProtobufDescriptors_FieldDescriptor_Type)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT ComGoogleProtobufDescriptors_FieldDescriptor_Type *ComGoogleProtobufDescriptors_FieldDescriptor_Type_values_[];

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_DOUBLE(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, DOUBLE)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_FLOAT(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, FLOAT)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_INT64(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, INT64)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_UINT64(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, UINT64)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_INT32(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, INT32)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_FIXED64(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, FIXED64)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_FIXED32(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, FIXED32)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_BOOL(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, BOOL)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_STRING(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, STRING)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_GROUP(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, GROUP)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_MESSAGE(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, MESSAGE)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_BYTES(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, BYTES)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_UINT32(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, UINT32)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_ENUM(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, ENUM)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_SFIXED32(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, SFIXED32)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_SFIXED64(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, SFIXED64)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_SINT32(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, SINT32)

inline ComGoogleProtobufDescriptors_FieldDescriptor_Type *
ComGoogleProtobufDescriptors_FieldDescriptor_Type_get_SINT64(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_Type, SINT64)

FOUNDATION_EXPORT ComGoogleProtobufDescriptors_FieldDescriptor_Type *ComGoogleProtobufDescriptors_FieldDescriptor_Type_valueOfWithComGoogleProtobufDescriptorProtos_FieldDescriptorProto_Type_(ComGoogleProtobufDescriptorProtos_FieldDescriptorProto_Type *type);

FOUNDATION_EXPORT IOSObjectArray *ComGoogleProtobufDescriptors_FieldDescriptor_Type_values(void);

FOUNDATION_EXPORT ComGoogleProtobufDescriptors_FieldDescriptor_Type *ComGoogleProtobufDescriptors_FieldDescriptor_Type_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT ComGoogleProtobufDescriptors_FieldDescriptor_Type *ComGoogleProtobufDescriptors_FieldDescriptor_Type_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufDescriptors_FieldDescriptor_Type)

typedef NS_ENUM(NSUInteger, ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum) {
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_INT = 0,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_LONG = 1,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_FLOAT = 2,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_DOUBLE = 3,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_BOOLEAN = 4,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_STRING = 5,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_BYTE_STRING = 6,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_ENUM = 7,
  ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum_MESSAGE = 8,
};

@interface ComGoogleProtobufDescriptors_FieldDescriptor_JavaType : JavaLangEnum < NSCopying >

#pragma mark Package-Private

+ (IOSObjectArray *)values;

+ (ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *)valueOfWithNSString:(NSString *)name;

- (id)copyWithZone:(NSZone *)zone;

@end

J2OBJC_STATIC_INIT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_values_[];

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_INT(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, INT)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_LONG(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, LONG)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_FLOAT(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, FLOAT)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_DOUBLE(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, DOUBLE)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_BOOLEAN(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, BOOLEAN)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_STRING(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, STRING)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_BYTE_STRING(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, BYTE_STRING)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_ENUM(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, ENUM)

inline ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *
ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_get_MESSAGE(void);
J2OBJC_ENUM_CONSTANT(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType, MESSAGE)

FOUNDATION_EXPORT IOSObjectArray *ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_values(
    void);

FOUNDATION_EXPORT ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufDescriptors_FieldDescriptor_JavaType)

#endif // __ComGoogleProtobufFieldTypes_H__

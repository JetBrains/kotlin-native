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

//  Hand written counterpart of com.google.protobuf.Descriptors.

#ifndef __ComGoogleProtobufDescriptors_H__
#define __ComGoogleProtobufDescriptors_H__

#import "JreEmulation.h"

#import "com/google/protobuf/FieldTypes.h"
#import "com/google/protobuf/common.h"

@class ComGoogleProtobufDescriptorProtos_FieldOptions;
@class ComGoogleProtobufDescriptors_Descriptor;
@class ComGoogleProtobufDescriptors_EnumDescriptor;
@class ComGoogleProtobufDescriptors_EnumValueDescriptor;
@class ComGoogleProtobufDescriptors_FieldDescriptor;
@class ComGoogleProtobufDescriptors_OneofDescriptor;
@class ComGoogleProtobufGeneratedMessage;
@class ComGoogleProtobufGeneratedMessage_Builder;
@protocol ComGoogleProtobufProtocolMessageEnum;
@protocol JavaUtilList;

typedef ComGoogleProtobufDescriptors_Descriptor CGPDescriptor;
typedef ComGoogleProtobufDescriptors_EnumDescriptor CGPEnumDescriptor;
typedef ComGoogleProtobufDescriptors_EnumValueDescriptor CGPEnumValueDescriptor;
typedef ComGoogleProtobufDescriptors_FieldDescriptor CGPFieldDescriptor;
typedef ComGoogleProtobufDescriptors_FieldDescriptor_JavaType_Enum CGPFieldJavaType;
typedef ComGoogleProtobufDescriptors_OneofDescriptor CGPOneofDescriptor;

@interface ComGoogleProtobufDescriptors_Descriptor : NSObject

- (NSString *)getName;

- (NSString *)getFullName;

- (id<JavaUtilList>)getFields;

- (id<JavaUtilList>)getOneofs;

- (CGPFieldDescriptor *)findFieldByNumberWithInt:(jint)fieldId;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufDescriptors_Descriptor)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufDescriptors_Descriptor)

@interface ComGoogleProtobufDescriptors_FieldDescriptor : NSObject

- (ComGoogleProtobufDescriptors_FieldDescriptor_Type *)getType;

- (ComGoogleProtobufDescriptors_FieldDescriptor_JavaType *)getJavaType;

- (jint)getNumber;

- (NSString *)getName;

- (BOOL)isRequired;

- (BOOL)isRepeated;

- (BOOL)isExtension;

- (CGPOneofDescriptor *)getContainingOneof;

- (CGPDescriptor *)getMessageType;

- (CGPEnumDescriptor *)getEnumType;

- (id)getDefaultValue;

- (ComGoogleProtobufDescriptorProtos_FieldOptions *)getOptions;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufDescriptors_FieldDescriptor)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufDescriptors_FieldDescriptor)

@interface ComGoogleProtobufDescriptors_EnumDescriptor : NSObject

- (CGPEnumValueDescriptor *)findValueByNumberWithInt:(jint)number;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufDescriptors_EnumDescriptor)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufDescriptors_EnumDescriptor)

@interface ComGoogleProtobufDescriptors_EnumValueDescriptor : NSObject

- (jint)getNumber;

- (NSString *)getName;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufDescriptors_EnumValueDescriptor)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufDescriptors_EnumValueDescriptor)

@interface ComGoogleProtobufDescriptors_OneofDescriptor : NSObject

- (NSString *)getName;

- (CGPDescriptor *)getContainingType;

- (id<JavaUtilList>)getFields;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufDescriptors_OneofDescriptor)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufDescriptors_OneofDescriptor)

#endif // __ComGoogleProtobufDescriptors_H__

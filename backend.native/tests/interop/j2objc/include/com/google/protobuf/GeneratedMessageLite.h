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

#ifndef __ComGoogleProtobufGeneratedMessageLite_H__
#define __ComGoogleProtobufGeneratedMessageLite_H__

#include "J2ObjC_header.h"
#include "com/google/protobuf/AbstractMessageLite.h"
#include "com/google/protobuf/ExtensionLite.h"

@class ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor;
@class ComGoogleProtobufWireFormat_FieldType;
@class ComGoogleProtobufWireFormat_FieldType;
@class IOSClass;
@protocol ComGoogleProtobufInternal_EnumLiteMap;
@protocol ComGoogleProtobufMessageLite;

@interface ComGoogleProtobufGeneratedMessageLite : ComGoogleProtobufAbstractMessageLite

#pragma mark Public

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessageLite)

FOUNDATION_EXPORT void ComGoogleProtobufGeneratedMessageLite_init(
    ComGoogleProtobufGeneratedMessageLite *self);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessageLite)


@interface ComGoogleProtobufGeneratedMessageLite_GeneratedExtension
    : ComGoogleProtobufExtensionLite {
 @public
  id<ComGoogleProtobufMessageLite> containingTypeDefaultInstance_;
  id defaultValue_;
  id<ComGoogleProtobufMessageLite> messageDefaultInstance_;
  ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor *descriptor_;
}

#pragma mark Public

- (id<ComGoogleProtobufMessageLite>)getContainingTypeDefaultInstance;

- (id)getDefaultValue;

- (ComGoogleProtobufWireFormat_FieldType *)getLiteType;

- (id<ComGoogleProtobufMessageLite>)getMessageDefaultInstance;

- (jint)getNumber;

- (jboolean)isRepeated;

#pragma mark Package-Private

- (instancetype)initWithComGoogleProtobufMessageLite:
                    (id<ComGoogleProtobufMessageLite>)containingTypeDefaultInstance
                                                           withId:(id)defaultValue
                                 withComGoogleProtobufMessageLite:
                                     (id<ComGoogleProtobufMessageLite>)messageDefaultInstance
    withComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor:
        (ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor *)descriptor
                                                     withIOSClass:(IOSClass *)singularType;

// Disallowed inherited constructor, do not use.
- (instancetype)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessageLite_GeneratedExtension)

J2OBJC_FIELD_SETTER(ComGoogleProtobufGeneratedMessageLite_GeneratedExtension,
                    containingTypeDefaultInstance_, id<ComGoogleProtobufMessageLite>)
J2OBJC_FIELD_SETTER(ComGoogleProtobufGeneratedMessageLite_GeneratedExtension, defaultValue_, id)
J2OBJC_FIELD_SETTER(ComGoogleProtobufGeneratedMessageLite_GeneratedExtension,
                    messageDefaultInstance_, id<ComGoogleProtobufMessageLite>)
J2OBJC_FIELD_SETTER(ComGoogleProtobufGeneratedMessageLite_GeneratedExtension, descriptor_,
                    ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor *)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessageLite_GeneratedExtension)


@interface ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor : NSObject {
 @public
  jint number_;
  ComGoogleProtobufWireFormat_FieldType *type_;
  jboolean isRepeated_;
  jboolean isPacked_;
}

#pragma mark Public

- (ComGoogleProtobufWireFormat_FieldType *)getLiteType;

- (jint)getNumber;

- (jboolean)isPacked;

- (jboolean)isRepeated;

#pragma mark Package-Private

- (instancetype)initWithComGoogleProtobufInternal_EnumLiteMap:
                    (id<ComGoogleProtobufInternal_EnumLiteMap>)enumTypeMap
                                                      withInt:(jint)number
                    withComGoogleProtobufWireFormat_FieldType:
                        (ComGoogleProtobufWireFormat_FieldType *)type
                                                  withBoolean:(jboolean)isRepeated
                                                  withBoolean:(jboolean)isPacked;

// Disallowed inherited constructors, do not use.
- (instancetype)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor)

J2OBJC_FIELD_SETTER(ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor, type_,
                    ComGoogleProtobufWireFormat_FieldType *)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessageLite_ExtensionDescriptor)

#endif  // __ComGoogleProtobufGeneratedMessageLite_H__

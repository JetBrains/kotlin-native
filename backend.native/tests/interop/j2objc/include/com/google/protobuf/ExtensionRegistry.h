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

//  Hand written counterpart for com.google.protobuf.ExtensionRegistry.

#ifndef __ComGoogleProtobufExtensionRegistry_H__
#define __ComGoogleProtobufExtensionRegistry_H__

#import "JreEmulation.h"
#import "com/google/protobuf/ExtensionRegistryLite.h"

@class ComGoogleProtobufDescriptors_Descriptor;
@class ComGoogleProtobufDescriptors_FieldDescriptor;
@class ComGoogleProtobufExtension;
@class ComGoogleProtobufExtensionRegistry;
@class ComGoogleProtobufExtensionRegistry_ExtensionInfo;
@class ComGoogleProtobufGeneratedMessage_GeneratedExtension;
@protocol ComGoogleProtobufMessage;

typedef ComGoogleProtobufExtensionRegistry CGPExtensionRegistry;
typedef ComGoogleProtobufExtensionRegistry_ExtensionInfo CGPExtensionInfo;

@interface ComGoogleProtobufExtensionRegistry : ComGoogleProtobufExtensionRegistryLite

+ (ComGoogleProtobufExtensionRegistry *)newInstance NS_RETURNS_NOT_RETAINED;

+ (ComGoogleProtobufExtensionRegistry *)getEmptyRegistry;

- (void)addWithComGoogleProtobufExtension:(ComGoogleProtobufExtension *)extension;
- (void)addWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (ComGoogleProtobufGeneratedMessage_GeneratedExtension *)extension;

- (ComGoogleProtobufExtensionRegistry_ExtensionInfo *)
    findExtensionByNumberWithComGoogleProtobufDescriptors_Descriptor:
        (ComGoogleProtobufDescriptors_Descriptor *)descriptor
    withInt:(jint)fieldId;

- (ComGoogleProtobufExtensionRegistry *)getUnmodifiable;

@end

J2OBJC_STATIC_INIT(ComGoogleProtobufExtensionRegistry)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufExtensionRegistry)


@interface ComGoogleProtobufExtensionRegistry_ExtensionInfo : NSObject {
 @public
  ComGoogleProtobufDescriptors_FieldDescriptor *descriptor_;
  id<ComGoogleProtobufMessage> defaultInstance_;
}

- (instancetype)initWithField:(ComGoogleProtobufDescriptors_FieldDescriptor *)field;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufExtensionRegistry_ExtensionInfo)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufExtensionRegistry_ExtensionInfo)

CF_EXTERN_C_BEGIN

void ComGoogleProtobufExtensionRegistry_initWithBoolean_(
    ComGoogleProtobufExtensionRegistry *self, jboolean empty);

ComGoogleProtobufExtensionRegistry *ComGoogleProtobufExtensionRegistry_newInstance();

ComGoogleProtobufExtensionRegistry *ComGoogleProtobufExtensionRegistry_getEmptyRegistry();

ComGoogleProtobufExtensionRegistry_ExtensionInfo *
ComGoogleProtobufExtensionRegistry_newExtensionInfoWithComGoogleProtobufExtension_(
    ComGoogleProtobufExtension *extension);

CF_EXTERN_C_END

#endif // __ComGoogleProtobufExtensionRegistry_H__

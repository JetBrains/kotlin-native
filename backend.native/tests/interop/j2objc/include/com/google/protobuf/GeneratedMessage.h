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

//  Created by Keith Stanger on Mar. 20, 2013.
//
//  Hand written counterpart for com.google.protobuf.GeneratedMessage and
//  friends.

#ifndef __ComGoogleProtobufGeneratedMessage_H__
#define __ComGoogleProtobufGeneratedMessage_H__

#include "JreEmulation.h"

#include "com/google/protobuf/AbstractMessage.h"
#include "com/google/protobuf/Extension.h"
#include "com/google/protobuf/Message.h"
#include "com/google/protobuf/MessageOrBuilder.h"
#include "com/google/protobuf/common.h"

@class ComGoogleProtobufDescriptors_FieldDescriptor;
@class ComGoogleProtobufExtensionRegistryLite;
@class ComGoogleProtobufGeneratedMessage_GeneratedExtension;
struct CGPFieldData;

typedef ComGoogleProtobufGeneratedMessage_GeneratedExtension CGPGeneratedExtension;

@interface ComGoogleProtobufGeneratedMessage : ComGoogleProtobufAbstractMessage

+ (id)getDescriptor;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessage)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessage)

@interface ComGoogleProtobufGeneratedMessage_Builder : ComGoogleProtobufAbstractMessage_Builder

- (id)mergeFromWithJavaIoInputStream:(JavaIoInputStream *)input;
- (id)mergeFromWithJavaIoInputStream:(JavaIoInputStream *)input
    withComGoogleProtobufExtensionRegistryLite:
        (ComGoogleProtobufExtensionRegistryLite *)extensionRegistry;
- (id)mergeFromWithComGoogleProtobufByteString:(ComGoogleProtobufByteString *)data;
- (id)mergeFromWithComGoogleProtobufByteString:(ComGoogleProtobufByteString *)data
    withComGoogleProtobufExtensionRegistryLite:
        (ComGoogleProtobufExtensionRegistryLite *)extensionRegistry;
- (id)mergeFromWithByteArray:(IOSByteArray *)data;
- (id)mergeFromWithByteArray:(IOSByteArray *)data
    withComGoogleProtobufExtensionRegistryLite:
        (ComGoogleProtobufExtensionRegistryLite *)extensionRegistry;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessage_Builder)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessage_Builder)

@protocol ComGoogleProtobufGeneratedMessage_ExtendableMessageOrBuilder
    <ComGoogleProtobufMessageOrBuilder, JavaObject>

- (jboolean)hasExtensionWithComGoogleProtobufExtensionLite:
    (ComGoogleProtobufExtensionLite *)extension;
- (jboolean)hasExtensionWithComGoogleProtobufExtension:(CGPExtension *)extension;
- (jboolean)hasExtensionWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (CGPGeneratedExtension *)extension;

- (id)getExtensionWithComGoogleProtobufExtensionLite:
    (ComGoogleProtobufExtensionLite *)extension;
- (id)getExtensionWithComGoogleProtobufExtension:(CGPExtension *)extension;
- (id)getExtensionWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (CGPGeneratedExtension *)extension;

- (id)getExtensionWithComGoogleProtobufExtensionLite:
    (ComGoogleProtobufExtensionLite *)extension withInt:(jint)index;
- (id)getExtensionWithComGoogleProtobufExtension:(CGPExtension *)extension withInt:(jint)index;
- (id)getExtensionWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (CGPGeneratedExtension *)extension withInt:(jint)index;

- (jint)getExtensionCountWithComGoogleProtobufExtensionLite:
    (ComGoogleProtobufExtensionLite *)extension;
- (jint)getExtensionCountWithComGoogleProtobufExtension:(CGPExtension *)extension;
- (jint)getExtensionCountWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (CGPGeneratedExtension *)extension;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessage_ExtendableMessageOrBuilder)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessage_ExtendableMessageOrBuilder)

@interface ComGoogleProtobufGeneratedMessage_ExtendableMessage :
    ComGoogleProtobufGeneratedMessage<ComGoogleProtobufGeneratedMessage_ExtendableMessageOrBuilder>
@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessage_ExtendableMessage)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessage_ExtendableMessage)

@interface ComGoogleProtobufGeneratedMessage_ExtendableBuilder :
    ComGoogleProtobufGeneratedMessage_Builder
    <ComGoogleProtobufGeneratedMessage_ExtendableMessageOrBuilder>

- (id)setExtensionWithComGoogleProtobufExtensionLite:
    (ComGoogleProtobufExtensionLite *)extension withId:(id)value;
- (id)setExtensionWithComGoogleProtobufExtension:(CGPExtension *)extension withId:(id)value;
- (id)setExtensionWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (CGPGeneratedExtension *)extension withId:(id)value;

- (id)addExtensionWithComGoogleProtobufExtensionLite:
    (ComGoogleProtobufExtensionLite *)extension withId:(id)value;
- (id)addExtensionWithComGoogleProtobufExtension:(CGPExtension *)extension withId:(id)value;
- (id)addExtensionWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (CGPGeneratedExtension *)extension withId:(id)value;

- (id)clearExtensionWithComGoogleProtobufExtensionLite:
    (ComGoogleProtobufExtensionLite *)extension;
- (id)clearExtensionWithComGoogleProtobufExtension:(CGPExtension *)extension;
- (id)clearExtensionWithComGoogleProtobufGeneratedMessage_GeneratedExtension:
    (CGPGeneratedExtension *)extension;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessage_ExtendableBuilder)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessage_ExtendableBuilder)

@interface ComGoogleProtobufGeneratedMessage_GeneratedExtension : CGPExtension
@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufGeneratedMessage_GeneratedExtension)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufGeneratedMessage_GeneratedExtension)

#endif // __ComGoogleProtobufGeneratedMessage_H__

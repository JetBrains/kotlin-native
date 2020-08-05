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

//  Hand written counterpart of com.google.protobuf.MessageLite.

#ifndef __ComGoogleProtobufMessageLite_H__
#define __ComGoogleProtobufMessageLite_H__

#import "com/google/protobuf/MessageLiteOrBuilder.h"

@class ComGoogleProtobufByteString;
@class ComGoogleProtobufExtensionRegistryLite;
@class JavaIoInputStream;
@class JavaIoOutputStream;
@protocol ComGoogleProtobufMessageLite_Builder;

@protocol ComGoogleProtobufMessageLite <ComGoogleProtobufMessageLiteOrBuilder, JavaObject>

- (id<ComGoogleProtobufMessageLite_Builder>)toBuilder;
- (id<ComGoogleProtobufMessageLite_Builder>)newBuilderForType NS_RETURNS_NOT_RETAINED;

- (jint)getSerializedSize;
- (ComGoogleProtobufByteString *)toByteString;
- (IOSByteArray *)toByteArray;

- (void)writeToWithJavaIoOutputStream:(JavaIoOutputStream *)output;
- (void)writeDelimitedToWithJavaIoOutputStream:(JavaIoOutputStream *)output;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufMessageLite)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufMessageLite)

@protocol ComGoogleProtobufMessageLite_Builder <ComGoogleProtobufMessageLiteOrBuilder, JavaObject>

- (id<ComGoogleProtobufMessageLite>)build;

- (id<ComGoogleProtobufMessageLite_Builder>)
    mergeFromWithComGoogleProtobufByteString:(ComGoogleProtobufByteString *)data;
- (id<ComGoogleProtobufMessageLite_Builder>)
    mergeFromWithComGoogleProtobufByteString:(ComGoogleProtobufByteString *)data
    withComGoogleProtobufExtensionRegistryLite:
        (ComGoogleProtobufExtensionRegistryLite *)extensionRegistry;
- (id<ComGoogleProtobufMessageLite_Builder>)mergeFromWithByteArray:(IOSByteArray *)data;
- (id<ComGoogleProtobufMessageLite_Builder>)mergeFromWithByteArray:(IOSByteArray *)data
    withComGoogleProtobufExtensionRegistryLite:
        (ComGoogleProtobufExtensionRegistryLite *)extensionRegistry;
- (id<ComGoogleProtobufMessageLite_Builder>)
    mergeFromWithJavaIoInputStream:(JavaIoInputStream *)input;
- (id<ComGoogleProtobufMessageLite_Builder>)
    mergeFromWithJavaIoInputStream:(JavaIoInputStream *)input
    withComGoogleProtobufExtensionRegistryLite:
        (ComGoogleProtobufExtensionRegistryLite *)extensionRegistry;
- (jboolean)mergeDelimitedFromWithJavaIoInputStream:(JavaIoInputStream *)input;
- (jboolean)mergeDelimitedFromWithJavaIoInputStream:(JavaIoInputStream *)input
    withComGoogleProtobufExtensionRegistryLite:
        (ComGoogleProtobufExtensionRegistryLite *)extensionRegistry;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufMessageLite_Builder)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufMessageLite_Builder)

#endif // __ComGoogleProtobufMessageLite_H__

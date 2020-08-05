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

//  Hand written counterpart for com.google.protobuf.ExtensionRegistryLite.

#ifndef __ComGoogleProtobufExtensionRegistryLite_H__
#define __ComGoogleProtobufExtensionRegistryLite_H__

#import "JreEmulation.h"

@class ComGoogleProtobufDescriptors_Descriptor;
@class ComGoogleProtobufDescriptors_FieldDescriptor;
@class ComGoogleProtobufExtension;
@class ComGoogleProtobufExtensionLite;
@class ComGoogleProtobufExtensionRegistryLite;

typedef ComGoogleProtobufExtensionRegistryLite CGPExtensionRegistryLite;

@interface ComGoogleProtobufExtensionRegistryLite : NSObject

+ (ComGoogleProtobufExtensionRegistryLite *)getEmptyRegistry;

- (void)addWithComGoogleProtobufExtensionLite:(ComGoogleProtobufExtensionLite *)extension;

- (ComGoogleProtobufExtensionRegistryLite *)getUnmodifiable;

@end

J2OBJC_STATIC_INIT(ComGoogleProtobufExtensionRegistryLite)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufExtensionRegistryLite)

CF_EXTERN_C_BEGIN

ComGoogleProtobufExtensionRegistryLite *ComGoogleProtobufExtensionRegistryLite_newInstance();

ComGoogleProtobufExtensionRegistryLite *ComGoogleProtobufExtensionRegistryLite_getEmptyRegistry();

void CGPExtensionRegistryAdd(
    CGPExtensionRegistryLite *registry, ComGoogleProtobufExtensionLite *extension);

ComGoogleProtobufDescriptors_FieldDescriptor *CGPExtensionRegistryFind(
    CGPExtensionRegistryLite *registry, ComGoogleProtobufDescriptors_Descriptor *descriptor,
    jint fieldNumber);

void ComGoogleProtobufExtensionRegistryLite_initWithBoolean_(
    ComGoogleProtobufExtensionRegistryLite *self, jboolean empty);

ComGoogleProtobufExtensionRegistryLite *new_ComGoogleProtobufExtensionRegistryLite_initWithBoolean_(
    jboolean empty) NS_RETURNS_RETAINED;

CF_EXTERN_C_END

#endif  // __ComGoogleProtobufExtensionRegistryLite_H__

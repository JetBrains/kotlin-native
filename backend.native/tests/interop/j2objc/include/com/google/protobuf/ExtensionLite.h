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

//  Created by Daniel Weis on Oct. 28, 2014.
//
//  Hand written counterpart for com.google.protobuf.GeneratedMessage and
//  friends.

#ifndef __ComGoogleProtobufExtensionLite_H__
#define __ComGoogleProtobufExtensionLite_H__

#import "com/google/protobuf/Descriptors.h"
#import "com/google/protobuf/Message.h"

struct CGPFieldData;

@interface ComGoogleProtobufExtensionLite : NSObject {
 @public
  CGPFieldDescriptor *fieldDescriptor_;
}

- (instancetype)initWithFieldData:(struct CGPFieldData *)data;

- (ComGoogleProtobufDescriptors_FieldDescriptor *)getDescriptor;

- (id<ComGoogleProtobufMessage>)getMessageDefaultInstance;

@end

typedef ComGoogleProtobufExtensionLite CGPExtensionLite;

CF_EXTERN_C_BEGIN
void ComGoogleProtobufExtensionLite_initWithFieldData_(CGPExtensionLite *self,
                                                      struct CGPFieldData *data);
CF_EXTERN_C_END

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufExtensionLite)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufExtensionLite)

#endif  // __ComGoogleProtobufExtensionLite_H__

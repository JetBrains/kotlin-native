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

//  Hand written counterpart for com.google.protobuf.AbstractMessageLite.

#ifndef __ComGoogleProtobufAbstractMessageLite_H__
#define __ComGoogleProtobufAbstractMessageLite_H__

#include "JreEmulation.h"

#include "com/google/protobuf/MessageLite.h"
#include "com/google/protobuf/MessageLiteOrBuilder.h"

@interface ComGoogleProtobufAbstractMessageLite :
    NSObject<ComGoogleProtobufMessageLite, ComGoogleProtobufMessageLiteOrBuilder>
@end

void ComGoogleProtobufAbstractMessageLite_init(ComGoogleProtobufAbstractMessageLite *self);

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufAbstractMessageLite)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufAbstractMessageLite)

@interface ComGoogleProtobufAbstractMessageLite_Builder :
    NSObject<ComGoogleProtobufMessageLite_Builder, ComGoogleProtobufMessageLiteOrBuilder>
@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufAbstractMessageLite_Builder)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufAbstractMessageLite_Builder)

#endif // __ComGoogleProtobufAbstractMessageLite_H__

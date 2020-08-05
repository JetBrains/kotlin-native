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

//  Created by Keith Stanger on Apr. 19, 2017.
//
//  Hand written counterpart for com.google.protobuf.MapEntry.

#ifndef __ComGoogleProtobufMapEntry_H__
#define __ComGoogleProtobufMapEntry_H__

#include "JreEmulation.h"

#include "com/google/protobuf/AbstractMessage.h"

@class ComGoogleProtobufMapEntry_Builder;

@interface ComGoogleProtobufMapEntry : ComGoogleProtobufAbstractMessage

- (instancetype)initWithKey:(id)key value:(id)value;

- (id)getKey;

- (id)getValue;

- (ComGoogleProtobufMapEntry_Builder *)toBuilder;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufMapEntry)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufMapEntry)

@interface ComGoogleProtobufMapEntry_Builder : ComGoogleProtobufAbstractMessage_Builder

- (ComGoogleProtobufMapEntry_Builder *)setKeyWithId:(id)key;

- (ComGoogleProtobufMapEntry_Builder *)setValueWithId:(id)value;

- (ComGoogleProtobufMapEntry *)build;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleProtobufMapEntry_Builder)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleProtobufMapEntry_Builder)

#endif // __ComGoogleProtobufMapEntry_H__

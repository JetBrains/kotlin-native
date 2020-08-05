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

// DO NOT INCLUDE EXTERNALLY.
// Contains declarations used within the runtime and generated protocol buffers.

#ifndef __ComGoogleProtobufGeneratedMessage_PackagePrivate_H__
#define __ComGoogleProtobufGeneratedMessage_PackagePrivate_H__

#import "com/google/protobuf/GeneratedMessage.h"

#import "com/google/protobuf/Descriptors_PackagePrivate.h"
#import "com/google/protobuf/ExtensionRegistryLite.h"

@interface ComGoogleProtobufGeneratedMessage () {
 @package
  int memoizedSize_;
  int memoizedHash_;
}
@end

CGP_ALWAYS_INLINE inline ComGoogleProtobufGeneratedMessage *CGPNewMessage(
    ComGoogleProtobufDescriptors_Descriptor *descriptor) {
  ComGoogleProtobufGeneratedMessage *msg =
      NSAllocateObject(descriptor->messageClass_, descriptor->storageSize_, nil);
  msg->memoizedSize_ = -1;
  return msg;
}

CGP_ALWAYS_INLINE inline ComGoogleProtobufGeneratedMessage_Builder *CGPNewBuilder(
    ComGoogleProtobufDescriptors_Descriptor *descriptor) {
  return NSAllocateObject(descriptor->builderClass_, descriptor->storageSize_, nil);
}

CF_EXTERN_C_BEGIN

ComGoogleProtobufGeneratedMessage_Builder *CGPBuilderFromPrototype(
    CGPDescriptor *descriptor, ComGoogleProtobufGeneratedMessage *prototype);

void CGPMergeFromRawData(id msg, CGPDescriptor *descriptor, const char *data, uint32_t length);

ComGoogleProtobufGeneratedMessage *CGPParseFromByteArray(
    CGPDescriptor *descriptor, IOSByteArray *bytes, CGPExtensionRegistryLite *registry);

ComGoogleProtobufGeneratedMessage *CGPParseFromInputStream(
    CGPDescriptor *descriptor, JavaIoInputStream *input, CGPExtensionRegistryLite *registry);

ComGoogleProtobufGeneratedMessage *CGPParseDelimitedFromInputStream(
    CGPDescriptor *descriptor, JavaIoInputStream *input, CGPExtensionRegistryLite *registry);

CF_EXTERN_C_END

#endif // __ComGoogleProtobufGeneratedMessage_PackagePrivate_H__

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

// Modified version of CodedInputSteram from google/protobuf/io/coded_stream.h.

// Author: kenton@google.com (Kenton Varda)
//  Based on original Protocol Buffers design by
//  Sanjay Ghemawat, Jeff Dean, and others.
//
// This file contains the CodedInputStream and CodedOutputStream classes,
// which wrap a ZeroCopyInputStream or ZeroCopyOutputStream, respectively,
// and allow you to read or write individual pieces of data in various
// formats.  In particular, these implement the varint encoding for
// integers, a simple variable-length encoding in which smaller numbers
// take fewer bytes.
//
// Typically these classes will only be used internally by the protocol
// buffer library in order to encode and decode protocol buffers.  Clients
// of the library only need to know about this class if they wish to write
// custom message parsing or serialization procedures.
//
// CodedInputStream example:
//   // Read a file created by the above code.
//   int fd = open("myfile", O_RDONLY);
//   ZeroCopyInputStream* raw_input = new FileInputStream(fd);
//   CodedInputStream coded_input = new CodedInputStream(raw_input);
//
//   coded_input->ReadLittleEndian32(&magic_number);
//   if (magic_number != 1234) {
//     cerr << "File not in expected format." << endl;
//     return;
//   }
//
//   uint32 size;
//   coded_input->ReadVarint32(&size);
//
//   char* text = new char[size + 1];
//   coded_input->ReadRaw(buffer, size);
//   text[size] = '\0';
//
//   delete coded_input;
//   delete raw_input;
//   close(fd);
//
//   cout << "Text is: " << text << endl;
//   delete [] text;
//
// For those who are interested, varint encoding is defined as follows:
//
// The encoding operates on unsigned integers of up to 64 bits in length.
// Each byte of the encoded value has the format:
// * bits 0-6: Seven bits of the number being encoded.
// * bit 7: Zero if this is the last byte in the encoding (in which
//   case all remaining bits of the number are zero) or 1 if
//   more bytes follow.
// The first byte contains the least-significant 7 bits of the number, the
// second byte (if present) contains the next-least-significant 7 bits,
// and so on.  So, the binary number 1011000101011 would be encoded in two
// bytes as "10101011 00101100".
//
// In theory, varint could be used to encode integers of any length.
// However, for practicality we set a limit at 64 bits.  The maximum encoded
// length of a number is thus 10 bytes.

#ifndef __ComGoogleProtobufCodedInputStream_H__
#define __ComGoogleProtobufCodedInputStream_H__

#import "JreEmulation.h"

#include <string>

#import "com/google/protobuf/common.h"

@class ComGoogleProtobufByteString;
@class JavaIoInputStream;

using std::string;

#define CGP_PREDICT_TRUE(x) (__builtin_expect(!!(x), 1))

// Class which reads and decodes binary data which is composed of varint-
// encoded integers and fixed-width pieces.
//
// Most methods of CodedInputStream that return a bool return false if an
// underlying I/O error occurs or if the data is malformed.  Once such a
// failure occurs, the CodedInputStream is broken and is no longer useful.
class CGPCodedInputStream {
 public:
  // Create a CGPCodedInputStream that reads from the given JavaIoInputStream.
  // This object must NOT outlive the current autorelease pool.
  explicit CGPCodedInputStream(JavaIoInputStream* input, int input_limit);

  // Create a CGPCodedInputStream that reads from the given buffer.
  explicit CGPCodedInputStream(const void *buffer, int size);

  ~CGPCodedInputStream();

  // Skips a number of bytes.  Returns false if an underlying read error
  // occurs.
  bool Skip(int count);

  // Read raw bytes, copying them into the given buffer.
  bool ReadRaw(void* buffer, int size);

  // Read a 32-bit little-endian integer.
  bool ReadLittleEndian32(uint32* value);
  // Read a 64-bit little-endian integer.
  bool ReadLittleEndian64(uint64* value);

  // Read an unsigned integer with Varint encoding, truncating to 32 bits.
  // Reading a 32-bit value is equivalent to reading a 64-bit one and casting
  // it to uint32, but may be more efficient.
  bool ReadVarint32(uint32* value);
  // Read an unsigned integer with Varint encoding.
  bool ReadVarint64(uint64* value);

  // Read a tag.  This calls ReadVarint32() and returns the result, or returns
  // zero (which is not a valid tag) if ReadVarint32() fails.  Also, it updates
  // the last tag value, which can be checked with LastTagWas().
  // Always inline because this is only called in once place per parse loop
  // but it is called for every iteration of said loop, so it should be fast.
  // GCC doesn't want to inline this by default.
  uint32 ReadTag() CGP_ALWAYS_INLINE;

  // If the last call to ReadTag() returned the given value, returns true.
  // Otherwise, returns false;
  //
  // This is needed because parsers for some types of embedded messages
  // (with field type TYPE_GROUP) don't actually know that they've reached the
  // end of a message until they see an ENDGROUP tag, which was actually part
  // of the enclosing message.  The enclosing message would like to check that
  // tag to make sure it had the right number, so it calls LastTagWas() on
  // return from the embedded parser to check.
  bool LastTagWas(uint32 expected);

  // When parsing message (but NOT a group), this method must be called
  // immediately after MergeFromCodedStream() returns (if it returns true)
  // to further verify that the message ended in a legitimate way.  For
  // example, this verifies that parsing did not end on an end-group tag.
  // It also checks for some cases where, due to optimizations,
  // MergeFromCodedStream() can incorrectly return true.
  bool ConsumedEntireMessage();

  // Limits ----------------------------------------------------------
  // Limits are used when parsing length-delimited embedded messages.
  // After the message's length is read, PushLimit() is used to prevent
  // the CodedInputStream from reading beyond that length.  Once the
  // embedded message has been parsed, PopLimit() is called to undo the
  // limit.

  // Opaque type used with PushLimit() and PopLimit().  Do not modify
  // values of this type yourself.  The only reason that this isn't a
  // struct with private internals is for efficiency.
  typedef int Limit;

  // Places a limit on the number of bytes that the stream may read,
  // starting from the current position.  Once the stream hits this limit,
  // it will act like the end of the input has been reached until PopLimit()
  // is called.
  //
  // As the names imply, the stream conceptually has a stack of limits.  The
  // shortest limit on the stack is always enforced, even if it is not the
  // top limit.
  //
  // The value returned by PushLimit() is opaque to the caller, and must
  // be passed unchanged to the corresponding call to PopLimit().
  Limit PushLimit(int byte_limit);

  // Pops the last limit pushed by PushLimit().  The input must be the value
  // returned by that call to PushLimit().
  void PopLimit(Limit limit);

  // Returns the number of bytes left until the nearest limit on the
  // stack is hit, or -1 if no limits are in place.
  int BytesUntilLimit() const;

  // Returns current position relative to the beginning of the input stream.
  int CurrentPosition() const;

  // J2ObjC additions ------------------------------------------------
  bool ReadRetainedNSString(NSString **value);
  bool ReadRetainedByteString(ComGoogleProtobufByteString **value);
  static bool ReadVarint32(int firstByte, JavaIoInputStream *input,
                           uint32 *value);

 private:
  CGPCodedInputStream(const CGPCodedInputStream&);
  void operator=(const CGPCodedInputStream&);

  JavaIoInputStream* input_;
  int input_limit_;       // Max bytes to read from input_.
  IOSByteArray* bytes_;
  const uint8* buffer_;
  const uint8* buffer_end_;     // pointer to the end of the buffer.
  int total_bytes_read_;  // total bytes read from input_, including
                          // the current buffer

  // LastTagWas() stuff.
  uint32 last_tag_;         // result of last ReadTag().

  // This is set true by ReadTag{Fallback/Slow}() if it is called when exactly
  // at EOF, or by ExpectAtEnd() when it returns true.  This happens when we
  // reach the end of a message and attempt to read another tag.
  bool legitimate_message_end_;

  // Limits
  Limit current_limit_;   // if position = -1, no limit is applied

  // For simplicity, if the current buffer crosses a limit (either a normal
  // limit created by PushLimit() or the total bytes limit), buffer_size_
  // only tracks the number of bytes before that limit.  This field
  // contains the number of bytes after it.  Note that this implies that if
  // buffer_size_ == 0 and buffer_size_after_limit_ > 0, we know we've
  // hit a limit.  However, if both are zero, it doesn't necessarily mean
  // we aren't at a limit -- the buffer may have ended exactly at the limit.
  int buffer_size_after_limit_;

  // Private member functions.

  // Advance the buffer by a given number of bytes.
  void Advance(int amount);

  // Recomputes the value of buffer_size_after_limit_.  Must be called after
  // current_limit_ or total_bytes_limit_ changes.
  void RecomputeBufferLimits();

  // Called when the buffer runs out to request more data.  Implies an
  // Advance(BufferSize()).
  bool Refresh();

  // When parsing varints, we optimize for the common case of small values, and
  // then optimize for the case when the varint fits within the current buffer
  // piece. The Fallback method is used when we can't use the one-byte
  // optimization. The Slow method is yet another fallback when the buffer is
  // not large enough. Making the slow path out-of-line speeds up the common
  // case by 10-15%. The slow path is fairly uncommon: it only triggers when a
  // message crosses multiple buffers.
  bool ReadVarint32Fallback(uint32* value);
  bool ReadVarint64Fallback(uint64* value);
  bool ReadVarint32Slow(uint32* value);
  bool ReadVarint64Slow(uint64* value);
  bool ReadLittleEndian32Fallback(uint32* value);
  bool ReadLittleEndian64Fallback(uint64* value);
  // Fallback/slow methods for reading tags. These do not update last_tag_,
  // but will set legitimate_message_end_ if we are at the end of the input
  // stream.
  uint32 ReadTagFallback();
  uint32 ReadTagSlow();
  bool ReadStringFallback(string* buffer, int size);

  // Return the size of the buffer.
  int BufferSize() const;
};

// inline methods ====================================================
// The vast majority of varints are only one byte.  These inline
// methods optimize for that case.

inline bool CGPCodedInputStream::ReadVarint32(uint32* value) {
  if (CGP_PREDICT_TRUE(buffer_ < buffer_end_) && *buffer_ < 0x80) {
    *value = *buffer_;
    Advance(1);
    return true;
  } else {
    return ReadVarint32Fallback(value);
  }
}

inline bool CGPCodedInputStream::ReadVarint64(uint64* value) {
  if (CGP_PREDICT_TRUE(buffer_ < buffer_end_) && *buffer_ < 0x80) {
    *value = *buffer_;
    Advance(1);
    return true;
  } else {
    return ReadVarint64Fallback(value);
  }
}

inline bool CGPCodedInputStream::ReadLittleEndian32(uint32* value) {
  if (CGP_PREDICT_TRUE(BufferSize() >= static_cast<int>(sizeof(*value)))) {
    uint32 readVal;
    memcpy(&readVal, buffer_, sizeof(readVal));
    *value = OSSwapLittleToHostInt32(readVal);
    Advance(sizeof(*value));
    return true;
  } else {
    return ReadLittleEndian32Fallback(value);
  }
}

inline bool CGPCodedInputStream::ReadLittleEndian64(uint64* value) {
  if (CGP_PREDICT_TRUE(BufferSize() >= static_cast<int>(sizeof(*value)))) {
    uint64 readVal;
    memcpy(&readVal, buffer_, sizeof(readVal));
    *value = OSSwapLittleToHostInt64(readVal);
    Advance(sizeof(*value));
    return true;
  } else {
    return ReadLittleEndian64Fallback(value);
  }
}

inline uint32 CGPCodedInputStream::ReadTag() {
  if (CGP_PREDICT_TRUE(buffer_ < buffer_end_) && buffer_[0] < 0x80) {
    last_tag_ = buffer_[0];
    Advance(1);
    return last_tag_;
  } else {
    last_tag_ = ReadTagFallback();
    return last_tag_;
  }
}

inline bool CGPCodedInputStream::LastTagWas(uint32 expected) {
  return last_tag_ == expected;
}

inline bool CGPCodedInputStream::ConsumedEntireMessage() {
  return legitimate_message_end_;
}

inline int CGPCodedInputStream::CurrentPosition() const {
  return total_bytes_read_ - (BufferSize() + buffer_size_after_limit_);
}

inline void CGPCodedInputStream::Advance(int amount) {
  buffer_ += amount;
}

inline int CGPCodedInputStream::BufferSize() const {
  return (int)(buffer_end_ - buffer_);
}

inline CGPCodedInputStream::CGPCodedInputStream(JavaIoInputStream *input,
                                                int input_limit)
  : input_(input),
    input_limit_(input_limit),
    bytes_([IOSByteArray newArrayWithLength:CGP_CODED_STREAM_BUFFER_SIZE]),
    buffer_(NULL),
    buffer_end_(NULL),
    total_bytes_read_(0),
    last_tag_(0),
    legitimate_message_end_(false),
    current_limit_(INT_MAX),
    buffer_size_after_limit_(0) {
  // Eagerly Refresh() so buffer space is immediately available.
  Refresh();
}

inline CGPCodedInputStream::CGPCodedInputStream(const void *buffer, int size)
  : input_(nil),
    input_limit_(INT_MAX),
    bytes_(nil),
    buffer_((uint8 *)buffer),
    buffer_end_((uint8 *)buffer + size),
    total_bytes_read_(size),
    last_tag_(0),
    legitimate_message_end_(false),
    current_limit_(size),
    buffer_size_after_limit_(0) {
}

#endif // __ComGoogleProtobufCodedInputStream_H__

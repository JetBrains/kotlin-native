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

// Modified version of CodedOutputSteram from google/protobuf/io/coded_stream.h.

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
// CodedOutputStream example:
//   // Write some data to "myfile".  First we write a 4-byte "magic number"
//   // to identify the file type, then write a length-delimited string.  The
//   // string is composed of a varint giving the length followed by the raw
//   // bytes.
//   int fd = open("myfile", O_WRONLY);
//   ZeroCopyOutputStream* raw_output = new FileOutputStream(fd);
//   CodedOutputStream* coded_output = new CodedOutputStream(raw_output);
//
//   int magic_number = 1234;
//   char text[] = "Hello world!";
//   coded_output->WriteLittleEndian32(magic_number);
//   coded_output->WriteVarint32(strlen(text));
//   coded_output->WriteRaw(text, strlen(text));
//
//   delete coded_output;
//   delete raw_output;
//   close(fd);
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

#ifndef __ComGoogleProtobufCodedOutputStream_H__
#define __ComGoogleProtobufCodedOutputStream_H__

#import "JreEmulation.h"

#import "com/google/protobuf/common.h"

@class JavaIoOutputStream;

// Class which encodes and writes binary data which is composed of varint-
// encoded integers and fixed-width pieces.
//
// Most methods of CodedOutputStream which return a bool return false if an
// underlying I/O error occurs.  Once such a failure occurs, the
// CodedOutputStream is broken and is no longer useful. The Write* methods do
// not return the stream status, but will invalidate the stream if an error
// occurs. The client can probe HadError() to determine the status.
//
// Note that every method of CodedOutputStream which writes some data has
// a corresponding static "ToArray" version. These versions write directly
// to the provided buffer, returning a pointer past the last written byte.
// They require that the buffer has sufficient capacity for the encoded data.
// This allows an optimization where we check if an output stream has enough
// space for an entire message before we start writing and, if there is, we
// call only the ToArray methods to avoid doing bound checks for each
// individual value.
// i.e., in the example above:
//
//   CodedOutputStream coded_output = new CodedOutputStream(raw_output);
//   int magic_number = 1234;
//   char text[] = "Hello world!";
//
//   int coded_size = sizeof(magic_number) +
//                    CodedOutputStream::VarintSize32(strlen(text)) +
//                    strlen(text);
//
//   uint8* buffer =
//       coded_output->GetDirectBufferForNBytesAndAdvance(coded_size);
//   if (buffer != NULL) {
//     // The output stream has enough space in the buffer: write directly to
//     // the array.
//     buffer = CodedOutputStream::WriteLittleEndian32ToArray(magic_number,
//                                                            buffer);
//     buffer = CodedOutputStream::WriteVarint32ToArray(strlen(text), buffer);
//     buffer = CodedOutputStream::WriteRawToArray(text, strlen(text), buffer);
//   } else {
//     // Make bound-checked writes, which will ask the underlying stream for
//     // more space as needed.
//     coded_output->WriteLittleEndian32(magic_number);
//     coded_output->WriteVarint32(strlen(text));
//     coded_output->WriteRaw(text, strlen(text));
//   }
//
//   delete coded_output;
class CGPCodedOutputStream {
 public:
  // Create a CGPCodedOutputStream that writes to the given JavaIoOutputStream.
  // This object must NOT outlive the current autorelease pool. Data written to
  // this coded stream is not guaranteed to be written through to the
  // JavaIoOutputStream until this object is destructed.
  explicit CGPCodedOutputStream(JavaIoOutputStream *output);

  // Create a CodedOutputStream that writes to the given buffer up to the given
  // size. If it needs to write past the given size, then HadError() will return
  // true.
  explicit CGPCodedOutputStream(void *buffer, int size);

  ~CGPCodedOutputStream();

  // Skips a number of bytes, leaving the bytes unmodified in the underlying
  // buffer.  Returns false if an underlying write error occurs.  This is
  // mainly useful with GetDirectBufferPointer().
  bool Skip(int count);

  // Sets *data to point directly at the unwritten part of the
  // CodedOutputStream's underlying buffer, and *size to the size of that
  // buffer, but does not advance the stream's current position.  This will
  // always either produce a non-empty buffer or return false.  If the caller
  // writes any data to this buffer, it should then call Skip() to skip over
  // the consumed bytes.  This may be useful for implementing external fast
  // serialization routines for types of data not covered by the
  // CodedOutputStream interface.
  bool GetDirectBufferPointer(void** data, int* size);

  // Write raw bytes, copying them from the given buffer.
  void WriteRaw(const void* buffer, int size);

  // Write a 32-bit little-endian integer.
  void WriteLittleEndian32(uint32 value);
  // Like WriteLittleEndian32()  but writing directly to the target array.
  static uint8* WriteLittleEndian32ToArray(uint32 value, uint8* target);
  // Write a 64-bit little-endian integer.
  void WriteLittleEndian64(uint64 value);
  // Like WriteLittleEndian64()  but writing directly to the target array.
  static uint8* WriteLittleEndian64ToArray(uint64 value, uint8* target);

  // Write an unsigned integer with Varint encoding.  Writing a 32-bit value
  // is equivalent to casting it to uint64 and writing it as a 64-bit value,
  // but may be more efficient.
  void WriteVarint32(uint32 value);
  // Write an unsigned integer with Varint encoding.
  void WriteVarint64(uint64 value);

  // Equivalent to WriteVarint32() except when the value is negative,
  // in which case it must be sign-extended to a full 10 bytes.
  void WriteVarint32SignExtended(int32 value);

  // This is identical to WriteVarint32(), but optimized for writing tags.
  // In particular, if the input is a compile-time constant, this method
  // compiles down to a couple instructions.
  // Always inline because otherwise the aformentioned optimization can't work,
  // but GCC by default doesn't want to inline this.
  void WriteTag(uint32 value);

  // Returns the number of bytes needed to encode the given value as a varint.
  static int VarintSize32(uint32 value);
  // Returns the number of bytes needed to encode the given value as a varint.
  static int VarintSize64(uint64 value);

  // If negative, 10 bytes.  Otheriwse, same as VarintSize32().
  static int VarintSize32SignExtended(int32 value);

  // Returns true if there was an underlying I/O error since this object was
  // created.
  bool HadError() const { return had_error_; }

  // Flushes the buffer contents. If called before GetDirectBufferPointer(),
  // the full buffer size will be available.
  bool FlushBuffer();

 private:
  CGPCodedOutputStream(const CGPCodedOutputStream&);
  void operator=(const CGPCodedOutputStream&);

  JavaIoOutputStream *output_;
  IOSByteArray *bytes_;
  uint8* buffer_;
  int buffer_size_;
  int total_bytes_;  // Sum of sizes of all buffers seen so far.
  bool had_error_;   // Whether an error occurred during output.

  // Advance the buffer by a given number of bytes.
  void Advance(int amount);

  // Called when the buffer runs out to request more data.  Implies an
  // Advance(buffer_size_).
  bool Refresh();

  // Always-inlined versions of WriteVarint* functions so that code can be
  // reused, while still controlling size. For instance, WriteVarint32ToArray()
  // should not directly call this: since it is inlined itself, doing so
  // would greatly increase the size of generated code. Instead, it should call
  // WriteVarint32FallbackToArray.  Meanwhile, WriteVarint32() is already
  // out-of-line, so it should just invoke this directly to avoid any extra
  // function call overhead.
  static uint8* WriteVarint32FallbackToArrayInline(
      uint32 value, uint8* target) CGP_ALWAYS_INLINE;
  static uint8* WriteVarint64ToArrayInline(
      uint64 value, uint8* target) CGP_ALWAYS_INLINE;

  static int VarintSize32Fallback(uint32 value);
};

// inline methods ====================================================
// The vast majority of varints are only one byte.  These inline
// methods optimize for that case.

inline void CGPCodedOutputStream::WriteVarint32SignExtended(int32 value) {
  if (value < 0) {
    WriteVarint64(static_cast<uint64>(value));
  } else {
    WriteVarint32(static_cast<uint32>(value));
  }
}

inline uint8* CGPCodedOutputStream::WriteLittleEndian32ToArray(uint32 value,
                                                               uint8* target) {
  OSWriteLittleInt32(target, 0, value);
  return target + sizeof(value);
}

inline uint8* CGPCodedOutputStream::WriteLittleEndian64ToArray(uint64 value,
                                                               uint8* target) {
  OSWriteLittleInt64(target, 0, value);
  return target + sizeof(value);
}

inline void CGPCodedOutputStream::WriteTag(uint32 value) {
  WriteVarint32(value);
}

inline int CGPCodedOutputStream::VarintSize32(uint32 value) {
  if (value < (1 << 7)) {
    return 1;
  } else  {
    return VarintSize32Fallback(value);
  }
}

inline int CGPCodedOutputStream::VarintSize32SignExtended(int32 value) {
  if (value < 0) {
    return 10;
  } else {
    return VarintSize32(static_cast<uint32>(value));
  }
}

inline void CGPCodedOutputStream::Advance(int amount) {
  buffer_ += amount;
  buffer_size_ -= amount;
}

inline bool CGPCodedOutputStream::Refresh() {
  buffer_size_ = 0; // Make sure that the entire buffer is flushed.
  return FlushBuffer();
}

#endif // __ComGoogleProtobufCodedOutputStream_H__

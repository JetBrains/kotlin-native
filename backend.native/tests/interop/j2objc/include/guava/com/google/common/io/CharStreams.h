//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/io/CharStreams.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonIoCharStreams")
#ifdef RESTRICT_ComGoogleCommonIoCharStreams
#define INCLUDE_ALL_ComGoogleCommonIoCharStreams 0
#else
#define INCLUDE_ALL_ComGoogleCommonIoCharStreams 1
#endif
#undef RESTRICT_ComGoogleCommonIoCharStreams

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonIoCharStreams_) && (INCLUDE_ALL_ComGoogleCommonIoCharStreams || defined(INCLUDE_ComGoogleCommonIoCharStreams))
#define ComGoogleCommonIoCharStreams_

@class JavaIoReader;
@class JavaIoWriter;
@class JavaLangStringBuilder;
@class JavaNioCharBuffer;
@protocol ComGoogleCommonIoLineProcessor;
@protocol JavaLangAppendable;
@protocol JavaLangReadable;
@protocol JavaUtilList;

/*!
 @brief Provides utility methods for working with character streams.
 <p>All method parameters must be non-null unless documented otherwise. 
 <p>Some of the methods in this class take arguments with a generic type of <code>Readable &
  Closeable</code>
 . A <code>java.io.Reader</code> implements both of those interfaces. Similarly for <code>Appendable & Closeable</code>
  and <code>java.io.Writer</code>.
 @author Chris Nokleberg
 @author Bin Zhu
 @author Colin Decker
 @since 1.0
 */
@interface ComGoogleCommonIoCharStreams : NSObject

#pragma mark Public

/*!
 @brief Returns a Writer that sends all output to the given <code>Appendable</code> target.Closing the
  writer will close the target if it is <code>Closeable</code>, and flushing the writer will flush the
  target if it is <code>java.io.Flushable</code>.
 @param target the object to which output will be sent
 @return a new Writer object, unless target is a Writer, in which case the target is returned
 */
+ (JavaIoWriter *)asWriterWithJavaLangAppendable:(id<JavaLangAppendable> __nonnull)target;

/*!
 @brief Copies all characters between the <code>Readable</code> and <code>Appendable</code> objects.Does not
  close or flush either object.
 @param from the object to read from
 @param to the object to write to
 @return the number of characters copied
 @throw IOExceptionif an I/O error occurs
 */
+ (jlong)copy__WithJavaLangReadable:(id<JavaLangReadable> __nonnull)from
             withJavaLangAppendable:(id<JavaLangAppendable> __nonnull)to OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Reads and discards data from the given <code>Readable</code> until the end of the stream is reached.
 Returns the total number of chars read. Does not close the stream.
 @since 20.0
 */
+ (jlong)exhaustWithJavaLangReadable:(id<JavaLangReadable> __nonnull)readable;

/*!
 @brief Returns a <code>Writer</code> that simply discards written chars.
 @since 15.0
 */
+ (JavaIoWriter *)nullWriter;

/*!
 @brief Reads all of the lines from a <code>Readable</code> object.The lines do not include
  line-termination characters, but do include other leading and trailing whitespace.
 <p>Does not close the <code>Readable</code>. If reading files or resources you should use the <code>Files.readLines</code>
  and <code>Resources.readLines</code> methods.
 @param r the object to read from
 @return a mutable <code>List</code> containing all the lines
 @throw IOExceptionif an I/O error occurs
 */
+ (id<JavaUtilList>)readLinesWithJavaLangReadable:(id<JavaLangReadable> __nonnull)r;

/*!
 @brief Streams lines from a <code>Readable</code> object, stopping when the processor returns <code>false</code>
  or all lines have been read and returning the result produced by the processor.Does not close 
 <code>readable</code>.
 Note that this method may not fully consume the contents of <code>readable</code>
  if the processor stops processing early.
 @throw IOExceptionif an I/O error occurs
 @since 14.0
 */
+ (id)readLinesWithJavaLangReadable:(id<JavaLangReadable> __nonnull)readable
 withComGoogleCommonIoLineProcessor:(id<ComGoogleCommonIoLineProcessor> __nonnull)processor;

/*!
 @brief Discards <code>n</code> characters of data from the reader.This method will block until the full
  amount has been skipped.
 Does not close the reader.
 @param reader the reader to read from
 @param n the number of characters to skip
 @throw EOFExceptionif this stream reaches the end before skipping all the characters
 @throw IOExceptionif an I/O error occurs
 */
+ (void)skipFullyWithJavaIoReader:(JavaIoReader * __nonnull)reader
                         withLong:(jlong)n;

/*!
 @brief Reads all characters from a <code>Readable</code> object into a <code>String</code>.Does not close the 
 <code>Readable</code>.
 @param r the object to read from
 @return a string containing all the characters
 @throw IOExceptionif an I/O error occurs
 */
+ (NSString *)toStringWithJavaLangReadable:(id<JavaLangReadable> __nonnull)r;

#pragma mark Package-Private

/*!
 @brief Copies all characters between the <code>Reader</code> and <code>StringBuilder</code> objects.Does not
  close or flush the reader.
 <p>This is identical to <code>copy(Readable, Appendable)</code> but optimized for these specific
  types. CharBuffer has poor performance when being written into or read out of so round tripping
  all the bytes through the buffer takes a long time. With these specialized types we can just
  use a char array.
 @param from the object to read from
 @param to the object to write to
 @return the number of characters copied
 @throw IOExceptionif an I/O error occurs
 */
+ (jlong)copyReaderToBuilderWithJavaIoReader:(JavaIoReader * __nonnull)from
                   withJavaLangStringBuilder:(JavaLangStringBuilder * __nonnull)to OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Copies all characters between the <code>Reader</code> and <code>Writer</code> objects.Does not close or
  flush the reader or writer.
 <p>This is identical to <code>copy(Readable, Appendable)</code> but optimized for these specific
  types. CharBuffer has poor performance when being written into or read out of so round tripping
  all the bytes through the buffer takes a long time. With these specialized types we can just
  use a char array.
 @param from the object to read from
 @param to the object to write to
 @return the number of characters copied
 @throw IOExceptionif an I/O error occurs
 */
+ (jlong)copyReaderToWriterWithJavaIoReader:(JavaIoReader * __nonnull)from
                           withJavaIoWriter:(JavaIoWriter * __nonnull)to OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Creates a new <code>CharBuffer</code> for buffering reads or writes.
 */
+ (JavaNioCharBuffer *)createBuffer;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonIoCharStreams)

FOUNDATION_EXPORT JavaNioCharBuffer *ComGoogleCommonIoCharStreams_createBuffer(void);

FOUNDATION_EXPORT jlong ComGoogleCommonIoCharStreams_copy__WithJavaLangReadable_withJavaLangAppendable_(id<JavaLangReadable> from, id<JavaLangAppendable> to);

FOUNDATION_EXPORT jlong ComGoogleCommonIoCharStreams_copyReaderToBuilderWithJavaIoReader_withJavaLangStringBuilder_(JavaIoReader *from, JavaLangStringBuilder *to);

FOUNDATION_EXPORT jlong ComGoogleCommonIoCharStreams_copyReaderToWriterWithJavaIoReader_withJavaIoWriter_(JavaIoReader *from, JavaIoWriter *to);

FOUNDATION_EXPORT NSString *ComGoogleCommonIoCharStreams_toStringWithJavaLangReadable_(id<JavaLangReadable> r);

FOUNDATION_EXPORT id<JavaUtilList> ComGoogleCommonIoCharStreams_readLinesWithJavaLangReadable_(id<JavaLangReadable> r);

FOUNDATION_EXPORT id ComGoogleCommonIoCharStreams_readLinesWithJavaLangReadable_withComGoogleCommonIoLineProcessor_(id<JavaLangReadable> readable, id<ComGoogleCommonIoLineProcessor> processor);

FOUNDATION_EXPORT jlong ComGoogleCommonIoCharStreams_exhaustWithJavaLangReadable_(id<JavaLangReadable> readable);

FOUNDATION_EXPORT void ComGoogleCommonIoCharStreams_skipFullyWithJavaIoReader_withLong_(JavaIoReader *reader, jlong n);

FOUNDATION_EXPORT JavaIoWriter *ComGoogleCommonIoCharStreams_nullWriter(void);

FOUNDATION_EXPORT JavaIoWriter *ComGoogleCommonIoCharStreams_asWriterWithJavaLangAppendable_(id<JavaLangAppendable> target);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonIoCharStreams)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonIoCharStreams")

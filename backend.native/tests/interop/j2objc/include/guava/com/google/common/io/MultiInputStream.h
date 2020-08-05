//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/io/MultiInputStream.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonIoMultiInputStream")
#ifdef RESTRICT_ComGoogleCommonIoMultiInputStream
#define INCLUDE_ALL_ComGoogleCommonIoMultiInputStream 0
#else
#define INCLUDE_ALL_ComGoogleCommonIoMultiInputStream 1
#endif
#undef RESTRICT_ComGoogleCommonIoMultiInputStream

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonIoMultiInputStream_) && (INCLUDE_ALL_ComGoogleCommonIoMultiInputStream || defined(INCLUDE_ComGoogleCommonIoMultiInputStream))
#define ComGoogleCommonIoMultiInputStream_

#define RESTRICT_JavaIoInputStream 1
#define INCLUDE_JavaIoInputStream 1
#include "java/io/InputStream.h"

@class IOSByteArray;
@protocol JavaUtilIterator;

/*!
 @brief An <code>InputStream</code> that concatenates multiple substreams.At most one stream will be open at
  a time.
 @author Chris Nokleberg
 @since 1.0
 */
@interface ComGoogleCommonIoMultiInputStream : JavaIoInputStream

#pragma mark Public

/*!
 @brief Creates a new instance.
 @param it an iterator of I/O suppliers that will provide each substream
 */
- (instancetype __nonnull)initPackagePrivateWithJavaUtilIterator:(id<JavaUtilIterator> __nonnull)it;

- (jint)available;

- (void)close;

- (jboolean)markSupported;

- (jint)read;

- (jint)readWithByteArray:(IOSByteArray * __nonnull)b
                  withInt:(jint)off
                  withInt:(jint)len;

- (jlong)skipWithLong:(jlong)n;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonIoMultiInputStream)

FOUNDATION_EXPORT void ComGoogleCommonIoMultiInputStream_initPackagePrivateWithJavaUtilIterator_(ComGoogleCommonIoMultiInputStream *self, id<JavaUtilIterator> it);

FOUNDATION_EXPORT ComGoogleCommonIoMultiInputStream *new_ComGoogleCommonIoMultiInputStream_initPackagePrivateWithJavaUtilIterator_(id<JavaUtilIterator> it) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonIoMultiInputStream *create_ComGoogleCommonIoMultiInputStream_initPackagePrivateWithJavaUtilIterator_(id<JavaUtilIterator> it);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonIoMultiInputStream)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonIoMultiInputStream")

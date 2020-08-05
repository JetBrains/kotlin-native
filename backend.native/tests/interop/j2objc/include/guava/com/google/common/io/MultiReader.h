//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/io/MultiReader.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonIoMultiReader")
#ifdef RESTRICT_ComGoogleCommonIoMultiReader
#define INCLUDE_ALL_ComGoogleCommonIoMultiReader 0
#else
#define INCLUDE_ALL_ComGoogleCommonIoMultiReader 1
#endif
#undef RESTRICT_ComGoogleCommonIoMultiReader

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonIoMultiReader_) && (INCLUDE_ALL_ComGoogleCommonIoMultiReader || defined(INCLUDE_ComGoogleCommonIoMultiReader))
#define ComGoogleCommonIoMultiReader_

#define RESTRICT_JavaIoReader 1
#define INCLUDE_JavaIoReader 1
#include "java/io/Reader.h"

@class IOSCharArray;
@protocol JavaUtilIterator;

/*!
 @brief A <code>Reader</code> that concatenates multiple readers.
 @author Bin Zhu
 @since 1.0
 */
@interface ComGoogleCommonIoMultiReader : JavaIoReader

#pragma mark Public

- (void)close;

- (jint)readWithCharArray:(IOSCharArray * __nonnull)cbuf
                  withInt:(jint)off
                  withInt:(jint)len;

- (jboolean)ready;

- (jlong)skipWithLong:(jlong)n;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivateWithJavaUtilIterator:(id<JavaUtilIterator> __nonnull)readers;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

- (instancetype __nonnull)initWithId:(id __nonnull)arg0 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonIoMultiReader)

FOUNDATION_EXPORT void ComGoogleCommonIoMultiReader_initPackagePrivateWithJavaUtilIterator_(ComGoogleCommonIoMultiReader *self, id<JavaUtilIterator> readers);

FOUNDATION_EXPORT ComGoogleCommonIoMultiReader *new_ComGoogleCommonIoMultiReader_initPackagePrivateWithJavaUtilIterator_(id<JavaUtilIterator> readers) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonIoMultiReader *create_ComGoogleCommonIoMultiReader_initPackagePrivateWithJavaUtilIterator_(id<JavaUtilIterator> readers);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonIoMultiReader)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonIoMultiReader")

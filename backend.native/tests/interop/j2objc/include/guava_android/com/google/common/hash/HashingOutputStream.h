//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/hash/HashingOutputStream.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonHashHashingOutputStream")
#ifdef RESTRICT_ComGoogleCommonHashHashingOutputStream
#define INCLUDE_ALL_ComGoogleCommonHashHashingOutputStream 0
#else
#define INCLUDE_ALL_ComGoogleCommonHashHashingOutputStream 1
#endif
#undef RESTRICT_ComGoogleCommonHashHashingOutputStream

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonHashHashingOutputStream_) && (INCLUDE_ALL_ComGoogleCommonHashHashingOutputStream || defined(INCLUDE_ComGoogleCommonHashHashingOutputStream))
#define ComGoogleCommonHashHashingOutputStream_

#define RESTRICT_JavaIoFilterOutputStream 1
#define INCLUDE_JavaIoFilterOutputStream 1
#include "java/io/FilterOutputStream.h"

@class ComGoogleCommonHashHashCode;
@class IOSByteArray;
@class JavaIoOutputStream;
@protocol ComGoogleCommonHashHashFunction;

/*!
 @brief An <code>OutputStream</code> that maintains a hash of the data written to it.
 @author Nick Piepmeier
 @since 16.0
 */
@interface ComGoogleCommonHashHashingOutputStream : JavaIoFilterOutputStream

#pragma mark Public

/*!
 @brief Creates an output stream that hashes using the given <code>HashFunction</code>, and forwards all
  data written to it to the underlying <code>OutputStream</code>.
 <p>The <code>OutputStream</code> should not be written to before or after the hand-off.
 */
- (instancetype __nonnull)initWithComGoogleCommonHashHashFunction:(id<ComGoogleCommonHashHashFunction> __nonnull)hashFunction
                                           withJavaIoOutputStream:(JavaIoOutputStream * __nonnull)outArg;

- (void)close;

/*!
 @brief Returns the <code>HashCode</code> based on the data written to this stream.The result is
  unspecified if this method is called more than once on the same instance.
 */
- (ComGoogleCommonHashHashCode *)hash__;

- (void)writeWithByteArray:(IOSByteArray * __nonnull)bytes
                   withInt:(jint)off
                   withInt:(jint)len;

- (void)writeWithInt:(jint)b;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initWithJavaIoOutputStream:(JavaIoOutputStream * __nonnull)arg0 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonHashHashingOutputStream)

FOUNDATION_EXPORT void ComGoogleCommonHashHashingOutputStream_initWithComGoogleCommonHashHashFunction_withJavaIoOutputStream_(ComGoogleCommonHashHashingOutputStream *self, id<ComGoogleCommonHashHashFunction> hashFunction, JavaIoOutputStream *outArg);

FOUNDATION_EXPORT ComGoogleCommonHashHashingOutputStream *new_ComGoogleCommonHashHashingOutputStream_initWithComGoogleCommonHashHashFunction_withJavaIoOutputStream_(id<ComGoogleCommonHashHashFunction> hashFunction, JavaIoOutputStream *outArg) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonHashHashingOutputStream *create_ComGoogleCommonHashHashingOutputStream_initWithComGoogleCommonHashHashFunction_withJavaIoOutputStream_(id<ComGoogleCommonHashHashFunction> hashFunction, JavaIoOutputStream *outArg);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonHashHashingOutputStream)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonHashHashingOutputStream")

//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/collect/ReverseNaturalOrdering.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectReverseNaturalOrdering")
#ifdef RESTRICT_ComGoogleCommonCollectReverseNaturalOrdering
#define INCLUDE_ALL_ComGoogleCommonCollectReverseNaturalOrdering 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectReverseNaturalOrdering 1
#endif
#undef RESTRICT_ComGoogleCommonCollectReverseNaturalOrdering

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectReverseNaturalOrdering_) && (INCLUDE_ALL_ComGoogleCommonCollectReverseNaturalOrdering || defined(INCLUDE_ComGoogleCommonCollectReverseNaturalOrdering))
#define ComGoogleCommonCollectReverseNaturalOrdering_

#define RESTRICT_ComGoogleCommonCollectOrdering 1
#define INCLUDE_ComGoogleCommonCollectOrdering 1
#include "com/google/common/collect/Ordering.h"

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@class IOSObjectArray;
@protocol JavaLangComparable;
@protocol JavaLangIterable;
@protocol JavaUtilIterator;

/*!
 @brief An ordering that uses the reverse of the natural order of the values.
 */
@interface ComGoogleCommonCollectReverseNaturalOrdering : ComGoogleCommonCollectOrdering < JavaIoSerializable >
@property (readonly, class, strong) ComGoogleCommonCollectReverseNaturalOrdering *INSTANCE NS_SWIFT_NAME(INSTANCE);

+ (ComGoogleCommonCollectReverseNaturalOrdering *)INSTANCE;

#pragma mark Public

- (jint)compareWithId:(id<JavaLangComparable> __nonnull)left
               withId:(id<JavaLangComparable> __nonnull)right;

- (id<JavaLangComparable>)maxWithId:(id<JavaLangComparable> __nonnull)a
                             withId:(id<JavaLangComparable> __nonnull)b;

- (id<JavaLangComparable>)maxWithId:(id<JavaLangComparable> __nonnull)a
                             withId:(id<JavaLangComparable> __nonnull)b
                             withId:(id<JavaLangComparable> __nonnull)c
                  withNSObjectArray:(IOSObjectArray * __nonnull)rest;

- (id<JavaLangComparable>)maxWithJavaLangIterable:(id<JavaLangIterable> __nonnull)iterable;

- (id<JavaLangComparable>)maxWithJavaUtilIterator:(id<JavaUtilIterator> __nonnull)iterator;

- (id<JavaLangComparable>)minWithId:(id<JavaLangComparable> __nonnull)a
                             withId:(id<JavaLangComparable> __nonnull)b;

- (id<JavaLangComparable>)minWithId:(id<JavaLangComparable> __nonnull)a
                             withId:(id<JavaLangComparable> __nonnull)b
                             withId:(id<JavaLangComparable> __nonnull)c
                  withNSObjectArray:(IOSObjectArray * __nonnull)rest;

- (id<JavaLangComparable>)minWithJavaLangIterable:(id<JavaLangIterable> __nonnull)iterable;

- (id<JavaLangComparable>)minWithJavaUtilIterator:(id<JavaUtilIterator> __nonnull)iterator;

- (ComGoogleCommonCollectOrdering *)reverse;

- (NSString *)description;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonCollectReverseNaturalOrdering)

inline ComGoogleCommonCollectReverseNaturalOrdering *ComGoogleCommonCollectReverseNaturalOrdering_get_INSTANCE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT ComGoogleCommonCollectReverseNaturalOrdering *ComGoogleCommonCollectReverseNaturalOrdering_INSTANCE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonCollectReverseNaturalOrdering, INSTANCE, ComGoogleCommonCollectReverseNaturalOrdering *)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectReverseNaturalOrdering)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectReverseNaturalOrdering")

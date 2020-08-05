//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/util/concurrent/CombinedFuture.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonUtilConcurrentCombinedFuture")
#ifdef RESTRICT_ComGoogleCommonUtilConcurrentCombinedFuture
#define INCLUDE_ALL_ComGoogleCommonUtilConcurrentCombinedFuture 0
#else
#define INCLUDE_ALL_ComGoogleCommonUtilConcurrentCombinedFuture 1
#endif
#undef RESTRICT_ComGoogleCommonUtilConcurrentCombinedFuture

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonUtilConcurrentCombinedFuture_) && (INCLUDE_ALL_ComGoogleCommonUtilConcurrentCombinedFuture || defined(INCLUDE_ComGoogleCommonUtilConcurrentCombinedFuture))
#define ComGoogleCommonUtilConcurrentCombinedFuture_

#define RESTRICT_ComGoogleCommonUtilConcurrentAggregateFuture 1
#define INCLUDE_ComGoogleCommonUtilConcurrentAggregateFuture 1
#include "com/google/common/util/concurrent/AggregateFuture.h"

@class ComGoogleCommonCollectImmutableCollection;
@protocol ComGoogleCommonUtilConcurrentAsyncCallable;
@protocol JavaUtilConcurrentCallable;
@protocol JavaUtilConcurrentExecutor;

/*!
 @brief Aggregate future that computes its value by calling a callable.
 */
@interface ComGoogleCommonUtilConcurrentCombinedFuture : ComGoogleCommonUtilConcurrentAggregateFuture

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectImmutableCollection:(ComGoogleCommonCollectImmutableCollection * __nonnull)futures
                                                                              withBoolean:(jboolean)allMustSucceed
                                                           withJavaUtilConcurrentExecutor:(id<JavaUtilConcurrentExecutor> __nonnull)listenerExecutor
                                           withComGoogleCommonUtilConcurrentAsyncCallable:(id<ComGoogleCommonUtilConcurrentAsyncCallable> __nonnull)callable;

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectImmutableCollection:(ComGoogleCommonCollectImmutableCollection * __nonnull)futures
                                                                              withBoolean:(jboolean)allMustSucceed
                                                           withJavaUtilConcurrentExecutor:(id<JavaUtilConcurrentExecutor> __nonnull)listenerExecutor
                                                           withJavaUtilConcurrentCallable:(id<JavaUtilConcurrentCallable> __nonnull)callable;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivate NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonUtilConcurrentCombinedFuture)

FOUNDATION_EXPORT void ComGoogleCommonUtilConcurrentCombinedFuture_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withBoolean_withJavaUtilConcurrentExecutor_withComGoogleCommonUtilConcurrentAsyncCallable_(ComGoogleCommonUtilConcurrentCombinedFuture *self, ComGoogleCommonCollectImmutableCollection *futures, jboolean allMustSucceed, id<JavaUtilConcurrentExecutor> listenerExecutor, id<ComGoogleCommonUtilConcurrentAsyncCallable> callable);

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentCombinedFuture *new_ComGoogleCommonUtilConcurrentCombinedFuture_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withBoolean_withJavaUtilConcurrentExecutor_withComGoogleCommonUtilConcurrentAsyncCallable_(ComGoogleCommonCollectImmutableCollection *futures, jboolean allMustSucceed, id<JavaUtilConcurrentExecutor> listenerExecutor, id<ComGoogleCommonUtilConcurrentAsyncCallable> callable) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentCombinedFuture *create_ComGoogleCommonUtilConcurrentCombinedFuture_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withBoolean_withJavaUtilConcurrentExecutor_withComGoogleCommonUtilConcurrentAsyncCallable_(ComGoogleCommonCollectImmutableCollection *futures, jboolean allMustSucceed, id<JavaUtilConcurrentExecutor> listenerExecutor, id<ComGoogleCommonUtilConcurrentAsyncCallable> callable);

FOUNDATION_EXPORT void ComGoogleCommonUtilConcurrentCombinedFuture_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withBoolean_withJavaUtilConcurrentExecutor_withJavaUtilConcurrentCallable_(ComGoogleCommonUtilConcurrentCombinedFuture *self, ComGoogleCommonCollectImmutableCollection *futures, jboolean allMustSucceed, id<JavaUtilConcurrentExecutor> listenerExecutor, id<JavaUtilConcurrentCallable> callable);

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentCombinedFuture *new_ComGoogleCommonUtilConcurrentCombinedFuture_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withBoolean_withJavaUtilConcurrentExecutor_withJavaUtilConcurrentCallable_(ComGoogleCommonCollectImmutableCollection *futures, jboolean allMustSucceed, id<JavaUtilConcurrentExecutor> listenerExecutor, id<JavaUtilConcurrentCallable> callable) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentCombinedFuture *create_ComGoogleCommonUtilConcurrentCombinedFuture_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withBoolean_withJavaUtilConcurrentExecutor_withJavaUtilConcurrentCallable_(ComGoogleCommonCollectImmutableCollection *futures, jboolean allMustSucceed, id<JavaUtilConcurrentExecutor> listenerExecutor, id<JavaUtilConcurrentCallable> callable);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonUtilConcurrentCombinedFuture)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonUtilConcurrentCombinedFuture")

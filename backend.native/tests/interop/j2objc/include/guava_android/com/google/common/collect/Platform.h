//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/collect/Platform.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectPlatform")
#ifdef RESTRICT_ComGoogleCommonCollectPlatform
#define INCLUDE_ALL_ComGoogleCommonCollectPlatform 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectPlatform 1
#endif
#undef RESTRICT_ComGoogleCommonCollectPlatform

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectPlatform_) && (INCLUDE_ALL_ComGoogleCommonCollectPlatform || defined(INCLUDE_ComGoogleCommonCollectPlatform))
#define ComGoogleCommonCollectPlatform_

@class ComGoogleCommonCollectMapMaker;
@class IOSObjectArray;
@protocol JavaUtilMap;
@protocol JavaUtilSet;

/*!
 @brief Methods factored out so that they can be emulated differently in GWT.
 @author Hayward Chan
 */
@interface ComGoogleCommonCollectPlatform : NSObject

#pragma mark Package-Private

/*!
 @brief Returns a new array of the given length with the same type as a reference array.
 @param reference any array of the desired type
 @param length the length of the new array
 */
+ (IOSObjectArray *)newArrayWithNSObjectArray:(IOSObjectArray * __nonnull)reference
                                      withInt:(jint)length OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Returns the platform preferred implementation of a map based on a hash table.
 */
+ (id<JavaUtilMap>)newHashMapWithExpectedSizeWithInt:(jint)expectedSize OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Returns the platform preferred implementation of a set based on a hash table.
 */
+ (id<JavaUtilSet>)newHashSetWithExpectedSizeWithInt:(jint)expectedSize OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Returns the platform preferred implementation of an insertion ordered map based on a hash
  table.
 */
+ (id<JavaUtilMap>)newLinkedHashMapWithExpectedSizeWithInt:(jint)expectedSize OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Returns the platform preferred implementation of an insertion ordered set based on a hash
  table.
 */
+ (id<JavaUtilSet>)newLinkedHashSetWithExpectedSizeWithInt:(jint)expectedSize OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Returns the platform preferred set implementation that preserves insertion order when used only
  for insertions.
 */
+ (id<JavaUtilSet>)preservesInsertionOrderOnAddsSet;

/*!
 @brief Returns the platform preferred map implementation that preserves insertion order when used only
  for insertions.
 */
+ (id<JavaUtilMap>)preservesInsertionOrderOnPutsMap;

+ (jint)reduceExponentIfGwtWithInt:(jint)exponent;

+ (jint)reduceIterationsIfGwtWithInt:(jint)iterations;

/*!
 @brief Configures the given map maker to use weak keys, if possible; does nothing otherwise (i.e., in
  GWT).This is sometimes acceptable, when only server-side code could generate enough volume
  that reclamation becomes important.
 */
+ (ComGoogleCommonCollectMapMaker *)tryWeakKeysWithComGoogleCommonCollectMapMaker:(ComGoogleCommonCollectMapMaker * __nonnull)mapMaker;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectPlatform)

FOUNDATION_EXPORT id<JavaUtilMap> ComGoogleCommonCollectPlatform_newHashMapWithExpectedSizeWithInt_(jint expectedSize);

FOUNDATION_EXPORT id<JavaUtilMap> ComGoogleCommonCollectPlatform_newLinkedHashMapWithExpectedSizeWithInt_(jint expectedSize);

FOUNDATION_EXPORT id<JavaUtilSet> ComGoogleCommonCollectPlatform_newHashSetWithExpectedSizeWithInt_(jint expectedSize);

FOUNDATION_EXPORT id<JavaUtilSet> ComGoogleCommonCollectPlatform_newLinkedHashSetWithExpectedSizeWithInt_(jint expectedSize);

FOUNDATION_EXPORT id<JavaUtilMap> ComGoogleCommonCollectPlatform_preservesInsertionOrderOnPutsMap(void);

FOUNDATION_EXPORT id<JavaUtilSet> ComGoogleCommonCollectPlatform_preservesInsertionOrderOnAddsSet(void);

FOUNDATION_EXPORT IOSObjectArray *ComGoogleCommonCollectPlatform_newArrayWithNSObjectArray_withInt_(IOSObjectArray *reference, jint length);

FOUNDATION_EXPORT ComGoogleCommonCollectMapMaker *ComGoogleCommonCollectPlatform_tryWeakKeysWithComGoogleCommonCollectMapMaker_(ComGoogleCommonCollectMapMaker *mapMaker);

FOUNDATION_EXPORT jint ComGoogleCommonCollectPlatform_reduceIterationsIfGwtWithInt_(jint iterations);

FOUNDATION_EXPORT jint ComGoogleCommonCollectPlatform_reduceExponentIfGwtWithInt_(jint exponent);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectPlatform)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectPlatform")

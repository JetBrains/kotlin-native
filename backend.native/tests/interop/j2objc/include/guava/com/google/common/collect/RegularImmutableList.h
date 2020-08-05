//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/RegularImmutableList.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableList")
#ifdef RESTRICT_ComGoogleCommonCollectRegularImmutableList
#define INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableList 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableList 1
#endif
#undef RESTRICT_ComGoogleCommonCollectRegularImmutableList

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectRegularImmutableList_) && (INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableList || defined(INCLUDE_ComGoogleCommonCollectRegularImmutableList))
#define ComGoogleCommonCollectRegularImmutableList_

#define RESTRICT_ComGoogleCommonCollectImmutableList 1
#define INCLUDE_ComGoogleCommonCollectImmutableList 1
#include "com/google/common/collect/ImmutableList.h"

@class ComGoogleCommonCollectUnmodifiableListIterator;
@class IOSObjectArray;
@protocol JavaUtilSpliterator;

/*!
 @brief Implementation of <code>ImmutableList</code> backed by a simple array.
 @author Kevin Bourrillion
 */
@interface ComGoogleCommonCollectRegularImmutableList : ComGoogleCommonCollectImmutableList {
 @public
  IOSObjectArray *array_;
}
@property (readonly, class, strong) ComGoogleCommonCollectImmutableList *EMPTY NS_SWIFT_NAME(EMPTY);

+ (ComGoogleCommonCollectImmutableList *)EMPTY;

#pragma mark Public

- (id)getWithInt:(jint)index;

- (ComGoogleCommonCollectUnmodifiableListIterator *)listIteratorWithInt:(jint)index;

- (jint)size;

- (id<JavaUtilSpliterator>)spliterator;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivateWithNSObjectArray:(IOSObjectArray *)array;

- (jint)copyIntoArrayWithNSObjectArray:(IOSObjectArray *)dst
                               withInt:(jint)dstOff OBJC_METHOD_FAMILY_NONE;

- (jboolean)isPartialView;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonCollectRegularImmutableList)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectRegularImmutableList, array_, IOSObjectArray *)

inline ComGoogleCommonCollectImmutableList *ComGoogleCommonCollectRegularImmutableList_get_EMPTY(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT ComGoogleCommonCollectImmutableList *ComGoogleCommonCollectRegularImmutableList_EMPTY;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonCollectRegularImmutableList, EMPTY, ComGoogleCommonCollectImmutableList *)

FOUNDATION_EXPORT void ComGoogleCommonCollectRegularImmutableList_initPackagePrivateWithNSObjectArray_(ComGoogleCommonCollectRegularImmutableList *self, IOSObjectArray *array);

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableList *new_ComGoogleCommonCollectRegularImmutableList_initPackagePrivateWithNSObjectArray_(IOSObjectArray *array) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableList *create_ComGoogleCommonCollectRegularImmutableList_initPackagePrivateWithNSObjectArray_(IOSObjectArray *array);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectRegularImmutableList)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableList")

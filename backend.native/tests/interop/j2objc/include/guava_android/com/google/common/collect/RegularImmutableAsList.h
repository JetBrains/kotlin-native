//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/collect/RegularImmutableAsList.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableAsList")
#ifdef RESTRICT_ComGoogleCommonCollectRegularImmutableAsList
#define INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableAsList 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableAsList 1
#endif
#undef RESTRICT_ComGoogleCommonCollectRegularImmutableAsList

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectRegularImmutableAsList_) && (INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableAsList || defined(INCLUDE_ComGoogleCommonCollectRegularImmutableAsList))
#define ComGoogleCommonCollectRegularImmutableAsList_

#define RESTRICT_ComGoogleCommonCollectImmutableAsList 1
#define INCLUDE_ComGoogleCommonCollectImmutableAsList 1
#include "com/google/common/collect/ImmutableAsList.h"

@class ComGoogleCommonCollectImmutableCollection;
@class ComGoogleCommonCollectImmutableList;
@class ComGoogleCommonCollectUnmodifiableListIterator;
@class IOSObjectArray;

/*!
 @brief An <code>ImmutableAsList</code> implementation specialized for when the delegate collection is already
  backed by an <code>ImmutableList</code> or array.
 @author Louis Wasserman
 */
@interface ComGoogleCommonCollectRegularImmutableAsList : ComGoogleCommonCollectImmutableAsList

#pragma mark Public

- (id)getWithInt:(jint)index;

- (ComGoogleCommonCollectUnmodifiableListIterator *)listIteratorWithInt:(jint)index;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectImmutableCollection:(ComGoogleCommonCollectImmutableCollection * __nonnull)delegate
                                                  withComGoogleCommonCollectImmutableList:(ComGoogleCommonCollectImmutableList * __nonnull)delegateList;

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectImmutableCollection:(ComGoogleCommonCollectImmutableCollection * __nonnull)delegate
                                                                        withNSObjectArray:(IOSObjectArray * __nonnull)array;

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectImmutableCollection:(ComGoogleCommonCollectImmutableCollection * __nonnull)delegate
                                                                        withNSObjectArray:(IOSObjectArray * __nonnull)array
                                                                                  withInt:(jint)size;

- (jint)copyIntoArrayWithNSObjectArray:(IOSObjectArray * __nonnull)dst
                               withInt:(jint)offset OBJC_METHOD_FAMILY_NONE;

- (ComGoogleCommonCollectImmutableCollection *)delegateCollection;

- (ComGoogleCommonCollectImmutableList *)delegateList;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivate NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectRegularImmutableAsList)

FOUNDATION_EXPORT void ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withComGoogleCommonCollectImmutableList_(ComGoogleCommonCollectRegularImmutableAsList *self, ComGoogleCommonCollectImmutableCollection *delegate, ComGoogleCommonCollectImmutableList *delegateList);

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableAsList *new_ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withComGoogleCommonCollectImmutableList_(ComGoogleCommonCollectImmutableCollection *delegate, ComGoogleCommonCollectImmutableList *delegateList) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableAsList *create_ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withComGoogleCommonCollectImmutableList_(ComGoogleCommonCollectImmutableCollection *delegate, ComGoogleCommonCollectImmutableList *delegateList);

FOUNDATION_EXPORT void ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withNSObjectArray_(ComGoogleCommonCollectRegularImmutableAsList *self, ComGoogleCommonCollectImmutableCollection *delegate, IOSObjectArray *array);

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableAsList *new_ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withNSObjectArray_(ComGoogleCommonCollectImmutableCollection *delegate, IOSObjectArray *array) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableAsList *create_ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withNSObjectArray_(ComGoogleCommonCollectImmutableCollection *delegate, IOSObjectArray *array);

FOUNDATION_EXPORT void ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withNSObjectArray_withInt_(ComGoogleCommonCollectRegularImmutableAsList *self, ComGoogleCommonCollectImmutableCollection *delegate, IOSObjectArray *array, jint size);

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableAsList *new_ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withNSObjectArray_withInt_(ComGoogleCommonCollectImmutableCollection *delegate, IOSObjectArray *array, jint size) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableAsList *create_ComGoogleCommonCollectRegularImmutableAsList_initPackagePrivateWithComGoogleCommonCollectImmutableCollection_withNSObjectArray_withInt_(ComGoogleCommonCollectImmutableCollection *delegate, IOSObjectArray *array, jint size);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectRegularImmutableAsList)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableAsList")

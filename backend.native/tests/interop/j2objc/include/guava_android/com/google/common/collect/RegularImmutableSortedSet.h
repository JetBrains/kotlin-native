//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/collect/RegularImmutableSortedSet.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableSortedSet")
#ifdef RESTRICT_ComGoogleCommonCollectRegularImmutableSortedSet
#define INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableSortedSet 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableSortedSet 1
#endif
#undef RESTRICT_ComGoogleCommonCollectRegularImmutableSortedSet

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectRegularImmutableSortedSet_) && (INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableSortedSet || defined(INCLUDE_ComGoogleCommonCollectRegularImmutableSortedSet))
#define ComGoogleCommonCollectRegularImmutableSortedSet_

#define RESTRICT_ComGoogleCommonCollectImmutableSortedSet 1
#define INCLUDE_ComGoogleCommonCollectImmutableSortedSet 1
#include "com/google/common/collect/ImmutableSortedSet.h"

@class ComGoogleCommonCollectImmutableList;
@class ComGoogleCommonCollectUnmodifiableIterator;
@class IOSObjectArray;
@protocol JavaUtilCollection;
@protocol JavaUtilComparator;

/*!
 @brief An immutable sorted set with one or more elements.TODO(jlevy): Consider separate class for a
  single-element sorted set.
 @author Jared Levy
 @author Louis Wasserman
 */
@interface ComGoogleCommonCollectRegularImmutableSortedSet : ComGoogleCommonCollectImmutableSortedSet {
 @public
  ComGoogleCommonCollectImmutableList *elements_;
}
@property (readonly, class, strong) ComGoogleCommonCollectRegularImmutableSortedSet *NATURAL_EMPTY_SET NS_SWIFT_NAME(NATURAL_EMPTY_SET);

+ (ComGoogleCommonCollectRegularImmutableSortedSet *)NATURAL_EMPTY_SET;

#pragma mark Public

- (ComGoogleCommonCollectImmutableList *)asList;

- (id)ceilingWithId:(id __nonnull)element;

- (jboolean)containsWithId:(id __nullable)o;

- (jboolean)containsAllWithJavaUtilCollection:(id<JavaUtilCollection> __nonnull)targets;

- (ComGoogleCommonCollectUnmodifiableIterator *)descendingIterator;

- (jboolean)isEqual:(id __nullable)object;

- (id)first;

- (id)floorWithId:(id __nonnull)element;

- (id)higherWithId:(id __nonnull)element;

- (ComGoogleCommonCollectUnmodifiableIterator *)iterator;

- (id)last;

- (id)lowerWithId:(id __nonnull)element;

- (jint)size;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectImmutableList:(ComGoogleCommonCollectImmutableList * __nonnull)elements
                                                             withJavaUtilComparator:(id<JavaUtilComparator> __nonnull)comparator;

- (jint)copyIntoArrayWithNSObjectArray:(IOSObjectArray * __nonnull)dst
                               withInt:(jint)offset OBJC_METHOD_FAMILY_NONE;

- (ComGoogleCommonCollectImmutableSortedSet *)createDescendingSet;

- (ComGoogleCommonCollectRegularImmutableSortedSet *)getSubSetWithInt:(jint)newFromIndex
                                                              withInt:(jint)newToIndex;

- (jint)headIndexWithId:(id __nonnull)toElement
            withBoolean:(jboolean)inclusive;

- (ComGoogleCommonCollectImmutableSortedSet *)headSetImplWithId:(id __nonnull)toElement
                                                    withBoolean:(jboolean)inclusive;

- (jint)indexOfWithId:(id __nullable)target;

- (jboolean)isPartialView;

- (ComGoogleCommonCollectImmutableSortedSet *)subSetImplWithId:(id __nonnull)fromElement
                                                   withBoolean:(jboolean)fromInclusive
                                                        withId:(id __nonnull)toElement
                                                   withBoolean:(jboolean)toInclusive;

- (jint)tailIndexWithId:(id __nonnull)fromElement
            withBoolean:(jboolean)inclusive;

- (ComGoogleCommonCollectImmutableSortedSet *)tailSetImplWithId:(id __nonnull)fromElement
                                                    withBoolean:(jboolean)inclusive;

- (id<JavaUtilComparator>)unsafeComparator;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initWithJavaUtilComparator:(id<JavaUtilComparator> __nonnull)arg0 NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonCollectRegularImmutableSortedSet)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectRegularImmutableSortedSet, elements_, ComGoogleCommonCollectImmutableList *)

inline ComGoogleCommonCollectRegularImmutableSortedSet *ComGoogleCommonCollectRegularImmutableSortedSet_get_NATURAL_EMPTY_SET(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableSortedSet *ComGoogleCommonCollectRegularImmutableSortedSet_NATURAL_EMPTY_SET;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonCollectRegularImmutableSortedSet, NATURAL_EMPTY_SET, ComGoogleCommonCollectRegularImmutableSortedSet *)

FOUNDATION_EXPORT void ComGoogleCommonCollectRegularImmutableSortedSet_initPackagePrivateWithComGoogleCommonCollectImmutableList_withJavaUtilComparator_(ComGoogleCommonCollectRegularImmutableSortedSet *self, ComGoogleCommonCollectImmutableList *elements, id<JavaUtilComparator> comparator);

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableSortedSet *new_ComGoogleCommonCollectRegularImmutableSortedSet_initPackagePrivateWithComGoogleCommonCollectImmutableList_withJavaUtilComparator_(ComGoogleCommonCollectImmutableList *elements, id<JavaUtilComparator> comparator) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectRegularImmutableSortedSet *create_ComGoogleCommonCollectRegularImmutableSortedSet_initPackagePrivateWithComGoogleCommonCollectImmutableList_withJavaUtilComparator_(ComGoogleCommonCollectImmutableList *elements, id<JavaUtilComparator> comparator);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectRegularImmutableSortedSet)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectRegularImmutableSortedSet")

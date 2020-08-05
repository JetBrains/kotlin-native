//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/SortedMultisets.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectSortedMultisets")
#ifdef RESTRICT_ComGoogleCommonCollectSortedMultisets
#define INCLUDE_ALL_ComGoogleCommonCollectSortedMultisets 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectSortedMultisets 1
#endif
#undef RESTRICT_ComGoogleCommonCollectSortedMultisets
#ifdef INCLUDE_ComGoogleCommonCollectSortedMultisets_NavigableElementSet
#define INCLUDE_ComGoogleCommonCollectSortedMultisets_ElementSet 1
#endif

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectSortedMultisets_) && (INCLUDE_ALL_ComGoogleCommonCollectSortedMultisets || defined(INCLUDE_ComGoogleCommonCollectSortedMultisets))
#define ComGoogleCommonCollectSortedMultisets_

/*!
 @brief Provides static utility methods for creating and working with <code>SortedMultiset</code> instances.
 @author Louis Wasserman
 */
@interface ComGoogleCommonCollectSortedMultisets : NSObject

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectSortedMultisets)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectSortedMultisets)

#endif

#if !defined (ComGoogleCommonCollectSortedMultisets_ElementSet_) && (INCLUDE_ALL_ComGoogleCommonCollectSortedMultisets || defined(INCLUDE_ComGoogleCommonCollectSortedMultisets_ElementSet))
#define ComGoogleCommonCollectSortedMultisets_ElementSet_

#define RESTRICT_ComGoogleCommonCollectMultisets 1
#define INCLUDE_ComGoogleCommonCollectMultisets_ElementSet 1
#include "com/google/common/collect/Multisets.h"

#define RESTRICT_JavaUtilSortedSet 1
#define INCLUDE_JavaUtilSortedSet 1
#include "java/util/SortedSet.h"

@protocol ComGoogleCommonCollectSortedMultiset;
@protocol JavaUtilComparator;
@protocol JavaUtilIterator;
@protocol JavaUtilSpliterator;

/*!
 @brief A skeleton implementation for <code>SortedMultiset.elementSet</code>.
 */
@interface ComGoogleCommonCollectSortedMultisets_ElementSet : ComGoogleCommonCollectMultisets_ElementSet < JavaUtilSortedSet >

#pragma mark Public

- (id<JavaUtilComparator>)comparator;

- (id)first;

- (id<JavaUtilSortedSet>)headSetWithId:(id)toElement;

- (id<JavaUtilIterator>)iterator;

- (id)last;

- (id<JavaUtilSortedSet>)subSetWithId:(id)fromElement
                               withId:(id)toElement;

- (id<JavaUtilSortedSet>)tailSetWithId:(id)fromElement;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectSortedMultiset:(id<ComGoogleCommonCollectSortedMultiset>)multiset;

- (id<ComGoogleCommonCollectSortedMultiset>)multiset;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectSortedMultisets_ElementSet)

FOUNDATION_EXPORT void ComGoogleCommonCollectSortedMultisets_ElementSet_initWithComGoogleCommonCollectSortedMultiset_(ComGoogleCommonCollectSortedMultisets_ElementSet *self, id<ComGoogleCommonCollectSortedMultiset> multiset);

FOUNDATION_EXPORT ComGoogleCommonCollectSortedMultisets_ElementSet *new_ComGoogleCommonCollectSortedMultisets_ElementSet_initWithComGoogleCommonCollectSortedMultiset_(id<ComGoogleCommonCollectSortedMultiset> multiset) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectSortedMultisets_ElementSet *create_ComGoogleCommonCollectSortedMultisets_ElementSet_initWithComGoogleCommonCollectSortedMultiset_(id<ComGoogleCommonCollectSortedMultiset> multiset);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectSortedMultisets_ElementSet)

#endif

#if !defined (ComGoogleCommonCollectSortedMultisets_NavigableElementSet_) && (INCLUDE_ALL_ComGoogleCommonCollectSortedMultisets || defined(INCLUDE_ComGoogleCommonCollectSortedMultisets_NavigableElementSet))
#define ComGoogleCommonCollectSortedMultisets_NavigableElementSet_

#define RESTRICT_JavaUtilNavigableSet 1
#define INCLUDE_JavaUtilNavigableSet 1
#include "java/util/NavigableSet.h"

@protocol ComGoogleCommonCollectSortedMultiset;
@protocol JavaUtilIterator;

/*!
 @brief A skeleton navigable implementation for <code>SortedMultiset.elementSet</code>.
 */
@interface ComGoogleCommonCollectSortedMultisets_NavigableElementSet : ComGoogleCommonCollectSortedMultisets_ElementSet < JavaUtilNavigableSet >

#pragma mark Public

- (id)ceilingWithId:(id)e;

- (id<JavaUtilIterator>)descendingIterator;

- (id<JavaUtilNavigableSet>)descendingSet;

- (id)floorWithId:(id)e;

- (id<JavaUtilNavigableSet>)headSetWithId:(id)toElement
                              withBoolean:(jboolean)inclusive;

- (id)higherWithId:(id)e;

- (id)lowerWithId:(id)e;

- (id)pollFirst;

- (id)pollLast;

- (id<JavaUtilNavigableSet>)subSetWithId:(id)fromElement
                             withBoolean:(jboolean)fromInclusive
                                  withId:(id)toElement
                             withBoolean:(jboolean)toInclusive;

- (id<JavaUtilNavigableSet>)tailSetWithId:(id)fromElement
                              withBoolean:(jboolean)inclusive;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectSortedMultiset:(id<ComGoogleCommonCollectSortedMultiset>)multiset;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectSortedMultisets_NavigableElementSet)

FOUNDATION_EXPORT void ComGoogleCommonCollectSortedMultisets_NavigableElementSet_initWithComGoogleCommonCollectSortedMultiset_(ComGoogleCommonCollectSortedMultisets_NavigableElementSet *self, id<ComGoogleCommonCollectSortedMultiset> multiset);

FOUNDATION_EXPORT ComGoogleCommonCollectSortedMultisets_NavigableElementSet *new_ComGoogleCommonCollectSortedMultisets_NavigableElementSet_initWithComGoogleCommonCollectSortedMultiset_(id<ComGoogleCommonCollectSortedMultiset> multiset) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectSortedMultisets_NavigableElementSet *create_ComGoogleCommonCollectSortedMultisets_NavigableElementSet_initWithComGoogleCommonCollectSortedMultiset_(id<ComGoogleCommonCollectSortedMultiset> multiset);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectSortedMultisets_NavigableElementSet)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectSortedMultisets")

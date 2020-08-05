//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/FilteredEntrySetMultimap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectFilteredEntrySetMultimap")
#ifdef RESTRICT_ComGoogleCommonCollectFilteredEntrySetMultimap
#define INCLUDE_ALL_ComGoogleCommonCollectFilteredEntrySetMultimap 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectFilteredEntrySetMultimap 1
#endif
#undef RESTRICT_ComGoogleCommonCollectFilteredEntrySetMultimap

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectFilteredEntrySetMultimap_) && (INCLUDE_ALL_ComGoogleCommonCollectFilteredEntrySetMultimap || defined(INCLUDE_ComGoogleCommonCollectFilteredEntrySetMultimap))
#define ComGoogleCommonCollectFilteredEntrySetMultimap_

#define RESTRICT_ComGoogleCommonCollectFilteredEntryMultimap 1
#define INCLUDE_ComGoogleCommonCollectFilteredEntryMultimap 1
#include "com/google/common/collect/FilteredEntryMultimap.h"

#define RESTRICT_ComGoogleCommonCollectFilteredSetMultimap 1
#define INCLUDE_ComGoogleCommonCollectFilteredSetMultimap 1
#include "com/google/common/collect/FilteredSetMultimap.h"

@protocol ComGoogleCommonBasePredicate;
@protocol ComGoogleCommonCollectMultimap;
@protocol ComGoogleCommonCollectSetMultimap;
@protocol JavaLangIterable;
@protocol JavaUtilSet;

/*!
 @brief Implementation of <code>Multimaps.filterEntries(SetMultimap, Predicate)</code>.
 @author Louis Wasserman
 */
@interface ComGoogleCommonCollectFilteredEntrySetMultimap : ComGoogleCommonCollectFilteredEntryMultimap < ComGoogleCommonCollectFilteredSetMultimap >

#pragma mark Public

- (id<JavaUtilSet>)entries;

- (id<JavaUtilSet>)getWithId:(id)key;

- (id<JavaUtilSet>)removeAllWithId:(id)key;

- (id<JavaUtilSet>)replaceValuesWithId:(id)key
                  withJavaLangIterable:(id<JavaLangIterable>)values;

- (id<ComGoogleCommonCollectSetMultimap>)unfiltered;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectSetMultimap:(id<ComGoogleCommonCollectSetMultimap>)unfiltered
                                                 withComGoogleCommonBasePredicate:(id<ComGoogleCommonBasePredicate>)predicate;

- (id<JavaUtilSet>)createEntries;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivateWithComGoogleCommonCollectMultimap:(id<ComGoogleCommonCollectMultimap>)arg0
                                              withComGoogleCommonBasePredicate:(id<ComGoogleCommonBasePredicate>)arg1 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectFilteredEntrySetMultimap)

FOUNDATION_EXPORT void ComGoogleCommonCollectFilteredEntrySetMultimap_initPackagePrivateWithComGoogleCommonCollectSetMultimap_withComGoogleCommonBasePredicate_(ComGoogleCommonCollectFilteredEntrySetMultimap *self, id<ComGoogleCommonCollectSetMultimap> unfiltered, id<ComGoogleCommonBasePredicate> predicate);

FOUNDATION_EXPORT ComGoogleCommonCollectFilteredEntrySetMultimap *new_ComGoogleCommonCollectFilteredEntrySetMultimap_initPackagePrivateWithComGoogleCommonCollectSetMultimap_withComGoogleCommonBasePredicate_(id<ComGoogleCommonCollectSetMultimap> unfiltered, id<ComGoogleCommonBasePredicate> predicate) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectFilteredEntrySetMultimap *create_ComGoogleCommonCollectFilteredEntrySetMultimap_initPackagePrivateWithComGoogleCommonCollectSetMultimap_withComGoogleCommonBasePredicate_(id<ComGoogleCommonCollectSetMultimap> unfiltered, id<ComGoogleCommonBasePredicate> predicate);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectFilteredEntrySetMultimap)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectFilteredEntrySetMultimap")

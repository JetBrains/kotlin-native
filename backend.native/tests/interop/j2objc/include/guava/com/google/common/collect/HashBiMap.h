//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/HashBiMap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectHashBiMap")
#ifdef RESTRICT_ComGoogleCommonCollectHashBiMap
#define INCLUDE_ALL_ComGoogleCommonCollectHashBiMap 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectHashBiMap 1
#endif
#undef RESTRICT_ComGoogleCommonCollectHashBiMap

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectHashBiMap_) && (INCLUDE_ALL_ComGoogleCommonCollectHashBiMap || defined(INCLUDE_ComGoogleCommonCollectHashBiMap))
#define ComGoogleCommonCollectHashBiMap_

#define RESTRICT_ComGoogleCommonCollectMaps 1
#define INCLUDE_ComGoogleCommonCollectMaps_IteratorBasedAbstractMap 1
#include "com/google/common/collect/Maps.h"

#define RESTRICT_ComGoogleCommonCollectBiMap 1
#define INCLUDE_ComGoogleCommonCollectBiMap 1
#include "com/google/common/collect/BiMap.h"

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@protocol JavaUtilFunctionBiConsumer;
@protocol JavaUtilFunctionBiFunction;
@protocol JavaUtilIterator;
@protocol JavaUtilMap;
@protocol JavaUtilSet;

/*!
 @brief A <code>BiMap</code> backed by two hash tables.This implementation allows null keys and values.
 A 
 <code>HashBiMap</code> and its inverse are both serializable. 
 <p>This implementation guarantees insertion-based iteration order of its keys. 
 <p>See the Guava User Guide article on <a href="https://github.com/google/guava/wiki/NewCollectionTypesExplained#bimap">
  <code>BiMap</code> </a>.
 @author Louis Wasserman
 @author Mike Bostock
 @since 2.0
 */
@interface ComGoogleCommonCollectHashBiMap : ComGoogleCommonCollectMaps_IteratorBasedAbstractMap < ComGoogleCommonCollectBiMap, JavaIoSerializable >

#pragma mark Public

- (void)clear;

- (jboolean)containsKeyWithId:(id __nullable)key;

- (jboolean)containsValueWithId:(id __nullable)value;

/*!
 @brief Returns a new, empty <code>HashBiMap</code> with the default initial capacity (16).
 */
+ (ComGoogleCommonCollectHashBiMap *)create;

/*!
 @brief Constructs a new, empty bimap with the specified expected size.
 @param expectedSize the expected number of entries
 @throw IllegalArgumentExceptionif the specified expected size is negative
 */
+ (ComGoogleCommonCollectHashBiMap *)createWithInt:(jint)expectedSize;

/*!
 @brief Constructs a new bimap containing initial values from <code>map</code>.The bimap is created with an
  initial capacity sufficient to hold the mappings in the specified map.
 */
+ (ComGoogleCommonCollectHashBiMap *)createWithJavaUtilMap:(id<JavaUtilMap>)map;

- (id)forcePutWithId:(id)key
              withId:(id)value;

- (void)forEachWithJavaUtilFunctionBiConsumer:(id<JavaUtilFunctionBiConsumer>)action;

- (id)getWithId:(id __nullable)key;

- (id<ComGoogleCommonCollectBiMap>)inverse;

- (id<JavaUtilSet>)keySet;

- (id)putWithId:(id)key
         withId:(id)value;

- (id)removeWithId:(id __nullable)key;

- (void)replaceAllWithJavaUtilFunctionBiFunction:(id<JavaUtilFunctionBiFunction>)function;

- (jint)size;

- (id<JavaUtilSet>)values;

#pragma mark Package-Private

- (id<JavaUtilIterator>)entryIterator;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectHashBiMap)

FOUNDATION_EXPORT ComGoogleCommonCollectHashBiMap *ComGoogleCommonCollectHashBiMap_create(void);

FOUNDATION_EXPORT ComGoogleCommonCollectHashBiMap *ComGoogleCommonCollectHashBiMap_createWithInt_(jint expectedSize);

FOUNDATION_EXPORT ComGoogleCommonCollectHashBiMap *ComGoogleCommonCollectHashBiMap_createWithJavaUtilMap_(id<JavaUtilMap> map);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectHashBiMap)

#endif

#if !defined (ComGoogleCommonCollectHashBiMap_BiEntry_) && (INCLUDE_ALL_ComGoogleCommonCollectHashBiMap || defined(INCLUDE_ComGoogleCommonCollectHashBiMap_BiEntry))
#define ComGoogleCommonCollectHashBiMap_BiEntry_

#define RESTRICT_ComGoogleCommonCollectImmutableEntry 1
#define INCLUDE_ComGoogleCommonCollectImmutableEntry 1
#include "com/google/common/collect/ImmutableEntry.h"

@interface ComGoogleCommonCollectHashBiMap_BiEntry : ComGoogleCommonCollectImmutableEntry {
 @public
  jint keyHash_;
  jint valueHash_;
  ComGoogleCommonCollectHashBiMap_BiEntry *nextInKToVBucket_;
  ComGoogleCommonCollectHashBiMap_BiEntry *nextInVToKBucket_;
  ComGoogleCommonCollectHashBiMap_BiEntry *nextInKeyInsertionOrder_;
  ComGoogleCommonCollectHashBiMap_BiEntry *prevInKeyInsertionOrder_;
}

#pragma mark Package-Private

- (instancetype __nonnull)initWithId:(id)key
                             withInt:(jint)keyHash
                              withId:(id)value
                             withInt:(jint)valueHash;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectHashBiMap_BiEntry)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectHashBiMap_BiEntry, nextInKToVBucket_, ComGoogleCommonCollectHashBiMap_BiEntry *)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectHashBiMap_BiEntry, nextInVToKBucket_, ComGoogleCommonCollectHashBiMap_BiEntry *)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectHashBiMap_BiEntry, nextInKeyInsertionOrder_, ComGoogleCommonCollectHashBiMap_BiEntry *)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectHashBiMap_BiEntry, prevInKeyInsertionOrder_, ComGoogleCommonCollectHashBiMap_BiEntry *)

FOUNDATION_EXPORT void ComGoogleCommonCollectHashBiMap_BiEntry_initWithId_withInt_withId_withInt_(ComGoogleCommonCollectHashBiMap_BiEntry *self, id key, jint keyHash, id value, jint valueHash);

FOUNDATION_EXPORT ComGoogleCommonCollectHashBiMap_BiEntry *new_ComGoogleCommonCollectHashBiMap_BiEntry_initWithId_withInt_withId_withInt_(id key, jint keyHash, id value, jint valueHash) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectHashBiMap_BiEntry *create_ComGoogleCommonCollectHashBiMap_BiEntry_initWithId_withInt_withId_withInt_(id key, jint keyHash, id value, jint valueHash);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectHashBiMap_BiEntry)

#endif

#if !defined (ComGoogleCommonCollectHashBiMap_Itr_) && (INCLUDE_ALL_ComGoogleCommonCollectHashBiMap || defined(INCLUDE_ComGoogleCommonCollectHashBiMap_Itr))
#define ComGoogleCommonCollectHashBiMap_Itr_

#define RESTRICT_JavaUtilIterator 1
#define INCLUDE_JavaUtilIterator 1
#include "java/util/Iterator.h"

@class ComGoogleCommonCollectHashBiMap;
@class ComGoogleCommonCollectHashBiMap_BiEntry;
@protocol JavaUtilFunctionConsumer;

@interface ComGoogleCommonCollectHashBiMap_Itr : NSObject < JavaUtilIterator > {
 @public
  ComGoogleCommonCollectHashBiMap_BiEntry *next_;
  ComGoogleCommonCollectHashBiMap_BiEntry *toRemove_;
  jint expectedModCount_;
  jint remaining_;
}

#pragma mark Public

- (jboolean)hasNext;

- (id)next;

- (void)remove;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectHashBiMap:(ComGoogleCommonCollectHashBiMap *)outer$;

- (id)outputWithComGoogleCommonCollectHashBiMap_BiEntry:(ComGoogleCommonCollectHashBiMap_BiEntry *)entry_;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectHashBiMap_Itr)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectHashBiMap_Itr, next_, ComGoogleCommonCollectHashBiMap_BiEntry *)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectHashBiMap_Itr, toRemove_, ComGoogleCommonCollectHashBiMap_BiEntry *)

FOUNDATION_EXPORT void ComGoogleCommonCollectHashBiMap_Itr_initWithComGoogleCommonCollectHashBiMap_(ComGoogleCommonCollectHashBiMap_Itr *self, ComGoogleCommonCollectHashBiMap *outer$);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectHashBiMap_Itr)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectHashBiMap")

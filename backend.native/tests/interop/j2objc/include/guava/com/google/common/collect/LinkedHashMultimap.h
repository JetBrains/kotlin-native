//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/LinkedHashMultimap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap")
#ifdef RESTRICT_ComGoogleCommonCollectLinkedHashMultimap
#define INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap 1
#endif
#undef RESTRICT_ComGoogleCommonCollectLinkedHashMultimap
#ifdef INCLUDE_ComGoogleCommonCollectLinkedHashMultimap_ValueSet
#define INCLUDE_ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink 1
#endif
#ifdef INCLUDE_ComGoogleCommonCollectLinkedHashMultimap_ValueEntry
#define INCLUDE_ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink 1
#endif

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectLinkedHashMultimap_) && (INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap || defined(INCLUDE_ComGoogleCommonCollectLinkedHashMultimap))
#define ComGoogleCommonCollectLinkedHashMultimap_

#define RESTRICT_ComGoogleCommonCollectLinkedHashMultimapGwtSerializationDependencies 1
#define INCLUDE_ComGoogleCommonCollectLinkedHashMultimapGwtSerializationDependencies 1
#include "com/google/common/collect/LinkedHashMultimapGwtSerializationDependencies.h"

@protocol ComGoogleCommonCollectMultimap;
@protocol JavaLangIterable;
@protocol JavaUtilCollection;
@protocol JavaUtilIterator;
@protocol JavaUtilMap;
@protocol JavaUtilSet;
@protocol JavaUtilSpliterator;

/*!
 @brief Implementation of <code>Multimap</code> that does not allow duplicate key-value entries and that
  returns collections whose iterators follow the ordering in which the data was added to the
  multimap.
 <p>The collections returned by <code>keySet</code>, <code>keys</code>, and <code>asMap</code> iterate through
  the keys in the order they were first added to the multimap. Similarly, <code>get</code>, <code>removeAll</code>
 , and <code>replaceValues</code> return collections that iterate through the values in the
  order they were added. The collections generated by <code>entries</code> and <code>values</code> iterate
  across the key-value mappings in the order they were added to the multimap. 
 <p>The iteration ordering of the collections generated by <code>keySet</code>, <code>keys</code>, and 
 <code>asMap</code> has a few subtleties. As long as the set of keys remains unchanged, adding or
  removing mappings does not affect the key iteration order. However, if you remove all values
  associated with a key and then add the key back to the multimap, that key will come last in the
  key iteration order. 
 <p>The multimap does not store duplicate key-value pairs. Adding a new key-value pair equal to an
  existing key-value pair has no effect. 
 <p>Keys and values may be null. All optional multimap methods are supported, and all returned
  views are modifiable. 
 <p>This class is not threadsafe when any concurrent operations update the multimap. Concurrent
  read operations will work correctly. To allow concurrent update operations, wrap your multimap
  with a call to <code>Multimaps.synchronizedSetMultimap</code>.
  
 <p>See the Guava User Guide article on <a href="https://github.com/google/guava/wiki/NewCollectionTypesExplained#multimap">
  <code>Multimap</code>
 </a>.
 @author Jared Levy
 @author Louis Wasserman
 @since 2.0
 */
@interface ComGoogleCommonCollectLinkedHashMultimap : ComGoogleCommonCollectLinkedHashMultimapGwtSerializationDependencies {
 @public
  jint valueSetCapacity_;
}
@property (readonly, class) jdouble VALUE_SET_LOAD_FACTOR NS_SWIFT_NAME(VALUE_SET_LOAD_FACTOR);

+ (jdouble)VALUE_SET_LOAD_FACTOR;

#pragma mark Public

- (void)clear;

/*!
 @brief Creates a new, empty <code>LinkedHashMultimap</code> with the default initial capacities.
 */
+ (ComGoogleCommonCollectLinkedHashMultimap *)create;

/*!
 @brief Constructs an empty <code>LinkedHashMultimap</code> with enough capacity to hold the specified
  numbers of keys and values without rehashing.
 @param expectedKeys the expected number of distinct keys
 @param expectedValuesPerKey the expected average number of values per key
 @throw IllegalArgumentExceptionif <code>expectedKeys</code> or <code>expectedValuesPerKey</code> is
      negative
 */
+ (ComGoogleCommonCollectLinkedHashMultimap *)createWithInt:(jint)expectedKeys
                                                    withInt:(jint)expectedValuesPerKey;

/*!
 @brief Constructs a <code>LinkedHashMultimap</code> with the same mappings as the specified multimap.If a
  key-value mapping appears multiple times in the input multimap, it only appears once in the
  constructed multimap.
 The new multimap has the same <code>Multimap.entries()</code> iteration order
  as the input multimap, except for excluding duplicate mappings.
 @param multimap the multimap whose contents are copied to this multimap
 */
+ (ComGoogleCommonCollectLinkedHashMultimap *)createWithComGoogleCommonCollectMultimap:(id<ComGoogleCommonCollectMultimap>)multimap;

/*!
 @brief Returns a set of all key-value pairs.Changes to the returned set will update the underlying
  multimap, and vice versa.
 The entries set does not support the <code>add</code> or <code>addAll</code>
  operations. 
 <p>The iterator generated by the returned set traverses the entries in the order they were
  added to the multimap. 
 <p>Each entry is an immutable snapshot of a key-value mapping in the multimap, taken at the
  time the entry is returned by a method call to the collection or its iterator.
 */
- (id<JavaUtilSet>)entries;

/*!
 @brief Returns a view collection of all <i>distinct</i> keys contained in this multimap.Note that the
  key set contains a key if and only if this multimap maps that key to at least one value.
 <p>The iterator generated by the returned set traverses the keys in the order they were first
  added to the multimap. 
 <p>Changes to the returned set will update the underlying multimap, and vice versa. However, 
 <i>adding</i> to the returned set is not possible.
 */
- (id<JavaUtilSet>)keySet;

/*!
 @brief <p>If <code>values</code> is not empty and the multimap already contains a mapping for <code>key</code>,
  the <code>keySet()</code> ordering is unchanged.
 However, the provided values always come last in the 
 <code>entries()</code> and <code>values()</code> iteration orderings.
 */
- (id<JavaUtilSet>)replaceValuesWithId:(id)key
                  withJavaLangIterable:(id<JavaLangIterable>)values;

/*!
 @brief Returns a collection of all values in the multimap.Changes to the returned collection will
  update the underlying multimap, and vice versa.
 <p>The iterator generated by the returned collection traverses the values in the order they
  were added to the multimap.
 */
- (id<JavaUtilCollection>)values;

#pragma mark Package-Private

/*!
 @brief <p>Creates an empty <code>LinkedHashSet</code> for a collection of values for one key.
 @return a new <code>LinkedHashSet</code> containing a collection of values for one key
 */
- (id<JavaUtilSet>)createCollection;

/*!
 @brief <p>Creates a decorated insertion-ordered set that also keeps track of the order in which
  key-value pairs are added to the multimap.
 @param key key to associate with values in the collection
 @return a new decorated set containing a collection of values for one key
 */
- (id<JavaUtilCollection>)createCollectionWithId:(id)key;

- (id<JavaUtilIterator>)entryIterator;

- (id<JavaUtilSpliterator>)entrySpliterator;

- (id<JavaUtilIterator>)valueIterator;

- (id<JavaUtilSpliterator>)valueSpliterator;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivateWithJavaUtilMap:(id<JavaUtilMap>)arg0 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectLinkedHashMultimap)

inline jdouble ComGoogleCommonCollectLinkedHashMultimap_get_VALUE_SET_LOAD_FACTOR(void);
#define ComGoogleCommonCollectLinkedHashMultimap_VALUE_SET_LOAD_FACTOR 1.0
J2OBJC_STATIC_FIELD_CONSTANT(ComGoogleCommonCollectLinkedHashMultimap, VALUE_SET_LOAD_FACTOR, jdouble)

FOUNDATION_EXPORT ComGoogleCommonCollectLinkedHashMultimap *ComGoogleCommonCollectLinkedHashMultimap_create(void);

FOUNDATION_EXPORT ComGoogleCommonCollectLinkedHashMultimap *ComGoogleCommonCollectLinkedHashMultimap_createWithInt_withInt_(jint expectedKeys, jint expectedValuesPerKey);

FOUNDATION_EXPORT ComGoogleCommonCollectLinkedHashMultimap *ComGoogleCommonCollectLinkedHashMultimap_createWithComGoogleCommonCollectMultimap_(id<ComGoogleCommonCollectMultimap> multimap);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectLinkedHashMultimap)

#endif

#if !defined (ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink_) && (INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap || defined(INCLUDE_ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink))
#define ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink_

@protocol ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink < JavaObject >

- (id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)getPredecessorInValueSet;

- (id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)getSuccessorInValueSet;

- (void)setPredecessorInValueSetWithComGoogleCommonCollectLinkedHashMultimap_ValueSetLink:(id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)entry_;

- (void)setSuccessorInValueSetWithComGoogleCommonCollectLinkedHashMultimap_ValueSetLink:(id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)entry_;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink)

#endif

#if !defined (ComGoogleCommonCollectLinkedHashMultimap_ValueEntry_) && (INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap || defined(INCLUDE_ComGoogleCommonCollectLinkedHashMultimap_ValueEntry))
#define ComGoogleCommonCollectLinkedHashMultimap_ValueEntry_

#define RESTRICT_ComGoogleCommonCollectImmutableEntry 1
#define INCLUDE_ComGoogleCommonCollectImmutableEntry 1
#include "com/google/common/collect/ImmutableEntry.h"

@protocol ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink;

/*!
 @brief LinkedHashMultimap entries are in no less than three coexisting linked lists: a bucket in the
  hash table for a <code>Set<V></code> associated with a key, the linked list of insertion-ordered
  entries in that <code>Set<V></code>, and the linked list of entries in the LinkedHashMultimap as a
  whole.
 */
@interface ComGoogleCommonCollectLinkedHashMultimap_ValueEntry : ComGoogleCommonCollectImmutableEntry < ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink > {
 @public
  jint smearedValueHash_;
  ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *nextInValueBucket_;
  id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink> predecessorInValueSet_;
  id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink> successorInValueSet_;
  ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *predecessorInMultimap_;
  ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *successorInMultimap_;
}

#pragma mark Public

- (ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *)getPredecessorInMultimap;

- (id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)getPredecessorInValueSet;

- (ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *)getSuccessorInMultimap;

- (id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)getSuccessorInValueSet;

- (void)setPredecessorInMultimapWithComGoogleCommonCollectLinkedHashMultimap_ValueEntry:(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *)multimapPredecessor;

- (void)setPredecessorInValueSetWithComGoogleCommonCollectLinkedHashMultimap_ValueSetLink:(id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)entry_;

- (void)setSuccessorInMultimapWithComGoogleCommonCollectLinkedHashMultimap_ValueEntry:(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *)multimapSuccessor;

- (void)setSuccessorInValueSetWithComGoogleCommonCollectLinkedHashMultimap_ValueSetLink:(id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)entry_;

#pragma mark Package-Private

- (instancetype __nonnull)initWithId:(id)key
                              withId:(id)value
                             withInt:(jint)smearedValueHash
withComGoogleCommonCollectLinkedHashMultimap_ValueEntry:(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry * __nullable)nextInValueBucket;

- (jboolean)matchesValueWithId:(id __nullable)v
                       withInt:(jint)smearedVHash;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivateWithId:(id)arg0
                                            withId:(id)arg1 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry, nextInValueBucket_, ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry, predecessorInValueSet_, id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry, successorInValueSet_, id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry, predecessorInMultimap_, ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *)
J2OBJC_FIELD_SETTER(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry, successorInMultimap_, ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *)

FOUNDATION_EXPORT void ComGoogleCommonCollectLinkedHashMultimap_ValueEntry_initWithId_withId_withInt_withComGoogleCommonCollectLinkedHashMultimap_ValueEntry_(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *self, id key, id value, jint smearedValueHash, ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *nextInValueBucket);

FOUNDATION_EXPORT ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *new_ComGoogleCommonCollectLinkedHashMultimap_ValueEntry_initWithId_withId_withInt_withComGoogleCommonCollectLinkedHashMultimap_ValueEntry_(id key, id value, jint smearedValueHash, ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *nextInValueBucket) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *create_ComGoogleCommonCollectLinkedHashMultimap_ValueEntry_initWithId_withId_withInt_withComGoogleCommonCollectLinkedHashMultimap_ValueEntry_(id key, id value, jint smearedValueHash, ComGoogleCommonCollectLinkedHashMultimap_ValueEntry *nextInValueBucket);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectLinkedHashMultimap_ValueEntry)

#endif

#if !defined (ComGoogleCommonCollectLinkedHashMultimap_ValueSet_) && (INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap || defined(INCLUDE_ComGoogleCommonCollectLinkedHashMultimap_ValueSet))
#define ComGoogleCommonCollectLinkedHashMultimap_ValueSet_

#define RESTRICT_ComGoogleCommonCollectSets 1
#define INCLUDE_ComGoogleCommonCollectSets_ImprovedAbstractSet 1
#include "com/google/common/collect/Sets.h"

@class ComGoogleCommonCollectLinkedHashMultimap;
@class IOSObjectArray;
@protocol ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink;
@protocol JavaUtilFunctionConsumer;
@protocol JavaUtilIterator;

@interface ComGoogleCommonCollectLinkedHashMultimap_ValueSet : ComGoogleCommonCollectSets_ImprovedAbstractSet < ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink > {
 @public
  IOSObjectArray *hashTable_;
}

#pragma mark Public

- (jboolean)addWithId:(id)value;

- (void)clear;

- (jboolean)containsWithId:(id __nullable)o;

- (void)forEachWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)action;

- (id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)getPredecessorInValueSet;

- (id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)getSuccessorInValueSet;

- (id<JavaUtilIterator>)iterator;

- (jboolean)removeWithId:(id __nullable)o;

- (void)setPredecessorInValueSetWithComGoogleCommonCollectLinkedHashMultimap_ValueSetLink:(id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)entry_;

- (void)setSuccessorInValueSetWithComGoogleCommonCollectLinkedHashMultimap_ValueSetLink:(id<ComGoogleCommonCollectLinkedHashMultimap_ValueSetLink>)entry_;

- (jint)size;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectLinkedHashMultimap:(ComGoogleCommonCollectLinkedHashMultimap *)outer$
                                                                    withId:(id)key
                                                                   withInt:(jint)expectedValues;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectLinkedHashMultimap_ValueSet)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectLinkedHashMultimap_ValueSet, hashTable_, IOSObjectArray *)

FOUNDATION_EXPORT void ComGoogleCommonCollectLinkedHashMultimap_ValueSet_initWithComGoogleCommonCollectLinkedHashMultimap_withId_withInt_(ComGoogleCommonCollectLinkedHashMultimap_ValueSet *self, ComGoogleCommonCollectLinkedHashMultimap *outer$, id key, jint expectedValues);

FOUNDATION_EXPORT ComGoogleCommonCollectLinkedHashMultimap_ValueSet *new_ComGoogleCommonCollectLinkedHashMultimap_ValueSet_initWithComGoogleCommonCollectLinkedHashMultimap_withId_withInt_(ComGoogleCommonCollectLinkedHashMultimap *outer$, id key, jint expectedValues) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectLinkedHashMultimap_ValueSet *create_ComGoogleCommonCollectLinkedHashMultimap_ValueSet_initWithComGoogleCommonCollectLinkedHashMultimap_withId_withInt_(ComGoogleCommonCollectLinkedHashMultimap *outer$, id key, jint expectedValues);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectLinkedHashMultimap_ValueSet)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectLinkedHashMultimap")

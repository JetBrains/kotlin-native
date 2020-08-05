//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/CompactLinkedHashMap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashMap")
#ifdef RESTRICT_ComGoogleCommonCollectCompactLinkedHashMap
#define INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashMap 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashMap 1
#endif
#undef RESTRICT_ComGoogleCommonCollectCompactLinkedHashMap

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectCompactLinkedHashMap_) && (INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashMap || defined(INCLUDE_ComGoogleCommonCollectCompactLinkedHashMap))
#define ComGoogleCommonCollectCompactLinkedHashMap_

#define RESTRICT_ComGoogleCommonCollectCompactHashMap 1
#define INCLUDE_ComGoogleCommonCollectCompactHashMap 1
#include "com/google/common/collect/CompactHashMap.h"

@class IOSLongArray;
@protocol JavaUtilCollection;
@protocol JavaUtilFunctionBiConsumer;
@protocol JavaUtilSet;

/*!
 @brief CompactLinkedHashMap is an implementation of a Map with insertion or LRU iteration order,
  maintained with a doubly linked list through the entries.All optional operations (put and
  remove) are supported.
 Null keys and values are supported. 
 <p><code>containsKey(k)</code>, <code>put(k, v)</code> and <code>remove(k)</code> are all (expected and
  amortized) constant time operations. Expected in the hashtable sense (depends on the hash
  function doing a good job of distributing the elements to the buckets to a distribution not far
  from uniform), and amortized since some operations can trigger a hash table resize. 
 <p>As compared with <code>java.util.LinkedHashMap</code>, this structure places significantly reduced
  load on the garbage collector by only using a constant number of internal objects. 
 <p>This class should not be assumed to be universally superior to <code>java.util.LinkedHashMap</code>
 . Generally speaking, this class reduces object allocation and memory
  consumption at the price of moderately increased constant factors of CPU. Only use this class
  when there is a specific reason to prioritize memory over CPU.
 @author Louis Wasserman
 */
@interface ComGoogleCommonCollectCompactLinkedHashMap : ComGoogleCommonCollectCompactHashMap {
 @public
  /*!
   @brief Contains the link pointers corresponding with the entries, in the range of [0, size()).The
  high 32 bits of each long is the "prev" pointer, whereas the low 32 bits is the "succ" pointer
  (pointing to the next entry in the linked list).
   The pointers in [size(), entries.length) are
  all "null" (UNSET). 
 <p>A node with "prev" pointer equal to <code>ENDPOINT</code> is the first node in the linked list,
  and a node with "next" pointer equal to <code>ENDPOINT</code> is the last node.
   */
  IOSLongArray *links_;
}

#pragma mark Public

- (void)clear;

/*!
 @brief Creates an empty <code>CompactLinkedHashMap</code> instance.
 */
+ (ComGoogleCommonCollectCompactLinkedHashMap *)create;

/*!
 @brief Creates a <code>CompactLinkedHashMap</code> instance, with a high enough "initial capacity"
  that it <i>should</i> hold <code>expectedSize</code> elements without growth.
 @param expectedSize the number of elements you expect to add to the returned set
 @return a new, empty <code>CompactLinkedHashMap</code> with enough capacity to hold <code>expectedSize</code>
  elements without resizing
 @throw IllegalArgumentExceptionif <code>expectedSize</code> is negative
 */
+ (ComGoogleCommonCollectCompactLinkedHashMap *)createWithExpectedSizeWithInt:(jint)expectedSize;

- (void)forEachWithJavaUtilFunctionBiConsumer:(id<JavaUtilFunctionBiConsumer>)action;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivate;

- (instancetype __nonnull)initPackagePrivateWithInt:(jint)expectedSize;

- (instancetype __nonnull)initPackagePrivateWithInt:(jint)expectedSize
                                          withFloat:(jfloat)loadFactor
                                        withBoolean:(jboolean)accessOrder;

- (void)accessEntryWithInt:(jint)index;

- (jint)adjustAfterRemoveWithInt:(jint)indexBeforeRemove
                         withInt:(jint)indexRemoved;

- (id<JavaUtilSet>)createEntrySet;

- (id<JavaUtilSet>)createKeySet;

- (id<JavaUtilCollection>)createValues;

- (jint)firstEntryIndex;

- (jint)getSuccessorWithInt:(jint)entry_;

- (void)init__WithInt:(jint)expectedSize
            withFloat:(jfloat)loadFactor OBJC_METHOD_FAMILY_NONE;

- (void)insertEntryWithInt:(jint)entryIndex
                    withId:(id)key
                    withId:(id)value
                   withInt:(jint)hash_;

- (void)moveLastEntryWithInt:(jint)dstIndex;

- (void)resizeEntriesWithInt:(jint)newCapacity;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivateWithInt:(jint)arg0
                                          withFloat:(jfloat)arg1 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectCompactLinkedHashMap)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectCompactLinkedHashMap, links_, IOSLongArray *)

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *ComGoogleCommonCollectCompactLinkedHashMap_create(void);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *ComGoogleCommonCollectCompactLinkedHashMap_createWithExpectedSizeWithInt_(jint expectedSize);

FOUNDATION_EXPORT void ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivate(ComGoogleCommonCollectCompactLinkedHashMap *self);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *new_ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivate(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *create_ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivate(void);

FOUNDATION_EXPORT void ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivateWithInt_(ComGoogleCommonCollectCompactLinkedHashMap *self, jint expectedSize);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *new_ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivateWithInt_(jint expectedSize) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *create_ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivateWithInt_(jint expectedSize);

FOUNDATION_EXPORT void ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivateWithInt_withFloat_withBoolean_(ComGoogleCommonCollectCompactLinkedHashMap *self, jint expectedSize, jfloat loadFactor, jboolean accessOrder);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *new_ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivateWithInt_withFloat_withBoolean_(jint expectedSize, jfloat loadFactor, jboolean accessOrder) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashMap *create_ComGoogleCommonCollectCompactLinkedHashMap_initPackagePrivateWithInt_withFloat_withBoolean_(jint expectedSize, jfloat loadFactor, jboolean accessOrder);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectCompactLinkedHashMap)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashMap")

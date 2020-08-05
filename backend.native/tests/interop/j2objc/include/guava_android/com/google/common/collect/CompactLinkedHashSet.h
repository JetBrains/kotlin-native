//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/collect/CompactLinkedHashSet.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashSet")
#ifdef RESTRICT_ComGoogleCommonCollectCompactLinkedHashSet
#define INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashSet 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashSet 1
#endif
#undef RESTRICT_ComGoogleCommonCollectCompactLinkedHashSet

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectCompactLinkedHashSet_) && (INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashSet || defined(INCLUDE_ComGoogleCommonCollectCompactLinkedHashSet))
#define ComGoogleCommonCollectCompactLinkedHashSet_

#define RESTRICT_ComGoogleCommonCollectCompactHashSet 1
#define INCLUDE_ComGoogleCommonCollectCompactHashSet 1
#include "com/google/common/collect/CompactHashSet.h"

@class IOSObjectArray;
@protocol JavaUtilCollection;

/*!
 @brief CompactLinkedHashSet is an implementation of a Set, which a predictable iteration order that
  matches the insertion order.All optional operations (adding and removing) are supported.
 All
  elements, including <code>null</code>, are permitted. 
 <p><code>contains(x)</code>, <code>add(x)</code> and <code>remove(x)</code>, are all (expected and amortized)
  constant time operations. Expected in the hashtable sense (depends on the hash function doing a
  good job of distributing the elements to the buckets to a distribution not far from uniform), and
  amortized since some operations can trigger a hash table resize. 
 <p>This implementation consumes significantly less memory than <code>java.util.LinkedHashSet</code> or
  even <code>java.util.HashSet</code>, and places considerably less load on the garbage collector. Like 
 <code>java.util.LinkedHashSet</code>, it offers insertion-order iteration, with identical behavior. 
 <p>This class should not be assumed to be universally superior to <code>java.util.LinkedHashSet</code>
 . Generally speaking, this class reduces object allocation and memory
  consumption at the price of moderately increased constant factors of CPU. Only use this class
  when there is a specific reason to prioritize memory over CPU.
 @author Louis Wasserman
 */
@interface ComGoogleCommonCollectCompactLinkedHashSet : ComGoogleCommonCollectCompactHashSet

#pragma mark Public

- (void)clear;

/*!
 @brief Creates an empty <code>CompactLinkedHashSet</code> instance.
 */
+ (ComGoogleCommonCollectCompactLinkedHashSet *)create;

/*!
 @brief Creates a <i>mutable</i> <code>CompactLinkedHashSet</code> instance containing the elements
  of the given collection in the order returned by the collection's iterator.
 @param collection the elements that the set should contain
 @return a new <code>CompactLinkedHashSet</code> containing those elements (minus duplicates)
 */
+ (ComGoogleCommonCollectCompactLinkedHashSet *)createWithJavaUtilCollection:(id<JavaUtilCollection> __nonnull)collection;

/*!
 @brief Creates a <code>CompactLinkedHashSet</code> instance containing the given elements in
  unspecified order.
 @param elements the elements that the set should contain
 @return a new <code>CompactLinkedHashSet</code> containing those elements (minus duplicates)
 */
+ (ComGoogleCommonCollectCompactLinkedHashSet *)createWithNSObjectArray:(IOSObjectArray * __nonnull)elements;

/*!
 @brief Creates a <code>CompactLinkedHashSet</code> instance, with a high enough "initial capacity"
  that it <i>should</i> hold <code>expectedSize</code> elements without rebuilding internal
  data structures.
 @param expectedSize the number of elements you expect to add to the returned set
 @return a new, empty <code>CompactLinkedHashSet</code> with enough capacity to hold <code>expectedSize</code>
  elements without resizing
 @throw IllegalArgumentExceptionif <code>expectedSize</code> is negative
 */
+ (ComGoogleCommonCollectCompactLinkedHashSet *)createWithExpectedSizeWithInt:(jint)expectedSize;

- (IOSObjectArray *)toArray;

- (IOSObjectArray *)toArrayWithNSObjectArray:(IOSObjectArray * __nonnull)a;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivate;

- (instancetype __nonnull)initPackagePrivateWithInt:(jint)expectedSize;

- (jint)adjustAfterRemoveWithInt:(jint)indexBeforeRemove
                         withInt:(jint)indexRemoved;

- (jint)firstEntryIndex;

- (jint)getSuccessorWithInt:(jint)entryIndex;

- (void)init__WithInt:(jint)expectedSize
            withFloat:(jfloat)loadFactor OBJC_METHOD_FAMILY_NONE;

- (void)insertEntryWithInt:(jint)entryIndex
                    withId:(id __nonnull)object
                   withInt:(jint)hash_;

- (void)moveEntryWithInt:(jint)dstIndex;

- (void)resizeEntriesWithInt:(jint)newCapacity;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectCompactLinkedHashSet)

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *ComGoogleCommonCollectCompactLinkedHashSet_create(void);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *ComGoogleCommonCollectCompactLinkedHashSet_createWithJavaUtilCollection_(id<JavaUtilCollection> collection);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *ComGoogleCommonCollectCompactLinkedHashSet_createWithNSObjectArray_(IOSObjectArray *elements);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *ComGoogleCommonCollectCompactLinkedHashSet_createWithExpectedSizeWithInt_(jint expectedSize);

FOUNDATION_EXPORT void ComGoogleCommonCollectCompactLinkedHashSet_initPackagePrivate(ComGoogleCommonCollectCompactLinkedHashSet *self);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *new_ComGoogleCommonCollectCompactLinkedHashSet_initPackagePrivate(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *create_ComGoogleCommonCollectCompactLinkedHashSet_initPackagePrivate(void);

FOUNDATION_EXPORT void ComGoogleCommonCollectCompactLinkedHashSet_initPackagePrivateWithInt_(ComGoogleCommonCollectCompactLinkedHashSet *self, jint expectedSize);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *new_ComGoogleCommonCollectCompactLinkedHashSet_initPackagePrivateWithInt_(jint expectedSize) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectCompactLinkedHashSet *create_ComGoogleCommonCollectCompactLinkedHashSet_initPackagePrivateWithInt_(jint expectedSize);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectCompactLinkedHashSet)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectCompactLinkedHashSet")

//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/collect/CompactHashSet.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectCompactHashSet")
#ifdef RESTRICT_ComGoogleCommonCollectCompactHashSet
#define INCLUDE_ALL_ComGoogleCommonCollectCompactHashSet 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectCompactHashSet 1
#endif
#undef RESTRICT_ComGoogleCommonCollectCompactHashSet

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectCompactHashSet_) && (INCLUDE_ALL_ComGoogleCommonCollectCompactHashSet || defined(INCLUDE_ComGoogleCommonCollectCompactHashSet))
#define ComGoogleCommonCollectCompactHashSet_

#define RESTRICT_JavaUtilAbstractSet 1
#define INCLUDE_JavaUtilAbstractSet 1
#include "java/util/AbstractSet.h"

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@class IOSObjectArray;
@protocol JavaUtilCollection;
@protocol JavaUtilIterator;

/*!
 @brief CompactHashSet is an implementation of a Set.All optional operations (adding and removing) are
  supported.
 The elements can be any objects. 
 <p><code>contains(x)</code>, <code>add(x)</code> and <code>remove(x)</code>, are all (expected and amortized)
  constant time operations. Expected in the hashtable sense (depends on the hash function doing a
  good job of distributing the elements to the buckets to a distribution not far from uniform), and
  amortized since some operations can trigger a hash table resize. 
 <p>Unlike <code>java.util.HashSet</code>, iteration is only proportional to the actual <code>size()</code>,
  which is optimal, and <i>not</i> the size of the internal hashtable, which could be much larger
  than <code>size()</code>. Furthermore, this structure only depends on a fixed number of arrays; <code>add(x)</code>
  operations <i>do not</i> create objects for the garbage collector to deal with, and for
  every element added, the garbage collector will have to traverse <code>1.5</code> references on
  average, in the marking phase, not <code>5.0</code> as in <code>java.util.HashSet</code>.
  
 <p>If there are no removals, then <code>iteration</code> order is the same as insertion
  order. Any removal invalidates any ordering guarantees. 
 <p>This class should not be assumed to be universally superior to <code>java.util.HashSet</code>.
  Generally speaking, this class reduces object allocation and memory consumption at the price of
  moderately increased constant factors of CPU.  Only use this class when there is a specific
  reason to prioritize memory over CPU.
 @author Dimitris Andreou
 */
@interface ComGoogleCommonCollectCompactHashSet : JavaUtilAbstractSet < JavaIoSerializable > {
 @public
  /*!
   @brief The elements contained in the set, in the range of [0, size()).
   */
  IOSObjectArray *elements_;
  /*!
   @brief The load factor.
   */
  jfloat loadFactor_;
  /*!
   @brief Keeps track of modifications of this set, to make it possible to throw
  ConcurrentModificationException in the iterator.Note that we choose not to make this volatile,
  so we do less of a "best effort" to track such errors, for better performance.
   */
  jint modCount_;
}
@property (readonly, class) jint UNSET NS_SWIFT_NAME(UNSET);

+ (jint)UNSET;

#pragma mark Public

- (jboolean)addWithId:(id __nullable)object;

- (void)clear;

- (jboolean)containsWithId:(id __nullable)object;

/*!
 @brief Creates an empty <code>CompactHashSet</code> instance.
 */
+ (ComGoogleCommonCollectCompactHashSet *)create;

/*!
 @brief Creates a <i>mutable</i> <code>CompactHashSet</code> instance containing the elements of the given
  collection in unspecified order.
 @param collection the elements that the set should contain
 @return a new <code>CompactHashSet</code> containing those elements (minus duplicates)
 */
+ (ComGoogleCommonCollectCompactHashSet *)createWithJavaUtilCollection:(id<JavaUtilCollection> __nonnull)collection;

/*!
 @brief Creates a <i>mutable</i> <code>CompactHashSet</code> instance containing the given elements in
  unspecified order.
 @param elements the elements that the set should contain
 @return a new <code>CompactHashSet</code> containing those elements (minus duplicates)
 */
+ (ComGoogleCommonCollectCompactHashSet *)createWithNSObjectArray:(IOSObjectArray * __nonnull)elements;

/*!
 @brief Creates a <code>CompactHashSet</code> instance, with a high enough "initial capacity" that it 
 <i>should</i> hold <code>expectedSize</code> elements without growth.
 @param expectedSize the number of elements you expect to add to the returned set
 @return a new, empty <code>CompactHashSet</code> with enough capacity to hold <code>expectedSize</code>
      elements without resizing
 @throw IllegalArgumentExceptionif <code>expectedSize</code> is negative
 */
+ (ComGoogleCommonCollectCompactHashSet *)createWithExpectedSizeWithInt:(jint)expectedSize;

- (jboolean)isEmpty;

- (id<JavaUtilIterator>)iterator;

- (jboolean)removeWithId:(id __nullable)object;

- (jint)size;

- (IOSObjectArray *)toArray;

- (IOSObjectArray *)toArrayWithNSObjectArray:(IOSObjectArray * __nonnull)a;

/*!
 @brief Ensures that this <code>CompactHashSet</code> has the smallest representation in memory, given its
  current size.
 */
- (void)trimToSize;

#pragma mark Package-Private

/*!
 @brief Constructs a new empty instance of <code>CompactHashSet</code>.
 */
- (instancetype __nonnull)initPackagePrivate;

/*!
 @brief Constructs a new instance of <code>CompactHashSet</code> with the specified capacity.
 @param expectedSize the initial capacity of this <code>CompactHashSet</code> .
 */
- (instancetype __nonnull)initPackagePrivateWithInt:(jint)expectedSize;

/*!
 @brief Updates the index an iterator is pointing to after a call to remove: returns the index of the
  entry that should be looked at after a removal on indexRemoved, with indexBeforeRemove as the
  index that *was* the next entry that would be looked at.
 */
- (jint)adjustAfterRemoveWithInt:(jint)indexBeforeRemove
                         withInt:(jint)indexRemoved;

- (jint)firstEntryIndex;

- (jint)getSuccessorWithInt:(jint)entryIndex;

/*!
 @brief Pseudoconstructor for serialization support.
 */
- (void)init__WithInt:(jint)expectedSize
            withFloat:(jfloat)loadFactor OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Creates a fresh entry with the specified object at the specified position in the entry arrays.
 */
- (void)insertEntryWithInt:(jint)entryIndex
                    withId:(id __nonnull)object
                   withInt:(jint)hash_;

/*!
 @brief Moves the last entry in the entry array into <code>dstIndex</code>, and nulls out its old position.
 */
- (void)moveEntryWithInt:(jint)dstIndex;

/*!
 @brief Resizes the internal entries array to the specified capacity, which may be greater or less than
  the current capacity.
 */
- (void)resizeEntriesWithInt:(jint)newCapacity;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectCompactHashSet)

J2OBJC_FIELD_SETTER(ComGoogleCommonCollectCompactHashSet, elements_, IOSObjectArray *)

inline jint ComGoogleCommonCollectCompactHashSet_get_UNSET(void);
#define ComGoogleCommonCollectCompactHashSet_UNSET -1
J2OBJC_STATIC_FIELD_CONSTANT(ComGoogleCommonCollectCompactHashSet, UNSET, jint)

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *ComGoogleCommonCollectCompactHashSet_create(void);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *ComGoogleCommonCollectCompactHashSet_createWithJavaUtilCollection_(id<JavaUtilCollection> collection);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *ComGoogleCommonCollectCompactHashSet_createWithNSObjectArray_(IOSObjectArray *elements);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *ComGoogleCommonCollectCompactHashSet_createWithExpectedSizeWithInt_(jint expectedSize);

FOUNDATION_EXPORT void ComGoogleCommonCollectCompactHashSet_initPackagePrivate(ComGoogleCommonCollectCompactHashSet *self);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *new_ComGoogleCommonCollectCompactHashSet_initPackagePrivate(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *create_ComGoogleCommonCollectCompactHashSet_initPackagePrivate(void);

FOUNDATION_EXPORT void ComGoogleCommonCollectCompactHashSet_initPackagePrivateWithInt_(ComGoogleCommonCollectCompactHashSet *self, jint expectedSize);

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *new_ComGoogleCommonCollectCompactHashSet_initPackagePrivateWithInt_(jint expectedSize) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectCompactHashSet *create_ComGoogleCommonCollectCompactHashSet_initPackagePrivateWithInt_(jint expectedSize);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectCompactHashSet)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectCompactHashSet")

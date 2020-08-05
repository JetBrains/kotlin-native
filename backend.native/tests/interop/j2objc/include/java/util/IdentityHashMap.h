//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/util/IdentityHashMap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaUtilIdentityHashMap")
#ifdef RESTRICT_JavaUtilIdentityHashMap
#define INCLUDE_ALL_JavaUtilIdentityHashMap 0
#else
#define INCLUDE_ALL_JavaUtilIdentityHashMap 1
#endif
#undef RESTRICT_JavaUtilIdentityHashMap
#ifdef INCLUDE_JavaUtilIdentityHashMap_EntrySpliterator
#define INCLUDE_JavaUtilIdentityHashMap_IdentityHashMapSpliterator 1
#endif
#ifdef INCLUDE_JavaUtilIdentityHashMap_ValueSpliterator
#define INCLUDE_JavaUtilIdentityHashMap_IdentityHashMapSpliterator 1
#endif
#ifdef INCLUDE_JavaUtilIdentityHashMap_KeySpliterator
#define INCLUDE_JavaUtilIdentityHashMap_IdentityHashMapSpliterator 1
#endif

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaUtilIdentityHashMap_) && (INCLUDE_ALL_JavaUtilIdentityHashMap || defined(INCLUDE_JavaUtilIdentityHashMap))
#define JavaUtilIdentityHashMap_

#define RESTRICT_JavaUtilAbstractMap 1
#define INCLUDE_JavaUtilAbstractMap 1
#include "java/util/AbstractMap.h"

#define RESTRICT_JavaUtilMap 1
#define INCLUDE_JavaUtilMap 1
#include "java/util/Map.h"

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@class IOSObjectArray;
@protocol JavaUtilCollection;
@protocol JavaUtilFunctionBiConsumer;
@protocol JavaUtilFunctionBiFunction;
@protocol JavaUtilSet;

/*!
 @brief This class implements the <tt>Map</tt> interface with a hash table, using
  reference-equality in place of object-equality when comparing keys (and
  values).In other words, in an <tt>IdentityHashMap</tt>, two keys 
 <tt>k1</tt> and <tt>k2</tt> are considered equal if and only if 
 <tt>(k1==k2)</tt>.
 (In normal <tt>Map</tt> implementations (like 
 <tt>HashMap</tt>) two keys <tt>k1</tt> and <tt>k2</tt> are considered equal
  if and only if <tt>(k1==null ? k2==null : k1.equals(k2))</tt>.)
  
 <p><b>This class is <i>not</i> a general-purpose <tt>Map</tt>
  implementation!  While this class implements the <tt>Map</tt> interface, it
  intentionally violates <tt>Map's</tt> general contract, which mandates the
  use of the <tt>equals</tt> method when comparing objects.  This class is
  designed for use only in the rare cases wherein reference-equality
  semantics are required.</b>
  
 <p>A typical use of this class is <i>topology-preserving object graph
  transformations</i>, such as serialization or deep-copying.  To perform such
  a transformation, a program must maintain a "node table" that keeps track
  of all the object references that have already been processed.  The node
  table must not equate distinct objects even if they happen to be equal.
  Another typical use of this class is to maintain <i>proxy objects</i>.  For
  example, a debugging facility might wish to maintain a proxy object for
  each object in the program being debugged. 
 <p>This class provides all of the optional map operations, and permits 
 <tt>null</tt> values and the <tt>null</tt> key.  This class makes no
  guarantees as to the order of the map; in particular, it does not guarantee
  that the order will remain constant over time. 
 <p>This class provides constant-time performance for the basic
  operations (<tt>get</tt> and <tt>put</tt>), assuming the system
  identity hash function (<code>System.identityHashCode(Object)</code>)
  disperses elements properly among the buckets. 
 <p>This class has one tuning parameter (which affects performance but not
  semantics): <i>expected maximum size</i>.  This parameter is the maximum
  number of key-value mappings that the map is expected to hold.  Internally,
  this parameter is used to determine the number of buckets initially
  comprising the hash table.  The precise relationship between the expected
  maximum size and the number of buckets is unspecified. 
 <p>If the size of the map (the number of key-value mappings) sufficiently
  exceeds the expected maximum size, the number of buckets is increased.
  Increasing the number of buckets ("rehashing") may be fairly expensive, so
  it pays to create identity hash maps with a sufficiently large expected
  maximum size.  On the other hand, iteration over collection views requires
  time proportional to the number of buckets in the hash table, so it
  pays not to set the expected maximum size too high if you are especially
  concerned with iteration performance or memory usage. 
 <p><strong>Note that this implementation is not synchronized.</strong>
  If multiple threads access an identity hash map concurrently, and at
  least one of the threads modifies the map structurally, it <i>must</i>
  be synchronized externally.  (A structural modification is any operation
  that adds or deletes one or more mappings; merely changing the value
  associated with a key that an instance already contains is not a
  structural modification.)  This is typically accomplished by
  synchronizing on some object that naturally encapsulates the map.
  If no such object exists, the map should be "wrapped" using the 
 <code>Collections.synchronizedMap</code>
  method.  This is best done at creation time, to prevent accidental
  unsynchronized access to the map:@code

    Map m = Collections.synchronizedMap(new IdentityHashMap(...));
@endcode
  
 <p>The iterators returned by the <tt>iterator</tt> method of the
  collections returned by all of this class's "collection view
  methods" are <i>fail-fast</i>: if the map is structurally modified
  at any time after the iterator is created, in any way except
  through the iterator's own <tt>remove</tt> method, the iterator
  will throw a <code>ConcurrentModificationException</code>.  Thus, in the
  face of concurrent modification, the iterator fails quickly and
  cleanly, rather than risking arbitrary, non-deterministic behavior
  at an undetermined time in the future. 
 <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
  as it is, generally speaking, impossible to make any hard guarantees in the
  presence of unsynchronized concurrent modification.  Fail-fast iterators
  throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
  Therefore, it would be wrong to write a program that depended on this
  exception for its correctness: <i>fail-fast iterators should be used only
  to detect bugs.</i>
  
 <p>Implementation note: This is a simple <i>linear-probe</i> hash table,
  as described for example in texts by Sedgewick and Knuth.  The array
  alternates holding keys and values.  (This has better locality for large
  tables than does using separate arrays.)  For many JRE implementations
  and operation mixes, this class will yield better performance than 
 <code>HashMap</code> (which uses <i>chaining</i> rather than linear-probing). 
 <p>This class is a member of the 
 <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/collections/index.html">
  Java Collections Framework</a>.
 - seealso: System#identityHashCode(Object)
 - seealso: Object#hashCode()
 - seealso: Collection
 - seealso: Map
 - seealso: HashMap
 - seealso: TreeMap
 @author Doug Lea and Josh Bloch
 @since 1.4
 */
@interface JavaUtilIdentityHashMap : JavaUtilAbstractMap < JavaUtilMap, JavaIoSerializable, NSCopying > {
 @public
  /*!
   @brief The table, resized as necessary.Length MUST always be a power of two.
   */
  IOSObjectArray *table_;
  /*!
   @brief The number of key-value mappings contained in this identity hash map.
   */
  jint size_;
  /*!
   @brief The number of modifications, to support fast-fail iterators
   */
  jint modCount_;
}
@property (readonly, class, strong) id NULL_KEY NS_SWIFT_NAME(NULL_KEY);

+ (id)NULL_KEY;

#pragma mark Public

/*!
 @brief Constructs a new, empty identity hash map with a default expected
  maximum size (21).
 */
- (instancetype __nonnull)init;

/*!
 @brief Constructs a new, empty map with the specified expected maximum size.
 Putting more than the expected number of key-value mappings into
  the map may cause the internal data structure to grow, which may be
  somewhat time-consuming.
 @param expectedMaxSize the expected maximum size of the map
 @throw IllegalArgumentExceptionif <tt>expectedMaxSize</tt> is negative
 */
- (instancetype __nonnull)initWithInt:(jint)expectedMaxSize;

/*!
 @brief Constructs a new identity hash map containing the keys-value mappings
  in the specified map.
 @param m the map whose mappings are to be placed into this map
 @throw NullPointerExceptionif the specified map is null
 */
- (instancetype __nonnull)initWithJavaUtilMap:(id<JavaUtilMap>)m;

/*!
 @brief Removes all of the mappings from this map.
 The map will be empty after this call returns.
 */
- (void)clear;

/*!
 @brief Returns a shallow copy of this identity hash map: the keys and values
  themselves are not cloned.
 @return a shallow copy of this map
 */
- (id)java_clone;

/*!
 @brief Tests whether the specified object reference is a key in this identity
  hash map.
 @param key possible key
 @return <code>true</code> if the specified object reference is a key
           in this map
 - seealso: #containsValue(Object)
 */
- (jboolean)containsKeyWithId:(id)key;

/*!
 @brief Tests whether the specified object reference is a value in this identity
  hash map.
 @param value value whose presence in this map is to be tested
 @return <tt>true</tt> if this map maps one or more keys to the
          specified object reference
 - seealso: #containsKey(Object)
 */
- (jboolean)containsValueWithId:(id)value;

/*!
 @brief Returns a <code>Set</code> view of the mappings contained in this map.
 Each element in the returned set is a reference-equality-based 
 <tt>Map.Entry</tt>.  The set is backed by the map, so changes
  to the map are reflected in the set, and vice-versa.  If the
  map is modified while an iteration over the set is in progress,
  the results of the iteration are undefined.  The set supports
  element removal, which removes the corresponding mapping from
  the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
  <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt>
  methods.  It does not support the <tt>add</tt> or 
 <tt>addAll</tt> methods. 
 <p>Like the backing map, the <tt>Map.Entry</tt> objects in the set
  returned by this method define key and value equality as
  reference-equality rather than object-equality.  This affects the
  behavior of the <tt>equals</tt> and <tt>hashCode</tt> methods of these 
 <tt>Map.Entry</tt> objects.  A reference-equality based <tt>Map.Entry
  e</tt> is equal to an object <tt>o</tt> if and only if <tt>o</tt> is a 
 <tt>Map.Entry</tt> and <tt>e.getKey()==o.getKey() &amp;&amp;
  e.getValue()==o.getValue()</tt>.  To accommodate these equals
  semantics, the <tt>hashCode</tt> method returns 
 <tt>System.identityHashCode(e.getKey()) ^
  System.identityHashCode(e.getValue())</tt>.
  
 <p><b>Owing to the reference-equality-based semantics of the 
 <tt>Map.Entry</tt> instances in the set returned by this method,
  it is possible that the symmetry and transitivity requirements of the 
 <code>Object.equals(Object)</code> contract may be violated if any of
  the entries in the set is compared to a normal map entry, or if
  the set returned by this method is compared to a set of normal map
  entries (such as would be returned by a call to this method on a normal
  map).  However, the <tt>Object.equals</tt> contract is guaranteed to
  hold among identity-based map entries, and among sets of such entries. 
 </b>
 @return a set view of the identity-mappings contained in this map
 */
- (id<JavaUtilSet>)entrySet;

/*!
 @brief Compares the specified object with this map for equality.Returns
  <tt>true</tt> if the given object is also a map and the two maps
  represent identical object-reference mappings.
 More formally, this
  map is equal to another map <tt>m</tt> if and only if 
 <tt>this.entrySet().equals(m.entrySet())</tt>.
  
 <p><b>Owing to the reference-equality-based semantics of this map it is
  possible that the symmetry and transitivity requirements of the 
 <tt>Object.equals</tt> contract may be violated if this map is compared
  to a normal map.  However, the <tt>Object.equals</tt> contract is
  guaranteed to hold among <tt>IdentityHashMap</tt> instances.</b>
 @param o object to be compared for equality with this map
 @return <tt>true</tt> if the specified object is equal to this map
 - seealso: Object#equals(Object)
 */
- (jboolean)isEqual:(id)o;

- (void)forEachWithJavaUtilFunctionBiConsumer:(id<JavaUtilFunctionBiConsumer>)action;

/*!
 @brief Returns the value to which the specified key is mapped,
  or <code>null</code> if this map contains no mapping for the key.
 <p>More formally, if this map contains a mapping from a key 
 <code>k</code> to a value <code>v</code> such that <code>(key == k)</code>,
  then this method returns <code>v</code>; otherwise it returns 
 <code>null</code>.  (There can be at most one such mapping.) 
 <p>A return value of <code>null</code> does not <i>necessarily</i>
  indicate that the map contains no mapping for the key; it's also
  possible that the map explicitly maps the key to <code>null</code>.
  The <code>containsKey</code> operation may be used to
  distinguish these two cases.
 - seealso: #put(Object, Object)
 */
- (id)getWithId:(id)key;

/*!
 @brief Returns the hash code value for this map.The hash code of a map is
  defined to be the sum of the hash codes of each entry in the map's 
 <tt>entrySet()</tt> view.
 This ensures that <tt>m1.equals(m2)</tt>
  implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two 
 <tt>IdentityHashMap</tt> instances <tt>m1</tt> and <tt>m2</tt>, as
  required by the general contract of <code>Object.hashCode</code>.
  
 <p><b>Owing to the reference-equality-based semantics of the 
 <tt>Map.Entry</tt> instances in the set returned by this map's 
 <tt>entrySet</tt> method, it is possible that the contractual
  requirement of <tt>Object.hashCode</tt> mentioned in the previous
  paragraph will be violated if one of the two objects being compared is
  an <tt>IdentityHashMap</tt> instance and the other is a normal map.</b>
 @return the hash code value for this map
 - seealso: Object#equals(Object)
 - seealso: #equals(Object)
 */
- (NSUInteger)hash;

/*!
 @brief Returns <tt>true</tt> if this identity hash map contains no key-value
  mappings.
 @return <tt>true</tt> if this identity hash map contains no key-value
          mappings
 */
- (jboolean)isEmpty;

/*!
 @brief Returns an identity-based set view of the keys contained in this map.
 The set is backed by the map, so changes to the map are reflected in
  the set, and vice-versa.  If the map is modified while an iteration
  over the set is in progress, the results of the iteration are
  undefined.  The set supports element removal, which removes the
  corresponding mapping from the map, via the <tt>Iterator.remove</tt>,
  <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and 
 <tt>clear</tt> methods.  It does not support the <tt>add</tt> or 
 <tt>addAll</tt> methods. 
 <p><b>While the object returned by this method implements the 
 <tt>Set</tt> interface, it does <i>not</i> obey <tt>Set's</tt> general
  contract.  Like its backing map, the set returned by this method
  defines element equality as reference-equality rather than
  object-equality.  This affects the behavior of its <tt>contains</tt>,
  <tt>remove</tt>, <tt>containsAll</tt>, <tt>equals</tt>, and 
 <tt>hashCode</tt> methods.</b>
  
 <p><b>The <tt>equals</tt> method of the returned set returns <tt>true</tt>
  only if the specified object is a set containing exactly the same
  object references as the returned set.  The symmetry and transitivity
  requirements of the <tt>Object.equals</tt> contract may be violated if
  the set returned by this method is compared to a normal set.  However, the 
 <tt>Object.equals</tt> contract is guaranteed to hold among sets
  returned by this method.</b>
  
 <p>The <tt>hashCode</tt> method of the returned set returns the sum of the 
 <i>identity hashcodes</i> of the elements in the set, rather than
  the sum of their hashcodes.  This is mandated by the change in the
  semantics of the <tt>equals</tt> method, in order to enforce the
  general contract of the <tt>Object.hashCode</tt> method among sets
  returned by this method.
 @return an identity-based set view of the keys contained in this map
 - seealso: Object#equals(Object)
 - seealso: System#identityHashCode(Object)
 */
- (id<JavaUtilSet>)keySet;

/*!
 @brief Associates the specified value with the specified key in this identity
  hash map.If the map previously contained a mapping for the key, the
  old value is replaced.
 @param key the key with which the specified value is to be associated
 @param value the value to be associated with the specified key
 @return the previous value associated with <tt>key</tt>, or
          <tt>null</tt> if there was no mapping for <tt>key</tt>.
          (A <tt>null</tt> return can also indicate that the map
          previously associated <tt>null</tt> with <tt>key</tt>.)
 - seealso: Object#equals(Object)
 - seealso: #get(Object)
 - seealso: #containsKey(Object)
 */
- (id)putWithId:(id)key
         withId:(id)value;

/*!
 @brief Copies all of the mappings from the specified map to this map.
 These mappings will replace any mappings that this map had for
  any of the keys currently in the specified map.
 @param m mappings to be stored in this map
 @throw NullPointerExceptionif the specified map is null
 */
- (void)putAllWithJavaUtilMap:(id<JavaUtilMap>)m;

/*!
 @brief Removes the mapping for this key from this map if present.
 @param key key whose mapping is to be removed from the map
 @return the previous value associated with <tt>key</tt>, or
          <tt>null</tt> if there was no mapping for <tt>key</tt>.
          (A <tt>null</tt> return can also indicate that the map
          previously associated <tt>null</tt> with <tt>key</tt>.)
 */
- (id)removeWithId:(id)key;

- (void)replaceAllWithJavaUtilFunctionBiFunction:(id<JavaUtilFunctionBiFunction>)function;

/*!
 @brief Returns the number of key-value mappings in this identity hash map.
 @return the number of key-value mappings in this map
 */
- (jint)size;

/*!
 @brief Returns a <code>Collection</code> view of the values contained in this map.
 The collection is backed by the map, so changes to the map are
  reflected in the collection, and vice-versa.  If the map is
  modified while an iteration over the collection is in progress,
  the results of the iteration are undefined.  The collection
  supports element removal, which removes the corresponding
  mapping from the map, via the <tt>Iterator.remove</tt>,
  <tt>Collection.remove</tt>, <tt>removeAll</tt>,
  <tt>retainAll</tt> and <tt>clear</tt> methods.  It does not
  support the <tt>add</tt> or <tt>addAll</tt> methods. 
 <p><b>While the object returned by this method implements the 
 <tt>Collection</tt> interface, it does <i>not</i> obey 
 <tt>Collection's</tt> general contract.  Like its backing map,
  the collection returned by this method defines element equality as
  reference-equality rather than object-equality.  This affects the
  behavior of its <tt>contains</tt>, <tt>remove</tt> and 
 <tt>containsAll</tt> methods.</b>
 */
- (id<JavaUtilCollection>)values;

#pragma mark Package-Private

/*!
 @brief Returns internal representation of null key back to caller as null.
 */
+ (id)unmaskNullWithId:(id)key;

@end

J2OBJC_STATIC_INIT(JavaUtilIdentityHashMap)

J2OBJC_FIELD_SETTER(JavaUtilIdentityHashMap, table_, IOSObjectArray *)

/*!
 @brief Value representing null keys inside tables.
 */
inline id JavaUtilIdentityHashMap_get_NULL_KEY(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT id JavaUtilIdentityHashMap_NULL_KEY;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilIdentityHashMap, NULL_KEY, id)

FOUNDATION_EXPORT id JavaUtilIdentityHashMap_unmaskNullWithId_(id key);

FOUNDATION_EXPORT void JavaUtilIdentityHashMap_init(JavaUtilIdentityHashMap *self);

FOUNDATION_EXPORT JavaUtilIdentityHashMap *new_JavaUtilIdentityHashMap_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilIdentityHashMap *create_JavaUtilIdentityHashMap_init(void);

FOUNDATION_EXPORT void JavaUtilIdentityHashMap_initWithInt_(JavaUtilIdentityHashMap *self, jint expectedMaxSize);

FOUNDATION_EXPORT JavaUtilIdentityHashMap *new_JavaUtilIdentityHashMap_initWithInt_(jint expectedMaxSize) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilIdentityHashMap *create_JavaUtilIdentityHashMap_initWithInt_(jint expectedMaxSize);

FOUNDATION_EXPORT void JavaUtilIdentityHashMap_initWithJavaUtilMap_(JavaUtilIdentityHashMap *self, id<JavaUtilMap> m);

FOUNDATION_EXPORT JavaUtilIdentityHashMap *new_JavaUtilIdentityHashMap_initWithJavaUtilMap_(id<JavaUtilMap> m) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilIdentityHashMap *create_JavaUtilIdentityHashMap_initWithJavaUtilMap_(id<JavaUtilMap> m);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilIdentityHashMap)

#endif

#if !defined (JavaUtilIdentityHashMap_IdentityHashMapSpliterator_) && (INCLUDE_ALL_JavaUtilIdentityHashMap || defined(INCLUDE_JavaUtilIdentityHashMap_IdentityHashMapSpliterator))
#define JavaUtilIdentityHashMap_IdentityHashMapSpliterator_

@class JavaUtilIdentityHashMap;

/*!
 @brief Similar form as array-based Spliterators, but skips blank elements,
  and guestimates size as decreasing by half per split.
 */
@interface JavaUtilIdentityHashMap_IdentityHashMapSpliterator : NSObject {
 @public
  JavaUtilIdentityHashMap *map_;
  jint index_;
  jint fence_;
  jint est_;
  jint expectedModCount_;
}

#pragma mark Public

- (jlong)estimateSize;

#pragma mark Package-Private

- (instancetype __nonnull)initWithJavaUtilIdentityHashMap:(JavaUtilIdentityHashMap *)map
                                                  withInt:(jint)origin
                                                  withInt:(jint)fence
                                                  withInt:(jint)est
                                                  withInt:(jint)expectedModCount;

- (jint)getFence;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaUtilIdentityHashMap_IdentityHashMapSpliterator)

J2OBJC_FIELD_SETTER(JavaUtilIdentityHashMap_IdentityHashMapSpliterator, map_, JavaUtilIdentityHashMap *)

FOUNDATION_EXPORT void JavaUtilIdentityHashMap_IdentityHashMapSpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap_IdentityHashMapSpliterator *self, JavaUtilIdentityHashMap *map, jint origin, jint fence, jint est, jint expectedModCount);

FOUNDATION_EXPORT JavaUtilIdentityHashMap_IdentityHashMapSpliterator *new_JavaUtilIdentityHashMap_IdentityHashMapSpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *map, jint origin, jint fence, jint est, jint expectedModCount) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilIdentityHashMap_IdentityHashMapSpliterator *create_JavaUtilIdentityHashMap_IdentityHashMapSpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *map, jint origin, jint fence, jint est, jint expectedModCount);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilIdentityHashMap_IdentityHashMapSpliterator)

#endif

#if !defined (JavaUtilIdentityHashMap_KeySpliterator_) && (INCLUDE_ALL_JavaUtilIdentityHashMap || defined(INCLUDE_JavaUtilIdentityHashMap_KeySpliterator))
#define JavaUtilIdentityHashMap_KeySpliterator_

#define RESTRICT_JavaUtilSpliterator 1
#define INCLUDE_JavaUtilSpliterator 1
#include "java/util/Spliterator.h"

@class JavaUtilIdentityHashMap;
@protocol JavaUtilComparator;
@protocol JavaUtilFunctionConsumer;

@interface JavaUtilIdentityHashMap_KeySpliterator : JavaUtilIdentityHashMap_IdentityHashMapSpliterator < JavaUtilSpliterator >

#pragma mark Public

- (jint)characteristics;

- (void)forEachRemainingWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)action;

- (jboolean)tryAdvanceWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)action;

- (JavaUtilIdentityHashMap_KeySpliterator *)trySplit;

#pragma mark Package-Private

- (instancetype __nonnull)initWithJavaUtilIdentityHashMap:(JavaUtilIdentityHashMap *)map
                                                  withInt:(jint)origin
                                                  withInt:(jint)fence
                                                  withInt:(jint)est
                                                  withInt:(jint)expectedModCount;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaUtilIdentityHashMap_KeySpliterator)

FOUNDATION_EXPORT void JavaUtilIdentityHashMap_KeySpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap_KeySpliterator *self, JavaUtilIdentityHashMap *map, jint origin, jint fence, jint est, jint expectedModCount);

FOUNDATION_EXPORT JavaUtilIdentityHashMap_KeySpliterator *new_JavaUtilIdentityHashMap_KeySpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *map, jint origin, jint fence, jint est, jint expectedModCount) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilIdentityHashMap_KeySpliterator *create_JavaUtilIdentityHashMap_KeySpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *map, jint origin, jint fence, jint est, jint expectedModCount);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilIdentityHashMap_KeySpliterator)

#endif

#if !defined (JavaUtilIdentityHashMap_ValueSpliterator_) && (INCLUDE_ALL_JavaUtilIdentityHashMap || defined(INCLUDE_JavaUtilIdentityHashMap_ValueSpliterator))
#define JavaUtilIdentityHashMap_ValueSpliterator_

#define RESTRICT_JavaUtilSpliterator 1
#define INCLUDE_JavaUtilSpliterator 1
#include "java/util/Spliterator.h"

@class JavaUtilIdentityHashMap;
@protocol JavaUtilComparator;
@protocol JavaUtilFunctionConsumer;

@interface JavaUtilIdentityHashMap_ValueSpliterator : JavaUtilIdentityHashMap_IdentityHashMapSpliterator < JavaUtilSpliterator >

#pragma mark Public

- (jint)characteristics;

- (void)forEachRemainingWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)action;

- (jboolean)tryAdvanceWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)action;

- (JavaUtilIdentityHashMap_ValueSpliterator *)trySplit;

#pragma mark Package-Private

- (instancetype __nonnull)initWithJavaUtilIdentityHashMap:(JavaUtilIdentityHashMap *)m
                                                  withInt:(jint)origin
                                                  withInt:(jint)fence
                                                  withInt:(jint)est
                                                  withInt:(jint)expectedModCount;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaUtilIdentityHashMap_ValueSpliterator)

FOUNDATION_EXPORT void JavaUtilIdentityHashMap_ValueSpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap_ValueSpliterator *self, JavaUtilIdentityHashMap *m, jint origin, jint fence, jint est, jint expectedModCount);

FOUNDATION_EXPORT JavaUtilIdentityHashMap_ValueSpliterator *new_JavaUtilIdentityHashMap_ValueSpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *m, jint origin, jint fence, jint est, jint expectedModCount) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilIdentityHashMap_ValueSpliterator *create_JavaUtilIdentityHashMap_ValueSpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *m, jint origin, jint fence, jint est, jint expectedModCount);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilIdentityHashMap_ValueSpliterator)

#endif

#if !defined (JavaUtilIdentityHashMap_EntrySpliterator_) && (INCLUDE_ALL_JavaUtilIdentityHashMap || defined(INCLUDE_JavaUtilIdentityHashMap_EntrySpliterator))
#define JavaUtilIdentityHashMap_EntrySpliterator_

#define RESTRICT_JavaUtilSpliterator 1
#define INCLUDE_JavaUtilSpliterator 1
#include "java/util/Spliterator.h"

@class JavaUtilIdentityHashMap;
@protocol JavaUtilComparator;
@protocol JavaUtilFunctionConsumer;

@interface JavaUtilIdentityHashMap_EntrySpliterator : JavaUtilIdentityHashMap_IdentityHashMapSpliterator < JavaUtilSpliterator >

#pragma mark Public

- (jint)characteristics;

- (void)forEachRemainingWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)action;

- (jboolean)tryAdvanceWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)action;

- (JavaUtilIdentityHashMap_EntrySpliterator *)trySplit;

#pragma mark Package-Private

- (instancetype __nonnull)initWithJavaUtilIdentityHashMap:(JavaUtilIdentityHashMap *)m
                                                  withInt:(jint)origin
                                                  withInt:(jint)fence
                                                  withInt:(jint)est
                                                  withInt:(jint)expectedModCount;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaUtilIdentityHashMap_EntrySpliterator)

FOUNDATION_EXPORT void JavaUtilIdentityHashMap_EntrySpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap_EntrySpliterator *self, JavaUtilIdentityHashMap *m, jint origin, jint fence, jint est, jint expectedModCount);

FOUNDATION_EXPORT JavaUtilIdentityHashMap_EntrySpliterator *new_JavaUtilIdentityHashMap_EntrySpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *m, jint origin, jint fence, jint est, jint expectedModCount) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilIdentityHashMap_EntrySpliterator *create_JavaUtilIdentityHashMap_EntrySpliterator_initWithJavaUtilIdentityHashMap_withInt_withInt_withInt_withInt_(JavaUtilIdentityHashMap *m, jint origin, jint fence, jint est, jint expectedModCount);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilIdentityHashMap_EntrySpliterator)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaUtilIdentityHashMap")

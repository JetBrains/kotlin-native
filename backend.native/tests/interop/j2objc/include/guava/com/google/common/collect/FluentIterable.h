//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/FluentIterable.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectFluentIterable")
#ifdef RESTRICT_ComGoogleCommonCollectFluentIterable
#define INCLUDE_ALL_ComGoogleCommonCollectFluentIterable 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectFluentIterable 1
#endif
#undef RESTRICT_ComGoogleCommonCollectFluentIterable

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectFluentIterable_) && (INCLUDE_ALL_ComGoogleCommonCollectFluentIterable || defined(INCLUDE_ComGoogleCommonCollectFluentIterable))
#define ComGoogleCommonCollectFluentIterable_

#define RESTRICT_JavaLangIterable 1
#define INCLUDE_JavaLangIterable 1
#include "java/lang/Iterable.h"

@class ComGoogleCommonBaseJoiner;
@class ComGoogleCommonBaseOptional;
@class ComGoogleCommonCollectImmutableList;
@class ComGoogleCommonCollectImmutableListMultimap;
@class ComGoogleCommonCollectImmutableMap;
@class ComGoogleCommonCollectImmutableMultiset;
@class ComGoogleCommonCollectImmutableSet;
@class ComGoogleCommonCollectImmutableSortedSet;
@class IOSClass;
@class IOSObjectArray;
@protocol ComGoogleCommonBaseFunction;
@protocol ComGoogleCommonBasePredicate;
@protocol JavaUtilCollection;
@protocol JavaUtilComparator;
@protocol JavaUtilFunctionConsumer;
@protocol JavaUtilSpliterator;
@protocol JavaUtilStreamStream;

/*!
 @brief A discouraged (but not deprecated) precursor to Java's superior <code>Stream</code> library.
 <p>The following types of methods are provided: 
 <ul>
    <li>chaining methods which return a new <code>FluentIterable</code> based in some way on the
        contents of the current one (for example <code>transform</code>)
    <li>element extraction methods which facilitate the retrieval of certain elements (for example
        <code>last</code>)
    <li>query methods which answer questions about the <code>FluentIterable</code>'s contents (for
        example <code>anyMatch</code>)
    <li>conversion methods which copy the <code>FluentIterable</code>'s contents into a new collection
        or array (for example <code>toList</code>)
  </ul>
  
 <p>Several lesser-used features are currently available only as static methods on the <code>Iterables</code>
  class. 
 <p><a name="streams"></a>
  
 <h3>Comparison to streams</h3>
  
 <p><code>Stream</code> is similar to this class, but generally more powerful, and certainly more
  standard. Key differences include: 
 <ul>
    <li>A stream is <i>single-use</i>; it becomes invalid as soon as any "terminal operation" such
        as <code>findFirst()</code> or <code>iterator()</code> is invoked. (Even though <code>Stream</code>
        contains all the right method <i>signatures</i> to implement <code>Iterable</code>, it does not
        actually do so, to avoid implying repeat-iterability.) <code>FluentIterable</code>, on the other
        hand, is multiple-use, and does implement <code>Iterable</code>.
    <li>Streams offer many features not found here, including <code>min/max</code>, <code>distinct</code>,
        <code>reduce</code>, <code>sorted</code>, the very powerful <code>collect</code>, and built-in support for
        parallelizing stream operations.   
 <li><code>FluentIterable</code> contains several features not available on <code>Stream</code>, which are
        noted in the method descriptions below.   
 <li>Streams include primitive-specialized variants such as <code>IntStream</code>, the use of which
        is strongly recommended.   
 <li>Streams are standard Java, not requiring a third-party dependency. 
 </ul>
  
 <h3>Example</h3>
  
 <p>Here is an example that accepts a list from a database call, filters it based on a predicate,
  transforms it by invoking <code>toString()</code> on each element, and returns the first 10 elements
  as a <code>List</code>:
  
 @code
 ImmutableList<String> results =
      FluentIterable.from(database.getClientList())
          .filter(Client::isActiveInLastMonth)
          .transform(Object::toString)
          .limit(10)
          .toList(); 
 
@endcode
  The approximate stream equivalent is: 
 @code
 List<String> results =
      database.getClientList()
          .stream()
          .filter(Client::isActiveInLastMonth)
          .map(Object::toString)
          .limit(10)
          .collect(Collectors.toList()); 
 
@endcode
 @author Marcin Mikosik
 @since 12.0
 */
@interface ComGoogleCommonCollectFluentIterable : NSObject < JavaLangIterable >

#pragma mark Public

/*!
 @brief Returns <code>true</code> if every element in this fluent iterable satisfies the predicate.If this
  fluent iterable is empty, <code>true</code> is returned.
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.allMatch</code> (same).
 */
- (jboolean)allMatchWithComGoogleCommonBasePredicate:(id<ComGoogleCommonBasePredicate>)predicate;

/*!
 @brief Returns <code>true</code> if any element in this fluent iterable satisfies the predicate.
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.anyMatch</code> (same).
 */
- (jboolean)anyMatchWithComGoogleCommonBasePredicate:(id<ComGoogleCommonBasePredicate>)predicate;

/*!
 @brief Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
  followed by <code>elements</code>.
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.concat(thisStream, Stream.of(elements))</code>.
 @since 18.0
 */
- (ComGoogleCommonCollectFluentIterable *)appendWithNSObjectArray:(IOSObjectArray *)elements;

/*!
 @brief Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
  followed by those of <code>other</code>.The iterators are not polled until necessary.
 <p>The returned iterable's <code>Iterator</code> supports <code>remove()</code> when the corresponding 
 <code>Iterator</code> supports it. 
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.concat</code>.
 @since 18.0
 */
- (ComGoogleCommonCollectFluentIterable *)appendWithJavaLangIterable:(id<JavaLangIterable>)other;

/*!
 @brief Returns a fluent iterable that combines several iterables.The returned iterable has an
  iterator that traverses the elements of each iterable in <code>inputs</code>.
 The input iterators
  are not polled until necessary. 
 <p>The returned iterable's iterator supports <code>remove()</code> when the corresponding input
  iterator supports it. 
 <p><b><code>Stream</code> equivalent:</b> to concatenate an arbitrary number of streams, use <code>Stream.of(stream1, stream2, ...).flatMap(s -> s)</code>
 . If the sources are iterables, use <code>Stream.of(iter1, iter2, ...).flatMap(Streams::stream)</code>
 .
 @throw NullPointerExceptionif any of the provided iterables is <code>null</code>
 @since 20.0
 */
+ (ComGoogleCommonCollectFluentIterable *)concatWithJavaLangIterableArray:(IOSObjectArray *)inputs;

/*!
 @brief Returns a fluent iterable that combines several iterables.The returned iterable has an
  iterator that traverses the elements of each iterable in <code>inputs</code>.
 The input iterators
  are not polled until necessary. 
 <p>The returned iterable's iterator supports <code>remove()</code> when the corresponding input
  iterator supports it. The methods of the returned iterable may throw <code>NullPointerException</code>
  if any of the input iterators is <code>null</code>.
  
 <p><b><code>Stream</code> equivalent:</b> <code>streamOfStreams.flatMap(s -> s)</code> or <code>streamOfIterables.flatMap(Streams::stream)</code>
 . (See <code>Streams.stream</code>.)
 @since 20.0
 */
+ (ComGoogleCommonCollectFluentIterable *)concatWithJavaLangIterable:(id<JavaLangIterable>)inputs;

/*!
 @brief Returns a fluent iterable that combines two iterables.The returned iterable has an iterator
  that traverses the elements in <code>a</code>, followed by the elements in <code>b</code>.
 The source
  iterators are not polled until necessary. 
 <p>The returned iterable's iterator supports <code>remove()</code> when the corresponding input
  iterator supports it. 
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.concat</code>.
 @since 20.0
 */
+ (ComGoogleCommonCollectFluentIterable *)concatWithJavaLangIterable:(id<JavaLangIterable>)a
                                                withJavaLangIterable:(id<JavaLangIterable>)b;

/*!
 @brief Returns a fluent iterable that combines three iterables.The returned iterable has an iterator
  that traverses the elements in <code>a</code>, followed by the elements in <code>b</code>, followed by
  the elements in <code>c</code>.
 The source iterators are not polled until necessary. 
 <p>The returned iterable's iterator supports <code>remove()</code> when the corresponding input
  iterator supports it. 
 <p><b><code>Stream</code> equivalent:</b> use nested calls to <code>Stream.concat</code>, or see the
  advice in <code>concat(Iterable...)</code>.
 @since 20.0
 */
+ (ComGoogleCommonCollectFluentIterable *)concatWithJavaLangIterable:(id<JavaLangIterable>)a
                                                withJavaLangIterable:(id<JavaLangIterable>)b
                                                withJavaLangIterable:(id<JavaLangIterable>)c;

/*!
 @brief Returns a fluent iterable that combines four iterables.The returned iterable has an iterator
  that traverses the elements in <code>a</code>, followed by the elements in <code>b</code>, followed by
  the elements in <code>c</code>, followed by the elements in <code>d</code>.
 The source iterators are not
  polled until necessary. 
 <p>The returned iterable's iterator supports <code>remove()</code> when the corresponding input
  iterator supports it. 
 <p><b><code>Stream</code> equivalent:</b> use nested calls to <code>Stream.concat</code>, or see the
  advice in <code>concat(Iterable...)</code>.
 @since 20.0
 */
+ (ComGoogleCommonCollectFluentIterable *)concatWithJavaLangIterable:(id<JavaLangIterable>)a
                                                withJavaLangIterable:(id<JavaLangIterable>)b
                                                withJavaLangIterable:(id<JavaLangIterable>)c
                                                withJavaLangIterable:(id<JavaLangIterable>)d;

/*!
 @brief Returns <code>true</code> if this fluent iterable contains any object for which <code>equals(target)</code>
  is true.
 <p><b><code>Stream</code> equivalent:</b> <code>stream.anyMatch(Predicate.isEqual(target))</code>.
 */
- (jboolean)containsWithId:(id __nullable)target;

/*!
 @brief Copies all the elements from this fluent iterable to <code>collection</code>.This is equivalent to
  calling <code>Iterables.addAll(collection, this)</code>.
 <p><b><code>Stream</code> equivalent:</b> <code>stream.forEachOrdered(collection::add)</code> or <code>stream.forEach(collection::add)</code>
 .
 @param collection the collection to copy elements to
 @return <code>collection</code>, for convenience
 @since 14.0
 */
- (id<JavaUtilCollection>)copyIntoWithJavaUtilCollection:(id<JavaUtilCollection>)collection OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Returns a fluent iterable whose <code>Iterator</code> cycles indefinitely over the elements of this
  fluent iterable.
 <p>That iterator supports <code>remove()</code> if <code>iterable.iterator()</code> does. After <code>remove()</code>
  is called, subsequent cycles omit the removed element, which is no longer in this
  fluent iterable. The iterator's <code>hasNext()</code> method returns <code>true</code> until this fluent
  iterable is empty. 
 <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
  should use an explicit <code>break</code> or be certain that you will eventually remove all the
  elements. 
 <p><b><code>Stream</code> equivalent:</b> if the source iterable has only a single element <code>e</code>
 , use <code>Stream.generate(() -> e)</code>. Otherwise, collect your stream into a collection and
  use <code>Stream.generate(() -> collection).flatMap(Collection::stream)</code>.
 */
- (ComGoogleCommonCollectFluentIterable *)cycle;

/*!
 @brief Returns the elements from this fluent iterable that are instances of class <code>type</code>.
 <p><b><code>Stream</code> equivalent:</b> <code>stream.filter(type::isInstance).map(type::cast)</code>.
  This does perform a little more work than necessary, so another option is to insert an
  unchecked cast at some later point: 
 @code

  @@SuppressWarnings("unchecked") // safe because of ::isInstance check
  ImmutableList<NewType> result =
      (ImmutableList) stream.filter(NewType.class::isInstance).collect(toImmutableList());
   
@endcode
 */
- (ComGoogleCommonCollectFluentIterable *)filterWithIOSClass:(IOSClass *)type;

/*!
 @brief Returns the elements from this fluent iterable that satisfy a predicate.The resulting fluent
  iterable's iterator does not support <code>remove()</code>.
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.filter</code> (same).
 */
- (ComGoogleCommonCollectFluentIterable *)filterWithComGoogleCommonBasePredicate:(id<ComGoogleCommonBasePredicate>)predicate;

/*!
 @brief Returns an <code>Optional</code> containing the first element in this fluent iterable.If the
  iterable is empty, <code>Optional.absent()</code> is returned.
 <p><b><code>Stream</code> equivalent:</b> if the goal is to obtain any element, <code>Stream.findAny</code>
 ; if it must specifically be the <i>first</i> element, <code>Stream#findFirst</code>.
 @throw NullPointerExceptionif the first element is null; if this is a possibility, use <code>iterator().next()</code>
  or <code>Iterables.getFirst</code> instead.
 */
- (ComGoogleCommonBaseOptional *)first;

/*!
 @brief Returns an <code>Optional</code> containing the first element in this fluent iterable that satisfies
  the given predicate, if such an element exists.
 <p><b>Warning:</b> avoid using a <code>predicate</code> that matches <code>null</code>. If <code>null</code>
  is matched in this fluent iterable, a <code>NullPointerException</code> will be thrown. 
 <p><b><code>Stream</code> equivalent:</b> <code>stream.filter(predicate).findFirst()</code>.
 */
- (ComGoogleCommonBaseOptional *)firstMatchWithComGoogleCommonBasePredicate:(id<ComGoogleCommonBasePredicate>)predicate;

/*!
 @brief Returns a fluent iterable containing <code>elements</code> in the specified order.
 <p>The returned iterable is an unmodifiable view of the input array. 
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.of(T...)</code>
 .
 @since 20.0 (since 18.0 as an overload of <code>of</code>)
 */
+ (ComGoogleCommonCollectFluentIterable *)fromWithNSObjectArray:(IOSObjectArray *)elements;

/*!
 @brief Construct a fluent iterable from another fluent iterable.This is obviously never necessary,
  but is intended to help call out cases where one migration from <code>Iterable</code> to <code>FluentIterable</code>
  has obviated the need to explicitly convert to a <code>FluentIterable</code>.
 */
+ (ComGoogleCommonCollectFluentIterable *)fromWithComGoogleCommonCollectFluentIterable:(ComGoogleCommonCollectFluentIterable *)iterable __attribute__((deprecated));

/*!
 @brief Returns a fluent iterable that wraps <code>iterable</code>, or <code>iterable</code> itself if it is
  already a <code>FluentIterable</code>.
 <p><b><code>Stream</code> equivalent:</b> <code>Collection.stream</code> if <code>iterable</code> is a <code>Collection</code>
 ; <code>Streams.stream(Iterable)</code> otherwise.
 */
+ (ComGoogleCommonCollectFluentIterable *)fromWithJavaLangIterable:(id<JavaLangIterable>)iterable;

/*!
 @brief Returns the element at the specified position in this fluent iterable.
 <p><b><code>Stream</code> equivalent:</b> <code>stream.skip(position).findFirst().get()</code> (but note
  that this throws different exception types, and throws an exception if <code>null</code> would be
  returned).
 @param position position of the element to return
 @return the element at the specified position in this fluent iterable
 @throw IndexOutOfBoundsExceptionif <code>position</code> is negative or greater than or equal to
      the size of this fluent iterable
 */
- (id)getWithInt:(jint)position;

/*!
 @brief Creates an index <code>ImmutableListMultimap</code> that contains the results of applying a
  specified function to each item in this <code>FluentIterable</code> of values.Each element of this
  iterable will be stored as a value in the resulting multimap, yielding a multimap with the same
  size as this iterable.
 The key used to store that value in the multimap will be the result of
  calling the function on that value. The resulting multimap is created as an immutable snapshot.
  In the returned multimap, keys appear in the order they are first encountered, and the values
  corresponding to each key appear in the same order as they are encountered. 
 <p><b><code>Stream</code> equivalent:</b> <code>stream.collect(Collectors.groupingBy(keyFunction))</code>
  behaves similarly, but returns a mutable <code>Map<K, List<E>></code> instead, and may not preserve
  the order of entries).
 @param keyFunction the function used to produce the key for each value
 @throw NullPointerExceptionif any element of this iterable is <code>null</code>, or if <code>keyFunction</code>
  produces <code>null</code> for any key
 @since 14.0
 */
- (ComGoogleCommonCollectImmutableListMultimap *)indexWithComGoogleCommonBaseFunction:(id<ComGoogleCommonBaseFunction>)keyFunction;

/*!
 @brief Determines whether this fluent iterable is empty.
 <p><b><code>Stream</code> equivalent:</b> <code>!stream.findAny().isPresent()</code>.
 */
- (jboolean)isEmpty;

/*!
 @brief Returns a <code>String</code> containing all of the elements of this fluent iterable joined with 
 <code>joiner</code>.
 <p><b><code>Stream</code> equivalent:</b> <code>joiner.join(stream.iterator())</code>, or, if you are not
  using any optional <code>Joiner</code> features, <code>stream.collect(Collectors.joining(delimiter)</code>
 .
 @since 18.0
 */
- (NSString *)joinWithComGoogleCommonBaseJoiner:(ComGoogleCommonBaseJoiner *)joiner;

/*!
 @brief Returns an <code>Optional</code> containing the last element in this fluent iterable.If the
  iterable is empty, <code>Optional.absent()</code> is returned.
 If the underlying <code>iterable</code> is
  a <code>List</code> with <code>java.util.RandomAccess</code> support, then this operation is guaranteed
  to be <code>O(1)</code>.
  
 <p><b><code>Stream</code> equivalent:</b> <code>stream.reduce((a, b) -> b)</code>.
 @throw NullPointerExceptionif the last element is null; if this is a possibility, use <code>Iterables.getLast</code>
  instead.
 */
- (ComGoogleCommonBaseOptional *)last;

/*!
 @brief Creates a fluent iterable with the first <code>size</code> elements of this fluent iterable.If this
  fluent iterable does not contain that many elements, the returned fluent iterable will have the
  same behavior as this fluent iterable.
 The returned fluent iterable's iterator supports <code>remove()</code>
  if this fluent iterable's iterator does. 
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.limit</code> (same).
 @param maxSize the maximum number of elements in the returned fluent iterable
 @throw IllegalArgumentExceptionif <code>size</code> is negative
 */
- (ComGoogleCommonCollectFluentIterable *)limitWithInt:(jint)maxSize;

/*!
 @brief Returns a fluent iterable containing no elements.
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.empty</code>.
 @since 20.0
 */
+ (ComGoogleCommonCollectFluentIterable *)of;

/*!
 @brief Returns a fluent iterable containing the specified elements in order.
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.of(T...)</code>
 .
 @since 20.0
 */
+ (ComGoogleCommonCollectFluentIterable *)ofWithId:(id)element
                                 withNSObjectArray:(IOSObjectArray *)elements;

/*!
 @brief Returns the number of elements in this fluent iterable.
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.count</code>.
 */
- (jint)size;

/*!
 @brief Returns a view of this fluent iterable that skips its first <code>numberToSkip</code> elements.If
  this fluent iterable contains fewer than <code>numberToSkip</code> elements, the returned fluent
  iterable skips all of its elements.
 <p>Modifications to this fluent iterable before a call to <code>iterator()</code> are reflected in
  the returned fluent iterable. That is, the its iterator skips the first <code>numberToSkip</code>
  elements that exist when the iterator is created, not when <code>skip()</code> is called. 
 <p>The returned fluent iterable's iterator supports <code>remove()</code> if the <code>Iterator</code> of
  this fluent iterable supports it. Note that it is <i>not</i> possible to delete the last
  skipped element by immediately calling <code>remove()</code> on the returned fluent iterable's
  iterator, as the <code>Iterator</code> contract states that a call to <code>* remove()</code> before a
  call to <code>next()</code> will throw an <code>IllegalStateException</code>.
  
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.skip</code> (same).
 */
- (ComGoogleCommonCollectFluentIterable *)skipWithInt:(jint)numberToSkip;

/*!
 @brief Returns a stream of this fluent iterable's contents (similar to calling <code>Collection.stream</code>
  on a collection).
 <p><b>Note:</b> the earlier in the chain you can switch to <code>Stream</code> usage (ideally not
  going through <code>FluentIterable</code> at all), the more performant and idiomatic your code will
  be. This method is a transitional aid, to be used only when really necessary.
 @since 21.0
 */
- (id<JavaUtilStreamStream>)stream;

/*!
 @brief Returns an array containing all of the elements from this fluent iterable in iteration order.
 <p><b><code>Stream</code> equivalent:</b> if an object array is acceptable, use <code>stream.toArray()</code>
 ; if <code>type</code> is a class literal such as <code>MyType.class</code>, use <code>stream.toArray(MyType[]::new)</code>
 . Otherwise use <code>stream.toArray( len -> (E[])
  Array.newInstance(type, len))</code>
 .
 @param type the type of the elements
 @return a newly-allocated array into which all the elements of this fluent iterable have been
      copied
 */
- (IOSObjectArray *)toArrayWithIOSClass:(IOSClass *)type;

/*!
 @brief Returns an <code>ImmutableList</code> containing all of the elements from this fluent iterable in
  proper sequence.
 <p><b><code>Stream</code> equivalent:</b> pass <code>ImmutableList.toImmutableList</code> to <code>stream.collect()</code>
 .
 @throw NullPointerExceptionif any element is <code>null</code>
 @since 14.0 (since 12.0 as <code>toImmutableList()</code>).
 */
- (ComGoogleCommonCollectImmutableList *)toList;

/*!
 @brief Returns an immutable map whose keys are the distinct elements of this <code>FluentIterable</code>
  and whose value for each key was computed by <code>valueFunction</code>.The map's iteration order
  is the order of the first appearance of each key in this iterable.
 <p>When there are multiple instances of a key in this iterable, it is unspecified whether 
 <code>valueFunction</code> will be applied to more than one instance of that key and, if it is,
  which result will be mapped to that key in the returned map. 
 <p><b><code>Stream</code> equivalent:</b> <code>stream.collect(ImmutableMap.toImmutableMap(k -> k,
  valueFunction))</code>
 .
 @throw NullPointerExceptionif any element of this iterable is <code>null</code>, or if <code>valueFunction</code>
  produces <code>null</code> for any key
 @since 14.0
 */
- (ComGoogleCommonCollectImmutableMap *)toMapWithComGoogleCommonBaseFunction:(id<ComGoogleCommonBaseFunction>)valueFunction;

/*!
 @brief Returns an <code>ImmutableMultiset</code> containing all of the elements from this fluent iterable.
 <p><b><code>Stream</code> equivalent:</b> pass <code>ImmutableMultiset.toImmutableMultiset</code> to 
 <code>stream.collect()</code>.
 @throw NullPointerExceptionif any element is null
 @since 19.0
 */
- (ComGoogleCommonCollectImmutableMultiset *)toMultiset;

/*!
 @brief Returns an <code>ImmutableSet</code> containing all of the elements from this fluent iterable with
  duplicates removed.
 <p><b><code>Stream</code> equivalent:</b> pass <code>ImmutableSet.toImmutableSet</code> to <code>stream.collect()</code>
 .
 @throw NullPointerExceptionif any element is <code>null</code>
 @since 14.0 (since 12.0 as <code>toImmutableSet()</code>).
 */
- (ComGoogleCommonCollectImmutableSet *)toSet;

/*!
 @brief Returns an <code>ImmutableList</code> containing all of the elements from this <code>FluentIterable</code>
  in the order specified by <code>comparator</code>.To produce an <code>ImmutableList</code>
  sorted by its natural ordering, use <code>toSortedList(Ordering.natural())</code>.
 <p><b><code>Stream</code> equivalent:</b> pass <code>ImmutableList.toImmutableList</code> to <code>stream.sorted(comparator).collect()</code>
 .
 @param comparator the function by which to sort list elements
 @throw NullPointerExceptionif any element of this iterable is <code>null</code>
 @since 14.0 (since 13.0 as <code>toSortedImmutableList()</code>).
 */
- (ComGoogleCommonCollectImmutableList *)toSortedListWithJavaUtilComparator:(id<JavaUtilComparator>)comparator;

/*!
 @brief Returns an <code>ImmutableSortedSet</code> containing all of the elements from this <code>FluentIterable</code>
  in the order specified by <code>comparator</code>, with duplicates (determined by 
 <code>comparator.compare(x, y) == 0</code>) removed.To produce an <code>ImmutableSortedSet</code> sorted
  by its natural ordering, use <code>toSortedSet(Ordering.natural())</code>.
 <p><b><code>Stream</code> equivalent:</b> pass <code>ImmutableSortedSet.toImmutableSortedSet</code> to 
 <code>stream.collect()</code>.
 @param comparator the function by which to sort set elements
 @throw NullPointerExceptionif any element of this iterable is <code>null</code>
 @since 14.0 (since 12.0 as <code>toImmutableSortedSet()</code>).
 */
- (ComGoogleCommonCollectImmutableSortedSet *)toSortedSetWithJavaUtilComparator:(id<JavaUtilComparator>)comparator;

/*!
 @brief Returns a string representation of this fluent iterable, with the format <code>[e1, e2, ...,
  en]</code>
 .
 <p><b><code>Stream</code> equivalent:</b> <code>stream.collect(Collectors.joining(", ", "[", "]"))</code>
  or (less efficiently) <code>stream.collect(Collectors.toList()).toString()</code>.
 */
- (NSString *)description;

/*!
 @brief Returns a fluent iterable that applies <code>function</code> to each element of this fluent
  iterable.
 <p>The returned fluent iterable's iterator supports <code>remove()</code> if this iterable's
  iterator does. After a successful <code>remove()</code> call, this fluent iterable no longer
  contains the corresponding element. 
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.map</code>.
 */
- (ComGoogleCommonCollectFluentIterable *)transformWithComGoogleCommonBaseFunction:(id<ComGoogleCommonBaseFunction>)function;

/*!
 @brief Applies <code>function</code> to each element of this fluent iterable and returns a fluent iterable
  with the concatenated combination of results.
 <code>function</code> returns an Iterable of results. 
 <p>The returned fluent iterable's iterator supports <code>remove()</code> if this function-returned
  iterables' iterator does. After a successful <code>remove()</code> call, the returned fluent
  iterable no longer contains the corresponding element. 
 <p><b><code>Stream</code> equivalent:</b> <code>Stream.flatMap</code> (using a function that produces
  streams, not iterables).
 @since 13.0 (required <code>Function<E, Iterable<T>></code> until 14.0)
 */
- (ComGoogleCommonCollectFluentIterable *)transformAndConcatWithComGoogleCommonBaseFunction:(id<ComGoogleCommonBaseFunction>)function;

/*!
 @brief Returns a map with the contents of this <code>FluentIterable</code> as its <code>values</code>, indexed
  by keys derived from those values.In other words, each input value produces an entry in the
  map whose key is the result of applying <code>keyFunction</code> to that value.
 These entries appear
  in the same order as they appeared in this fluent iterable. Example usage: 
 @code
 Color red = new Color("red", 255, 0, 0);
  ...
  FluentIterable<Color> allColors = FluentIterable.from(ImmutableSet.of(red, green, blue));
  Map<String, Color> colorForName = allColors.uniqueIndex(toStringFunction());
  assertThat(colorForName).containsEntry("red", red); 
 
@endcode
  
 <p>If your index may associate multiple values with each key, use <code>index</code>
 .
  
 <p><b><code>Stream</code> equivalent:</b> <code>stream.collect(ImmutableMap.toImmutableMap(keyFunction, v -> v))</code>
 .
 @param keyFunction the function used to produce the key for each value
 @return a map mapping the result of evaluating the function <code>keyFunction</code> on each value
      in this fluent iterable to that value
 @throw IllegalArgumentExceptionif <code>keyFunction</code> produces the same key for more than one
      value in this fluent iterable
 @throw NullPointerExceptionif any element of this iterable is <code>null</code>, or if <code>keyFunction</code>
  produces <code>null</code> for any key
 @since 14.0
 */
- (ComGoogleCommonCollectImmutableMap *)uniqueIndexWithComGoogleCommonBaseFunction:(id<ComGoogleCommonBaseFunction>)keyFunction;

#pragma mark Protected

/*!
 @brief Constructor for use by subclasses.
 */
- (instancetype __nonnull)init;

#pragma mark Package-Private

- (instancetype __nonnull)initWithJavaLangIterable:(id<JavaLangIterable>)iterable;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectFluentIterable)

FOUNDATION_EXPORT void ComGoogleCommonCollectFluentIterable_init(ComGoogleCommonCollectFluentIterable *self);

FOUNDATION_EXPORT void ComGoogleCommonCollectFluentIterable_initWithJavaLangIterable_(ComGoogleCommonCollectFluentIterable *self, id<JavaLangIterable> iterable);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_fromWithJavaLangIterable_(id<JavaLangIterable> iterable);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_fromWithNSObjectArray_(IOSObjectArray *elements);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_fromWithComGoogleCommonCollectFluentIterable_(ComGoogleCommonCollectFluentIterable *iterable);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_concatWithJavaLangIterable_withJavaLangIterable_(id<JavaLangIterable> a, id<JavaLangIterable> b);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_concatWithJavaLangIterable_withJavaLangIterable_withJavaLangIterable_(id<JavaLangIterable> a, id<JavaLangIterable> b, id<JavaLangIterable> c);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_concatWithJavaLangIterable_withJavaLangIterable_withJavaLangIterable_withJavaLangIterable_(id<JavaLangIterable> a, id<JavaLangIterable> b, id<JavaLangIterable> c, id<JavaLangIterable> d);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_concatWithJavaLangIterableArray_(IOSObjectArray *inputs);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_concatWithJavaLangIterable_(id<JavaLangIterable> inputs);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_of(void);

FOUNDATION_EXPORT ComGoogleCommonCollectFluentIterable *ComGoogleCommonCollectFluentIterable_ofWithId_withNSObjectArray_(id element, IOSObjectArray *elements);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectFluentIterable)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectFluentIterable")

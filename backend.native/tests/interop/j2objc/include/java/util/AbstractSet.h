//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/util/AbstractSet.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaUtilAbstractSet")
#ifdef RESTRICT_JavaUtilAbstractSet
#define INCLUDE_ALL_JavaUtilAbstractSet 0
#else
#define INCLUDE_ALL_JavaUtilAbstractSet 1
#endif
#undef RESTRICT_JavaUtilAbstractSet

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaUtilAbstractSet_) && (INCLUDE_ALL_JavaUtilAbstractSet || defined(INCLUDE_JavaUtilAbstractSet))
#define JavaUtilAbstractSet_

#define RESTRICT_JavaUtilAbstractCollection 1
#define INCLUDE_JavaUtilAbstractCollection 1
#include "java/util/AbstractCollection.h"

#define RESTRICT_JavaUtilSet 1
#define INCLUDE_JavaUtilSet 1
#include "java/util/Set.h"

@protocol JavaUtilCollection;
@protocol JavaUtilSpliterator;

/*!
 @brief This class provides a skeletal implementation of the <tt>Set</tt>
  interface to minimize the effort required to implement this
  interface.
 <p>
  The process of implementing a set by extending this class is identical
  to that of implementing a Collection by extending AbstractCollection,
  except that all of the methods and constructors in subclasses of this
  class must obey the additional constraints imposed by the <tt>Set</tt>
  interface (for instance, the add method must not permit addition of
  multiple instances of an object to a set).<p>
  Note that this class does not override any of the implementations from the 
 <tt>AbstractCollection</tt> class.  It merely adds implementations
  for <tt>equals</tt> and <tt>hashCode</tt>.<p>
  This class is a member of the 
 <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/collections/index.html">
  Java Collections Framework</a>.
 @author Josh Bloch
 @author Neal Gafter
 - seealso: Collection
 - seealso: AbstractCollection
 - seealso: Set
 @since 1.2
 */
@interface JavaUtilAbstractSet : JavaUtilAbstractCollection < JavaUtilSet >

#pragma mark Public

/*!
 @brief Compares the specified object with this set for equality.Returns
  <tt>true</tt> if the given object is also a set, the two sets have
  the same size, and every member of the given set is contained in
  this set.
 This ensures that the <tt>equals</tt> method works
  properly across different implementations of the <tt>Set</tt>
  interface.<p>
  This implementation first checks if the specified object is this
  set; if so it returns <tt>true</tt>.  Then, it checks if the
  specified object is a set whose size is identical to the size of
  this set; if not, it returns false.  If so, it returns 
 <tt>containsAll((Collection) o)</tt>.
 @param o object to be compared for equality with this set
 @return <tt>true</tt> if the specified object is equal to this set
 */
- (jboolean)isEqual:(id)o;

/*!
 @brief Returns the hash code value for this set.The hash code of a set is
  defined to be the sum of the hash codes of the elements in the set,
  where the hash code of a <tt>null</tt> element is defined to be zero.
 This ensures that <tt>s1.equals(s2)</tt> implies that 
 <tt>s1.hashCode()==s2.hashCode()</tt> for any two sets <tt>s1</tt>
  and <tt>s2</tt>, as required by the general contract of 
 <code>Object.hashCode</code>.
  
 <p>This implementation iterates over the set, calling the 
 <tt>hashCode</tt> method on each element in the set, and adding up
  the results.
 @return the hash code value for this set
 - seealso: Object#equals(Object)
 - seealso: Set#equals(Object)
 */
- (NSUInteger)hash;

/*!
 @brief Removes from this set all of its elements that are contained in the
  specified collection (optional operation).If the specified
  collection is also a set, this operation effectively modifies this
  set so that its value is the <i>asymmetric set difference</i> of
  the two sets.
 <p>This implementation determines which is the smaller of this set
  and the specified collection, by invoking the <tt>size</tt>
  method on each.  If this set has fewer elements, then the
  implementation iterates over this set, checking each element
  returned by the iterator in turn to see if it is contained in
  the specified collection.  If it is so contained, it is removed
  from this set with the iterator's <tt>remove</tt> method.  If
  the specified collection has fewer elements, then the
  implementation iterates over the specified collection, removing
  from this set each element returned by the iterator, using this
  set's <tt>remove</tt> method. 
 <p>Note that this implementation will throw an 
 <tt>UnsupportedOperationException</tt> if the iterator returned by the 
 <tt>iterator</tt> method does not implement the <tt>remove</tt> method.
 @param c collection containing elements to be removed from this set
 @return <tt>true</tt> if this set changed as a result of the call
 @throw UnsupportedOperationExceptionif the <tt>removeAll</tt> operation
          is not supported by this set
 @throw ClassCastExceptionif the class of an element of this set
          is incompatible with the specified collection
  (<a href="Collection.html#optional-restrictions">optional</a>)
 @throw NullPointerExceptionif this set contains a null element and the
          specified collection does not permit null elements
  (<a href="Collection.html#optional-restrictions">optional</a>),
          or if the specified collection is null
 - seealso: #remove(Object)
 - seealso: #contains(Object)
 */
- (jboolean)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)c;

#pragma mark Protected

/*!
 @brief Sole constructor.
 (For invocation by subclass constructors, typically
  implicit.)
 */
- (instancetype __nonnull)init;

#pragma mark Package-Private

@end

J2OBJC_EMPTY_STATIC_INIT(JavaUtilAbstractSet)

FOUNDATION_EXPORT void JavaUtilAbstractSet_init(JavaUtilAbstractSet *self);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilAbstractSet)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaUtilAbstractSet")
